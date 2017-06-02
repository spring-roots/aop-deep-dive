package io.pivotal.springroots.logging;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.LoggingEvent;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.Signature;
import org.assertj.core.util.Lists;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Begin here.  These tests help guide you through these objectives:
 *
 * Given a method call:
 * - log the invocation: the method name along with each parameter and the corresponding argument;
 * - log the return value;
 * - do all that in a way that guarantees that including this aspect does NOT change the behavior of the core logic;
 *   - if an error occurs, contain it;
 *   - place an upper bound on how much logging this aspect can generate.
 */
public class RequestLoggingAdviceTest {


	private RequestLoggingAdvice subject;  // i.e. the unit under test
	private Advisee advisee;

	// Fixtures
	private ch.qos.logback.classic.Logger rootLogger;
	private LoggingSpy logged;

	// an example of some methods with a variety of arity and type of params.
	static class Advisee {
		public void method0() {}
		public void method1(ArrayList<String> theParameter) {}
		public String method2(String firstParameter, SomeType secondParameter) {
			return "result";
		}
	}

	// an example of a custom type
	static class SomeType {
		private final String value;

		SomeType(String value) {
			this.value = value;
		}

		// we're going to rely on the `toString()` of custom types while logging.
		public String toString() {
			return value;
		}
	}

	@Before
	public void setUp() {
		subject = new RequestLoggingAdvice();
		advisee = new Advisee();

		// Tap into logging so that we can make assertions in tests about output.
		rootLogger = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
		logged = new LoggingSpy(rootLogger);
	}

	@Test
	public void logRequest_includesTheNameOfTheMethodInvoked_0() throws Exception {
		subject.logRequest(mockCallTo(advisee, "method0"));
		assertThat(logged.output()).contains("method0");
	}

	@Test
   @Ignore
	public void logRequest_includesTheNameOfTheMethodInvoked_1() throws Exception {
		subject.logRequest(mockCallTo(advisee, "method1", Lists.newArrayList()));
		assertThat(logged.output()).contains("method1");
	}

	@Test
   @Ignore
	public void logRequest_includesTheNameOfEachParameterOfTheMethod_1() throws Exception {
		subject.logRequest(mockCallTo(advisee, "method1", Lists.newArrayList()));

		assertThat(logged.output()).contains("theParameter");
	}

	@Test
   @Ignore
	public void logRequest_includesTheNameOfEachParameterOfTheMethod_2() throws Exception {
		subject.logRequest(mockCallTo(advisee, "method2", "firstArg", new SomeType("secondArg")));

		assertThat(logged.output()).contains("firstParameter");
		assertThat(logged.output()).contains("secondParameter");
	}

	@Test
   @Ignore
	public void logRequest_includesWithEachParameterTheValueOfEachArgumentPassedIn_1() throws Exception {
		subject.logRequest(mockCallTo(advisee, "method1", Lists.newArrayList("a", "b", "c")));

		assertThat(logged.output()).contains("theParameter: [a, b, c]");
	}

	@Test
   @Ignore
	public void logRequest_includesWithEachParameterTheValueOfEachArgumentPassedIn_2() throws Exception {
		subject.logRequest(mockCallTo(advisee, "method2", "firstArg", new SomeType("secondArg")));

		// if the argument is literally a String, surround it with double-quotes
		assertThat(logged.output()).contains("firstParameter: \"firstArg\"");
		assertThat(logged.output()).contains("secondParameter: secondArg");
	}

	@Test
   @Ignore
	public void logRequest_whenAnArgumentIsNull_logsAsNull_1() throws Exception {
		subject.logRequest(mockCallTo(advisee, "method1", Null.LIST));

		assertThat(logged.output()).contains("theParameter: null");
	}

	@Test
   @Ignore
	public void logRequest_whenAnArgumentIsNull_logsAsNull_2() throws Exception {
		subject.logRequest(mockCallTo(advisee, "method2", Null.STRING, Null.SOMETYPE));

		// even though type of first parameter is String, the argument is NOT in double-quotes.
		assertThat(logged.output()).contains("firstParameter: null");
		assertThat(logged.output()).contains("secondParameter: null");
	}

	@Test
   @Ignore
	public void logRequest_whenSomethingGoesWrong_logsAWarningAboutIt() throws Exception {
		JoinPoint callToMethod0 = mockCallTo(advisee, "method0");
		// inject an error through a call that, by now, the advice is dependent.
		when(callToMethod0.getSignature()).thenThrow(new RuntimeException("oops!"));

		subject.logRequest(callToMethod0);

		List<LoggingEvent> loggingEvents = logged.events();

		assertThat(loggingEvents.get(0).getLevel()).isEqualTo(Level.WARN);
		assertThat(loggingEvents.get(0).getMessage()).containsIgnoringCase("unable to generate");
		assertThat(loggingEvents.get(0).getThrowableProxy()).withFailMessage("Expected exception to be logged but was not.").isNotNull();
		assertThat(loggingEvents.get(0).getThrowableProxy().getMessage()).contains("oops!");
	}

	@Test
   @Ignore
	public void logResponse_includesTheNameOfTheMethodInvoked_0() throws Exception {
		subject.logResponse(mockCallTo(advisee, "method0"), null);

		assertThat(logged.output()).contains("method0");
	}

	@Test
   @Ignore
	public void logResponse_includesTheNameOfTheMethodInvoked_1() throws Exception {
		subject.logResponse(mockCallTo(advisee, "method1", Lists.newArrayList()), null);

		assertThat(logged.output()).contains("method1");
	}

