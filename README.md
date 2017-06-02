# Spring Aspect-Oriented Programming Deep Dive

## Audience

You are comfortable with the essential inner-workings of the Application Context (e.g. what Beans/Components, BeanPostProcessors, BeanFactory, etc. are, what they do and when they are invoked).

You want to better understand how Spring uses AOP to affect features like Transaction Management.

## Objectives

Achieve fluency in weaving in behavior (both included within Spring projects and custom) through proxy-based aspects (i.e. _not_ using AspectJ):

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
        testCompile 'org.junit.jupiter:junit-jupiter-api:5.0.0-M3'
        testCompile 'org.assertj:assertj-core:3.6.2'
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

However, `Despicable` can only decorate instances of `Me`.  If we want to make, say (imagine a companion interface) `You` also `Despicable`, we'd need to create a separate decorator.  There's a non-trival incremental cost of reusing the `Despicable` behavior.

Isn't there a better way?

## Part 2: Advice on proxies

**Objective:** learn how to apply a given behavior to multiple types by extracting it as "advice" and applying it through a proxy.

The core Spring Framework includes facilities that makes it relatively easy to dynamically create a wrapper around any non-final class.  It does so by creating a class a runtime that `extends` your class.  This wrapper is known as a "proxy."

1. Add Spring Framework as a dependency so we can use the `ProxyFactory` feature:

   **`build.gradle`**
   ```groovy
   ...
    dependencies {
        compile 'org.springframework:spring-context:4.3.7.RELEASE'
        testCompile 'org.junit.jupiter:junit-jupiter-api:5.0.0-M3'
        testCompile 'org.assertj:assertj-core:3.6.2'
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

	... replacing `// make result despicable` with your implementation from `Despicable`.

	> HINT: the transition from decorator to method interceptor can be challenging if there is more than one way to apply "despicability".
	>
	> This challenge can be eased if you first DRY-up the decorator until there's only method for modifying a string to be despicable; that method can rather easily become the body of `DespicableAdvice.invoke()`.

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
    Notice that `DespicableAdvice` can be applied to any kind of class without change!

