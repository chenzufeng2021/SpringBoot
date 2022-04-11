---
typora-copy-images-to: SpringBootNotesPictures
---

# SpringCache 实现原理

SpringCache 使用 Spring AOP 来实现，当我们在 Configuration 类打上 @EnableCaching 注释时，该标注通过 `ImportSelector` 机制启动 `AbstractAdvisorAutoProxyCreator` 的一个实例，该实例本身是一个 Ordered BeanPostProcessor，<font color=red>BeanPostProcessor 的作用是在 bean 创建、初始化的前后对其进行一些额外的操作（包括创建代理）</font>。因此可以认为当在 Configuration 类打上 @EnableCaching 注释时，做的第一件事情就是启用 Spring AOP 机制。

SpringCache 使用 Spring AOP 面向切面编程的机制来实现，当我们在 Configuration 类打上 @EnableCaching 注释时，除了启动 Spring AOP 机制外，引入的另一个类 `ProxyCachingConfiguration` 就是 SpringCache 具体实现相关 bean 的配置类。

其代码如下：

```java
package org.springframework.cache.annotation;

@Configuration
@Role(2)
public class ProxyCachingConfiguration extends AbstractCachingConfiguration {
    public ProxyCachingConfiguration() {
    }

    @Bean(
        name = {"org.springframework.cache.config.internalCacheAdvisor"}
    )
    @Role(2)
    public BeanFactoryCacheOperationSourceAdvisor cacheAdvisor() {
        BeanFactoryCacheOperationSourceAdvisor advisor = new BeanFactoryCacheOperationSourceAdvisor();
        advisor.setCacheOperationSource(this.cacheOperationSource());
        advisor.setAdvice(this.cacheInterceptor());
        if (this.enableCaching != null) {
            advisor.setOrder((Integer)this.enableCaching.getNumber("order"));
        }

        return advisor;
    }

    @Bean
    @Role(2)
    public CacheOperationSource cacheOperationSource() {
        // 获取定义在类和方法上的 SpringCache 相关的注解，并将其转换为对应的 CacheOperation 属性
        return new AnnotationCacheOperationSource();
    }

    @Bean
    @Role(2)
    public CacheInterceptor cacheInterceptor() {
        CacheInterceptor interceptor = new CacheInterceptor();
        interceptor.configure(this.errorHandler, this.keyGenerator, this.cacheResolver, this.cacheManager);
        interceptor.setCacheOperationSource(this.cacheOperationSource());
        return interceptor;
    }
}
```



可以看到在其中配置了三个bean：BeanFactoryCacheOperationSourceAdvisor、AnnotationCacheOperationSource、CacheInterceptor。

- `AnnotationCacheOperationSource`的主要作用是，<font color=red>获取定义在类和方法上的 SpringCache 相关的注解，并将其转换为对应的 CacheOperation 属性</font>。
- `BeanFactoryCacheOperationSourceAdvisor`是一个`PointcutAdvisor`，是 SpringCache 使用 Spring AOP 机制的关键所在，该 advisor 会织入到需要执行缓存操作的 bean 的增强代理中，形成一个切面。并在方法调用时，在该切面上执行拦截器 CacheInterceptor 的业务逻辑。
- `CacheInterceptor`是一个拦截器，当方法调用时碰到了 BeanFactoryCacheOperationSourceAdvisor 定义的切面，就会执行 CacheInterceptor 的业务逻辑，该业务逻辑就是==缓存的核心业务逻辑==。



## AnnotationCacheOperationSource

其继承路径是：

`AnnotationCacheOperationSource` $\rightarrow$ `AbstractFallbackCacheOperationSource` $\rightarrow$ `CacheOperationSource`

其中 `CacheOperationSource` 接口定义了一个方法：

```java
package org.springframework.cache.interceptor;

public interface CacheOperationSource {
    default boolean isCandidateClass(Class<?> targetClass) {
        return true;
    }

    @Nullable
    Collection<CacheOperation> getCacheOperations(Method var1, @Nullable Class<?> var2);
}
```

该方法用于根据<font color=red>指定类上的指定方法上打的 SpringCache 注释来得到对应的 CacheOperation 集合</font>。

### AbstractFallbackCacheOperationSource

`AbstractFallbackCacheOperationSource`是 CacheOperationSource 的抽象实现类，采用==模板模式==将获取某类的某方法上的 CacheOperation 的业务流程固化。该固化的流程可<font color=red>将方法上的属性缓存，并实现了一个获取 CacheOperation 的 fallback 策略</font>，执行的顺序为：

1、目标方法；2、目标类；3、声明方法；4、声明类/接口。

当方法被调用过一次之后，其上的属性就会被缓存。

它提供了两个抽象模板方法，供具体的子类来实现：

```java
@Nullable
public Collection<CacheOperation> getCacheOperations(Method method, @Nullable Class<?> targetClass) {
    if (method.getDeclaringClass() == Object.class) {
        return null;
    } else {
        Object cacheKey = this.getCacheKey(method, targetClass);
        Collection<CacheOperation> cached = (Collection)this.attributeCache.get(cacheKey);
        if (cached != null) {
            return cached != NULL_CACHING_ATTRIBUTE ? cached : null;
        } else {
            Collection<CacheOperation> cacheOps = this.computeCacheOperations(method, targetClass);
            if (cacheOps != null) {
                if (this.logger.isTraceEnabled()) {
                    this.logger.trace("Adding cacheable method '" + method.getName() + "' with attribute: " + cacheOps);
                }

                this.attributeCache.put(cacheKey, cacheOps);
            } else {
                this.attributeCache.put(cacheKey, NULL_CACHING_ATTRIBUTE);
            }

            return cacheOps;
        }
    }
}

@Nullable
protected abstract Collection<CacheOperation> findCacheOperations(Class<?> var1);

@Nullable
protected abstract Collection<CacheOperation> findCacheOperations(Method var1);
```

### AnnotationCacheOperationSource

AnnotationCacheOperationSource 内部持有一个`Set<CacheAnnotaionParser>`的集合，默认包含`SpringCacheAnnotationParser`，并使用 SpringCacheAnnotationParser 来实现 AbstractFallbackCacheOperationSource 定义的两个抽象方法。

`class AnnotationCacheOperationSource`：

