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

### Servlet container
Applications powered by the `portal-atmosphere` module requires a servlet container. By typing the following command, you can run the application on the [Jetty](http://www.eclipse.org/jetty/) that supports the Servlet Specification 3.0 and WebSocket. The Jetty starts on port `8080` by default.
```
mvn jetty:run-war
```

Then, open a browser and connect to `http://localhost:8080`.

### Play
Applications powered by the `portal-play` module requires the [Play](http://www.playframework.org/) 2. Because the whole project is managed by Maven, the Play application also can run by Maven. Note that the play should be executable on your `PATH`.
```
mvn package play2:run
```

If you want to run the application without Maven as usual, modify `Build.scala` according to comments on it and run the application as usual.
```
play run
```

Then, open a browser and connect to `http://localhost:9000`.

## Applications
### Chat for Atmosphere
A simple chat application using `Room` to broadcast message which can run on servlet container.

### Chat for Play
A simple chat application using `Room` to broadcast message which can run on the Play.

## External applications
### [Demos](http://ha-bio.rasc.ch/portal-demos/) by [Ralph](https://github.com/ralscha)
Source codes and online applications of chat, scheduler, twitter, snake, etc are available.