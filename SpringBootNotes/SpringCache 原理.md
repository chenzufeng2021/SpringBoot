---
typora-copy-images-to: SpringBootNotesPictures

---

# SpringCache 实现原理[^4]

## 基本概念

核心类：

- CacheManager：缓存管理器，获取缓存的接口。
- Cache：缓存操作抽象接口，抽象实现类为 AbstractValueAdaptingCache。

配置类：

- CachingConfigurerSupport：缓存配置支持类，需要使用 Spring Cache 的项目继承，一个项目只能有一个相关 bean。
- AbstractCachingConfiguration：抽象缓存配置，用于==初始化配置==。加载 cacheManager、 cacheResolver、 keyGenerator
- ProxyCachingConfiguration：继承 AbstractCachingConfiguration，默认的==缓存代理配置==，用来==配置拦截器==。

拦截器：

- BeanFactoryCacheOperationSourceAdvisor：缓存切面，重写拦截器时不需要修改；
- CacheInterceptor：缓存拦截器，继承 CacheAspectSupport。重写拦截器时可以继承。
- CacheAspectSupport：缓存拦截核心实现，缓存拦截器都需要继承



## 基本原理

1、项目继承 CachingConfigurerSupport，进行==缓存配置==，一个项目只能有一个缓存配置类。项目启动时自动加载。

2、ProxyCachingConfiguration 代理缓存配置类进行缓存拦截器注册，项目启动时自动加载。

3、<font color=red>方法执行的时候，缓存拦截器会拦截带有缓存注解（cacheable、cacheput、cacheEvit）的方法，进入 CacheAspectSupport 的 execute 方法中</font>。

4、CacheAspectSupport 的 execute 的主逻辑：前置删除缓存操作$\longrightarrow$查询缓存$\longrightarrow$(如果查询不到缓存执行方法)$\longrightarrow$添加缓存$\longrightarrow$后置删除缓存操作。每次都会按照这个流程走，在具体的方法里面会判断当前操作是否被执行。比如 @cacheable 注解的时候调用删除缓存操作方法就不会有任何执行。

5、如果缓存操作(get/put/evict)失败就会调用 CacheErrorHandler 的相应方法。如果没有 CacheErrorHandler，默认使用 SimpleCacheErrorHandler。







# 开启缓存[^2]

SpringCache 使用 Spring AOP 来实现，当我们在 Configuration 类打上 @EnableCaching 注释时，该标注通过 `ImportSelector` 机制启动 `AbstractAdvisorAutoProxyCreator` 的一个实例，该实例本身是一个 Ordered BeanPostProcessor，<font color=red>BeanPostProcessor 的作用是在 bean 创建、初始化的前后对其进行一些额外的操作（包括创建代理）</font>。因此可以认为当在 Configuration 类打上 @EnableCaching 注释时，做的第一件事情就是启用 Spring AOP 机制。

```java
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import({CachingConfigurationSelector.class})
public @interface EnableCaching {
    // 是否直接代理目标类
    boolean proxyTargetClass() default false;
	// 通知模式，默认是代理
    AdviceMode mode() default AdviceMode.PROXY;
	// 顺序
    int order() default 2147483647;
}
```

这个注解和@EnableAsync注解特别像，说明都是基于==Aop和代理==做了能力增强，该类导入了 `CachingConfigurationSelector` 类：

```java
public class CachingConfigurationSelector extends AdviceModeImportSelector<EnableCaching> {
    private static final String PROXY_JCACHE_CONFIGURATION_CLASS = "org.springframework.cache.jcache.config.ProxyJCacheConfiguration";
    private static final String CACHE_ASPECT_CONFIGURATION_CLASS_NAME = "org.springframework.cache.aspectj.AspectJCachingConfiguration";
    private static final String JCACHE_ASPECT_CONFIGURATION_CLASS_NAME = "org.springframework.cache.aspectj.AspectJJCacheConfiguration";
    private static final boolean jsr107Present;
    private static final boolean jcacheImplPresent;

    public CachingConfigurationSelector() {
    }

    public String[] selectImports(AdviceMode adviceMode) {
        switch(adviceMode) {
        case PROXY:
            return this.getProxyImports();
        case ASPECTJ:
            return this.getAspectJImports();
        default:
            return null;
        }
    }

    private String[] getProxyImports() {
        List<String> result = new ArrayList(3);
        result.add(AutoProxyRegistrar.class.getName());
        result.add(ProxyCachingConfiguration.class.getName());
        if (jsr107Present && jcacheImplPresent) {
            result.add("org.springframework.cache.jcache.config.ProxyJCacheConfiguration");
        }

        return StringUtils.toStringArray(result);
    }

    private String[] getAspectJImports() {
        List<String> result = new ArrayList(2);
        result.add("org.springframework.cache.aspectj.AspectJCachingConfiguration");
        if (jsr107Present && jcacheImplPresent) {
            result.add("org.springframework.cache.aspectj.AspectJJCacheConfiguration");
        }

        return StringUtils.toStringArray(result);
    }

    static {
        ClassLoader classLoader = CachingConfigurationSelector.class.getClassLoader();
        jsr107Present = ClassUtils.isPresent("javax.cache.Cache", classLoader);
        jcacheImplPresent = ClassUtils.isPresent("org.springframework.cache.jcache.config.ProxyJCacheConfiguration", classLoader);
    }
}
```