1. Explore the proxy.

    In one of the tests, set a breakpoint on the first `assert()`; fire-up the test in the debugger and explore the proxy.

    1. Use [`AopUtils`](http://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/aop/support/AopUtils.html) to determine which kind of proxy it is (JDK Dynamic? CGLIB?).
    1. Notice that the proxy implements three (3) interfaces in addition to subclassing our own.  What features do those methods provide?

This is a definite improvement.  In fact, the proxies that we are creating are exactly the creatures you use day-to-day whenever you have a Spring Bean that is advised (e.g. `@Transactional` or `@Retryable`).  That said, we are required to create a proxy, aim it and attach advice for each use.  This repeated work quickly becomes boilerplate.

Spring can do all that work for us!

## Part 3: Advice through Spring Auto Proxies

**Objective:** learn how to register advice and weave it in through proxies automatically created in the Application Context.

> **Word Salad, anyone?**
>
> We're about to become steeped in some technical terminology as we move head-long into Aspect-Oriented Programming.
>
> If this is your first foray into AOP, you may well find it useful to pause from this deep dive to prime yourself with some basic terminology and concepts.
>
> - [Spring Framework Reference: Aspect Oriented Programming with Spring](https://docs.spring.io/spring/docs/current/spring-framework-reference/html/aop.html) — the official guide does an excellent job at defining terms.  At a minimum familiarize yourself with section "11.1 Introduction".



Indeed, the Application Context can be configured to generate proxies automatically through configuration.  Let's do that.

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

1. Apply `@Despicable` to the Spring Beans we want to be ... despicable.

    **`Minion.java`**
    ```java
    @Despicable
    public class Minion implements Me {
       ...
    }
    ```

    **`Human.java`**
    ```java
    @Despicable
    public class Human implements You {
       ...
    }
    ```

    With all this in place, run the revised test.

1. Find the implementation of `DefaultAdvisorAutoProxyCreator#createProxy()`.  Do you see any familiar chunks of code?

1. What other pointcut implementations are there?  (review implementations of `Pointcut` [ [src](https://github.com/spring-projects/spring-framework/blob/master/spring-aop/src/main/java/org/springframework/aop/Pointcut.java) | [javadoc](https://docs.spring.io/spring/docs/current/javadoc-api/org/springframework/aop/Pointcut.html) ])

1. Replace explicitly registering an Auto Proxy creator with an annotation enabling the Auto-Proxy feature:

    Include AspectJ's `aspectjweaver`.  This is _only_ to allow us to use AspectJ's annotations — Spring directly supports recognizing and interpreting these annotations (such as those used by `AbstractAspectJAdvisorFactory` [ [src](https://github.com/spring-projects/spring-framework/blob/master/spring-aop/src/main/java/org/springframework/aop/aspectj/annotation/AbstractAspectJAdvisorFactory.java) | [javadoc](https://docs.spring.io/spring/docs/current/javadoc-api/org/springframework/aop/aspectj/annotation/AspectJAdvisorFactory.html) ]); we're not using AspectJ's compile-time weaving (yet).

    **`build.gradle`**
    ```groovy
    dependencies {
        ...
        compile 'org.aspectj:aspectjweaver:1.8.10'
    }
    ```

    ... and annotate the Spring config to enable this more feature-rich Auto Proxy creator:

    **`DespicableConfig.java`**
    ```java
    @Configuration
    @EnableAspectJAutoProxy
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
        public DefaultPointcutAdvisor adviseDespicableClasses() {
            return new DefaultPointcutAdvisor(
                new AnnotationMatchingPointcut(Despicable.class),
                new DespicableAdvice());
        }
    }
    ```
    (notice that the annotation has replaced the `DefaultAdvisorAutoProxyCreator`; digging into `EnableAspectJAutoProxy` and you'll find a replacement `...AutoProxyCreator`).

1. Declare `DespicableAdvice` as an aspect:

    So that we can continue to use `DespicableAdvice`, we'll make a copy of it and turn it into Advice:

    **`DespicableAspect.java`**
    ```java
    @Aspect
    public class DespicableAspect {

        @Around("@target(io.pivotal.springroots.aspects.Despicable)")
        public Object makeDespicable(ProceedingJoinPoint pjp) throws Throwable {
            Object result = pjp.proceed();

            if (result instanceof String) {
                result = makeDespicable((String) result, getNameFrom(pjp.getTarget()));
            }
            return result;
        }

        // ... the rest exactly like DespicableAdvice
    }
    ```

    The `@Aspect` annotation indicates tells the [AspectJ Auto Proxy Creator Bean Post Processor](https://docs.spring.io/spring/docs/current/javadoc-api/org/springframework/aop/aspectj/annotation/AnnotationAwareAspectJAutoProxyCreator.html) (APC-BPP) that this class contains advice and that the BPP should look for pointcuts have been declared using AspectJ annotations.

    We mark `makeDespicable(ProceedingJoinPoint)` as being "around advice" using the `@Around()` annotation.  The value of `@Around()` is a Pointcut Designator (as described in [Spring Framework Reference: Declaring a Pointcut](http://docs.spring.io/spring/docs/current/spring-framework-reference/htmlsingle/#aop-pointcuts)).  The APC-BPP will attach this advice to any Spring Bean whose implementation class is annotated with `@Despicable`.

   In order for the APC-BPP to pick up our aspect, we need to register that aspect with the application context.

   Replace the `DefaultPointcutAdvisor` with an instance of our new aspect:

   **`DespicableConfig.java`**
   ```java
    @Bean
    public DespicableAspect despicableAspect() {
        return new DespicableAspect();
    }
   ```

   Q: What is the difference between our `DespicableAspect` and `DefaultPointcutAdvisor`?

   At this point, with core Spring Framework features, we've been able to wrap and apply behavior on any Spring Bean.  That's powerful stuff!

   With this power comes the responsibility to ensure we don't accidentally spill that behavior over into other Spring beans we do NOT intend to advise.

   **Visualizing applied advise**

   One very helpful means of fulfilling that responsibility is when your IDE can mark the specific methods that are being advised.  IntelliJ does just that ... with a caveat.

   IntelliJ will include an icon in the gutter next to a method that delivers advise.  Click on it and you see a list of advised methods. Similarly, methods that are advised also have an icon in the gutter next to them.  Click on _that_ icon and you see a list of advice being applied.

   There's one catch: only Spring beans that have been registered through a component scan get this treatment in IntelliJ.

1. Registering Beans through a Component Scan:

   Replace the configured bean definitions with `@Component` annotated classes and a `@ComponentScan`:

   **`DespicableConfig.java`**
   ```java
    @Configuration
    @ComponentScan
    @EnableAspectJAutoProxy
    public class DespicableConfig {
    }
   ```

   **`Minion.java`**
   ```java
    @Component
    @Despicable
    public class Minion implements Me {
    ...
   ```

   **`Human.java`**
   ```java
    @Component
    @Despicable
    public class Human implements You {
    ...
   ```

   **`DespicableAspect.java`**
   ```java
    @Aspect
    @Component
    public class DespicableAspect {
    ...
   ```

   Do the tests still pass?

   Q: What differences do you see in how IntelliJ treats advice and advised methods?


# Part 4: Pragmatic Programming Challenge

**Objective:** go through the process of writing a useful aspect to reinforce the material in this deep-dive and to walk away with a useful chunk of code.

It's now time to put your understanding to the test!

## Background

One common cross-cutting concern is logging.  It's a chunk of behavior that applies in a wide variety of places, cutting across the hierarchies of classes that make up the application.

In a web application, it is reasonable to want to log each web request as it arrives.  It is useful to perform that logging within the controller itself to take advantage of the strongly typed parameters.  In a Spring-based web application, those methods are annotated as `@RequestMapping`.

## The Challenge

In this programming challenge, you will write an aspect that wraps all `@RequestMapping` in two log entries:

 1. one right before the invocation of the method (including the name of each parameter and corresponding argument value); and
 2. another right after the invocation that includes the result.

To access the challenge:

```bash
$ git clone https://github.com/spring-roots/aop-deep-dive.git
$ cd aop-deep-dive
$ git checkout challenge
```

The challenge is organized as a pair of fully-baked test suites.  Your job is to use these tests to guide your implementation of the aspect.

You will find the following:

In the test source set:
- `RequestLoggingAdviceTest` — the first suite.  It is focused on the core behavior of the advice.
- `RequestLoggingAdviceWeavingTest` — the second suite; in fact this suite depends on the aspect being fully functional to verify.  This suite is focused on verifying the pointcut expression.
- `LoggingSpy` — a testing utility for sniffing application logging.  This is used by the tests to assert on the output of the logging system.

In the main source set:
- `RequestLoggingAdvice` — a skeleton implementation of the aspect, just enough to satisfy the compiler so that when you run the test suites, the first error is a good test fail (and not a complication error).  You're welcome. :)
- `DisableRequestLogging` — an annotation used to switch off logging for a given controller.  For example, if you use Spring REST Docs, you probably don't want to log that output, so you'd drop this annotation on those controllers.

To work your way through the challenge:

1. Run the test suite.
2. Write enough production code to satisfy the test.
3. (if applicable) refactor
4. Remove the `@Ignore` from the next test method.  Goto step 1.

Good luck and enjoy!

# Coda: Where to from here?

- Explore out-of-the-box implementations of aspects from Spring
  - `@ControllerAdvice`
  - `@Transactional`
  - `@Retryable`
  - `@Cacheable`
- Dive deeper into the topic of Aspect-Oriented Programming:
  - Lecture: [Aspect-Oriented Programming: Radical Research in Modularity](https://www.youtube.com/watch?v=40Q16Ix-src) (YouTube) — a Google Talk by [Gregor Kiczales](https://en.wikipedia.org/wiki/Gregor_Kiczales).
  - Book: [AspectJ in Action, Second Edition](https://www.manning.com/books/aspectj-in-action-second-edition) by Ramnivas Laddad
