# Guice
`portal-guice` module delegates bean creation to the [Guice](http://code.google.com/p/google-guice/). Therefore, Portal application can consist of Guice beans completely, and service or repository beans in Guice can be called by browser directly via the portal inversely. Guice bean is found by only the type of Portal bean, and if there is no matching bean, it will be created as usual.

## Installing
### Updating pom.xml
Add the following dependency to your pom.xml:
```xml
<dependency>
    <groupId>com.github.flowersinthesand</groupId>
    <artifactId>portal-guice</artifactId>
    <version>${portal.version}</version>
</dependency>
```
### Creating the module
Use the following constructor:
```java
GuiceModule(Injector)
```

## Usage
### Declaration
A bean to be managed by Guice and Portal should be declared as both Guice bean and Portal bean.

```java
@Bean
public class AccountHandler {

    @Inject
    private AccountService service;
    
    @On("get")
    public Account get(@Data long id) {
        return service.get(id);
    }

}
```

### Scope
When there are a single Guice bean container and multiple Portal applications, if a bean will be used by only a single application, its scope can be singleton. For the rest, it must be defined as prototype. In other words, handler beans which are subordinate to an application can be **singleton**, but SPI beans which are instantiated for each application such as Dispatcher and SocketManager must be **prototype**.

### Injection
Since the `App` is also bean, it can be managed and injected by the Guice. Here, you don't need to call the register method. Module is like this:

```java
public class PortalModule extends AbstractModule {

    @Override
    public void configure() {}

    @Provides
    @Singleton
    App app(Injector injector) {
        return new App(new Options().url("/chat").packageOf(AccountHandler.class), new GuiceModule(injector));
    }
    
}
```

Consequently, the app can be wired to any bean in container gracefully without calling static methods.

```java
public class AccountServiceImpl implements AccountService {

    @Inject
    private AccountDao dao;
    @Inject
    private App app;
    
    @Override
    public void delete(long id) {
        dao.delete(id);
        app.room("account").send("account.delete", id);
    }

}
```