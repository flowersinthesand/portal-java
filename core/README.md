# Core
`portal-core` module provides API for the the users to construct a portal application and SPI for bridge modules to run a portal application on their environment.

Requires Java 6.

## Installing
### Updating pom.xml
Add the following dependency:
```xml
<dependency>
    <groupId>com.github.flowersinthesand</groupId>
    <artifactId>portal-core</artifactId>
    <version>${portal.version}</version>
</dependency>
```

### Running the app
Simply, create a `App` with options and modules:
```java
new App(new Options().url("/event").packageOf("ch.rasc.portaldemos"), new AtmosphereModule(servletContext));
```

## Application
A Portal application is a small bean container which contains beans defined by the user and beans used by the application. Here, the user can define a bean as a controller, service or repository, but the expected use case is to write a handler as a component of presentation layer.

For a declarative programming, a lot of annotations are provided and defined in the package `com.github.flowersinthesand.portal`.

## Bean
Bean is an application component and is instantiated once per each application like singleton. Therefore, all declared fields on the bean must be thread-safe.

### @Bean
* `String value() default ""`

Indicates that the annotated class is a bean. This annotation is required for all beans. The `value` element defines the bean name. If it is not provided, the bean name will be the class's name.

### @Wire
* `String value() default ""`

Marks the annotated field as to be wired. The `value` element indicates a bean name. If it exists, the container will find a bean by the name. Otherwise, the container will find a bean by the type of the field. The field does not need to be public.

Exceptionally, if the field type is `Room`, it will be wired regarding the value as the room name, even though it is not bean. If the corresponding room does not exist, it will be opened first before being wired.    

### @Prepare
Specifies that the annotated method should be be executed after dependency injection is done to perform any initialization. Only public methods with no arguments can be executed.

## Event handler
Any method annotated with `On` on any bean in the application is treated as event handler. According to the method signature, the following parameters will be provided.

* `Socket`: The socket instance that sent the event.

### @On
* `String value()`

Defines an annotated method or an annotated annotation as the event handler. The `value` element is an event name. The method should be `public` and a return type of the method doesn't matter. `On.open`, `On.message`, `On.close` are provided as special annotations for `open`, `message`, `close` event respectively.

### @Data
Specifies that the event data will be converted to the annotated parameter's type and set to the annotated parameter. By default, [Jackson](http://wiki.fasterxml.com/JacksonHome) library is used to create an instance from a JSON string. Any object the client sent can be converted into the `Map<String, Object>` type.

### @Reply
Specifies that the annotated parameter is a reply callback. The parameter's type should be `Fn.Callback` or `Fn.Callback1` and the method's return type should be `void`. If the reply is requested and the method's return type is not void, the result is used as the callback data, and the callback is executed accordingly. So, use this way when you need to execute the callback asynchronously.

## Model 

### App

The App is the standalone application context.

* `static App find()`

Returns the first application from the default repository. Use this method only if there is a single application to avoid any ambiguousness.

* `static App find(String name)`

Finds an application which corresponds to the given name from the default repository. These methods are static, however, app intances are initialized and configured during runtime. So, only if each app's initialization is done, finder functions can work correctly.

* `App(Options options, Modules... modules)`

Creates a new application with modules. 

* `String name()`

The application name.

* `Object get(String key)`

Returns a value bound with the specified key.

* `App set(String key, Object value)`

Binds a value to this application using the given key.

* `Room room(String name)`

Finds corresponding room or opens new one if it doesn't exist. Utilize this method when trying to access rooms which are not available by injection, namely, are supposed to be created in runtime.

* `App fire(String event, Socket socket)`
* `App fire(String event, Socket socket, Object data)`
* `App fire(String event, Socket socket, Object data, Fn.Callback1<Object> reply)`

Fires the given event to the given socket with data and reply callback.

* `Object bean(String name)`
* `<T> T bean(Class<? super T> class)`

Returns the corresponding bean by name or type from the bean container.

* `App register()`

Registers the application to the default repository. Then, the app can be retrieved by `App.find(String name)`. However, injection by container like the Spring or the Guice is more preferred than calling this method and static methods.

### Options

* `String url()`
* `Options url(String url)`

The mapping url. This is required.

* `String name()`
* `Options name(String name)`

The application name. If it is null, the url will be returned instead.

* `Set<String> packages()`
* `Options packageOf(String... packages)`
* `Options packageOf(Class<?>... classes)`
* `Options packageOf(Object... objects)`

Package names that given classes and objects belong to which will be scanned for beans. Actually required.

### Room

The Room is where a group of opened sockets are staying under a specific concept in the real world. Always use a room instead of a socket when handling an entity. For example, a single user can correspond to a single room, but it cannot correspond to a single socket, because multiple sockets from other browsers or devices may indicate the same user simultaneously.

* `String name()`

The room's name.

* `Object get(String key)`

Returns a value bound with the specified key.

* `Room set(String key, Object value)`

Binds a value to this room using the given key.

* `Room add(Socket socket)`
* `Room add(Room room)`

Adds a socket or sockets in the given room. Only opened socket can be added, and duplicate addition is not possible on account of the Set semantics.

* `Room remove(Socket socket)`
* `Room remove(Room room)`

Removes a socket or sockets in the given room. If one of added sockets is closed, it will be removed from the room as well automatically. Use this method to exclude opened socket.

* `Room send(String event)`
* `Room send(String event, Object data)`

Sends an event with data to all sockets in the room.

* `Set<Socket> sockets()`

Returns a cloned set of the sockets contained in the room. Modification on the returned set is not allowed.

* `int size()`

The number of sockets.

* `Room close()`

Closes all the connections of the sockets, clears all sockets and attributes and deletes the room from the application.

### Socket

The Socket is a physical bidirectional connectivity between the client and the server and also where the event occurs.

* `boolean opened()`

Whether the socket is opened or closed.

* `String param(String key)`

Getter for the `params` option in the client.

* `Socket send(String event)`
* `Socket send(String event, Object data)`
* `Socket send(String event, Object data, Fn.Callback callback)`
* `Socket send(String event, Object data, Fn.Callback1<A> callback)`

Sends an event with data and attaches a callback that takes data which will be sent by the client.

* `void close()`

Closes a connection.

### Fn

Amorphous functions.

#### Callback

* `void call()`

#### Callback1\<A\>

* `void call(A arg1)`