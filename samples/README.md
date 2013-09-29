# Samples
You need to install the [Maven](http://maven.apache.org/).

## Running
Clone or download the repository. You don't need to install the Git to run applications.
```
git clone git://github.com/flowersinthesand/portal-java.git
```

In the `samples` directory, there are several samples applications.
```
cd portal-java/samples/chat-atmosphere
```

The way to run an application varies with runtime environment, of course. However, because core API has nothing to do with runtime environment, you can switch runtime environment with no modifications on the application unless the application use extended API depending on runtime environment.

### Atmosphere
Applications powered by the `portal-atmosphere` module requires a servlet container. By typing the following command, you can run the application on the [Jetty](http://www.eclipse.org/jetty/).
```
mvn jetty:run-war
```

Or, you can use the [Apache Tomcat](http://tomcat.apache.org/).
```
mvn tomcat7:run
```

Then, open a browser and connect to `http://localhost:8080`.

### Play
Applications powered by the `portal-play` module requires the [Play](http://www.playframework.org/) 2. Play applications also can run by Maven by the help of the [play2-maven-plugin](https://github.com/cescoffier/maven-play2-plugin). Note that the `play` should be executable on your `PATH`.
```
mvn package play2:run
```

If you want to run the application without Maven as usual, modify `Build.scala` according to comments on it and run the application as usual.
```
play run
```

Then, open a browser and connect to `http://localhost:9000`.

### Vert.x
Applications powered by the `portal-vertx` module requires the [Vert.x](http://vertx.io/). Vert.x applications also can run by Maven by the help of [vertx-maven-plugin](https://github.com/rhart/vertx-maven-plugin/). Note that the `vertx` should be executable on your `PATH`.
```
mvn vertx:run
```

If you want to run the application without Maven as usual, execute the following commands, replacing `${portal.version}` with a real version you want. However, these commands differ on a case by case.

```
mvn compile
mkdir -p temp/src/main/webapp
cp -r src/main/webapp temp/src/main/webapp
cd temp
vertx run samples.Server -includes com.github.flowersinthesand.portal-vertx-v${portal.version}
```

Then, open a browser and connect to `http://localhost:8080`.

## External applications
### [Demos](https://demo.rasc.ch/portal/) by [Ralph](https://github.com/ralscha)
Source codes and online applications of chat, scheduler, twitter, snake, etc are available.