package io.pivotal.springroots.aspects;

@Despicable
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
