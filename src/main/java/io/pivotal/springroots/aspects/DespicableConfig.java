package io.pivotal.springroots.aspects;

import org.springframework.aop.framework.autoproxy.DefaultAdvisorAutoProxyCreator;
import org.springframework.aop.support.DefaultPointcutAdvisor;
import org.springframework.aop.support.annotation.AnnotationMatchingPointcut;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
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
	public DefaultPointcutAdvisor adviseDespicableClasses() {
		return new DefaultPointcutAdvisor(
			new AnnotationMatchingPointcut(Despicable.class),
			new DespicableAdvice());
	}
}
