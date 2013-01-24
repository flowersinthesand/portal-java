# Portal for Java
The **Portal for Java** is a reference implementation written in Java of the server counterpart of the [**Portal**](https://github.com/flowersinthesand/portal) project which provides useful semantics and concepts for modern web application development in the server side as well as server implementation.

The **Portal** and **Portal for Java** project is developed and maintained by [Donghwan Kim](http://twitter.com/flowersits). If you are interested, please subscribe to the [discussion group](https://groups.google.com/d/forum/portal_project).

## Modules
The following list of modules are available, and **bold** modules are required.

* [**`core`**](https://github.com/flowersinthesand/portal-java/tree/master/core): provides API and SPI.
* **`bridges`**: makes the application run in the following runtime environments.
 * [`atmosphere`](https://github.com/flowersinthesand/portal-java/tree/master/atmosphere): supports servlet container.
*  `plugins`: supports the following frameworks. 
 * [**`javascript`**](https://github.com/flowersinthesand/portal-java/tree/master/javascript): provides static JavaScript resources.
 * [`spring`](https://github.com/flowersinthesand/portal-java/tree/master/spring): delegates bean creation to Spring.
 * [`guice`](https://github.com/flowersinthesand/portal-java/tree/master/guice): delegates bean creation to Guice.

## Demos
The easiest way to get started with Portal is to try out and look at examples. Thanks to [Ralph](https://github.com/ralscha), various online demos and source codes are available now at http://ha-bio.rasc.ch/portal-demos Try out!

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

    @Wire("chat")
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

### Notifying changes of model
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
portal.find("/post")
.send("find", 123, function(post) {
    console.log(post);
})
.send("load", 700, function(post) {
    console.log(post);
});
```

#### Server
```java
@Bean
public class PostHandler {

    private EntityManager em;
    
    @Prepare
    public void prepare() {
        em = Persistence.createEntityManagerFactory("app1").createEntityManager(); 
    }
    
    @On("find")
    public Post find(@Data Integer id) {
        return em.find(Post.class, id);
    }
    
    @On("load")
    public void load(@Data Integer id, @Reply Fn.Callback1<Post> reply) {
        reply.call(em.find(Post.class, id));
    }

}
```

### Calling a service bean method remotely
Service bean can be executed directly via the portal.

#### Browser
```js
portal.find("/account").send("remove", 45).send("find", 23, function(account) {
    console.log(account);
});
```

#### Server
```java
@Bean
@Service
public class AccountServiceImpl implements AccountService {

    @Autowired
    private AccountDao dao;
    
    @Override
    @On("find")
    public Account find(@Data long id) {
        return dao.find(id);
    }

    @Override
    @On("remove")
    public void remove(@Data long id) {
        dao.remove(id);
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