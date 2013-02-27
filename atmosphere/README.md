# Atmosphere
`portal-atmosphere` module integrates the portal application with the [Atmosphere framework](https://github.com/atmosphere/atmosphere/) which makes the application run on most servlet containers that support the Servlet Specification 2.3.

## Installing
### Adding dependency
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

you don't need to configure anything. According to the spec, every file in a `META-INF/resources` directory in a jar in `WEB-INF/lib` directory should be automatically exposed as a static resource.

```html
<script src="/portal/portal.js"></script>
<script src="/portal/atmosphere.js"></script>
```

## Gluing
To run an application in Servlet environment, define a `ServletContextListener` and create an `App` with the module created with the `ServletContext` in the `contextInitialized` method. The Atmosphere will be installed automatically per app.

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

### Configuring Atmosphere
You can configure Atmosphere by setting initialization parameters of AtmosphereServlet in the `modifyAtmosphereServletRegistration` method of the module. For more about options, [see this table](http://pastehtml.com/view/cgwfei5nu.html).

```java
new App(options, new AtmosphereModule(servletContext) {

    @Override
    protected void modifyAtmosphereServletRegistration(ServletRegistration.Dynamic registration) {
        registration.setInitParameter("key", "value");
    }
    
});
```


## Workaround for Servlet 2.x
### Loading resources
Install the fallback filter in web.xml:

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

### Gluing
Install Atmosphere by writing a servlet extending `AtmosphereServlet` and declaring it in web.xml. In this case, the module have to be created with `AtmosphereFramework`. 

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