```java
private final Set<CacheAnnotationParser> annotationParsers;

public AnnotationCacheOperationSource(Set<CacheAnnotationParser> annotationParsers) {
    this.publicMethodsOnly = true;
    Assert.notEmpty(annotationParsers, "At least one CacheAnnotationParser needs to be specified");
    this.annotationParsers = annotationParsers;
}

@Nullable
protected Collection<CacheOperation> findCacheOperations(Class<?> clazz) {
    return this.determineCacheOperations((parser) -> {
        return parser.parseCacheAnnotations(clazz);
    });
}

@Nullable
protected Collection<CacheOperation> findCacheOperations(Method method) {
    return this.determineCacheOperations((parser) -> {
        return parser.parseCacheAnnotations(method);
    });
}

@Nullable
protected Collection<CacheOperation> determineCacheOperations(AnnotationCacheOperationSource.CacheOperationProvider provider) {
    Collection<CacheOperation> ops = null;
    Iterator var3 = this.annotationParsers.iterator();

    while(var3.hasNext()) {
        CacheAnnotationParser parser = (CacheAnnotationParser)var3.next();
        Collection<CacheOperation> annOps = provider.getCacheOperations(parser);
        if (annOps != null) {
            if (ops == null) {
                ops = annOps;
            } else {
                Collection<CacheOperation> combined = new ArrayList(((Collection)ops).size() + annOps.size());
                combined.addAll((Collection)ops);
                combined.addAll(annOps);
                ops = combined;
            }
        }
    }

    return (Collection)ops;
}

@FunctionalInterface
protected interface CacheOperationProvider {
    @Nullable
    Collection<CacheOperation> getCacheOperations(CacheAnnotationParser var1);
}
```

具体实现使用回调模式，用`Set<CacheAnnotaionParser>`中的每一个 CacheAnnotaionParser 去解析一个方法或类，然后将得到的`List<CacheOperation>`合并，最终返回。

### CacheAnnotaionParser

该接口定义了两个方法：

```java
public interface CacheAnnotationParser {
    default boolean isCandidateClass(Class<?> targetClass) {
        return true;
    }
	// 解析类上的标注，并相应创建 CacheOperation
    @Nullable
    Collection<CacheOperation> parseCacheAnnotations(Class<?> var1);
	// 解析方法上的标注，并相应创建 CacheOperation
    @Nullable
    Collection<CacheOperation> parseCacheAnnotations(Method var1);
}
```

其默认实现类为`SpringCacheAnnotationParser`，在其内部对 SpringCache 的几个注解 @Cacheable、@CachePut、@CacheEvict、@Caching 进行了解析，并相应的创建 CacheableOperation、CacheEvictOperation、CachePutOperation。

### SpringCacheAnnotationParser

```java
@Nullable
private Collection<CacheOperation> parseCacheAnnotations(SpringCacheAnnotationParser.DefaultCacheConfig cachingConfig, AnnotatedElement ae, boolean localOnly) {
    Collection<? extends Annotation> anns = localOnly ? AnnotatedElementUtils.getAllMergedAnnotations(ae, CACHE_OPERATION_ANNOTATIONS) : AnnotatedElementUtils.findAllMergedAnnotations(ae, CACHE_OPERATION_ANNOTATIONS);
    if (anns.isEmpty()) {
        return null;
    } else {
        Collection<CacheOperation> ops = new ArrayList(1);
        
        anns.stream().filter((ann) -> {
            return ann instanceof Cacheable;
        }).forEach((ann) -> {
            ops.add(this.parseCacheableAnnotation(ae, cachingConfig, (Cacheable)ann));
        });
        
        anns.stream().filter((ann) -> {
            return ann instanceof CacheEvict;
        }).forEach((ann) -> {
            ops.add(this.parseEvictAnnotation(ae, cachingConfig, (CacheEvict)ann));
        });
        
        anns.stream().filter((ann) -> {
            return ann instanceof CachePut;
        }).forEach((ann) -> {
            ops.add(this.parsePutAnnotation(ae, cachingConfig, (CachePut)ann));
        });
        
        anns.stream().filter((ann) -> {
            return ann instanceof Caching;
        }).forEach((ann) -> {
            this.parseCachingAnnotation(ae, cachingConfig, (Caching)ann, ops);
        });
        
        return ops;
    }
}
```

参数中 AnnotatedElement 是 Class 和 Method 的父类，代表了一个可被注释的元素，该参数可以是一个 Class 也可以是一个 Method。



总结：

- 首先查找该类、方法上的 Cacheable 注解并进行合并。

- 针对合并后的每个 Cacheable 创建对应的 CacheableOperation；然后同样逻辑执行 CacheEvict 和 CachePut。
- 最后处理Caching，Caching 表示的是若干组 Cache 标注的集合，将其解析成一组 CacheOperation 并添加到`Collection<CacheOperation> ops`中。



## CacheInterceptor

CacheInterceptor 继承了 `CacheAspectSupport` 并实现了 `MethodInterceptor` 接口，因此它本质上是一个 ==Advice==，也就是可在切面上执行的==增强逻辑==。

CacheInterceptor 切面的拦截方法代码如下：

```java
package org.springframework.cache.interceptor;

public class CacheInterceptor extends CacheAspectSupport implements MethodInterceptor, Serializable {
    public CacheInterceptor() {
    }

    @Nullable
    public Object invoke(MethodInvocation invocation) throws Throwable {
        Method method = invocation.getMethod();
        CacheOperationInvoker aopAllianceInvoker = () -> {
            try {
                return invocation.proceed();
            } catch (Throwable var2) {
                throw new ThrowableWrapper(var2);
            }
        };

        try {
            return this.execute(aopAllianceInvoker, invocation.getThis(), method, invocation.getArguments());
        } catch (ThrowableWrapper var5) {
            throw var5.getOriginal();
        }
    }
}
```

### CacheAspectSupport

这个增强逻辑的核心功能是在 `CacheAspectSupport` 中实现的，

首先调用`AnnotationCacheOperationSource.getCacheOperations(method, targetClass)`方法得到被调用方法的`Collection<CacheOperation>`；

然后将这些 CacheOperation 以及被调用方法、调用参数、目标类、相应的 Cache 信息统统封装到 CacheOperation 上下文里，随后调用真正的核心方法。

