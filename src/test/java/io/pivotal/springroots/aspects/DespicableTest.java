package io.pivotal.springroots.aspects;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class DespicableTest {
	@Test
	@DisplayName("Decorated Me is despicable.")
	public void makesMeDespicable() {
		Me me = new Minion("Kevin");
		Me despicableMe = new Despicable(me);

		assertThat(me.name()).isEqualTo("Kevin");
		assertThat(me.greet("World")).isEqualTo("Hello, World, it is I, Kevin!");

		assertThat(despicableMe.name()).isEqualTo("Despicable Kevin");
		assertThat(despicableMe.greet("World")).isEqualTo("Hello, World, it is I, Despicable Kevin!");
	}
}
