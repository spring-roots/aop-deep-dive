package io.pivotal.springroots.aspects;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

public class DespicableAdvice implements MethodInterceptor {
	private final String name;
	public DespicableAdvice(String name) {
		this.name = name;
	}

	@Override
	public Object invoke(MethodInvocation invocation) throws Throwable {
		Object result = invocation.proceed();
		if(result instanceof String) {
			result = makeDespicable((String) result, name);
		}
		return result;
	}

	private String makeDespicable(String string, String name) {
		return string.replaceAll(name, "Despicable " + name);
	}
}
