# 总结

## Autowire 和 @Resource 的区别

1. `@Autowire` 和 `@Resource`都可以用来装配bean，都可以用于字段或setter方法。
2. `@Autowire` 默认==按类型装配==，默认情况下<font color=red>必须要求依赖对象必须存在</font>，如果要==允许 null 值==，可以设置它的 required 属性为 false。
3. `@Resource` <font color=red>默认==按名称装配==，当找不到与名称匹配的 bean 时才按照类型进行装配</font>。名称可以通过 name 属性指定，如果没有指定 name 属性，当注解写在字段上时，默认取字段名，当注解写在 setter 方法上时，默认取属性名进行装配。

注意：如果 name 属性一旦指定，就只会按照名称进行装配。

`@Autowire`和`@Qualifier`配合使用效果和`@Resource`一样：

```java
@Autowired(required = false) 
@Qualifier("example")
private Example example;

@Resource(name = "example")
private Example example;
```

`@Resource` 装配顺序

1. 如果同时指定 name 和 type，则从容器中查找唯一匹配的 bean 装配，找不到则抛出异常；
2. 如果指定 name 属性，则从容器中查找名称匹配的 bean 装配，找不到则抛出异常；
3. 如果指定 type 属性，则从容器中查找类型唯一匹配的 bean 装配，找不到或者找到多个抛出异常；
4. 如果不指定，则自动按照 byName 方式装配，如果没有匹配，则回退一个原始类型进行匹配，如果匹配则自动装配。



# 参考资料

https://blog.csdn.net/weixin_35544490/article/details/112143211