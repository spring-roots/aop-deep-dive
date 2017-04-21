# Spring Aspect-Oriented Programming Deep Dive

## Audience

You are comfortable with the essential inner-workings of the Application Context (e.g. what Beans/Components, BeanPostProcessors, BeanFactory, etc. are, what they do and when they are invoked).

You want to better understand how Spring uses AOP to affect features like Transaction Management

## Objectives

Achieve fluency in weaving in behavior (both included within Spring projects and custom) through proxy-based aspects (i.e. _not_ using AspectJ).

- what a proxy is and how it gets created;
- how proxies are used to weave in behavior to beans (both singletons and prototypes) from the BeanFactory;
- choices in how much "magic" to use in applying a given aspect;
- ingredients required to activate an aspect within an Application Context.

## Part 1: Decorating

**Objective:** establish the essential usefulness of aspects by starting with a simple Decorator.

1. Create a Java project with a unit testing framework (this example is using JUnit 5):

   **`build.gradle`**
   ```groovy
    buildscript {
        repositories {
            mavenCentral()
        }
        dependencies {
            classpath 'org.junit.platform:junit-platform-gradle-plugin:1.0.0-M3'
        }
    }

    apply plugin: 'java'
    apply plugin: 'org.junit.platform.gradle.plugin'

    repositories {
        mavenCentral()
    }

    dependencies {
        compile 'org.assertj:assertj-core:3.6.2'
        testCompile 'org.junit.jupiter:junit-jupiter-api:5.0.0-M3'
        testRuntime 'org.junit.jupiter:junit-jupiter-engine:5.0.0-M3'
    }
   ```
   **`Me.java`**
   ```java
    public interface Me {
        String name();
        String greet(String other);
    }
   ```
   **`Minion.java`**
   ```java
    public class Minion implements Me {
        private final String name;

        public Minion(String name) {
            this.name = name;
        }

        @Override
        public String name() {
            return name;
        }

        @Override
        public String greet(String other) {
            return String.format("Hello, %s, it is I, %s!", other, name);
        }
    }
   ```
   **`DespicableTest.java`**
   ```java
    import org.junit.jupiter.api.DisplayName;
    import org.junit.jupiter.api.Test;

    import static org.assertj.core.api.Assertions.assertThat;

    public class DespicableTest {
        @Test
        @DisplayName("Decorated Me is despicable.")
        public void makesMeDespicable() {
            Me me = new Minion("Kevin");
            Me despicableMe = new Despicable(me);

            assertThat(me.name()).isEqualTo("Kevin");
            assertThat(me.greet("World")).isEqualTo("Hello, World, it is I, Kevin!");

            assertThat(despicableMe.name()).isEqualTo("Despicable Kevin");
            assertThat(despicableMe.greet("World")).isEqualTo("Hello, World, it is I, Despicable Kevin!");
        }
    }
   ```

1. Implement `Despicable`.

At this point, `Despicable` decorates `Me`, separating the optional behavior from the base behavior.

However, `Despicable` can only decorate instances of `Me`.  We can do better.  We have ways of reusing this behavior on instances of _any_ class.

## Part 2: Advice on proxies

1. Add Spring Framework as a dependency so we can use the `ProxyFactory` feature:

   **`build.gradle`**
   ```groovy
   ...
    dependencies {
        compile 'org.assertj:assertj-core:3.6.2'
        compile 'org.springframework:spring-context:4.3.7.RELEASE'
        testCompile 'org.junit.jupiter:junit-jupiter-api:5.0.0-M3'
        testRuntime 'org.junit.jupiter:junit-jupiter-engine:5.0.0-M3'
    }
   ```

