# Vert.x
`portal-vertx` module integrates the portal application with the [Vert.x](http://vertx.io/) which is an event driven application framework.

## Installing
### Including module
Include the following module in a basic manner:
```
com.github.flowersinthesand~portal-vertx~${portal.version}
```

Or if you are using the Vert.x Maven archetype, add the following dependency to your pom.xml:
```xml
<dependency>
    <groupId>com.github.flowersinthesand</groupId>
    <artifactId>portal-vertx</artifactId>
    <version>${portal.version}</version>
</dependency>
```

### Loading resources
The following static resources are located in `META-INF/resources/portal/` in the jar.

* [`vertx.js`](https://github.com/flowersinthesand/portal-java/blob/master/vertx/src/main/resources/META-INF/resources/portal/vertx.js), [`vertx.min.js`](https://github.com/flowersinthesand/portal-java/blob/master/vertx/src/main/resources/META-INF/resources/portal/vertx.min.js): A set of options to fully benefit from the integration with the vertx module.

In the server where an application is installed, these resources will be exposed in the following location.

```html
<script src="/portal/portal.js"></script>
<script src="/portal/vertx.js"></script>
```

## Gluing
To run an application in Vert.x, define a `Verticle` and create an `App` with the module created with `HttpServer` in the `start` method. You should call `HttpServer`'s `listen` method explicitly.

```java
public class Initializer extends Verticle {

    @Override
    public void start() {
        HttpServer httpServer = vertx.createHttpServer();
        httpServer.requestHandler(new Handler<HttpServerRequest>() {
            public void handle(HttpServerRequest req) {
                if (req.path().equals("/")) {
                    req.response().sendFile("webapp/index.html");
                }
            }
        });
        
        new App(new Options().url("/portal").packageOf(this), new VertxModule(httpServer));
        httpServer.listen(8080);
    }

}
```
