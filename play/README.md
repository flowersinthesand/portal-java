# Play
`portal-play` module integrates the portal application with the [Play framework](http://www.playframework.org/) 2 which is a high productivity Java and Scala Web application framework.

## Installing
### Updating Build.scala
Add the following dependency to your Build.scala:
```scala
"com.github.flowersinthesand" % "portal-play" % "${portal.version}"
```

### Loading resources
The following static resources are located in `META-INF/resources/portal/` in the jar.

* [`play.js`](https://github.com/flowersinthesand/portal-java/blob/master/play/src/main/resources/META-INF/resources/portal/play.js), [`play.min.js`](https://github.com/flowersinthesand/portal-java/blob/master/play/src/main/resources/META-INF/resources/portal/play.min.js): A set of options to fully benefit from the integration with the play module.

Add new route for portal resources to the routes file.

```
GET     /portal/*file               controllers.PortalAssets.at(file)
```

Then, call the above reverse controller.

```scala
<script src="@routes.PortalAssets.at("portal.js")"></script>
<script src="@routes.PortalAssets.at("play.js")"></script>
```

### Creating the module
Use the following constructor:
```java
PlayModule()
```

`Global` is a recommended entry point where the application starts. For `Handlers.get` to confirm that an incoming request is for the portal application and route the request to the application, any portal application have to be registered to the default repository with its url, even though dependency injection framework is used. If an incoming request is not for the portal, `Handlers.get` returns null which is a default value of GlobalSettings.onRouteRequest, and further routing is processed as usual by the framework. You don't need to modify the `routes` file.

```java
public class Global extends GlobalSettings {

    @Override
    public void onStart(Application application) {
        new App(new Options().url("/chat").packageOf("controllers"), new PlayModule()).register();
    }

    @Override
    public Handler onRouteRequest(RequestHeader request) {
        return Handlers.get(request);
    }

}
```