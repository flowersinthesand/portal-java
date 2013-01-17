# Core
`portal-core` module provides API for the the users to construct a portal application and SPI for bridge modules to run a portal application on their environment.

## Installing
Add the following dependency to your pom.xml:
```xml
<dependency>
    <groupId>com.github.flowersinthesand</groupId>
    <artifactId>portal-core</artifactId>
    <version>${portal.version}</version>
</dependency>
```

Bootstrapping an application.
```java
new App().init("/events").register();
```

## Options

* `Set<Class<?>> handlers()`
* `Options handlers(Class<?>... classes)`

Handler classes.

* `Set<String> packages()`
* `Options packages(String... packageNames)`

Package names which will be scanned for handlers including sub packages.

* `String base()`
* `Options base(String base)`

A base path for locations.

* `Set<String> locations()`
* `Options locations(String... paths)`

Paths of files, directories and jars which will be scanned for handlers.

* `Map<Class<?>, Class<?>> classes()`
* `Options classes(Class<A> spec, Class<? extends A> impl)`

Classes are instantiated once per each application. The following table is the default classes map.

* `Map<String, Object> props()`
* `Object prop(String key)`
* `Options prop(String key, Object value)`

Properties to configure bridge or plugin.

## API
Package `com.github.flowersinthesand.portal`

### App

The App is the context where an application is run.

* `static App find()`

Returns the first application from the default repository. Use this method only if there is a single application to avoid any ambiguousness.

* `static App find(String name)`

Finds an application which corresponds to the given name from the default repository. Use finder functions when injection by the Name annotation is not available. These methods are static, however, app intances are created and configured during initialization. So, only if initialization is done, finder functions can work correctly.

* `App init(String name)`
* `App init(String name, Options options)`
* `App init(String name, Initializer initializer)`
* `App init(String name, Options options, Initializer initializer)`

Initializes the application.

* `App register()`
* `App register(ConcurrentMap<String, App> repository)`

Registers the applicaton to the given repository or the default repository.

* `String name()`

The application name.

* `Object get(String key)`

Returns a value bound with the specified key.

* `App set(String key, Object value)`

Binds a value to this application using the given key.

* `Room room(String name)`

Finds corresponding room or opens new one if it doesn't exist. Utilize this method when trying to access  rooms which are supposed to be created in runtime.

* `App fire(String event, Socket socket)`
* `App fire(String event, Socket socket, Object data)`
* `App fire(String event, Socket socket, Object data, Fn.Callback1<Object> reply)`

Fires the given event to the given socket with data and reply callback.

* `<T> T unwrap(Class<? super T> class)`

Return a bean instantiated from the classes option.

### Handler

The application consists of traditional handlers which consist of event handlers. All declared fields on the handler must be thread-safe. Also, the handler class must have a default constructor.

#### @Handler
* `String value()`

A class-level annotation for a handler. The `value` element indicates a url of the socket and is regarded as an application name. One application can consist of multiple handler classes.

#### @Name
* `String value()`

A field-level annotation for injection of `App` and `Room`. According to the type of the field and the `value` element of the annotation, corresponding resource is injected regardless of the access modifier of the field. Field injection is done before execution of `@Prepare` methods.

#### @Prepare

A method-level annotation for preparation. Annotated methods are executed during initialization. Only public methods with no arguments can be executed.

### Event handler

The event handler is a plain method marked as an event handler. According to the method signature, corresponding parameters are provided.

#### @On
* `String value()`

A method-level and annotation-level annotation for marking a method or an annotation as an event handler. The `value` element is an event name. The access modifier of the method must be `public` and a return type of the method signifies nothing. `On.open`, `On.message`, `On.close` are special annotations for `open`, `message`, `close` event respectively.

#### Socket

If the Socket class is present on parameters in the method signature, the socket instance that sent the event will be passed.

#### @Data

A parameter-level annotation for specifying and converting the event data. If this annotation is present on parameters in the method signature, the event data will be converted to the declared parameter's type and provided. By default, [Jackson](http://wiki.fasterxml.com/JacksonHome) library is used to create an instance from a JSON string. Any object the client sent can be converted into the `Map<String, Object>` type.

#### @Reply

A parameter-level annotation for a reply callback for the client. The parameter's type must be `Fn.Callback` or `Fn.Callback1`.

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

Removes a socket. If one of added sockets is closed, it will be removed from the room as well automatically. Use this method to exclude opened socket.

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