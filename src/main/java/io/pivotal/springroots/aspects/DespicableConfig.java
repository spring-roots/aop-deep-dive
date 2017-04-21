package io.pivotal.springroots.aspects;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@Configuration
@EnableAspectJAutoProxy
public class DespicableConfig {
	@Bean
	public You you() {
		return new Human("Gru");
	}

	@Bean
	public Me me() {
		return new Minion("Kevin");
	}

	@Bean
	public DespicableAspect despicableAspect() {
		return new DespicableAspect();
	}
}
