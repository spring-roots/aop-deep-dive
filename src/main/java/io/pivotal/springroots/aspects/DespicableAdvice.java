package io.pivotal.springroots.aspects;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class DespicableAdvice implements MethodInterceptor {
	@Override
	public Object invoke(MethodInvocation invocation) throws Throwable {
		Object result = invocation.proceed();
		if (result instanceof String) {
			result = makeDespicable((String) result, getNameFrom(invocation.getThis()));
		}
		return result;
	}

	private String makeDespicable(String string, String name) {
		return string.replaceAll(name, "Despicable " + name);
	}

	private String getNameFrom(Object target) {
		String name;
		try {
			Method nameGetter = target.getClass().getMethod("name");
			name = (String) nameGetter.invoke(target);
		} catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException excp) {
			throw new RuntimeException(String.format("Unable to make %s despicable.", target.toString()), excp);
		}
		return name;
	}
}
