# Spring
`portal-spring` module delegates bean creation to the [Spring framework](http://www.springsource.org/spring-framework). Therefore, Portal application can consist of Spring beans completely, and service or repository beans in Spring can be called by browser directly via the portal inversely. Spring bean is found by the name and the type of Portal bean, and if there is no matching bean, it will be created as usual.

## Installing
### Updating pom.xml
Add the following dependency to your pom.xml:
```xml
<dependency>
    <groupId>com.github.flowersinthesand</groupId>
    <artifactId>portal-spring</artifactId>
    <version>${portal.version}</version>
</dependency>
```

### Creating the module
Use the following constructor:
```java
SpringModule(BeanFactory)
```

## Usage
### Declaration
A bean to be managed by Spring and Portal should be declared as both Spring bean and Portal bean. Namely, such bean should be annotated with `@org.springframework.stereotype.Component` and `@com.github.flowersinthesand.portal.Bean` together.

```java
@Bean
@Component
public class AccountHandler {

    @Autowired
    private AccountService service;
    
    @On("get")
    public Account get(@Data long id) {
        return service.get(id);
    }

}
```

### Scope
When there are a single Spring bean container and multiple Portal applications, if a bean will be used by only a single application, its scope can be singleton. For the rest, it must be defined as prototype. In other words, handler beans which are subordinate to an application can be **singleton**, but SPI beans which are instantiated for each application such as Dispatcher and SocketManager must be **prototype**.

### Injection
Since the `App` is also bean, it can be managed and injected by the Spring. Here, you don't need to call the register method. Configuration class is like this:

```java
@Configuration
public class PortalConfig {

    @Autowired
    private BeanFactory beanFactory;

    @Bean
    public App app() {
        return new App(new Options().url("/chat").packageOf(AccountHandler.class), new SpringModule(beanFactory));
    }

}
```

Consequently, the app can be wired to any bean in container gracefully without calling static methods.

```java
@Service
public class TwitterReader {

    @Autowired
    private App app;

    @Scheduled(fixedDelay = 20000)
    public void read() throws TwitterException {
        Twitter twitter = TwitterFactory.getSingleton();
        Room room = app.room("twitter");
        
        Query query = new Query("java");
        QueryResult result = twitter.search(query);
        room.send("tweets", result.getTweets());
    }

}
```