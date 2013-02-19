# Atmosphere
`portal-atmosphere` module integrates the portal application with the [Atmosphere framework](https://github.com/atmosphere/atmosphere/) which makes the application run on most servlet containers that support the Servlet Specification 2.3.

## Installing
### Updating pom.xml
Add the following dependency to your pom.xml:
```xml
<dependency>
    <groupId>com.github.flowersinthesand</groupId>
    <artifactId>portal-atmosphere</artifactId>
    <version>${portal.version}</version>
</dependency>
```

### Loading resources
The following static resources are located in `META-INF/resources/portal/` in the jar.

* [`atmosphere.js`](https://github.com/flowersinthesand/portal-java/blob/master/atmosphere/src/main/resources/META-INF/resources/portal/atmosphere.js), [`atmosphere.min.js`](https://github.com/flowersinthesand/portal-java/blob/master/atmosphere/src/main/resources/META-INF/resources/portal/atmosphere.min.js): A set of options to fully benefit from the integration with the atmosphere module.

#### Servlet 3
According to Servlet 3 spec, every file in a `META-INF/resources` directory in a jar in `WEB-INF/lib` directory should be automatically exposed as a static resource. So, you don't need to configure anything.

```html
<script src="/portal/portal.js"></script>
<script src="/portal/atmosphere.js"></script>
```

#### Servlet 2.x
Install the following fallback filter in web.xml:

```xml
<filter>
    <filter-name>portalResource</filter-name>
    <filter-class>com.github.flowersinthesand.portal.atmosphere.StaticResourceFilter</filter-class>
</filter>
<filter-mapping>
    <filter-name>portalResource</filter-name>
    <url-pattern>/*</url-pattern>
</filter-mapping>
```

Then, you can import scripts like with Servlet 3.

```html
<script src="/portal/portal.js"></script>
<script src="/portal/atmosphere.js"></script>
```

### Creating the module
Use the following constructor in accordance with your servlet container

#### Servlet 3
```java
AtmosphereModule(ServletContext)
```
Thanks to dynamic registration support in Servlet 3, you don't need to install the Atmosphere and find `AtmosphereFramework` manually. Instead, only `ServletContext` is required. The Atmosphere will be installed automatically per app. In this case, `ServletContextListener` is a recommended entry point where the application starts. 

```java
@WebListener
public class Initializer implements ServletContextListener {

    private App app;

    @Override
    public void contextInitialized(ServletContextEvent event) {
        app = new App(new Options().url("/event").packageOf(this), new AtmosphereModule(event.getServletContext()));
    }

    @Override
    public void contextDestroyed(ServletContextEvent event) {
        app.close();
    }

}
```

You can access a registration object for AtmosphereServlet through `modifyAtmosphereServletRegistration` method of the module.
```java
new App(new Options().url("/event").packageOf(this), new AtmosphereModule(servletContext) {

    @Override
    protected void modifyAtmosphereServletRegistration(ServletRegistration.Dynamic registration) {
        registration.setInitParameter("key", "value");
    }
    
});
```

#### Servlet 2.x
```java
AtmosphereModule(AtmosphereFramework)
```

To install Atmosphere and initialize application, you have to write a servlet extending `AtmosphereServlet` and declare it in web.xml. The initialization can be done in `void init(ServletConfig)` method only after calling the method of the super class.

```java
public class Initializer extends AtmosphereServlet {

    private App app;
    
    @Override
    public void init(ServletConfig servletConfig) throws ServletException {
        super.init(servletConfig);
        app = new App(new Options().url("/event").packageOf(this), new AtmosphereModule(framework));
    }
    
    @Override
    public void destroy() {
        super.destroy();
        app.close();
    }    
    
}

```

In the servlet declaration, the url of the applications to be integrated with the Atmosphere must be defined in the mapping and setting the `load-on-startup` to `0` is recommended for eager initialization.

```xml
<?xml version="1.0" encoding="UTF-8"?>
<web-app version="2.5" 
         xmlns="http://java.sun.com/xml/ns/javaee" 
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xmlns:web="http://java.sun.com/xml/ns/javaee/web-app_2_5.xsd"
         xsi:schemaLocation="http://java.sun.com/xml/ns/javaee http://java.sun.com/xml/ns/javaee/web-app_2_5.xsd">
    <servlet>
        <servlet-name>portal</servlet-name>
        <servlet-class>ch.rasc.portaldemos.twitter.Initializer</servlet-class>
        <load-on-startup>0</load-on-startup>
    </servlet>
    <servlet-mapping>
        <servlet-name>portal</servlet-name>
        <url-pattern>/event</url-pattern>
    </servlet-mapping>
</web-app>
```