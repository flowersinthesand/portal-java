# Spring Expression Language
`portal-spel` module allows handler to use the [Spring Expression Language](http://static.springsource.org/spring/docs/3.2.x/spring-framework-reference/html/expressions.html) when evaluating a data expression. Utilizing expression language, sophisticated operation in handler becomes possible.

## Installing
### Updating pom.xml
Add the following dependency to your pom.xml:
```xml
<dependency>
    <groupId>com.github.flowersinthesand</groupId>
    <artifactId>portal-spel</artifactId>
    <version>${portal.version}</version>
</dependency>
```

### Creating the module
Use the following constructor:
```java
SpelModule()
```

A StandardEvaluationContext is used as EvaluationContext globally in application, and customizations like declaring a variable or registering a function can be done in modifyStandardEvaluationContext method of the module.
```java
new App(new Options().url("/event").packageOf(this), new SpelModule() {

    @Override
    protected void modifyStandardEvaluationContext(StandardEvaluationContext context) {
        context.setVariable("locale", "ko");
        context.registerFunction("isEmpty", StringUtils.class.getMethod("isEmpty", new Class[] { Object.class }));
    }
    
});
```