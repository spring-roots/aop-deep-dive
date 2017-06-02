package io.pivotal.springroots.logging;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class RequestLoggingAdvice {
	private static Logger log = LoggerFactory.getLogger(RequestLoggingAdvice.class);

	public void setVerboseMaxArgumentSize(int verboseMaxArgumentSize) {
	}

	public void setTerseMaxArgumentSize(int terseMaxArgumentSize) {
	}

	public void setVerboseMaxResultSize(int verboseMaxResultSize) {
	}

	public void setTerseMaxResultSize(int terseMaxResultSize) {
	}

	private final ParameterNameDiscoverer parameterNameDiscoverer = new DefaultParameterNameDiscoverer();

	public RequestLoggingAdvice() {
		log.info("Activated.  All @RequestMapping annotated invocations (and results) will be logged.");
	}

	@Pointcut("")
	private void requestMappings() {
	}

	// not @Around() to minimize the possibility of affecting application flow.
	@Before("requestMappings()")
	public void logRequest(JoinPoint methodCall) {
	}

	@AfterReturning(pointcut = "requestMappings()", returning = "result")
	public void logResponse(JoinPoint methodCall, Object result) {
	}
}