CachingConfigurationSelector 类的核心是 `selectImports` 方法，根据 @EnableCaching 配置的模式，选择不同的配置类型，默认是PROXY模式，导入 `AutoProxyRegistrar` 和 `ProxyCachingConfiguration` 两个配置。

# 缓存通知配置

## AbstractCachingConfiguration

父类 AbstractCachingConfiguration 实现：

```java
@Configuration
public abstract class AbstractCachingConfiguration implements ImportAware {
    @Nullable
    protected AnnotationAttributes enableCaching;
    @Nullable
    protected Supplier<CacheManager> cacheManager;
    @Nullable
    protected Supplier<CacheResolver> cacheResolver;
    @Nullable
    protected Supplier<KeyGenerator> keyGenerator;
    @Nullable
    protected Supplier<CacheErrorHandler> errorHandler;

    public AbstractCachingConfiguration() {
    }

    public void setImportMetadata(AnnotationMetadata importMetadata) {
        this.enableCaching = AnnotationAttributes.fromMap(importMetadata.getAnnotationAttributes(EnableCaching.class.getName(), false));
        if (this.enableCaching == null) {
            throw new IllegalArgumentException("@EnableCaching is not present on importing class " + importMetadata.getClassName());
        }
    }

    @Autowired(
        required = false
    )
    void setConfigurers(Collection<CachingConfigurer> configurers) {
        if (!CollectionUtils.isEmpty(configurers)) {
            if (configurers.size() > 1) {
                throw new IllegalStateException(configurers.size() + " implementations of CachingConfigurer were found when only 1 was expected. Refactor the configuration such that CachingConfigurer is implemented only once or not at all.");
            } else {
                CachingConfigurer configurer = (CachingConfigurer)configurers.iterator().next();
                this.useCachingConfigurer(configurer);
            }
        }
    }

    protected void useCachingConfigurer(CachingConfigurer config) {
        this.cacheManager = config::cacheManager;
        this.cacheResolver = config::cacheResolver;
        this.keyGenerator = config::keyGenerator;
        this.errorHandler = config::errorHandler;
    }
}
```

这里主要做了两件事：首先把注解==元数据属性==解析出来，然后把==用户自定义的缓存组件==装配进来（CacheManager、KeyGenerator 和异常处理器）。

## ProxyCachingConfiguration

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

ProxyCachingConfiguration 复用了父类的能力，并且定了AOP的三个核心组件（Pointcut、Advice 和 Advisor）先看AnnotationCacheOperationSource（此时还不能被称作Pointcut）[^2]

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

cacheKey 是由 method 和 class 构造成的 MethodClassKey。如果缓存中有缓存，操作集合直接返回。否则调用 `computeCacheOperations` 计算：

```java
@Nullable
private Collection<CacheOperation> computeCacheOperations(Method method, @Nullable Class<?> targetClass) {
    if (this.allowPublicMethodsOnly() && !Modifier.isPublic(method.getModifiers())) {
        return null;
    } else {
        Method specificMethod = AopUtils.getMostSpecificMethod(method, targetClass);
        Collection<CacheOperation> opDef = this.findCacheOperations(specificMethod);
        if (opDef != null) {
            return opDef;
        } else {
            opDef = this.findCacheOperations(specificMethod.getDeclaringClass());
            if (opDef != null && ClassUtils.isUserLevelMethod(method)) {
                return opDef;
            } else {
                if (specificMethod != method) {
                    opDef = this.findCacheOperations(method);
                    if (opDef != null) {
                        return opDef;
                    }

                    opDef = this.findCacheOperations(method.getDeclaringClass());
                    if (opDef != null && ClassUtils.isUserLevelMethod(method)) {
                        return opDef;
                    }
                }

                return null;
            }
        }
    }
}
```

该方法是从目标类和目标方法上（优先方法维度）解析缓存注解装配成缓存操作（`@Cacheable` $\rightarrow$ `CacheableOperation`），看子类  `AnnotationCacheOperationSource` 实现。

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

AnnotationCacheOperationSource 默认构造器使用的是 `SpringCacheAnnotationParser` 解析器，解析操作最终委托给 `SpringCacheAnnotationParser.parseCacheAnnotations`，<font color=red>将注解分别解析成对应的操作</font>[^2]。



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

Spring 注册缓存管理器后，会对需要缓存方法对应的类进行 AOP 处理，核心逻辑为自定了一个`MethodInterceptor`拦截器`org.springframework.cache.interceptor.CacheInterceptor`，该拦截器会将方法调用转到`CacheAspectSupport.execute()`中。

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

当拦截到调用时，将调用封装成 `CacheOperationInvoker` 并交给父类执行，父类 `CacheAspectSupport` 实现了 `SmartInitializingSingleton` 接口，在单例初始化后容器会调用 `afterSingletonsInstantiated` 方法[^2]。



### CacheAspectSupport

