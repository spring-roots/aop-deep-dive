package io.pivotal.springroots.aspects;

public class Despicable implements Me {
	private final Me delegate;

	public Despicable(Me me) {
		delegate = me;
	}

	@Override
	public String name() {
		return makeDespicable(delegate.name(), delegate.name());
	}

	@Override
	public String greet(String other) {
		return makeDespicable(delegate.greet(other), delegate.name());
	}

	private String makeDespicable(String string, String name) {
		return string.replaceAll(name, "Despicable " + name);
	}
}
