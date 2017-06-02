package io.pivotal.springroots.logging;


import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.LoggingEvent;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tackle this test suite, second.  If you have not completed {@link RequestLoggingAdviceTest},
 * do so *before* attempting this suite.
 *
 * This advice should apply:
 * - to any method annotated with {@link RequestMapping}
 * - whose containing class is *NOT* annotated with {@link DisableRequestLogging}
 */
@RunWith(SpringRunner.class)
@EnableAspectJAutoProxy
@ContextConfiguration(classes = {
	RequestLoggingAdvice.class,
	RequestLoggingAdviceWeavingTest.FakeController.class,
	RequestLoggingAdviceWeavingTest.FakeControllerOptingOutOfLogging.class
})
public class RequestLoggingAdviceWeavingTest {

	// A pair of classes with a variety of conditions to test against.
	// These were manually included in the Application Context, above.
	public static class FakeController {
		@RequestMapping
		public String methodWithRequestMapping() { return ""; }
		@RequestMapping
		public void requestMappingWithNoReturn() {}
		public String methodWithoutRequestMapping() { return ""; }
	}

	@DisableRequestLogging
	public static class FakeControllerOptingOutOfLogging {
		@RequestMapping
		public String methodWithRequestMapping() { return ""; }
		public void methodWithoutRequestMapping() {}
	}

	// the Application Context has wrapped these in a proxy, weaving in advice
	@Autowired
	private FakeController fakeController;
	@Autowired
	private FakeControllerOptingOutOfLogging fakeControllerOptingOutOfLogging;

	private LoggingSpy logged;


	@Before
	public void setUp() {
		// ensure logging from Spring does not pollute test
		((ch.qos.logback.classic.Logger) LoggerFactory.getLogger("org.springframework")).setLevel(Level.ERROR);

		// Tap into logging so that we can verify that advice occurred.
		logged = new LoggingSpy((ch.qos.logback.classic.Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME));
	}

	@Test
	public void advisesRequestMappings() throws Exception {
		fakeController.methodWithRequestMapping();

		List<LoggingEvent> loggingEvents = logged.events();

		assertThat(loggingEvents).hasSize(2);
		assertThat(loggingEvents.get(0).getMessage()).contains("methodWithRequestMapping()");
		assertThat(loggingEvents.get(1).getMessage()).contains("methodWithRequestMapping() ==>");
	}

	@Test
	public void advisesRequestMappingsWithNoReturnValue() throws Exception {
		fakeController.requestMappingWithNoReturn();

		List<LoggingEvent> loggingEvents = logged.events();

		assertThat(loggingEvents).hasSize(2);
		assertThat(loggingEvents.get(0).getMessage()).contains("requestMappingWithNoReturn()");
		assertThat(loggingEvents.get(1).getMessage()).contains("requestMappingWithNoReturn() ==>");
	}

	@Test
	public void skipsNonRequestMappings() throws Exception {
		fakeController.methodWithoutRequestMapping();
		assertThat(logged.output()).isEmpty();
	}

	@Test
	public void skipsRequestMappingsWithLoggingDisabled() throws Exception {
		fakeControllerOptingOutOfLogging.methodWithRequestMapping();
		assertThat(logged.output()).isEmpty();
	}

	@Test
	public void skipsNonRequestMappingsWithLoggingDisabled() throws Exception {
		fakeControllerOptingOutOfLogging.methodWithoutRequestMapping();
		assertThat(logged.output()).isEmpty();
	}
}