```java
public void afterSingletonsInstantiated() {
    if (this.getCacheResolver() == null) {
        Assert.state(this.beanFactory != null, "CacheResolver or BeanFactory must be set on cache aspect");

        try {
            this.setCacheManager((CacheManager)this.beanFactory.getBean(CacheManager.class));
        } catch (NoUniqueBeanDefinitionException var2) {
            throw new IllegalStateException("No CacheResolver specified, and no unique bean of type CacheManager found. Mark one as primary or declare a specific CacheManager to use.");
        } catch (NoSuchBeanDefinitionException var3) {
            throw new IllegalStateException("No CacheResolver specified, and no bean of type CacheManager found. Register a CacheManager bean or remove the @EnableCaching annotation from your configuration.");
        }
    }

    this.initialized = true;
}
```

检查有没有合适的 CacheManager，并且将 initialized 设置为 true。

继续看`CacheAspectSupport.execute`：

这个增强逻辑的核心功能是在 `CacheAspectSupport` 中实现的，Spring 注册缓存管理器后，会对需要缓存方法对应的类进行 AOP 处理，核心逻辑为自定了一个`MethodInterceptor`拦截器`org.springframework.cache.interceptor.CacheInterceptor`，该拦截器会将方法调用转到`CacheAspectSupport.execute()`中：

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
```

使用 AnnotationCacheOperationSource 目标类和方法上的缓存注解解析成操作集合，然后构造`CacheAspectSupport#class CacheOperationContexts#CacheOperationContexts`上下文并调用重载方法[^2]：

```java
private class CacheOperationContexts {
    private final MultiValueMap<Class<? extends CacheOperation>, CacheAspectSupport.CacheOperationContext> contexts;
    private final boolean sync;

    public CacheOperationContexts(Collection<? extends CacheOperation> operations, Method method, Object[] args, Object target, Class<?> targetClass) {
        this.contexts = new LinkedMultiValueMap(operations.size());
        Iterator var7 = operations.iterator();

        while(var7.hasNext()) {
            CacheOperation op = (CacheOperation)var7.next();
            this.contexts.add(op.getClass(), CacheAspectSupport.this.getOperationContext(op, method, args, target, targetClass));
        }

        this.sync = this.determineSyncFlag(method);
    }
}
```

将每个操作包装对应上下文映射关系，并检查是否是同步操作（@Cacheable独有属性），继续看 execute：

```java
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

    // 删除操作
    this.processCacheEvicts(contexts.get(CacheEvictOperation.class), true, CacheOperationExpressionEvaluator.NO_RESULT);
    // 获取对应的缓存
    ValueWrapper cacheHit = this.findCachedItem(contexts.get(CacheableOperation.class));
    // 缓存不存在，则从context中获取
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

    // 后置缓存删除操作
    this.processCacheEvicts(contexts.get(CacheEvictOperation.class), false, cacheValue);
    return returnValue;
}
```

该方法是缓存操作的核心逻辑[^2]：

- 首先检查是否是同步操作（@Cacheable特性）。
  - 如果是且满足条件，调用缓存获取逻辑并返回；
  - 否则返回业务逻辑免缓存调用 invokeOperation。
- 然后执行 @CacheEvict 的前置清除（beforeInvocation=true）。
- 接着检查 @Cacheable 是否命中缓存：如果没有命中则放入需要执行 CachePutRequest 列表暂存。
- 然后检查是否缓存命中且没有需要更新的缓存：
  - 如果满足则返回结果，使用缓存结果。否则使用业务查询结果作为返回结果，并且填充需要缓存的结果。
- 然后收集 @CachePut 操作，把 @CachePut 和 @Cacheable 未命中的请求同步到缓存。
- 最后清理 @CacheEvict 的缓存（beforeInvocation=false）。



# 缓存代理装配

前边讲述了缓存配置和工作流程，那么上述的 Aop 配置什么时候生效？在哪里生效?如何生效？

接下来将从`AutoProxyRegistrar`作为切入点，展开分析缓存代理的装配逻辑[^2]。

## AutoProxyRegistrar

```java
public class AutoProxyRegistrar implements ImportBeanDefinitionRegistrar {
    private final Log logger = LogFactory.getLog(this.getClass());

    public AutoProxyRegistrar() {
    }

    public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
        boolean candidateFound = false;
        Set<String> annTypes = importingClassMetadata.getAnnotationTypes();
        Iterator var5 = annTypes.iterator();

        while(var5.hasNext()) {
            String annType = (String)var5.next();
            AnnotationAttributes candidate = AnnotationConfigUtils.attributesFor(importingClassMetadata, annType);
            if (candidate != null) {
                Object mode = candidate.get("mode");
                Object proxyTargetClass = candidate.get("proxyTargetClass");
                if (mode != null && proxyTargetClass != null && AdviceMode.class == mode.getClass() && Boolean.class == proxyTargetClass.getClass()) {
                    candidateFound = true;
                    if (mode == AdviceMode.PROXY) {
                        // 手动注册自动代理创建器
                        AopConfigUtils.registerAutoProxyCreatorIfNecessary(registry);
                        if ((Boolean)proxyTargetClass) {
                            AopConfigUtils.forceAutoProxyCreatorToUseClassProxying(registry);
                            return;
                        }
                    }
                }
            }
        }

        if (!candidateFound && this.logger.isInfoEnabled()) {
            String name = this.getClass().getSimpleName();
            this.logger.info(String.format("%s was imported but no annotations were found having both 'mode' and 'proxyTargetClass' attributes of type AdviceMode and boolean respectively. This means that auto proxy creator registration and configuration may not have occurred as intended, and components may not be proxied as expected. Check to ensure that %s has been @Import'ed on the same class where these annotations are declared; otherwise remove the import of %s altogether.", name, name, name));
        }

    }
}
```

