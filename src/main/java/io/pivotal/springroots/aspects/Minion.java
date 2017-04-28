package io.pivotal.springroots.aspects;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Despicable
@Component
public class Minion implements Me {
	private final String name;

	public Minion(@Value("${names.minion}") String name) {
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
