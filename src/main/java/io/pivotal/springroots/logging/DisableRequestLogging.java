package io.pivotal.springroots.logging;

import java.lang.annotation.*;

/**
 * Disables automatic logging of any {@link org.springframework.web.bind.annotation.RequestMapping} methods.
 *
 * @see RequestLoggingAdvice
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DisableRequestLogging {
}
