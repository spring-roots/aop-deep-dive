package io.pivotal.springroots.aspects;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Despicable
@Component
public class Human implements You {
	private final String name;

	public Human(@Value("${names.human}") String name) {
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