1. Adjust the test so that instead of decorating `me`, we wrap it in a proxy and give that proxy "despicability" in the form of "advice" (that we'll define in the next step):

   **`DespicableTest.java`**
   ```java
    @Test
    @DisplayName("Advised Me is despicable.")
    public void makesMeDespicable() {
        Me me = new Minion("Kevin");

        ProxyFactory despicabilityFactory = new ProxyFactory();
        despicabilityFactory.setTarget(me);
        despicabilityFactory.addAdvice(new DespicableAdvice(me.name()));
        Me despicableMe = (Me) despicabilityFactory.getProxy();

        assertThat(me.name()).isEqualTo("Kevin");
        assertThat(me.greet("World")).isEqualTo("Hello, World, it is I, Kevin!");

        assertThat(despicableMe.name()).isEqualTo("Despicable Kevin");
        assertThat(despicableMe.greet("World")).isEqualTo("Hello, World, it is I, Despicable Kevin!");
    }
	```

1. Morph `Despicable` into an `Advice`: rename `Despicable` to `DespicableAdvice`, implementing `MethodInterceptor` (a kind of `Advice`):

	**`DespicableAdvice.java`** _(implement `MethodInterceptor`)_
	```java
    import org.aopalliance.intercept.MethodInterceptor;
    import org.aopalliance.intercept.MethodInvocation;

    public class DespicableAdvice implements MethodInterceptor {
        private final String name;
        public DespicableAdvice(String name) {
            this.name = name;
        }

        @Override
        public Object invoke(MethodInvocation invocation) throws Throwable {
            Object result = invocation.proceed();

            // make result despicable

            return result;
        }
    }
	```

	replacing `// make result despicable` with your implementation from `Despicable`.

	For simplicity's sake, we've required that this `Advice` be given the `name` value (i.e. "Kevin").

	Make sure to get the unit test passing.

1. Apply `DespicableAdvice` to a whole different type of class.  Now that we've encapsulated "despicability", let's demonstrate using it against a whole other kind of object.

    _Add_ another test case:

    **`DespicableTest.java`**
    ```java
    ...

    @Test
    @DisplayName("Advised You is despicable.")
    public void makesYouDespicable() {
        You you = new Human("Gru");

        ProxyFactory despicabilityFactory = new ProxyFactory();
        despicabilityFactory.setTarget(you);
        despicabilityFactory.addAdvice(new DespicableAdvice(you.name()));
        You despicableYou = (You) despicabilityFactory.getProxy();

        assertThat(you.name()).isEqualTo("Gru");
        assertThat(you.claim("the Statue of Liberty")).isEqualTo("I, Gru, have stolen the Statue of Liberty!");

        assertThat(despicableYou.name()).isEqualTo("Despicable Gru");
        assertThat(despicableYou.claim("the Statue of Liberty")).isEqualTo("I, Despicable Gru, have stolen the Statue of Liberty!");
    }
    ...
    ```

    ... and add the types implied by the test:

    **`You.java`**
    ```java
    public interface You {
        String name();

        String claim(String item);
    }
    ```


    **`Human.java`**
    ```java
    public class Human implements You {
        private final String name;

        public Human(String name) {
            this.name = name;
        }

        @Override
        public String name() {
            return name;
        }

        @Override
        public String claim(String item) {
            return String.format("I, %s, have stolen %s!", name, item);
        }
    }
    ```

Notice that `DespicableAdvice` can be applied to any kind of class without change.

However, the ceremony involved in creating the proxies feels like boilerplate.

## Part 3: Advice through Spring Auto Proxies

Indeed, the Application Context can be configured to generate proxies automatically.  Let's do that.

1. Adjust `DespicableAdvice` to determine the "name" value dynamically.

   We are about to create just one instance of `DespicableAdvice` and apply it to many objects.  This means, it should determine what the "name" of each person is on-the-fly.

   Specifically, adjust `DespicableAdvice` to

   1. No longer accept the `name` value as a parameter to the constructor (in fact, there's no need for a constructor anymore).
   1. in `invoke()`, reflect the target object for a method named `name()` and invoke _that_ method to get the "name" value.

   _(for an example, see https://github.com/spring-roots/aspects-deep-dive/blob/get-name-dynamically/src/main/java/io/pivotal/springroots/aspects/DespicableAdvice.java)_

1. Add another test case that obtains instances of `You` and `Me` from an Application Context:

    **`DespicableTest.java`**
    ```java
    @Test
    @DisplayName("You and Me are despicable beans.")
    public void yieldsDespicableYouAndMeBeans() {
        AnnotationConfigApplicationContext appCtx = new AnnotationConfigApplicationContext();
        appCtx.register(DespicableConfig.class);
        appCtx.refresh();

        You you = appCtx.getBean(You.class);
        Me me = appCtx.getBean(Me.class);

        assertThat(you.name()).isEqualTo("Despicable Gru");
        assertThat(you.claim("the Statue of Liberty")).isEqualTo("I, Despicable Gru, have stolen the Statue of Liberty!");
        assertThat(me.name()).isEqualTo("Despicable Kevin");
        assertThat(me.greet("World")).isEqualTo("Hello, World, it is I, Despicable Kevin!");
    }
    ```
1. Configure the application context to enable auto-proxying and marry the `@Despicable` annotation with our `DespicableAdvice`.

    **`DespicableConfig.java`**
    ```java
    @Configuration
    public class DespicableConfig {
        @Bean
        public You you() {
            return new Human("Gru");
        }

        @Bean
        public Me me() {
            return new Minion("Kevin");
        }

        @Bean
        public DefaultAdvisorAutoProxyCreator proxyAllTheBeans() {
            return new DefaultAdvisorAutoProxyCreator();
        }

        @Bean
        public DefaultPointcutAdvisor adviseDespicableClasses() {
            return new DefaultPointcutAdvisor(
                new AnnotationMatchingPointcut(Despicable.class, true),
                new DespicableAdvice());
        }
    }
    ```

    Our first two beans are simply registering our POJOs in the container.

    **Enabling Auto-Proxies**

    The next bean, `DefaultAdvisorAutoProxyCreator`, is supplied by Spring Framework.  It is a `BeanPostProcessor` — a chunk of logic that get applied to all Spring Beans created in application context.  The mere presence of `DefaultAdvisorAutoProxyCreator` is enough to enable the "Auto Proxy" feature.

    **Registering Advice**

    The last bean is a "pointcut advisor".  The `DefaultAdvisorAutoProxyCreator` automatically detects Spring Beans that implement the `Advisor` interface.

    A "pointcut" is just a point in the code where an advice code can be spliced in (hence, a "cut").  In our case, that "pointcut" is any class that has the `@Despicable` annotation (or any of its super classes).

    The final part is our trusty `DespicableAdvice`.  This is the same advice we've been applying.

    The net of this configuration is:

    _Any Spring Bean annotated with `@Despicable` will have `DespicableAdvice` applied to it through a proxy automatically generated by the application context._

1. Apply `@Despicable` to the Spring Beans that are ... despicable.

    Here on one of the concrete implementations...

    **`Minion.java`**
    ```java
    @Despicable
    public class Minion implements Me {
       ...
    }
    ```

    ... and another on the interface (demonstrating that the annotation is working through inheritance).

    **`You.java`**
    ```java
    @Despicable
    public interface You {
       ...
    }
    ```

    With all this in place, run the revised test.

1. Find the implementation of `DefaultAdvisorAutoProxyCreator#createProxy()`.  Do you see any familiar chunks of code?



