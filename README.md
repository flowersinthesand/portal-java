# Portal for Java
The **Portal for Java** is a reference implementation written in Java of the server counterpart of the [**Portal**](https://github.com/flowersinthesand/portal) project which provides useful semantics and concepts for modern web application development in the server side as well as server implementation.

## Installing
Add the following dependency to your pom.xml:
```xml
<dependency>
    <groupId>com.github.flowersinthesand</groupId>
    <artifactId>portal-core</artifactId>
    <version>${portal.version}</version>
</dependency>
```

Now you can write a complete portal application, but you need to choose runtime enviornment where your application will be run such as servlet container or stand-alone server and a corresponding module for the integration.

* [atmosphere](https://github.com/flowersinthesand/portal-java/tree/master/atmosphere/README.md) for servlet container

## References
* [API](https://github.com/flowersinthesand/portal-java/wiki/API)
* [Samples](https://github.com/flowersinthesand/portal-java/tree/master/samples/README.md)