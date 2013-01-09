# Core
`portal-core` module provides API for the the users to construct a portal application and SPI for bridge modules to run a portal application.

## Installing
Add the following dependency to your pom.xml:
```xml
<dependency>
    <groupId>com.github.flowersinthesand</groupId>
    <artifactId>portal-core</artifactId>
    <version>${portal.version}</version>
</dependency>
```

## Options
Options to be used to initialize an application is a plain map.

* `controllers`

A Set of controller class names.

* `packages`

A Set of package names which will be scanned for controllers.

* `base`

A base path for locations.

* `locations`

Paths of files, directories and jars containing controllers.

* `dispatcher`

A class name of the Dispatcher implementation. The default implementation is `DefaultDispatcher` which can be used in production.

* `socketManager`

A class name of the SocketManager implementation. The default implementation is `NoOpSocketManager` which does nothing and cannot be used in production.

## API
Package `com.github.flowersinthesand.portal`

### Application

The application consists of traditional controllers and a controller consists of event handlers. Because each controller is created as singleton, all declared fields must be thread-safe. Also, a controller class must have a default constructor.

#### @Handler
* `String value()`

A class-level annotation for a controller. The `value` element indicates a url of the socket and is regarded as an application name. One application can consist of multiple controller classes.

#### @Name
* `String value()`

A field-level annotation for injection of `App` and `Room`. According to the type of the field and the `value` element of the annotation, corresponding resource is injected regardless of the access modifier of the field. Field injection is done before execution of `@Prepare` methods.

#### @Prepare

A method-level annotation for preparation. Annotated methods are executed during initialization. Only public methods with no arguments can be executed.

### Event handler

The event handler is a plain method makred as an event handler. According to the method signature, corresponding parameters are provided.

#### @On
* `String value()`

A method-level and annotation-level annotation for marking a method or an annotation as an event handler. The `value` element is an event name. The access modifier of the method must be `public` and a return type of the method signifies nothing. `On.open`, `On.message`, `On.close` are special annotations for `open`, `message`, `close` event respectively.

#### Socket

If the Socket class is present on parameters in the method signature, the socket instance that sent the event will be passed.

#### @Data

A parameter-level annotation for specifying and converting the event data. If this annotation is present on parameters in the method signature, the event data will be converted to the declared parameter's type and provided. By default, [Jackson](http://wiki.fasterxml.com/JacksonHome) library is used to create an instance from a JSON string. Any object the client sent can be converted into the `Map<String, Object>` type.

#### @Reply

A parameter-level annotation for a reply callback for the client. The parameter's type must be `Fn.Callback` or `Fn.Callback1`.

### App

The App is the context where an application is run.

* `static App find()`

Returns the first application. Use this method only if there is a single application to avoid any ambiguousness.

* `static App find(String name)`

Finds an application which corresponds to the given name. Use finder functions when injection by the Name annotation is not available. These methods are static, however, app intances are created and configured during initialization. So, only if initialization is done, finder functions can work correctly.

* `String name()`

The application name.

* `Object get(String key)`

Returns a value bound with the specified key.

* `App set(String key, Object value)`

Binds a value to this application using the given key.

* `Room room(String name)`

Finds corresponding room or opens new one if it doesn't exist. Utilize this method when trying to access  rooms which are supposed to be created in runtime.

### Room

The Room is where a group of opened sockets are staying under a specific concept in the real world. Always use a room instead of a socket when handling an entity. For example, a single user can correspond to a single room, but it cannot correspond to a single socket, because multiple sockets from other browsers or devices may indicate the same user simultaneously.

* `String name()`

The room's name.

* `Object get(String key)`

Returns a value bound with the specified key.

* `Room set(String key, Object value)`

Binds a value to this room using the given key.

* `Room add(Socket socket)`

Adds a socket. Only opened socket can be added, and duplicate addition is not possible on account of the Set semantics.

* `Room remove(Socket socket)`

Removes a socket. If one of added sockets is closed, it will be removed from the room as well automatically.

* `Room send(String event)`
* `Room send(String event, Object data)`

Sends an event with data to all sockets in the room.

* `Room close()`

Closes all the connections of the sockets.

* `Set<Socket> sockets()`

Returns a cloned set of the sockets contained in the room. Modifications on the returned set don't affect the internal set of the original room.

* `int size()`

The number of sockets.

* `Room delete()`

Clears all sockets and attributes and deletes the room from the application.

### Socket

The Socket is a physical bidirectional connectivity between the client and the server.

* `boolean opened()`

Whether the socket is opened or closed.

* `String param(String key)`

Getter for the `params` option in the client.

* `Socket on(String event, Fn.Callback handler)`
* `Socket on(String event, Fn.Callback1<A> handler)`
* `Socket on(String event, Fn.Callback2<A, Fn.Callback> handler)`
* `Socket on(String event, Fn.Callback2<A, Fn.Callback1<B>> handler)`

Adds a event handler for a event type. Utilize these methods only when dynamic binding is more resonable than static binding for some reason. The event data is converted and passed to the handler according to the type variable `A`, if it exists. The second generic type from `Fn.Callback2<A, Fn.Callback>)` and `Fn.Callback2<A, Fn.Callback1<B>>)` corresponds to a reply callback.

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

#### Callback2\<A,B\>

* `void call(A arg1, B arg2)`

## SPI
Package `com.github.flowersinthesand.portal.spi`

### Dispatcher
TODO

### SocketManager
TODO