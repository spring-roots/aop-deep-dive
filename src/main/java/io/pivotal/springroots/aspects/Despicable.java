package io.pivotal.springroots.aspects;

public class Despicable implements Me {
	private final Me delegate;

	public Despicable(Me me) {
		delegate = me;
	}

	@Override
	public String name() {
		return "Despicable " + delegate.name();
	}

	@Override
	public String greet(String other) {
		StringBuilder greeting = new StringBuilder(delegate.greet(other));
		greeting.insert(greeting.indexOf(delegate.name()), "Despicable ");
		return greeting.toString();
	}
}
