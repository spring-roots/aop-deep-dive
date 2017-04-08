package io.pivotal.springroots.aspects;

import org.springframework.aop.framework.autoproxy.DefaultAdvisorAutoProxyCreator;
import org.springframework.aop.support.DefaultPointcutAdvisor;
import org.springframework.aop.support.annotation.AnnotationMatchingPointcut;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
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
	public DefaultAdvisorAutoProxyCreator proxyAllTheBeans() {
		return new DefaultAdvisorAutoProxyCreator();
	}

	@Bean
	public DefaultPointcutAdvisor adviseDespicableClasses() {
		return new DefaultPointcutAdvisor(
			new AnnotationMatchingPointcut(Despicable.class),
			new DespicableAdvice());
	}
}