```java
@Nullable
protected Object execute(CacheOperationInvoker invoker, Object target, Method method, Object[] args) {
    if (this.initialized) {
        Class<?> targetClass = this.getTargetClass(target);
        CacheOperationSource cacheOperationSource = this.getCacheOperationSource();
        if (cacheOperationSource != null) {
            // 得到被调用方法的Collection<CacheOperation>
            Collection<CacheOperation> operations = cacheOperationSource.getCacheOperations(method, targetClass);
            if (!CollectionUtils.isEmpty(operations)) {
                // 将这些 CacheOperation 以及被调用方法、调用参数、目标类、相应的 Cache 信息统统封装到 CacheOperation 上下文里
                return this.execute(invoker, method, new CacheAspectSupport.CacheOperationContexts(operations, method, args, target, targetClass));
            }
        }
    }

    return invoker.invoke();
}

// 该方法封装了SpringCache核心的处理逻辑，也就是使用 Cache 配合来完成用户的方法调用，并返回结果
@Nullable
private Object execute(CacheOperationInvoker invoker, Method method, CacheAspectSupport.CacheOperationContexts contexts) {
    if (contexts.isSynchronized()) {
        CacheAspectSupport.CacheOperationContext context = (CacheAspectSupport.CacheOperationContext)contexts.get(CacheableOperation.class).iterator().next();
        if (!this.isConditionPassing(context, CacheOperationExpressionEvaluator.NO_RESULT)) {
            return this.invokeOperation(invoker);
        }

        Object key = this.generateKey(context, CacheOperationExpressionEvaluator.NO_RESULT);
        Cache cache = (Cache)context.getCaches().iterator().next();

        try {
            return this.wrapCacheValue(method, this.handleSynchronizedGet(invoker, key, cache));
        } catch (ValueRetrievalException var10) {
            ReflectionUtils.rethrowRuntimeException(var10.getCause());
        }
    }

    this.processCacheEvicts(contexts.get(CacheEvictOperation.class), true, CacheOperationExpressionEvaluator.NO_RESULT);
    ValueWrapper cacheHit = this.findCachedItem(contexts.get(CacheableOperation.class));
    List<CacheAspectSupport.CachePutRequest> cachePutRequests = new LinkedList();
    if (cacheHit == null) {
        this.collectPutRequests(contexts.get(CacheableOperation.class), CacheOperationExpressionEvaluator.NO_RESULT, cachePutRequests);
    }

    Object returnValue;
    Object cacheValue;
    if (cacheHit != null && !this.hasCachePut(contexts)) {
        cacheValue = cacheHit.get();
        returnValue = this.wrapCacheValue(method, cacheValue);
    } else {
        returnValue = this.invokeOperation(invoker);
        cacheValue = this.unwrapReturnValue(returnValue);
    }

    this.collectPutRequests(contexts.get(CachePutOperation.class), cacheValue, cachePutRequests);
    Iterator var8 = cachePutRequests.iterator();

    while(var8.hasNext()) {
        CacheAspectSupport.CachePutRequest cachePutRequest = (CacheAspectSupport.CachePutRequest)var8.next();
        cachePutRequest.apply(cacheValue);
    }

    this.processCacheEvicts(contexts.get(CacheEvictOperation.class), false, cacheValue);
    return returnValue;
}
```

## BeanFactoryCacheOperationSourceAdvisor

它负责将`CacheInterceptor`与`CacheOperationSourcePointcut`结合起来。其内部注入了`AnnotationCacheOperationSource`，并创建了`CacheOperationSourcePointcut`：

```java
package org.springframework.cache.interceptor;

@SuppressWarnings("serial")
public class BeanFactoryCacheOperationSourceAdvisor extends AbstractBeanFactoryPointcutAdvisor {

	@Nullable
	private CacheOperationSource cacheOperationSource;

	private final CacheOperationSourcePointcut pointcut = new CacheOperationSourcePointcut() {
		@Override
		@Nullable
		protected CacheOperationSource getCacheOperationSource() {
			return cacheOperationSource;
		}
	};

	public void setCacheOperationSource(CacheOperationSource cacheOperationSource) {
		this.cacheOperationSource = cacheOperationSource;
	}

	public void setClassFilter(ClassFilter classFilter) {
		this.pointcut.setClassFilter(classFilter);
	}

	@Override
	public Pointcut getPointcut() {
		return this.pointcut;
	}
}
```

`BeanFactoryCacheOperationSourceAdvisor`利用 Spring AOP 的面向切面机制，<font color=red>将配置了 SpringCache 相关注释的类进行代理增强，并加入到其 advisors 处理链中</font>。

在 bean 创建的时候，如果需要 Spring AOP 代理增强，会首先取出 beanFactory 中所有的 advisors，然后过滤出适合该 Bean 的advisors，加入到代理类中。

其中对于 PointcutAdvisor 类型的 advisor 是通过

```java
// 逻辑抽取，非实际源码
advisor.getPointcut().getMethodMatcher().matches(method, targetClass)
```

来判断该 advisor 是否适合用于被创建的 Bean。

因此最终会调用到 `CacheOperationSourcePointcut` 的 matches 方法，代码如下：

```java
@Override
public boolean matches(Method method, Class<?> targetClass) {
    CacheOperationSource cas = getCacheOperationSource();
    return (cas != null && !CollectionUtils.isEmpty(cas.getCacheOperations(method, targetClass)));
}
```

结合上面的代码，最终会调用`AnnotationCacheOperationSource.getCacheOperations(method, targetClass)`方法。

因此 matches 方法的意思就是：如果 bean 目标类的任何一个方法存在 SpringCache 相关的注解，从而可以获得`List<CacheOperation>`，那么该 bean 需要由`BeanFactoryCacheOperationSourceAdvisor`来做切面增强，参见配置类`ProxyCachingConfiguration`中的定义：

```java
@Bean(name = CacheManagementConfigUtils.CACHE_ADVISOR_BEAN_NAME)
@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
public BeanFactoryCacheOperationSourceAdvisor cacheAdvisor(
        CacheOperationSource cacheOperationSource, CacheInterceptor cacheInterceptor) {

    BeanFactoryCacheOperationSourceAdvisor advisor = new BeanFactoryCacheOperationSourceAdvisor();
    advisor.setCacheOperationSource(cacheOperationSource);
    advisor.setAdvice(cacheInterceptor);
    if (this.enableCaching != null) {
        advisor.setOrder(this.enableCaching.<Integer>getNumber("order"));
    }
    return advisor;
}
```

