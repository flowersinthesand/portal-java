# Portal for Java
The **Portal for Java** is a reference implementation written in Java of the server counterpart of the [**Portal**](https://github.com/flowersinthesand/portal) project which provides useful semantics and concepts for modern web application development in the server side as well as server implementation.

The **Portal** and **Portal for Java** project is developed and maintained by [Donghwan Kim](http://twitter.com/flowersits). If you are interested, please subscribe to the [discussion group](https://groups.google.com/d/forum/portal_project).

## Modules
The following list of modules are available, and **bold** modules are required.

* [**`core`**](https://github.com/flowersinthesand/portal-java/tree/master/core): provides API and SPI.
* **`bridge`**: makes the application run in the following environment.
    * [`atmosphere`](https://github.com/flowersinthesand/portal-java/tree/master/atmosphere): Servlet container.
    * [`play`](https://github.com/flowersinthesand/portal-java/tree/master/play): Play.
* `objectfactory`: delegates bean creation to the following framework.
    * [`spring`](https://github.com/flowersinthesand/portal-java/tree/master/spring): Spring.
    * [`guice`](https://github.com/flowersinthesand/portal-java/tree/master/guice): Guice.
* `evaluator`: evaluate an expression using the following expression language.
    * [`spel`](https://github.com/flowersinthesand/portal-java/tree/master/spel): Spring Expression Language.

## Demos
The easiest way to get started with Portal is to try out and look at examples. Thanks to [Ralph](https://github.com/ralscha), various online demos and source codes are available now at http://ha-bio.rasc.ch/portal-demos Try out!

Also, officialy a [very simple chat application](https://github.com/flowersinthesand/portal-java/tree/master/samples) is provided with each bridge module to help getting started.

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

    @On
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

    @Wire
    private Room hall;
    
    @On
    public void message(@Data String message) {
        hall.send(message);
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

    @Inject
    private App app;

    @Override
    public void onApplicationEvent(NotificationEvent e) {
        Notification n = e.notification();
        app.hall().send(n.type(), n.data());
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
portal.find("/band")
.send("find", 45, function(band) {
    console.log(band);
})
.send("query", query, function(bands) {
    console.log(bands);
});
```

#### Server
```java
@Bean
public class BandHandler {

    @On
    @Reply
    public Band find(@Data Long id) {
        return Band.byId(Band.class, id));
    }
    
    @On
    public void query(@Data String query, @Reply final Reply.Fn reply) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    reply.done(Band.query...list);
                } catch (EntityException e) {
                    reply.fail(e);
                }
            }
        })
        .start();
    }

}
```

### Calling a service bean method remotely
Service bean can be executed directly via the portal.

#### Browser
```js
portal.find("/account")
.send("find", 23, function(account) {
    console.log('found');
    console.log(account);
}, function(info) {
    console.log(info.type + ":" + info.message);
})
.send("remove", 45, function() {
    console.log('deleted');
}, function(info) {
    console.log(info.type + ":" + info.message);
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
    @On
    @Reply
    public Account find(@Data long id) throws EntityNotFoundException {
        return dao.find(id);
    }

    @Override
    @On
    @Reply(failFor = EntityNotFoundException.class)
    public void remove(@Data long id) {
        dao.remove(id);
    }
    
}
```