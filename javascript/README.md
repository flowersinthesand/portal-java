# JavaScript
`portal-javascript` module provides static JavaScript resources.
* `portal.js`: The latest [portal.js](https://github.com/flowersinthesand/portal-java/javascript/src/main/resources/META-INF/resources/javascripts/portal.js) that Portal for Java supports. If there is a newest version of portal.js than provided one, you can use it unless otherwise noted.
* `portal-extension.js`: A set of options to fully benefit from the integration with Portal for Java, not requiring further configuration in the server side. It enables the heartbeat for every 20 seconds and transports using XDomainRequest. This is optional but recommended.
 
## Installing
### Updating pom.xml
Add the following dependency to your pom.xml:
```xml
<dependency>
    <groupId>com.github.flowersinthesand</groupId>
    <artifactId>portal-javascript</artifactId>
    <version>${portal.version}</version>
</dependency>
```

### Loading resources
Static assets are located in `META-INF/resources/javascripts/` in the jar. Serve them properly according to your environment. The [documentation](http://www.webjars.org/documentation) of WebJARS will give you some help for this. Otherwise, just copy and paste them.