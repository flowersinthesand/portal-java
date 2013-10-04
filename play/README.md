# Play
`portal-play` module integrates the portal application with the [Play framework](http://www.playframework.org/) 2 which is a high productivity Java and Scala web application framework. The minimum supported version is `2.2.0`. 

## Installing
### Adding dependency
Add the following dependency to your Build.scala:
```scala
"com.github.flowersinthesand" % "portal-play" % "${portal.version}"
```

### Loading resources
The following static resources are located in `META-INF/resources/portal/` in the jar.

* [`play.js`](https://github.com/flowersinthesand/portal-java/blob/master/play/src/main/resources/META-INF/resources/portal/play.js), [`play.min.js`](https://github.com/flowersinthesand/portal-java/blob/master/play/src/main/resources/META-INF/resources/portal/play.min.js): A set of options to fully benefit from the integration with the play module.

Add new route for portal resources to the routes file.

```
GET     /portal/*file               com.github.flowersinthesand.portal.play.Assets.at(file)
```

Then, call the above reverse controller.

```scala
<script src="@com.github.flowersinthesand.portal.play.routes.Assets.at("portal.js")"></script>
<script src="@com.github.flowersinthesand.portal.play.routes.Assets.at("play.js")"></script>
```

## Gluing
To run an application in Play framework, define the `Global` and create an `App` with the module in the `onStart` method. Also, let `Handlers` intercept an incoming request. It will return a proper handler if an incoming request is for the portal automatically.

```java
public class Global extends GlobalSettings {

    private App app;

    @Override
    public void onStart(Application application) {
        app = new App(new Options().url("/chat").packageOf("controllers"), new PlayModule());
    }

    @Override
    public void onStop(Application application) {
        app.close();
    }
    
    @Override
    public Handler onRouteRequest(RequestHeader request) {
        return Handlers.get(request);
    }

}
```
