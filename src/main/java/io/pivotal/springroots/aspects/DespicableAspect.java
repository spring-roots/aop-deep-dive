package io.pivotal.springroots.aspects;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

@Aspect
@Component
public class DespicableAspect {

	@Around("@within(io.pivotal.springroots.aspects.Despicable)")
	public Object makeDespicable(ProceedingJoinPoint pjp) throws Throwable {
		Object result = pjp.proceed();

		if (result instanceof String) {
			result = makeDespicable((String) result, getNameFrom(pjp.getTarget()));
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