`BeanFactoryCacheOperationSourceAdvisor`切入后执行的业务逻辑就是`CacheInterceptor`中的`invoke(MethodInvocation invocation)`方法。在该方法中，调用其父类`CacheAspectSupport`中的方法来完成缓存的核心处理逻辑，并返回结果。

# SpringCache 自动配置

当我们在 SpringBoot 中使用默认实现时，由于其自动配置机制，我们甚至都不需要自己配置 CacheManager。在`spring-boot-autoconfigure`模块里有专门配置 SpringCache 的配置类 `CacheAutoConfiguration`：

```java
package org.springframework.boot.autoconfigure.cache;

@Configuration(
    proxyBeanMethods = false
)
@ConditionalOnClass({CacheManager.class})
@ConditionalOnBean({CacheAspectSupport.class})
@ConditionalOnMissingBean(
    value = {CacheManager.class},
    name = {"cacheResolver"}
)
@EnableConfigurationProperties({CacheProperties.class})
@AutoConfigureAfter({CouchbaseDataAutoConfiguration.class, HazelcastAutoConfiguration.class, HibernateJpaAutoConfiguration.class, RedisAutoConfiguration.class})
@Import({CacheAutoConfiguration.CacheConfigurationImportSelector.class, CacheAutoConfiguration.CacheManagerEntityManagerFactoryDependsOnPostProcessor.class})
public class CacheAutoConfiguration {
    public CacheAutoConfiguration() {
    }

   ......

    static class CacheConfigurationImportSelector implements ImportSelector {
        CacheConfigurationImportSelector() {
        }

        public String[] selectImports(AnnotationMetadata importingClassMetadata) {
            CacheType[] types = CacheType.values();
            String[] imports = new String[types.length];

            for(int i = 0; i < types.length; ++i) {
                imports[i] = CacheConfigurations.getConfigurationClass(types[i]);
            }

            return imports;
        }
    }

    ......
}
```

意思是当满足条件：

- CacheManager.class 存在；
- CacheAspectSupport 对应的 bean 存在；
- CacheManager 对应的 bean 不存在；
- 等等

时 CacheAutoConfiguration 配置类就生效。

其中 CacheAspectSupport 是 CacheInterceptor 的父类，SpringCache 真正的核心业务逻辑是由它实现的。当打上 @CacheEnable 注释时，自动配置了 CacheInterceptor 的 bean ，也就是 CacheAspectSupport 的 bean，因此肯定存在。



CacheAutoConfiguration 通过 @Import 机制引入 `CacheManagerCustomizers.class` 和 `CacheConfigurationImportSelector.class` 。

其中 CacheConfigurationImportSelector 使用`CacheType.values()`作为 Key，遍历并创建将要加载的配置类全名的字符串数组。枚举 CacheType（cache 类型）的代码为：

```java
package org.springframework.boot.autoconfigure.cache;

public enum CacheType {
    GENERIC,
    JCACHE,
    EHCACHE,
    HAZELCAST,
    INFINISPAN,
    COUCHBASE,
    REDIS,
    CAFFEINE,
    SIMPLE,
    NONE;

    private CacheType() {
    }
}
```

同时在 CacheConfigurations 类中定义了各不同类型的 cache 对应的配置类：

```java
package org.springframework.boot.autoconfigure.cache;

final class CacheConfigurations {
    private static final Map<CacheType, String> MAPPINGS;

    private CacheConfigurations() {
    }

    static String getConfigurationClass(CacheType cacheType) {
        String configurationClassName = (String)MAPPINGS.get(cacheType);
        Assert.state(configurationClassName != null, () -> {
            return "Unknown cache type " + cacheType;
        });
        return configurationClassName;
    }

    static CacheType getType(String configurationClassName) {
        Iterator var1 = MAPPINGS.entrySet().iterator();

        Entry entry;
        do {
            if (!var1.hasNext()) {
                throw new IllegalStateException("Unknown configuration class " + configurationClassName);
            }

            entry = (Entry)var1.next();
        } while(!((String)entry.getValue()).equals(configurationClassName));

        return (CacheType)entry.getKey();
    }

    static {
        Map<CacheType, String> mappings = new EnumMap(CacheType.class);
        mappings.put(CacheType.GENERIC, GenericCacheConfiguration.class.getName());
        mappings.put(CacheType.EHCACHE, EhCacheCacheConfiguration.class.getName());
        mappings.put(CacheType.HAZELCAST, HazelcastCacheConfiguration.class.getName());
        mappings.put(CacheType.INFINISPAN, InfinispanCacheConfiguration.class.getName());
        mappings.put(CacheType.JCACHE, JCacheCacheConfiguration.class.getName());
        mappings.put(CacheType.COUCHBASE, CouchbaseCacheConfiguration.class.getName());
        mappings.put(CacheType.REDIS, RedisCacheConfiguration.class.getName());
        mappings.put(CacheType.CAFFEINE, CaffeineCacheConfiguration.class.getName());
        mappings.put(CacheType.SIMPLE, SimpleCacheConfiguration.class.getName());
        mappings.put(CacheType.NONE, NoOpCacheConfiguration.class.getName());
        MAPPINGS = Collections.unmodifiableMap(mappings);
    }
}
```

这些配置类各自生效的条件并不相同：

- GenericCacheConfiguration 的生效条件是：存在 Cache 的 bean，但是不存在 CacheManager 的 bean，并且==没有定义 spring.cache.type==或者 spring.cache.type 的值为 generic。
- SimpleCacheConfiguration的生效条件是：不存在 CacheManager 的 bean，并且没有定义 spring.cache.type 或者 spring.cache.type 的值为 simple。
- NoOpCacheConfiguration的生效条件是：不存在 CacheManager 的 bean，并且没有定义 spring.cache.type 或者 spring.cache.type 的值为 none。
- 其他的配置类都有各自额外的要求，例如需要引入相应的类库支持。