AutoProxyRegistrar 实现了`ImportBeanDefinitionRegistrar`接口，`registerBeanDefinitions` 会从启用缓存注解 @EnableCaching 提取属性，然后手动注册自动代理创建器：

```java
public abstract class AopConfigUtils {
    ......
    @Nullable
    public static BeanDefinition registerAutoProxyCreatorIfNecessary(BeanDefinitionRegistry registry, @Nullable Object source) {
        return registerOrEscalateApcAsRequired(InfrastructureAdvisorAutoProxyCreator.class, registry, source);
    }
    ......
    
    @Nullable
    private static BeanDefinition registerOrEscalateApcAsRequired(Class<?> cls, BeanDefinitionRegistry registry, @Nullable Object source) {
        Assert.notNull(registry, "BeanDefinitionRegistry must not be null");
        if (registry.containsBeanDefinition("org.springframework.aop.config.internalAutoProxyCreator")) {
            BeanDefinition apcDefinition = registry.getBeanDefinition("org.springframework.aop.config.internalAutoProxyCreator");
            if (!cls.getName().equals(apcDefinition.getBeanClassName())) {
                int currentPriority = findPriorityForClass(apcDefinition.getBeanClassName());
                int requiredPriority = findPriorityForClass(cls);
                if (currentPriority < requiredPriority) {
                    apcDefinition.setBeanClassName(cls.getName());
                }
            }

            return null;
        } else {
            RootBeanDefinition beanDefinition = new RootBeanDefinition(cls);
            beanDefinition.setSource(source);
            beanDefinition.getPropertyValues().add("order", -2147483648);
            beanDefinition.setRole(2);
            registry.registerBeanDefinition("org.springframework.aop.config.internalAutoProxyCreator", beanDefinition);
            return beanDefinition;
        }
    }
    ......
}
```

手动注册了 `InfrastructureAdvisorAutoProxyCreato`r 到容器中，看一下 InfrastructureAdvisorAutoProxyCreator 继承关系：



```java
package org.springframework.aop.framework.autoproxy;

public class InfrastructureAdvisorAutoProxyCreator extends AbstractAdvisorAutoProxyCreator {
    @Nullable
    private ConfigurableListableBeanFactory beanFactory;

    public InfrastructureAdvisorAutoProxyCreator() {
    }

    protected void initBeanFactory(ConfigurableListableBeanFactory beanFactory) {
        super.initBeanFactory(beanFactory);
        this.beanFactory = beanFactory;
    }

    protected boolean isEligibleAdvisorBean(String beanName) {
        return this.beanFactory != null && this.beanFactory.containsBeanDefinition(beanName) && this.beanFactory.getBeanDefinition(beanName).getRole() == 2;
    }
}
```

InfrastructureAdvisorAutoProxyCreator 继承了 AbstractAdvisorAutoProxyCreator 类，实现了 BeanFactory 初始化和 isEligibleAdvisorBean 方法。

## AbstractAdvisorAutoProxyCreator 

AbstractAdvisorAutoProxyCreator 定义了 Advisor 操作的工具方法，并且定义了 Advisor 提取适配器 BeanFactoryAdvisorRetrievalHelperAdapter，委托给子类 isEligibleAdvisorBean 方法实现（InfrastructureAdvisorAutoProxyCreator）。 



重点在于 AbstractAdvisorAutoProxyCreator 父类 AbstractAutoProxyCreator 实现的 postProcessBeforeInstantiation 方法。该方法在 InstantiationAwareBeanPostProcessor 接口定义，该方法在 bean 创建之前调用。如果该方法返回非null对象，那么 bean 的创建过程将会短路。此处的作用是为满足 BeanFactoryCacheOperationSourceAdvisor 增强器切入逻辑的类织入增强逻辑，也就是缓存能力。

```java
// AbstractAutoProxyCreator 
public Object postProcessBeforeInstantiation(Class<?> beanClass, String beanName) {
    Object cacheKey = this.getCacheKey(beanClass, beanName);
    if (!StringUtils.hasLength(beanName) || !this.targetSourcedBeans.contains(beanName)) {
        if (this.advisedBeans.containsKey(cacheKey)) {
            return null;
        }

        if (this.isInfrastructureClass(beanClass) || this.shouldSkip(beanClass, beanName)) {
            this.advisedBeans.put(cacheKey, Boolean.FALSE);
            return null;
        }
    }

    TargetSource targetSource = this.getCustomTargetSource(beanClass, beanName);
    if (targetSource != null) {
        if (StringUtils.hasLength(beanName)) {
            this.targetSourcedBeans.add(beanName);
        }

        Object[] specificInterceptors = this.getAdvicesAndAdvisorsForBean(beanClass, beanName, targetSource);
        Object proxy = this.createProxy(beanClass, beanName, specificInterceptors, targetSource);
        this.proxyTypes.put(cacheKey, proxy.getClass());
        return proxy;
    } else {
        return null;
    }
}
```

