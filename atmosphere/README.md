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

### Adding context.xml
Copy the following into a file named `context.xml` in `META-INF` if the target server is Tomcat or in `WEB-INF` if the target server is JBoss. If the target server supports Servlet Specification 3.0, you don't need to add it. This is required by the Atmosphere.
```xml
<?xml version="1.0" encoding="UTF-8"?>
<Context>
    <Loader delegate="true"/>
</Context>
```

## Bootstrapping

### Servlet 3
The following types of beans are required. 
 * `javax.servlet.ServletContext`
 
ServletContextListener is a recommended entry point where the application starts. The AtmosphereServlet named `portal` will be created and added to the context automatically.

```java
@WebListener
public class PortalInitializer implements ServletContextListener {

    @Override
    public void contextInitialized(ServletContextEvent event) {
        new App().init("/event", new Options().beans(event.getServletContext())).register();
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {}

}
```

### Servlet 2.x
The following types of beans are required.
 * `javax.servlet.ServletContext`
 * `org.atmosphere.cpr.AtmosphereFramework`

You need to write a servlet extending AtmosphereServlet and declare it in web.xml. The app's initialization can be done in `void init(ServletConfig)` method only after calling the method of the super class.

```java
public class PortalInitializer extends AtmosphereServlet {
    
    @Override
    public void init(ServletConfig servletConfig) throws ServletException {
        super.init(servletConfig);
        new App().init("/event", new Options().beans(getServletContext(), framework)).register();
    }
    
}

```

In the servlet declaration, the url of the application to be integrated with the Atmosphere must be defined in the mapping and setting the `load-on-startup` to `0` is recommended for eager initialization.

```xml
<?xml version="1.0" encoding="UTF-8"?>
<web-app version="2.5" 
         xmlns="http://java.sun.com/xml/ns/javaee" 
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xmlns:web="http://java.sun.com/xml/ns/javaee/web-app_2_5.xsd"
         xsi:schemaLocation="http://java.sun.com/xml/ns/javaee http://java.sun.com/xml/ns/javaee/web-app_2_5.xsd">
    <servlet>
        <servlet-name>portal</servlet-name>
        <servlet-class>kr.ac.korea.eku.PortalInitializer</servlet-class>
        <load-on-startup>0</load-on-startup>
    </servlet>
    <servlet-mapping>
        <servlet-name>portal</servlet-name>
        <url-pattern>/event</url-pattern>
    </servlet-mapping>
</web-app>
```

## Options
The following options will override the default options of the core module.

* `base`

`servletContext.getRealPath("")` is used as the value, but it can be null in some cases. For example, the case where servlet container is Tomcat and unpackWARs option is set to false. Though you can still scan the class path by setting `packages` option. 

* `locations`

For convenience, scanning of resources is enabled by this. If the `base` is available, `/WEB-INF/classes` directory is added.

* `classes`

|Specification|Implementation
|:--|:--
|com.github.flowersinthesand.portal.spi.SocketManager|com.github.flowersinthesand.portal.atmosphere.AtmosphereSocketManager