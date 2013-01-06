# JavaScript
`portal-javascript` module provides static JavaScript resources.
* `portal-1.x.js` - The latest portal.js that Portal for Java supports. If there is a newest version of portal.js than provided one, you can use it unless otherwise noted.
* `portal-extension.js` - A set of extension of the browser side for integration with Portal for Java. The extension is additional resource to enable advanced functions of the Portal for Java.

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
By default, resources are located in `javascripts/` directory. These files should be loaded earlier than files which uses `portal`.

```html
<script src="javascripts/portal-1.x.js"></script>
<script src="javascripts/portal-extension.js"></script>
```