# Spring
`portal-spring` module delegates bean creation to the [Spring framework](http://www.springsource.org/spring-framework). Therefore, Portal application can consist of Spring beans completely, and service or repository beans in Spring can be called by browser directly via the portal inversely. Spring bean is found by the name and the type of Portal bean, and if there is no matching bean, it will be created as usual.

## Installing
Add the following dependency to your pom.xml:
```xml
<dependency>
    <groupId>com.github.flowersinthesand</groupId>
    <artifactId>portal-spring</artifactId>
    <version>${portal.version}</version>
</dependency>
```

## Beans
 * `org.springframework.beans.factory.BeanFactory`

## Usage
### Definition
A bean to be managed by Spring and Portal should be declared as both Spring bean and Portal bean. Namely, such bean should be annotated with `@org.springframework.stereotype.Component` and `@com.github.flowersinthesand.portal.Bean` together.

```java
@Bean
@Component
public class TwitterHandler {}
```

### Scope
When there are a single Spring bean container and multiple Portal applications, if a bean will be used by only a single application, its scope can be singleton. For the rest, it must be defined as prototype. In other words, handler beans which are subordinate to an application can be **singleton**, but SPI beans which are instantiated for each application such as Dispatcher and SocketManager must be **prototype**.
