# Vert.x
`portal-vertx` module integrates the portal application with the [Vert.x](http://vertx.io/) which is an event driven application framework.

## Installing
### Including module
Include the following module when running verticle or module:
```
com.github.flowersinthesand.portal-vertx-v${portal.version}
```

After executing `mvn package`, the module is generated as a zip file called `mod.zip` under the `target` folder.

### Loading resources
The following static resources are located in `META-INF/resources/portal/` in the jar.

* [`vertx.js`](https://github.com/flowersinthesand/portal-java/blob/master/vertx/src/main/resources/META-INF/resources/portal/vertx.js), [`vertx.min.js`](https://github.com/flowersinthesand/portal-java/blob/master/vertx/src/main/resources/META-INF/resources/portal/vertx.min.js): A set of options to fully benefit from the integration with the vertx module.

In the server where an application is installed, these resources will be exposed in the following location.

```html
<script src="/portal/portal.js"></script>
<script src="/portal/vertx.js"></script>
```

## Gluing
To run an application in Vert.x, define a `Verticle` and create an `App` with the module created with the `Vertx` and `HttpServer` in the `start` method.

```java
public class Initializer extends Verticle {

    @Override
    public void start() throws Exception {
        HttpServer httpServer = vertx.createHttpServer();
        httpServer.requestHandler(new Handler<HttpServerRequest>() {
            public void handle(HttpServerRequest req) {
                if (req.path.equals("/")) {
                    req.response.sendFile("web/index.html");
                }
            }
        });
        
        new App(new Options().url("/portal").packageOf(this), new VertxModule(vertx, httpServer));
        httpServer.listen(8080);
    }

}
```