因此当我们没有引入任何其他类库，没有配置 Cache bean 并且没有指定 spring.cache.type 时，从上到下判断，GenericCacheConfiguration 不起作用（未定义Cache bean）。后续的一系列与第三方存储实现方案集成的配置类也不起作用（未引入相应类库），最后轮到 SimpleCacheConfiguration 符合条件起作用了。因此此时使用 SimpleCacheConfiguration 来进行 SpringCache 的配置：

```java
package org.springframework.boot.autoconfigure.cache;

@Configuration(
    proxyBeanMethods = false
)
@ConditionalOnMissingBean({CacheManager.class})
@Conditional({CacheCondition.class})
class SimpleCacheConfiguration {
    SimpleCacheConfiguration() {
    }

    @Bean
    ConcurrentMapCacheManager cacheManager(CacheProperties cacheProperties, CacheManagerCustomizers cacheManagerCustomizers) {
        ConcurrentMapCacheManager cacheManager = new ConcurrentMapCacheManager();
        List<String> cacheNames = cacheProperties.getCacheNames();
        if (!cacheNames.isEmpty()) {
            cacheManager.setCacheNames(cacheNames);
        }

        return (ConcurrentMapCacheManager)cacheManagerCustomizers.customize(cacheManager);
    }
}
```

该配置类指明使用 `ConcurrentMapCacheManager `作为 CacheManager bean，在其内部使用缓存方法的注释中指明的 cache 名称来创建 ConcurrentMapCache 类型的cache，创建 cache 的代码如下：

```java
// ConcurrentMapCacheManager.java
public void setCacheNames(@Nullable Collection<String> cacheNames) {
    if (cacheNames != null) {
        for (String name : cacheNames) {
            this.cacheMap.put(name, createConcurrentMapCache(name));
        }
        this.dynamic = false;
    }
    else {
        this.dynamic = true;
    }
}
```

因此，由于 SpringBoot 的自动配置机制， 我们只需要打上 @EnableCaching 标注就可以启动 SpringCache 机制，使用其开箱即用的缓存实现方案。

# SpringCache 注解解析

## AnnotationCacheOperationSource

`AnnotationCacheOperationSource`的作用是<font color=red>对方法上的 Cache 注解进行解析，并将其转化为对应的 CacheOperation</font>。执行注释解析动作的时机是在第一次调用该方法的时候（并缓存解析结果供后面方法调用时使用）。

AnnotationCacheOperationSource 内部持有一个`Set<CacheAnnotaionParser>`的集合，默认只包含一个 `SpringCacheAnnotationParser`。并使用 `CacheAnnotaionParser`来实现 `AbstractFallbackCacheOperationSource` 定义的两个抽象模板方法：

```java
public class AnnotationCacheOperationSource extends AbstractFallbackCacheOperationSource implements Serializable {
    private final Set<CacheAnnotationParser> annotationParsers;
    
    public AnnotationCacheOperationSource() {
        this(true);
    }
    
    public AnnotationCacheOperationSource(boolean publicMethodsOnly) {
        this.publicMethodsOnly = publicMethodsOnly;
        this.annotationParsers = Collections.singleton(new SpringCacheAnnotationParser());
    }
    
    @Override
    @Nullable
    protected Collection<CacheOperation> findCacheOperations(Class<?> clazz) {
        return determineCacheOperations(parser -> parser.parseCacheAnnotations(clazz));
    }

    @Override
    @Nullable
    protected Collection<CacheOperation> findCacheOperations(Method method) {
        return determineCacheOperations(parser -> parser.parseCacheAnnotations(method));
    }
    
    @Nullable
    protected Collection<CacheOperation> determineCacheOperations(CacheOperationProvider provider) {
        Collection<CacheOperation> ops = null;
        for (CacheAnnotationParser parser : this.annotationParsers) {
            Collection<CacheOperation> annOps = provider.getCacheOperations(parser);
            if (annOps != null) {
                if (ops == null) {
                    ops = annOps;
                }
                else {
                    Collection<CacheOperation> combined = new ArrayList<>(ops.size() + annOps.size());
                    combined.addAll(ops);
                    combined.addAll(annOps);
                    ops = combined;
                }
            }
        }
        return ops;
    }
    
    @FunctionalInterface
    protected interface CacheOperationProvider {
        @Nullable
        Collection<CacheOperation> getCacheOperations(CacheAnnotationParser parser);
    }
}
```

该实现中使用回调模式，用`Set<CacheAnnotaionParser>`中的每一个 CacheAnnotaionParser 去解析一个方法或类，然后将得到的`List<CacheOperation>`合并，最终返回。

## SpringCacheAnnotationParser

如上面代码所示，默认`Set<CacheAnnotaionParser>`中只有一个 SpringCacheAnnotationParser ，因此初次调用某方法的时候会在 Cache 切面上首先调用 SpringCacheAnnotationParser 的方法：

```java
public class SpringCacheAnnotationParser implements CacheAnnotationParser, Serializable {
    ......
    @Override
    @Nullable
    public Collection<CacheOperation> parseCacheAnnotations(Method method) {
        DefaultCacheConfig defaultConfig = new DefaultCacheConfig(method.getDeclaringClass());
        return parseCacheAnnotations(defaultConfig, method);
    }
    ......
}
```

来获取方法上的 Cache 相关注释，并将其封装成对应的 CacheOperation 集合并返回。

其中：

```java
DefaultCacheConfig defaultConfig = new DefaultCacheConfig(method.getDeclaringClass());
```

首先判断方法所在的类上是否配置了 @CacheConfig 注释，如果有就将该注释中的属性值设置到 defaultConfig 类中，用于后续配置。

然后调用核心方法：

```java
public class SpringCacheAnnotationParser implements CacheAnnotationParser, Serializable {
    ......
    @Nullable
    private Collection<CacheOperation> parseCacheAnnotations(
            DefaultCacheConfig cachingConfig, AnnotatedElement ae, boolean localOnly) {

        Collection<? extends Annotation> anns = (localOnly ?
                AnnotatedElementUtils.getAllMergedAnnotations(ae, CACHE_OPERATION_ANNOTATIONS) :
                AnnotatedElementUtils.findAllMergedAnnotations(ae, CACHE_OPERATION_ANNOTATIONS));
        if (anns.isEmpty()) {
            return null;
        }

        final Collection<CacheOperation> ops = new ArrayList<>(1);
        anns.stream().filter(ann -> ann instanceof Cacheable).forEach(
                ann -> ops.add(parseCacheableAnnotation(ae, cachingConfig, (Cacheable) ann)));
        anns.stream().filter(ann -> ann instanceof CacheEvict).forEach(
                ann -> ops.add(parseEvictAnnotation(ae, cachingConfig, (CacheEvict) ann)));
        anns.stream().filter(ann -> ann instanceof CachePut).forEach(
                ann -> ops.add(parsePutAnnotation(ae, cachingConfig, (CachePut) ann)));
        anns.stream().filter(ann -> ann instanceof Caching).forEach(
                ann -> parseCachingAnnotation(ae, cachingConfig, (Caching) ann, ops));
        return ops;
    }
    ......
}
```

