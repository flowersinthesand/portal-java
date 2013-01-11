# JavaScript
`portal-javascript` module provides static JavaScript resources.
* `portal-1.x.js`: The latest portal.js that Portal for Java supports. If there is a newest version of portal.js than provided one, you can use it unless otherwise noted.
* `portal-extension.js`: A set of options to fully benefit from the integration with Portal for Java, not requiring further configuration in the server side. It enables the heartbeat for every 20 seconds and transports using XDomainRequest. This is optional but recommended.
 
## Installing
### Updating pom.xml
Add the following dependency to your pom.xml:
```xml
<dependency>
    <groupId>com.github.flowersinthesand</groupId>
    <artifactId>portal-javascript</artifactId>
    <version>${portal.version}</version>
    <type>war</type>
</dependency>
```

### Loading resources
Resources are located in `javascripts/` directory. These scripts should be loaded earlier than scripts that use `portal` object.

```html
<script src="javascripts/portal-1.x.js"></script>
<script src="javascripts/portal-extension.js"></script>
```