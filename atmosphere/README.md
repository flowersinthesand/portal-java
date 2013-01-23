# Atmosphere
`portal-atmosphere` module integrates the portal application with the [Atmosphere framework](https://github.com/atmosphere/atmosphere/) which makes the application run on most servlet containers that support the Servlet Specification 2.3.

## Installing
Add the following dependency to your pom.xml:
```xml
<dependency>
    <groupId>com.github.flowersinthesand</groupId>
    <artifactId>portal-atmosphere</artifactId>
    <version>${portal.version}</version>
</dependency>
```

## Beans
 * `org.atmosphere.cpr.AtmosphereFramework`

## Bootstrapping

### Servlet 3
Thanks to dynamic registration support in Servlet 3, you don't need to install the Atmosphere and find AtmosphereFramework manually. Instead, only `javax.servlet.ServletContext` is required. The Atmosphere will be installed automatically per app. In this case, ServletContextListener is a recommended entry point where the application starts. 

```java
@WebListener
public class Initializer implements ServletContextListener {

    @Override
    public void contextInitialized(ServletContextEvent event) {
        ServletContext servletContext = event.getServletContext();
        new App(new Options().url("/event").packages("ch.rasc.portaldemos").beans(servletContext)).register();
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {}

}
```

### Servlet 2.x
To install Atmosphere and initialize application, you have to write a servlet extending AtmosphereServlet and declare it in web.xml. The initialization can be done in `void init(ServletConfig)` method only after calling the method of the super class.

```java
public class Initializer extends AtmosphereServlet {
    
    @Override
    public void init(ServletConfig servletConfig) throws ServletException {
        super.init(servletConfig);
        new App(new Options().url("/event").packages("ch.rasc.portaldemos").beans(framework)).register();
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