此处的逻辑和 AsyncAnnotationBeanPostProcessor 的 postProcessAfterInitialization 方法很相似，都是拦截 bean 创建过程并织入增强逻辑。

这里是自动生成代理类并且将缓存逻辑织入进去。也是自动代理实现APC的核心逻辑。

该方法前半段是从缓存中获取目标类是否被代理过，如果被代理过直接把增强逻辑织入，避免重复创建代理。后半段就是生成代理的逻辑，创建代理过程我们之前分析过，此处不再分析，重点分析一下从候选增强器中获取增强逻辑的方法getAdvicesAndAdvisorsForBean：

```java
// AbstractAdvisorAutoProxyCreator 
@Nullable
protected Object[] getAdvicesAndAdvisorsForBean(Class<?> beanClass, String beanName, @Nullable TargetSource targetSource) {
    List<Advisor> advisors = this.findEligibleAdvisors(beanClass, beanName);
    return advisors.isEmpty() ? DO_NOT_PROXY : advisors.toArray();
}
```

该方法在子类 AbstractAdvisorAutoProxyCreator 中实现，接着调用了 findEligibleAdvisors 方法：

```java
protected List<Advisor> findEligibleAdvisors(Class<?> beanClass, String beanName) {
    List<Advisor> candidateAdvisors = this.findCandidateAdvisors();
    List<Advisor> eligibleAdvisors = this.findAdvisorsThatCanApply(candidateAdvisors, beanClass, beanName);
    this.extendAdvisors(eligibleAdvisors);
    if (!eligibleAdvisors.isEmpty()) {
        eligibleAdvisors = this.sortAdvisors(eligibleAdvisors);
    }

    return eligibleAdvisors;
}
```

先通过前边定义的 BeanFactoryAdvisorRetrievalHelper 获取候选增强器，然后调用 findAdvisorsThatCanApply 方法筛选出对当前代理类适用的增强器：

```java
protected List<Advisor> findAdvisorsThatCanApply(List<Advisor> candidateAdvisors, Class<?> beanClass, String beanName) {
    ProxyCreationContext.setCurrentProxiedBeanName(beanName);

    List var4;
    try {
        var4 = AopUtils.findAdvisorsThatCanApply(candidateAdvisors, beanClass);
    } finally {
        ProxyCreationContext.setCurrentProxiedBeanName((String)null);
    }

    return var4;
}
```

### AopUtils

该方法将筛选逻辑委托为Aop工具类（AopUtils）的 findAdvisorsThatCanApply 方法处理：

```java
// AopUtils.java
public static List<Advisor> findAdvisorsThatCanApply(List<Advisor> candidateAdvisors, Class<?> clazz) {
    if (candidateAdvisors.isEmpty()) {
        return candidateAdvisors;
    } else {
        List<Advisor> eligibleAdvisors = new ArrayList();
        Iterator var3 = candidateAdvisors.iterator();

        while(var3.hasNext()) {
            Advisor candidate = (Advisor)var3.next();
            if (candidate instanceof IntroductionAdvisor && canApply(candidate, clazz)) {
                eligibleAdvisors.add(candidate);
            }
        }

        boolean hasIntroductions = !eligibleAdvisors.isEmpty();
        Iterator var7 = candidateAdvisors.iterator();

        while(var7.hasNext()) {
            Advisor candidate = (Advisor)var7.next();
            if (!(candidate instanceof IntroductionAdvisor) && canApply(candidate, clazz, hasIntroductions)) {
                eligibleAdvisors.add(candidate);
            }
        }

        return eligibleAdvisors;
    }
}
```

从 ProxyCachingConfiguration 中增强器的定义来看，BeanFactoryCacheOperationSourceAdvisor 是 PointcutAdvisor 类型，方法前半段 IntroductionAdvisor 逻辑跳过，通过 canApply 检查是否符合条件,如果符合则加入返回列表：

```java
public static boolean canApply(Advisor advisor, Class<?> targetClass, boolean hasIntroductions) {
    if (advisor instanceof IntroductionAdvisor) {
        return ((IntroductionAdvisor)advisor).getClassFilter().matches(targetClass);
    } else if (advisor instanceof PointcutAdvisor) {
        PointcutAdvisor pca = (PointcutAdvisor)advisor;
        return canApply(pca.getPointcut(), targetClass, hasIntroductions);
    } else {
        return true;
    }
}
```

直接进入第二个条件分支，检查 PointcutAdvisor 是否符合切入逻辑：

