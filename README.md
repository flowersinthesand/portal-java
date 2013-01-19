# Portal for Java
The **Portal for Java** is a reference implementation written in Java of the server counterpart of the [**Portal**](https://github.com/flowersinthesand/portal) project which provides useful semantics and concepts for modern web application development in the server side as well as server implementation.

Thanks to [Ralph](https://github.com/ralscha), online applications and source codes of chat, scheduler, twitter, snake etc are available now at http://ha-bio.rasc.ch/portal-demos/index.html

## Installing
Add the following dependency to your pom.xml:
```xml
<dependency>
    <groupId>com.github.flowersinthesand</groupId>
    <artifactId>portal-core</artifactId>
    <version>${portal.version}</version>
</dependency>
```

Now you can write a complete portal application, but you need to choose runtime environment where your application will be run such as servlet container or stand-alone server and a corresponding module for the integration.

* [atmosphere](https://github.com/flowersinthesand/portal-java/tree/master/atmosphere/README.md) for servlet container

Browser side resources such as `portal.js` and `portal-extension.js` are also provided to fully benefit from integration. See the [javascript module](https://github.com/flowersinthesand/portal-java/tree/master/javascript).

## References
* [API](https://github.com/flowersinthesand/portal-java/blob/master/core/README.md)
* [Samples](https://github.com/flowersinthesand/portal-java/tree/master/samples/README.md)

## Snippets

### Echoing a message
A simple echo handler which echoes back any message.

#### Browser
```js
portal.open("/echo").send("message", "hello").message(function(data) {
    console.log(data);
});
```

#### Server
```java
@Bean
public class EchoHandler {

    @On.message
    public void message(Socket socket, @Data String message) {
        socket.send("message", message);
    }

}
```

### Broadcasting a message using room
A simple chat handler which broadcasts a received message to the room.

#### Browser
```js
portal.open("/chat").on({
    open: function() {
        this.send("message", "Hi, there");
    },
    message: function(message) {
        console.log(message);
    }
});
```

#### Server
```java
@Bean
public class ChatHandler {

    @Name("chat")
    private Room room;

    @On.open
    public void open(Socket socket) {
        room.add(socket);
    }
    
    @On.message
    public void message(@Data String message) {
        room.send(message);
    }

}
```

### Notification delivery
Any event which occurs in anywhere in the server side can be sent to the client.

#### Browser
```js
portal.open("/notifications").on(notifiers);
```

#### Server
```java
@Component
public class NotificationEventListener implements ApplicationListener<NotificationEvent> {

    public void onApplicationEvent(NotificationEvent e) {
        Notification n = e.notification();
        App.find().room(n.target()).send(n.type(), n.data());
    }

}
```

### Notifying change of model
Changes in domain layer can be applied to presentation layer in real time as well.

#### Browser
```js
portal.find("/entity").on("account#" + id, function(model) {
    console.log(model);
});
```

#### Server
```java
@Entity
public class Account extend Model {

    @PostUpdate
    public void updated() {
        App.find("/entity").room("account#" + id).send("updated", this);
    }

}
```

### Type conversion of event data
Event data can be object and be converted to the specific type based on JSON format.

#### Browser
```js
portal.find().send("account.save", {username: "flowersinthesand", email: "flowersinthesand@gmail.com"});
```

#### Server
```java
@Bean
public class AccountHandler {

    @On("account.save")
    public void save(@Data Account account) {
        account.save();
    }

}
```

### Retrieving data
Using reply callback, the client can retrieve data from the server asynchronously like AJAX.

#### Browser
```js
portal.find("/post").send("find", 5, function(post) {
    console.log(post);
});
```

#### Server
```java
@Bean
public class PostHandler {

    private EntityManager em;
    
    @On("find")
    public void find(@Data Integer id, @Reply Fn.Callback1<Post> reply) {
        reply.call(em.find(Post.class, id));
    }

}
```

### Sharing a data store
Room can be used as a shared data store.

#### Browser
```js
portal.find("/data").send("set", {key: "key", value: "value"}, function() {
    this.send("get", "key", function(value) {
        console.log(value);
    });
});
```

#### Server
```java
@Bean
public class DataHandler {

    @Name("data")
    private Room room;
    
    @On("set")
    public void set(@Data Entity e, @Reply Fn.Callback reply) {
        room.set(e.key(), e.data());
        if (reply != null) {
            reply.call();
        }
    }

    @On("get")
    public void get(@Data String key, @Reply Fn.Callback1<Object> reply) {
        reply.call(room.get(key));
    }
    
}
```

### Creating a custom event handler marker
You can externalize event names by creating a custom event handler annotation using `On` annotation.

#### Browser
```js
portal.open("/event").send("custom", "data").send("welcome", "data");
```

#### Server
```java
public interface Event {

    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    @Documented
    @On("welcome")
    public static @interface welcome {}

}
```
```java
@Bean
public class EventHandler {

    @On("custom")
    public void custom(@Data String data) {
        System.out.println(data);
    }

    @Event.welcome
    public void welcome(@Data String data) {
        System.out.println(data);
    }

}
```