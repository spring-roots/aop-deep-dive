package io.pivotal.springroots.logging;

import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.read.ListAppender;

import java.util.List;

import static java.util.stream.Collectors.joining;

public class LoggingSpy {
	private ListAppender<LoggingEvent> appender;

	public LoggingSpy(ch.qos.logback.classic.Logger logger) {
		appender = new ListAppender<>();
		appender.setName("LoggingSpy");
		appender.start();
		logger.addAppender((Appender) appender);
	}

	public String output() {
		return appender.list.stream()
			.map(LoggingEvent::getMessage)
			.collect(joining("\n"));
	}

	public List<LoggingEvent> events() {
		return appender.list;
	}
}