该方法中使用 Spring Core 模块的`AnnotatedElementUtils`来得到标注到可被标注对象（在这里包括类和方法）上的指定类型的注释，包括了注释上再打注释等层级结构以及层级属性的合并操作。

整体逻辑是，首先获得标注在方法上的@Cacheable注释集合，并对其中的每个注释调用：

```java
private CacheableOperation parseCacheableAnnotation(
        AnnotatedElement ae, DefaultCacheConfig defaultConfig, Cacheable cacheable) {

    CacheableOperation.Builder builder = new CacheableOperation.Builder();

    builder.setName(ae.toString());
    builder.setCacheNames(cacheable.cacheNames());
    builder.setCondition(cacheable.condition());
    builder.setUnless(cacheable.unless());
    builder.setKey(cacheable.key());
    builder.setKeyGenerator(cacheable.keyGenerator());
    builder.setCacheManager(cacheable.cacheManager());
    builder.setCacheResolver(cacheable.cacheResolver());
    builder.setSync(cacheable.sync());

    defaultConfig.applyDefault(builder);
    CacheableOperation op = builder.build();
    validateCacheOperation(ae, op);

    return op;
}
```

利用`CacheableOperation.Builder`来构建一个`CacheableOperation`，并添加到`Collection<CacheOperation> ops`中。

接下来对 @CacheEvict 和 @CachePut 也执行同样的操作，区别是它们分别使用 CacheEvictOperation.Builder 和 CachePutOperation.Builder 来构建 CacheEvictOperation 和 CachePutOperation。

另外对于 @Caching，调用：

```java
private void parseCachingAnnotation(
        AnnotatedElement ae, DefaultCacheConfig defaultConfig, Caching caching, Collection<CacheOperation> ops) {

    Cacheable[] cacheables = caching.cacheable();
    for (Cacheable cacheable : cacheables) {
        ops.add(parseCacheableAnnotation(ae, defaultConfig, cacheable));
    }
    CacheEvict[] cacheEvicts = caching.evict();
    for (CacheEvict cacheEvict : cacheEvicts) {
        ops.add(parseEvictAnnotation(ae, defaultConfig, cacheEvict));
    }
    CachePut[] cachePuts = caching.put();
    for (CachePut cachePut : cachePuts) {
        ops.add(parsePutAnnotation(ae, defaultConfig, cachePut));
    }
}
```

其中对 caching 的 cacheable、evict 及 put 属性对应的各组 @Cacheable、@CacheEvict 及 @CachePut 注释分别调用前面介绍的执行逻辑来构建相应的 CacheOperation 并添加到`Collection<CacheOperation> ops`中，然后将其返回。

当首次调用某方法执行上述的解析操作后，AnnotationCacheOperationSource会将其缓存起来，后续再调用该方法时会直接从缓存中得到该方法对应的`Collection<CacheOperation> ops`以增加效率。

# 根据 CacheOperation 执行核心 Cache 业务逻辑

## 核心业务逻辑源码分析

对于打了 Cache 相关注释的类，在创建其 bean 的时候已经由 Spring AOP 为其创建代理增强，并将 BeanFactoryCacheOperationSourceAdvisor 加入其代理中。

 当调用其方法的时候，会通过代理执行到 BeanFactoryCacheOperationSourceAdvisor 定义的切面。该切面是一个 PointcutAdvisor，在 Spring AOP 底层框架 DefaultAdvisorChainFactory.getInterceptorsAndDynamicInterceptionAdvice方法中使用

```java
pointcutAdvisor.getPointcut().getMethodMatcher().matches(method, targetClass))
```

来判断被调用方法是否匹配切点逻辑，如果匹配就执行其拦截器中的逻辑， BeanFactoryCacheOperationSourceAdvisor 中注册的拦截器是 CacheInterceptor，其执行逻辑为：

```java
public class CacheInterceptor extends CacheAspectSupport implements MethodInterceptor, Serializable {
	@Override
	@Nullable
	public Object invoke(final MethodInvocation invocation) throws Throwable {
		Method method = invocation.getMethod();

		CacheOperationInvoker aopAllianceInvoker = () -> {
			try {
				return invocation.proceed();
			}
			catch (Throwable ex) {
				throw new CacheOperationInvoker.ThrowableWrapper(ex);
			}
		};

		Object target = invocation.getThis();
		Assert.state(target != null, "Target must not be null");
		try {
			return execute(aopAllianceInvoker, target, method, invocation.getArguments());
		}
		catch (CacheOperationInvoker.ThrowableWrapper th) {
			throw th.getOriginal();
		}
	}
}
```

其中 Spring AOP 底层调用该方法时，传递来的参数`MethodInvocation invocation`是一个把调用方法 method 与其对应的切面拦截器 interceptors 组装成类似于 ChainFilter 一样的结构。

CacheOperationInvoker 回调接口的意义是，将决定切面逻辑与实际调用方法顺序的权利转交给 CacheAspectSupport 的 execute 方法。
该逻辑中调用了 CacheAspectSupport 的方法：

```java
public abstract class CacheAspectSupport extends AbstractCacheInvoker
		implements BeanFactoryAware, InitializingBean, SmartInitializingSingleton {
    ......
    @Nullable
    protected Object execute(CacheOperationInvoker invoker, Object target, Method method, Object[] args) {
        // Check whether aspect is enabled (to cope with cases where the AJ is pulled in automatically)
        if (this.initialized) {
            Class<?> targetClass = getTargetClass(target);
            CacheOperationSource cacheOperationSource = getCacheOperationSource();
            if (cacheOperationSource != null) {
                Collection<CacheOperation> operations = cacheOperationSource.getCacheOperations(method, targetClass);
                if (!CollectionUtils.isEmpty(operations)) {
                    return execute(invoker, method,
                            new CacheOperationContexts(operations, method, args, target, targetClass));
                }
            }
        }

        return invoker.invoke();
    }
    ......
}
```