```java
public static boolean canApply(Pointcut pc, Class<?> targetClass, boolean hasIntroductions) {
    Assert.notNull(pc, "Pointcut must not be null");
    if (!pc.getClassFilter().matches(targetClass)) {
        return false;
    } else {
        MethodMatcher methodMatcher = pc.getMethodMatcher();
        if (methodMatcher == MethodMatcher.TRUE) {
            return true;
        } else {
            IntroductionAwareMethodMatcher introductionAwareMethodMatcher = null;
            if (methodMatcher instanceof IntroductionAwareMethodMatcher) {
                introductionAwareMethodMatcher = (IntroductionAwareMethodMatcher)methodMatcher;
            }

            Set<Class<?>> classes = new LinkedHashSet();
            if (!Proxy.isProxyClass(targetClass)) {
                classes.add(ClassUtils.getUserClass(targetClass));
            }

            classes.addAll(ClassUtils.getAllInterfacesForClassAsSet(targetClass));
            Iterator var6 = classes.iterator();

            while(var6.hasNext()) {
                Class<?> clazz = (Class)var6.next();
                Method[] methods = ReflectionUtils.getAllDeclaredMethods(clazz);
                Method[] var9 = methods;
                int var10 = methods.length;

                for(int var11 = 0; var11 < var10; ++var11) {
                    Method method = var9[var11];
                    if (introductionAwareMethodMatcher != null) {
                        if (introductionAwareMethodMatcher.matches(method, targetClass, hasIntroductions)) {
                            return true;
                        }
                    } else if (methodMatcher.matches(method, targetClass)) {
                        return true;
                    }
                }
            }

            return false;
        }
    }
}
```

这个方法也不复杂，其实就是检查目标类和方法上是否有缓存相关注解（@Cacheable,@CachePut,@CacheEvict等）。如果有，说明增强器对目标代理类适用，然后找到合适的增强器列表在APC中调用 createProxy 创建代理：

```java
// AbstractAutoProxyCreator 
protected Object createProxy(Class<?> beanClass, @Nullable String beanName, @Nullable Object[] specificInterceptors, TargetSource targetSource) {
    if (this.beanFactory instanceof ConfigurableListableBeanFactory) {
        AutoProxyUtils.exposeTargetClass((ConfigurableListableBeanFactory)this.beanFactory, beanName, beanClass);
    }

    ProxyFactory proxyFactory = new ProxyFactory();
    proxyFactory.copyFrom(this);
    if (!proxyFactory.isProxyTargetClass()) {
        if (this.shouldProxyTargetClass(beanClass, beanName)) {
            proxyFactory.setProxyTargetClass(true);
        } else {
            this.evaluateProxyInterfaces(beanClass, proxyFactory);
        }
    }

    Advisor[] advisors = this.buildAdvisors(beanName, specificInterceptors);
    proxyFactory.addAdvisors(advisors);
    proxyFactory.setTargetSource(targetSource);
    this.customizeProxyFactory(proxyFactory);
    proxyFactory.setFrozen(this.freezeProxy);
    if (this.advisorsPreFiltered()) {
        proxyFactory.setPreFiltered(true);
    }

    return proxyFactory.getProxy(this.getProxyClassLoader());
}
```

这里创建代理工厂，然后选择是否需要直接代理目标类，然后装配增强器，然后调用 JdkDynamicAopProxy 或者 CglibAopProxy 创建代理。



## BeanFactoryCacheOperationSourceAdvisor[^1]

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

# 缓存配置

引入依赖：

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-cache</artifactId>
    <version>2.1.3.RELEASE</version>
