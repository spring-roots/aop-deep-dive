package io.pivotal.springroots.logging;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static java.util.stream.Collectors.joining;

@Aspect
@Component
public class RequestLoggingAdvice {
	private static Logger log = LoggerFactory.getLogger(RequestLoggingAdvice.class);

	@Value("${logging.requests.verboseMaxArgumentSize:10240}")
	public void setVerboseMaxArgumentSize(int verboseMaxArgumentSize) {
		this.verboseMaxArgumentSize = verboseMaxArgumentSize;
	}

	@Value("${logging.requests.terseMaxArgumentSize:4096}")
	public void setTerseMaxArgumentSize(int terseMaxArgumentSize) {
		this.terseMaxArgumentSize = terseMaxArgumentSize;
	}

	@Value("${logging.requests.verboseMaxResultSize:1024000}")
	public void setVerboseMaxResultSize(int verboseMaxResultSize) {
		this.verboseMaxResultSize = verboseMaxResultSize;
	}

	@Value("${logging.requests.terseMaxResultSize:10240}")
	public void setTerseMaxResultSize(int terseMaxResultSize) {
		this.terseMaxResultSize = terseMaxResultSize;
	}

	private int verboseMaxArgumentSize = 10240;
	private int terseMaxArgumentSize = 4096;
	private int verboseMaxResultSize = 1_024_000;
	private int terseMaxResultSize = 10240;

	private final ParameterNameDiscoverer parameterNameDiscoverer = new DefaultParameterNameDiscoverer();

	public RequestLoggingAdvice() {
		log.info("Activated.  All @RequestMapping annotated invocations (and results) will be logged.");
	}

	@Pointcut("@annotation(org.springframework.web.bind.annotation.RequestMapping) && !@within(io.pivotal.springroots.logging.DisableRequestLogging)")
	private void requestMappings() {
	}

	// not @Around() to minimize the possibility of affecting application flow.
	@Before("requestMappings()")
	public void logRequest(JoinPoint methodCall) {
		try {
			Logger logger = loggerForTarget(methodCall);
			int argSizeLimit = logger.isDebugEnabled() ? verboseMaxArgumentSize : terseMaxArgumentSize;
			String methodName = methodCall.getSignature().getName();
			logger.info(String.format("%s(%s)", methodName, parameterAndArgPairsFrom(methodCall, argSizeLimit)));
		} catch (Throwable t) {
			log.warn("Unable to generate request log for " + methodCall + "; caused by:", t);
		}
	}

	@AfterReturning(pointcut = "requestMappings()", returning = "result")
	public void logResponse(JoinPoint methodCall, Object result) {
		try {
			Logger logger = loggerForTarget(methodCall);
			int resultSizeLimit = logger.isDebugEnabled() ? verboseMaxResultSize : terseMaxResultSize;
			String methodName = methodCall.getSignature().getName();
			logger.info(String.format("%s() ==> %s", methodName, stringOf(result, resultSizeLimit)));
		} catch (Throwable t) {
			log.warn("Unable to generate response log for " + methodCall + "; caused by:", t);
		}
	}

	private Logger loggerForTarget(JoinPoint methodCall) {
		return LoggerFactory.getLogger(methodCall.getTarget().getClass());
	}

	private String parameterAndArgPairsFrom(JoinPoint methodCall, int maxArgLength) {
		return stringOf(mapOf(extractParameterNamesFrom(methodCall), methodCall.getArgs(), maxArgLength));
	}

	private Map<String, String> mapOf(String[] parameters, Object[] args, int maxArgLength) {
		return IntStream.range(0, parameters.length).boxed()
			.collect(Collectors.toMap(idx -> parameters[idx], idx -> stringOf(args[idx], maxArgLength)));
	}

	private String stringOf(Map<String, String> paramsToArgs) {
		return paramsToArgs.entrySet().stream()
			.map(paramToArg -> String.format("%s: %s", paramToArg.getKey(), paramToArg.getValue()))
			.collect(joining(", "));
	}

	private String stringOf(Object arg, int maxLength) {
		String string = String.valueOf(arg);
		string = string.length() > maxLength ? string.substring(0, maxLength) + "[...]" : string;
		return (arg instanceof String) ? String.format("\"%s\"", string) : string;
	}

	private String[] extractParameterNamesFrom(JoinPoint methodCall) {
		return findMethod(methodCall.getSignature().getName(), methodCall.getTarget().getClass().getMethods())
			.map(parameterNameDiscoverer::getParameterNames)
			.orElse(genericParameterNames(methodCall));
	}

	private Optional<Method> findMethod(String nameOfMethod, Method[] methods) {
		return Stream.of(methods)
			.filter(method -> method.getName().equals(nameOfMethod))
			.findFirst();
	}

	private String[] genericParameterNames(JoinPoint methodCall) {
		return IntStream.range(0, methodCall.getArgs().length).mapToObj(idx -> "arg" + idx).toArray(String[]::new);
	}

}