	@Test
   @Ignore
	public void logResponse_includesTheResult() throws Exception {
		subject.logResponse(mockCallTo(advisee, "method2", "", new SomeType("")), "result");

		assertThat(logged.output()).contains("==> \"result\"");
	}

	@Test
   @Ignore
	public void logResponse_whenResultIsNull_logsAsNull() throws Exception {
		subject.logResponse(mockCallTo(advisee, "method2", "", new SomeType("")), null);

		assertThat(logged.output()).contains("==> null");
	}

	@Test
   @Ignore
	public void logResponse_whenSomethingGoesWrong_logsAWarningAboutIt() throws Exception {
		JoinPoint callToMethod0 = mockCallTo(advisee, "method0");
		// inject an error through a call that, by now, the advice is dependent.
		when(callToMethod0.getSignature()).thenThrow(new RuntimeException("oops!"));

		subject.logResponse(callToMethod0, "result");

		List<LoggingEvent> loggingEvents = logged.events();

		assertThat(loggingEvents.get(0).getLevel()).isEqualTo(Level.WARN);
		assertThat(loggingEvents.get(0).getMessage()).containsIgnoringCase("unable to generate");
		assertThat(loggingEvents.get(0).getThrowableProxy()).withFailMessage("Expected exception to be logged but was not.").isNotNull();
		assertThat(loggingEvents.get(0).getThrowableProxy().getMessage()).contains("oops!");
	}

	/**
	 * Let's limit how much of each argument we render so that terse logging remains ... terse.
	 */
	@Test
   @Ignore
	public void logRequest_whenInfoOrMoreTerse_truncatesLogMessagesThatExceedTerseMaxArgumentSize() throws Exception {
		rootLogger.setLevel(Level.INFO);
		subject.setTerseMaxArgumentSize(5); // artificially small to facilitate testing

		subject.logRequest(mockCallTo(advisee, "method2", "firstArg", new SomeType("secondArg")));

		assertThat(logged.output()).doesNotContain("firstArg").contains("\"first[...]\"");
		assertThat(logged.output()).doesNotContain("secondArg").contains("secon[...]");
	}

	/**
	 * Set an upper-bound to just how much logging we'll spew.
	 *
	 * Given that this advise targets calls from the web (i.e. untrustworthy source), this is secure coding.
	 */
	@Test
   @Ignore
	public void logRequest_whenDebugOrMoreVerbose_truncatesArgumentsThatExceedVerboseMaxArgumentSize() throws Exception {
		rootLogger.setLevel(Level.DEBUG);
		subject.setVerboseMaxArgumentSize(8); // artificially small to facilitate testing

		subject.logRequest(mockCallTo(advisee, "method2", "firstArg", new SomeType("secondArg")));

		assertThat(logged.output()).contains("\"firstArg\"");
		assertThat(logged.output()).doesNotContain("secondArg").contains("secondAr[...]");
	}

	/**
	 * Also limit how much of the response we log, likely smaller when logging level is terse-ish.
	 */
	@Test
   @Ignore
	public void logResponse_whenInfoOrMoreTerse_truncatesResultsThatExceedTerseMaxResultSize() throws Exception {
		rootLogger.setLevel(Level.INFO);
		subject.setTerseMaxResultSize(3); // artificially small to facilitate testing

		subject.logResponse(mockCallTo(advisee, "method2", "", new SomeType("")), "result");

		assertThat(logged.output()).contains("==> \"res[...]\"");
	}

	/**
	 * Set an upper-bound to just how much logging we'll spew.
	 */
	@Test
   @Ignore
	public void logResponse_whenDebugOrMoreVerbose_truncatesResultsThatExceedVerboseMaxResultSize() throws Exception {
		rootLogger.setLevel(Level.DEBUG);
		subject.setVerboseMaxResultSize(4); // artificially small to facilitate testing

		subject.logResponse(mockCallTo(advisee, "method2", "", new SomeType("")), "result");

		assertThat(logged.output()).contains("==> \"resu[...]\"");
	}

	// Type-capturing Null Objects
	private static final class Null {
		private static final List<String> LIST = Lists.newArrayList("NULL");
		private static final SomeType SOMETYPE = new SomeType("NULL");
		private static final String STRING = "NULL";

		private static final List OBJECTS = Lists.newArrayList(LIST, SOMETYPE, STRING);

		private static Object[] outNullObjectsIn(Object[] objects) {
			return Stream.of(objects).map(o -> OBJECTS.contains(o) ? null : o).toArray();
		}
	}

	private JoinPoint mockCallTo(Advisee advisedObject, String methodName, Object... arguments) throws Exception {
		Class[] paramTypes = Stream.of(arguments).map(Object::getClass).toArray(Class[]::new);
		arguments = Null.outNullObjectsIn(arguments);
		Method method = advisedObject.getClass().getMethod(methodName, paramTypes);
		JoinPoint callToMethod = mock(JoinPoint.class, String.format("(JoinPoint on '%s')", method.getName()));
		Signature methodSignature = mock(Signature.class);

		when(callToMethod.getSignature()).thenReturn(methodSignature);
		when(methodSignature.getName()).thenReturn(method.getName());
		when(callToMethod.getArgs()).thenReturn(arguments);
		when(callToMethod.getTarget()).thenReturn(advisedObject);

		return callToMethod;
	}
}
