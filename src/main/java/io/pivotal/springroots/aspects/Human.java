package io.pivotal.springroots.aspects;

@Despicable
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