</dependency>
```

在应用启动类添加 @EnableCaching 注解：

```java
@SpringBootApplication
@EnableCaching
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
```

在业务方法添加 @Cacheable 注解：

```java
@Cacheable(cacheNames = {"task"})
public TaskInfoDTO getTask(String taskId) {
    log.info("TestBuzz.getTask mock query from DB......");
    TaskInfoDTO taskInfoDTO = new TaskInfoDTO();
    taskInfoDTO.setTaskId(taskId);
    taskInfoDTO.setApplicantId("system");
    taskInfoDTO.setDescription("test");
    return taskInfoDTO;
}
```

引入了缓存依赖、开启缓存能力就能直接使用缓存了，并没有引入或者配置其他的缓存组件。

为什么直接就能使用缓存？

如果应用架构基于 Spring 而不是 Springboot，那么肯定是要自己配置 CacheResolver 或者 CacheManager。为什么这里不需要？

这一切还是要归功于` spring-boot-autoconfigure`，我们使用 SpringBoot 作为基础框架时，一般都会显式或者间接把其引入：



spring-boot-autoconfigure 有个包叫 cache，毫无疑问，这里就是 SpringBoot <font color=red>定义并自动开启缓存配置</font>的地方。该包下基本都是`*Configuration`类型的类，也就是 SpringBoot 自带的缓存相关配置。

我们简单分析一下CacheAutoConfiguration、CacheConfigurations、GenericCacheConfiguration、NoOpCacheConfiguration、SimpleCacheConfiguration、CaffeineCacheConfiguration 和 RedisCacheConfiguration这几个配置类。 



## CacheAutoConfiguration

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
   
    @Bean
    @ConditionalOnMissingBean
    public CacheManagerCustomizers cacheManagerCustomizers(ObjectProvider<CacheManagerCustomizer<?>> customizers) {
        return new CacheManagerCustomizers((List)customizers.orderedStream().collect(Collectors.toList()));
    }

    @Bean
    public CacheAutoConfiguration.CacheManagerValidator cacheAutoConfigurationValidator(CacheProperties cacheProperties, ObjectProvider<CacheManager> cacheManager) {
        return new CacheAutoConfiguration.CacheManagerValidator(cacheProperties, cacheManager);
    }

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

这个类是 SpringBoot <font color=red>默认缓存配置的入口</font>，类名上有很多注解，限制了改配置的启动条件和装配规则等[^2]：

- `@ConditionalOnClass(CacheManager.class)`限制应用类路径中必须有 CacheManager 实现；
- `@ConditionalOnBean(CacheAspectSupport.class)`限制应用容器中必须有 CacheAspectSupport 或者子类实例；
- `@ConditionalOnMissingBean(value = CacheManager.class, name = "cacheResolver")`限制应用容器中不能有类型为 CacheManager，且名称为 cacheResolver 的bean，<font color=red>如果用户自定义了那么该配置就失效</font>；
- `@EnableConfigurationProperties(CacheProperties.class)`是表示启用缓存属性配置；
- `@AutoConfigureAfter`限制该类在 CouchbaseAutoConfiguration、HazelcastAutoConfiguration、HibernateJpaAutoConfiguration 和 RedisAutoConfiguration 之后配置；
- `@Import(CacheConfigurationImportSelector.class)`引入了内部定义的 CacheConfigurationImportSelector 配置。



### CacheConfigurationImportSelector

CacheAutoConfiguration 通过 @Import 机制引入 `CacheManagerCustomizers.class` 和 `CacheConfigurationImportSelector.class` 。

```java
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
```

该类会导入 CacheType 中定义的所有支持的缓存类型配置。

CacheConfigurationImportSelector 使用`CacheType.values()`作为 Key，遍历并创建将要加载的配置类全名的字符串数组。枚举 CacheType（cache 类型）的代码为：

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

CacheAutoConfiguration 中还定义了几个 bean，CacheManagerCustomizers 是 CacheManager 容器；CacheManagerValidator 在调用时检查 CacheManager 是否存在并给出自定义异常；CacheManagerJpaDependencyConfiguration 是对 CacheManager 依赖 Jpa 相关属性的定义和后置处理。

## CacheConfigurations

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

在项目中添加某个缓存管理组件（例如 Redis）后，Spring Boot 项目会选择并启用对应的缓存管理器。

如果项目中同时添加了==多个缓存组件==，且==没有指定缓存管理器或者缓存解析器==（CacheManager或者cacheResolver），那么Spring Boot 会按照上述顺序，在添加的多个缓存中优先启用指定的缓存组件进行缓存管理。

Spring Boot 默认缓存管理中，没有添加任何缓存管理组件能实现缓存管理。这是因为开启缓存管理后，Spring Boot 会按照上述列表顺序查找有效的缓存组件进行缓存管理，如果没有任何缓存组件，会默认使用Simple缓存组件进行管理。Simple 缓存组件是 Spring Boot 默认的缓存管理组件，它默认使用内存中的 ConcurrentMap 进行缓存存储，所以在没有添加任何第三方缓存组件的情况下，可以实现内存中的缓存管理，但是不推荐使用这种缓存管理方式。

<font color=red>当在 Spring Boot 默认缓存管理的基础上引入 Redis 缓存组件，即在 pom.xml 文件中添加 Spring Data Redis 依赖启动器后， SpringBoot 会使用 RedisCacheConfigratioin 当做生效的自动配置类进行缓存相关的自动装配，容器中使用的缓存管理器是 RedisCacheManager，这个缓存管理器创建的 Cache 为 RedisCache，进而操控 Redis 进行数据的缓存</font>[^3]。

## SimpleCacheConfiguration 

当我们==没有引入任何其他类库==，==没有配置 Cache bean== 并且==没有指定 spring.cache.type== 时，从上到下判断，GenericCacheConfiguration 不起作用（未定义Cache bean）。后续的一系列与第三方存储实现方案集成的配置类也不起作用（未引入相应类库），最后轮到 SimpleCacheConfiguration 符合条件起作用了。

此时使用 SimpleCacheConfiguration 来进行 SpringCache 的配置：

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

## RedisCacheConfiguration

```java
package org.springframework.boot.autoconfigure.cache;

@Configuration(
    proxyBeanMethods = false
)
@ConditionalOnClass({RedisConnectionFactory.class})
@AutoConfigureAfter({RedisAutoConfiguration.class})
@ConditionalOnBean({RedisConnectionFactory.class})
@ConditionalOnMissingBean({CacheManager.class})
@Conditional({CacheCondition.class})
class RedisCacheConfiguration {
    RedisCacheConfiguration() {
    }

