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

### Extending InitializerListener
The only thing left is to declare the [InitializerServlet](https://github.com/flowersinthesand/portal-java/blob/master/atmosphere/src/main/java/com/github/flowersinthesand/portal/atmosphere/InitializerServlet.java). However, if your servlet container supports the Servlet Specificiation 3.0, the [InitializerListener](https://github.com/flowersinthesand/portal-java/blob/master/atmosphere/src/main/java/com/github/flowersinthesand/portal/atmosphere/InitializerListener.java) can do that for you.

```java
@WebListener
public class PortalListener extends InitializerListener {}
```

### Declaring InitializerServlet
In the declaration, the url of portal applications to be integrated with the Atmosphere must be defined in the servlet mapping and setting the `load-on-startup` to `0` is recommended for eager initialization. A portal application can't declare more than one InitializerServlet. If you want to add a new portal application, add new mapping to the existing declaration.

```xml
<?xml version="1.0" encoding="UTF-8"?>
<web-app version="2.5" 
         xmlns="http://java.sun.com/xml/ns/javaee" 
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xmlns:web="http://java.sun.com/xml/ns/javaee/web-app_2_5.xsd"
         xsi:schemaLocation="http://java.sun.com/xml/ns/javaee http://java.sun.com/xml/ns/javaee/web-app_2_5.xsd">
    <servlet>
        <servlet-name>portal</servlet-name>
        <servlet-class>com.github.flowersinthesand.portal.atmosphere.InitializerServlet</servlet-class>
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

`getServletContext().getRealPath("")` is used as the value, but it can be null in some cases. For example, the case where servlet container is Tomcat and unpackWARs option is set to false. Though you can still scan the class path by setting `packages` option. 

* `locations`

For convenience, scanning of resources is enabled by this. If the `base` is available, `/WEB-INF/classes` directory is added.

* `classes`

|Specification|Implementation
|:--|:--
|com.github.flowersinthesand.portal.spi.SocketManager|com.github.flowersinthesand.portal.atmosphere.AtmosphereSocketManager

## Configuring
### Using class
Both InitializerListener and InitializerServlet have `configure(Options options)` which can manipulate the module default options. In InitializerListener, the portal servlet definition can be accessed and modified by overriding `configure(ServletRegistration)` as well. 

```java
@WebListener
public class PortalListener extends InitializerListener {

    @Override
    protected void configure(Options options) {
        options.packages("ch.rasc.portaldemos");
    }

}
```

### Using web.xml
The context parameter `portal.options` which is JSON representing Options instance is used to override the module default options.

```xml
<context-param>
    <param-name>portal.options</param-name>
    <param-value>
    {
        "locations": ["/WEB-INF/classes", "/WEB-INF/lib/myapp.jar"],
        "classes": {
            "com.github.flowersinthesand.portal.spi.SocketManager": "kr.ac.korea.MySecretSocketManager"
        }
    }
    </param-value>
</context-param>
```

### Atmosphere options
Since InitializerServlet extends AtmosphereServlet, Atmosphere's various options are still available. To see what can be configured and how can it be done, see the [document](http://pastehtml.com/view/cgwfei5nu.html)