该方法首先通过前面描述的`AnnotationCacheOperationSource.getCacheOperations(method, targetClass)`来获得调用方法上的`Collection<CacheOperation>`，然后将其和调用方法 method、方法参数 args、目标对象 target、目标类 targetClass 一起创建`CacheOperationContexts`：

```java
public abstract class CacheAspectSupport extends AbstractCacheInvoker
		implements BeanFactoryAware, InitializingBean, SmartInitializingSingleton {
    ......
    private class CacheOperationContexts {

        private final MultiValueMap<Class<? extends CacheOperation>, CacheOperationContext> contexts;

        private final boolean sync;

        public CacheOperationContexts(Collection<? extends CacheOperation> operations, Method method,
                Object[] args, Object target, Class<?> targetClass) {

            this.contexts = new LinkedMultiValueMap<>(operations.size());
            for (CacheOperation op : operations) {
                this.contexts.add(op.getClass(), getOperationContext(op, method, args, target, targetClass));
            }
            this.sync = determineSyncFlag(method);
        }
        ......
    }
}
```

其中，为每个 operation 分别创建 CacheOperationContext：

```java
public abstract class CacheAspectSupport extends AbstractCacheInvoker
		implements BeanFactoryAware, InitializingBean, SmartInitializingSingleton {
    ......
    protected CacheOperationContext getOperationContext(
            CacheOperation operation, Method method, Object[] args, Object target, Class<?> targetClass) {

        CacheOperationMetadata metadata = getCacheOperationMetadata(operation, method, targetClass);
        return new CacheOperationContext(metadata, args, target);
    }
    ......
}
```

获取`CacheOperationMetadata metadata`时的较重要的动作就是获取 CacheOperation 中用 String 名称定义的 CacheResolver 和 KeyGenerator 的 bean。

然后在创建 CacheOperationContext 时使用 CacheResolver bean 获得 cache 的信息：

```java
public abstract class CacheAspectSupport extends AbstractCacheInvoker
		implements BeanFactoryAware, InitializingBean, SmartInitializingSingleton {
    ......
    protected class CacheOperationContext implements CacheOperationInvocationContext<CacheOperation> {
        ......
        public CacheOperationContext(CacheOperationMetadata metadata, Object[] args, Object target) {
            this.metadata = metadata;
            this.args = extractArgs(metadata.method, args);
            this.target = target;
            this.caches = CacheAspectSupport.this.getCaches(this, metadata.cacheResolver);
            this.cacheNames = createCacheNames(this.caches);
        }
        ......
    }
    ......
}
```

在创建完上下文 CacheOperationContexts 后，调用 SpringCache 真正的核心业务逻辑（`execute`）：

```java
public abstract class CacheAspectSupport extends AbstractCacheInvoker
		implements BeanFactoryAware, InitializingBean, SmartInitializingSingleton {
    ......
    @Nullable
    private Object execute(final CacheOperationInvoker invoker, Method method, CacheOperationContexts contexts) {
        // Special handling of synchronized invocation
        if (contexts.isSynchronized()) {
            CacheOperationContext context = contexts.get(CacheableOperation.class).iterator().next();
            if (isConditionPassing(context, CacheOperationExpressionEvaluator.NO_RESULT)) {
                Object key = generateKey(context, CacheOperationExpressionEvaluator.NO_RESULT);
                Cache cache = context.getCaches().iterator().next();
                try {
                    return wrapCacheValue(method, handleSynchronizedGet(invoker, key, cache));
                }
                catch (Cache.ValueRetrievalException ex) {
                    // Directly propagate ThrowableWrapper from the invoker,
                    // or potentially also an IllegalArgumentException etc.
                    ReflectionUtils.rethrowRuntimeException(ex.getCause());
                }
            }
            else {
                // No caching required, only call the underlying method
                return invokeOperation(invoker);
            }
        }


        // Process any early evictions
        // 首先判断是否需要在方法调用前执行缓存清除
        processCacheEvicts(contexts.get(CacheEvictOperation.class), true,
                CacheOperationExpressionEvaluator.NO_RESULT);

        // Check if we have a cached item matching the conditions
        // 然后检查是否能得到一个符合条件的缓存值
        Cache.ValueWrapper cacheHit = findCachedItem(contexts.get(CacheableOperation.class));

        // Collect puts from any @Cacheable miss, if no cached item is found
        // 随后如果Cacheable miss（没有获取到缓存），就会创建一个对应的CachePutRequest并收集起来
        List<CachePutRequest> cachePutRequests = new ArrayList<>();
        if (cacheHit == null) {
            collectPutRequests(contexts.get(CacheableOperation.class),
                    CacheOperationExpressionEvaluator.NO_RESULT, cachePutRequests);
        }

        // 接下来判断返回缓存值还是实际调用方法的结果
        Object cacheValue;
        Object returnValue;

        if (cacheHit != null && !hasCachePut(contexts)) {
            // If there are no put requests, just use the cache hit
            cacheValue = cacheHit.get();
            returnValue = wrapCacheValue(method, cacheValue);
        }
        else {
            // Invoke the method if we don't have a cache hit
            returnValue = invokeOperation(invoker);
            cacheValue = unwrapReturnValue(returnValue);
        }

        // Collect any explicit @CachePuts
        // 方法调用后收集@CachePut明确定义的CachePutRequest
        collectPutRequests(contexts.get(CachePutOperation.class), cacheValue, cachePutRequests);

        // Process any collected put requests, either from @CachePut or a @Cacheable miss
        // 执行CachePutRequest将符合条件的数据写入缓存
        for (CachePutRequest cachePutRequest : cachePutRequests) {
            cachePutRequest.apply(cacheValue);
        }

        // Process any late evictions
        // 最后判断是否需要在方法调用后执行缓存清除
        processCacheEvicts(contexts.get(CacheEvictOperation.class), false, cacheValue);

        return returnValue;
    }
    ......
}
```



**首先判断是否需要在方法调用前执行缓存清除**：