    @Bean
    RedisCacheManager cacheManager(CacheProperties cacheProperties, CacheManagerCustomizers cacheManagerCustomizers, ObjectProvider<org.springframework.data.redis.cache.RedisCacheConfiguration> redisCacheConfiguration, ObjectProvider<RedisCacheManagerBuilderCustomizer> redisCacheManagerBuilderCustomizers, RedisConnectionFactory redisConnectionFactory, ResourceLoader resourceLoader) {
        RedisCacheManagerBuilder builder = RedisCacheManager.builder(redisConnectionFactory).cacheDefaults(this.determineConfiguration(cacheProperties, redisCacheConfiguration, resourceLoader.getClassLoader()));
        List<String> cacheNames = cacheProperties.getCacheNames();
        if (!cacheNames.isEmpty()) {
            builder.initialCacheNames(new LinkedHashSet(cacheNames));
        }

        redisCacheManagerBuilderCustomizers.orderedStream().forEach((customizer) -> {
            customizer.customize(builder);
        });
        return (RedisCacheManager)cacheManagerCustomizers.customize(builder.build());
    }

    private org.springframework.data.redis.cache.RedisCacheConfiguration determineConfiguration(CacheProperties cacheProperties, ObjectProvider<org.springframework.data.redis.cache.RedisCacheConfiguration> redisCacheConfiguration, ClassLoader classLoader) {
        return (org.springframework.data.redis.cache.RedisCacheConfiguration)redisCacheConfiguration.getIfAvailable(() -> {
            return this.createConfiguration(cacheProperties, classLoader);
        });
    }

    private org.springframework.data.redis.cache.RedisCacheConfiguration createConfiguration(CacheProperties cacheProperties, ClassLoader classLoader) {
        Redis redisProperties = cacheProperties.getRedis();
        org.springframework.data.redis.cache.RedisCacheConfiguration config = org.springframework.data.redis.cache.RedisCacheConfiguration.defaultCacheConfig();
        config = config.serializeValuesWith(SerializationPair.fromSerializer(new JdkSerializationRedisSerializer(classLoader)));
        if (redisProperties.getTimeToLive() != null) {
            config = config.entryTtl(redisProperties.getTimeToLive());
        }

        if (redisProperties.getKeyPrefix() != null) {
            config = config.prefixCacheNameWith(redisProperties.getKeyPrefix());
        }

        if (!redisProperties.isCacheNullValues()) {
            config = config.disableCachingNullValues();
        }

        if (!redisProperties.isUseKeyPrefix()) {
            config = config.disableKeyPrefix();
        }

        return config;
    }
}
```

RedisCacheConfiguration 注入了 RedisCacheManager 类型的 bean，该配置生效有几个条件：

- 只有应用引入了 redis 依赖，并且定义了 RedisConnectionFactory；
- 没有定义其他类型的 CacheManager；
- `spring.cache.type` 属性为 redis；
- 在 RedisAutoConfiguration 之后配置。

redis 类型的缓存配置稍微复杂，依赖了 RedisAutoConfiguration 配置：

```java
package org.springframework.boot.autoconfigure.data.redis;

@Configuration(
    proxyBeanMethods = false
)
@ConditionalOnClass({RedisOperations.class})
@EnableConfigurationProperties({RedisProperties.class})
@Import({LettuceConnectionConfiguration.class, JedisConnectionConfiguration.class})
public class RedisAutoConfiguration {
    public RedisAutoConfiguration() {
    }

    @Bean
    @ConditionalOnMissingBean(
        name = {"redisTemplate"}
    )
    public RedisTemplate<Object, Object> redisTemplate(RedisConnectionFactory redisConnectionFactory) throws UnknownHostException {
        RedisTemplate<Object, Object> template = new RedisTemplate();
        template.setConnectionFactory(redisConnectionFactory);
        return template;
    }

    @Bean
    @ConditionalOnMissingBean
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory redisConnectionFactory) throws UnknownHostException {
        StringRedisTemplate template = new StringRedisTemplate();
        template.setConnectionFactory(redisConnectionFactory);
        return template;
    }
}
```

RedisAutoConfiguration 依赖 redis，并且导入了 LettuceConnectionConfiguration 和 JedisConnectionConfiguration 连接配置，定义了 RedisTemplate 和 StringRedisTemplate 两个 bean 供 RedisCacheManager 使用。





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

[^1]: [SpringCache实现原理及核心业务逻辑（一）_不动明王1984的博客-CSDN博客_springcache](https://blog.csdn.net/m0_37962779/article/details/78671468)
[^2]: [Spring cache原理详解 - 掘金 (juejin.cn)](https://juejin.cn/post/6959002694539444231)
[^3]: [浅析SpringBoot缓存原理探究、SpringCache常用注解介绍及如何集成Redis](https://itcn.blog/p/1648146775684444.html)
[^4]: [spring cache原理——草丛里的码农](https://blog.csdn.net/wzl1369248650/article/details/95656093)
[Spring缓存基础设施介绍 | Java工匠 (czwer.github.io)](https://czwer.github.io/2018/06/02/Spring缓存基础设施介绍/)：重要
[Spring缓存管理原理 | Java工匠 (czwer.github.io)](https://czwer.github.io/2018/06/02/Spring缓存管理原理/)：重要
