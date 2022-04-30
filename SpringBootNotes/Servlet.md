# Servlet

## Servlet 生命周期[^2]

Servlet 生命周期可被定义为从创建直到毁灭的整个过程。以下是 Servlet 遵循的过程：

- Servlet 初始化后调用 **init ()** 方法。
- Servlet 调用 **service()** 方法来处理客户端的请求。
- Servlet 销毁前调用 **destroy()** 方法。
- 最后，Servlet 是由 JVM 的垃圾回收器进行垃圾回收的。

servlet 生命周期图：[^4]

## HttpServlet

## 响应流程[^1]

1、Web 客户向 Servlet 容器发出 Http 请求；
2、Servlet 容器解析 Web 客户的 Http 请求；
3、Servlet 容器创建一个 HttpRequest 对象，在这个对象中封装 Http 请求信息；
4、Servlet 容器创建一个 HttpResponse 对象；
5、==Servlet 容器（如果访问的该 servlet 不是在服务器启动时创建的，则先创建 servlet 实例并调用 init() 方法初始化对象）调用 HttpServlet 的 service() 方法，把 HttpRequest 和 HttpResponse 对象作为 service 方法的参数传给 HttpServlet 对象==；
6、HttpServlet 调用 HttpRequest 的有关方法，==获取 HTTP 请求信息==；
7、HttpServlet 调用 HttpResponse 的有关方法，==生成响应数据==；
8、Servlet 容器把 HttpServlet 的响应结果传给 Web 客户。

其中 HttpServlet 首先必须读取 Http 请求的内容，Servlet 容器负责创建 HttpServlet 对象，并把 Http 请求直接封装到 HttpServlet 对象中。

## 使用

### 重写 service[^5]

从源码可以看出，这里的 service 是用来==转向==的。但是如果你在自己的 servlet 类中覆盖了 service 方法，这时 service 就不是用来转向的，而是用来==处理业务==的。现在不论你的客户端是用 post 还是 get 来请求，==此 servlet 都会执行 service() 方法，也只能执行 service() 方法，不会去执行 doPost 或是 doGet 方法==。

```java
public class MyServlet extends HttpServlet {
    @Override
    public void service(ServletRequest req, ServletResponse res) throws ServletException, IOException {
        System.out.println("test.......");
    }
}

@Configuration
public class MyServletConfiguration {
    @Bean
    public ServletRegistrationBean servletRegistrationBean() {
        return new ServletRegistrationBean(new MyServlet(), "/cache");
    }
}
```



# HttpClient



# 参考资料

[^1]: [HttpServlet-此情有憾、然无对错的博客-CSDN博客](https://blog.csdn.net/qq_41007534/article/details/99696559)
[^2]: [Servlet 教程 | 菜鸟教程 (runoob.com)](https://www.runoob.com/servlet/servlet-tutorial.html)
[^3]: [Java servlet执行的完整流程（图解含源码分析）-阿顾同学的博客-CSDN博客](https://blog.csdn.net/u010452388/article/details/80395679)
[^4]: [servlet的执行原理与生命周期-逍遥不羁的博客-CSDN博客](https://blog.csdn.net/javaloveiphone/article/details/8154791)
[^5]: [Servlet - service 方法重写问题-放羊的牧码的博客-CSDN博客](https://blog.csdn.net/Dream_Weave/article/details/83012714)
[^6]: [servlet service()请求处理方法详解 (51gjie.com)](http://www.51gjie.com/javaweb/851.html)
[^7]: [springboot如何把HttpServletRequest传入到controller - 简书 (jianshu.com)](https://www.jianshu.com/p/a37aa4c0295e)

[SpringBoot 是如何请求到对应Controller的@RequestMapping方法的？-LLLDa_&的博客-CSDN博客](https://blog.csdn.net/Ming_5257/article/details/124352220)

[^8]: [SpringBoot中使用拦截器、servlet、过滤器Filter_北海冥鱼未眠的博客-CSDN博客](https://blog.csdn.net/qq_45401910/article/details/122747746)
[^9]: [springboot中使用Servlet - 知乎 (zhihu.com)](https://zhuanlan.zhihu.com/p/166430516)