```java
processCacheEvicts(contexts.get(CacheEvictOperation.class), true,
                CacheOperationExpressionEvaluator.NO_RESULT);
```

其作用是判断是否需要在方法调用前执行缓存清除。判断是否存在`beforeInvocation==true`并且 condition 符合条件的 @CacheEvict 注释，如果存在则最终执行方法：

```java
public abstract class CacheAspectSupport extends AbstractCacheInvoker
		implements BeanFactoryAware, InitializingBean, SmartInitializingSingleton {
    ......
    private void processCacheEvicts(
            Collection<CacheOperationContext> contexts, boolean beforeInvocation, @Nullable Object result) {

        for (CacheOperationContext context : contexts) {
            CacheEvictOperation operation = (CacheEvictOperation) context.metadata.operation;
            if (beforeInvocation == operation.isBeforeInvocation() && isConditionPassing(context, result)) {
                performCacheEvict(context, operation, result);
            }
        }
    }

    private void performCacheEvict(
            CacheOperationContext context, CacheEvictOperation operation, @Nullable Object result) {

        Object key = null;
        for (Cache cache : context.getCaches()) {
            if (operation.isCacheWide()) {
                logInvalidating(context, operation, null);
                doClear(cache, operation.isBeforeInvocation());
            }
            else {
                if (key == null) {
                    key = generateKey(context, result);
                }
                logInvalidating(context, operation, key);
                doEvict(cache, key, operation.isBeforeInvocation());
            }
        }
    }
    ......
}
```

对于注释中定义的每一个 cache 都根据 allEntries 是否为 true 执行其 clear() 方法或 evict(key) 方法来清除全部或部分缓存。



**然后检查是否能得到一个符合条件的缓存值**：

```java
Cache.ValueWrapper cacheHit = findCachedItem(contexts.get(CacheableOperation.class));
```



**随后如果Cacheable miss（没有获取到缓存），就会创建一个对应的CachePutRequest并收集起来**：

```java
List<CachePutRequest> cachePutRequests = new ArrayList<>();
if (cacheHit == null) {
    collectPutRequests(contexts.get(CacheableOperation.class),
            CacheOperationExpressionEvaluator.NO_RESULT, cachePutRequests);
}
```

注意，此时方法尚未执行，因此第二个参数为CacheOperationExpressionEvaluator.NO_RESULT，意思是当前上下文中#result不存在。



**接下来判断返回缓存值还是实际调用方法的结果**：

如果得到了缓存，并且CachePutRequests为空并且不含有符合条件（condition match）的@CachePut注释，那么就将returnValue赋值为缓存值；否则实际执行方法，并将returnValue赋值为方法返回值。

```java
Object cacheValue;
Object returnValue;

if (cacheHit != null && !hasCachePut(contexts)) {
    // If there are no put requests, just use the cache hit
    cacheValue = cacheHit.get();
    returnValue = wrapCacheValue(method, cacheValue);
}
else {
    // Invoke the method if we don't have a cache hit
    returnValue = invokeOperation(invoker);
    cacheValue = unwrapReturnValue(returnValue);
}
```



**方法调用后收集@CachePut明确定义的CachePutRequest**：

收集符合条件的@CachePut定义的CachePutRequest，并添加到上面的cachePutRequests中

```java
collectPutRequests(contexts.get(CachePutOperation.class), cacheValue, cachePutRequests);
```

注意，此时处于方法调用后，返回结果已经存在了，因此在condition定义中可以使用上下文#result。



**执行CachePutRequest将符合条件的数据写入缓存**：

对于上面收集到的cachePutRequests，逐个调用其apply(cacheValue)方法

```java
for (CachePutRequest cachePutRequest : cachePutRequests) {
    cachePutRequest.apply(cacheValue);
}
```

其中，CachePutRequest.apply方法首先判断unless条件，unless不符合的时候才会对operationContext中的每一个cache执行put动作：

```java
public void apply(@Nullable Object result) {
    if (this.context.canPutToCache(result)) {
        for (Cache cache : this.context.getCaches()) {
            doPut(cache, this.key, result);
        }
    }
}
```

判断是否执行put动作的方法如下：

```java
protected boolean canPutToCache(@Nullable Object value) {
    String unless = "";
    if (this.metadata.operation instanceof CacheableOperation) {
        unless = ((CacheableOperation) this.metadata.operation).getUnless();
    }
    else if (this.metadata.operation instanceof CachePutOperation) {
        unless = ((CachePutOperation) this.metadata.operation).getUnless();
    }
    if (StringUtils.hasText(unless)) {
        EvaluationContext evaluationContext = createEvaluationContext(value);
        return !evaluator.unless(unless, this.metadata.methodKey, evaluationContext);
    }
    return true;
}
```



**最后判断是否需要在方法调用后执行缓存清除**：

```java
processCacheEvicts(contexts.get(CacheEvictOperation.class), false, cacheValue);
```

与步骤（1）相对应，其作用是判断是否需要在方法调用后执行缓存清除。判断是否存在beforeInvocation==false并且condition符合条件的@CacheEvict注释，注意此时上下文中结果#result是可用的。如果存在则最终执行缓存清除逻辑。



## 整体执行业务逻辑概述

1. 首先执行@CacheEvict（如果beforeInvocation=true且condition通过），如果allEntries=true，则清空所有；
2. 然后收集@Cacheable并检查是否能得到一个符合条件的缓存值；
3. 如果@Cacheable的condition通过，并且key对应的数据不在缓存中，就创建一个CachePutRequest实例放入cachePutRequests中；
4. 如果得到了缓存值并且cachePutRequests为空并且没有符合条件的@CachePut操作，那么将returnValue=缓存数据；
5. 如果没有找到缓存，那么实际执行方法调用，并把返回结果放入returnValue；
6. 收集符合条件的@CachePut操作(此时是方法执行后，condition上下文中#result可用)，并放入cachePutRequests；
7. 执行cachePutRequests，将数据写入缓存（unless为空或者unless解析结果为false）；
8. 执行@CacheEvict（如果beforeInvocation=false且condition通过），如果allEntries=true，则清空所有。

# 参考资料

[^1]:[SpringCache实现原理及核心业务逻辑（一）_不动明王1984的博客-CSDN博客_springcache](https://blog.csdn.net/m0_37962779/article/details/78671468)



