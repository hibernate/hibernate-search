/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.util.impl.test.extension.parameterized;

import static org.hibernate.search.util.impl.test.extension.parameterized.ParameterizedClassUtils.findParameters;
import static org.junit.platform.commons.util.AnnotationUtils.isAnnotated;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.platform.commons.util.AnnotationUtils;

class ParameterizedClassTestMethodDiscoverer {

	private ParameterizedClassTestMethodDiscoverer() {
	}

	public static List<ParameterizedTestMethodInvoker> discover(Class<?> testClass, ExtensionContext extensionContext) {
		List<ParameterizedTestMethodInvoker> testMethods = new ArrayList<>();
		findTests( testClass, extensionContext, testMethods );

		return testMethods;
	}

	public static List<ParameterizedTestMethodInvoker> discoverBeforeTest(Class<?> testClass,
			ExtensionContext extensionContext) {
		List<ParameterizedTestMethodInvoker> invokers = new ArrayList<>();
		findBeforeTests( testClass, extensionContext, invokers );

		return invokers;
	}

	private static void findBeforeTests(Class<?> testClass, ExtensionContext extensionContext,
			List<ParameterizedTestMethodInvoker> beforeTestMethods) {
		if ( Object.class.equals( testClass ) ) {
			return;
		}
		for ( Method method : testClass.getDeclaredMethods() ) {
			if ( isAnnotated( method, ParameterizedSetupBeforeTest.class ) ) {
				beforeTestMethods.add( toBeforeTestMethods( method, extensionContext ) );
			}
		}
		findBeforeTests( testClass.getSuperclass(), extensionContext, beforeTestMethods );
	}

	private static void findTests(Class<?> testClass, ExtensionContext extensionContext,
			List<ParameterizedTestMethodInvoker> testMethods) {
		if ( Object.class.equals( testClass ) ) {
			return;
		}
		for ( Method method : testClass.getDeclaredMethods() ) {
			if ( ParameterizedClassUtils.isActualTestMethodToExecute( method ) ) {
				testMethods.addAll( toTestMethods( method, extensionContext ) );
			}
		}
		findTests( testClass.getSuperclass(), extensionContext, testMethods );
	}

	private static List<ParameterizedTestMethodInvoker> toTestMethods(Method method, ExtensionContext extensionContext) {
		if ( AnnotationUtils.isAnnotated( method, Test.class ) ) {
			if ( method.getParameterCount() != 0 ) {
				throw new IllegalStateException(
						method + " is annotated with @Test but has arguments. "
								+ "Change it to a @Parameterized test and add a parameter source, or remove the arguments from the test method." );
			}
			return List.of( new TestMethod( method ) );
		}
		else if ( AnnotationUtils.isAnnotated( method, ParameterizedTest.class ) ) {
			ArrayList<Object[]> arguments = new ArrayList<>();
			findParameters( arguments, extensionContext, method );

			if ( arguments.isEmpty() ) {
				throw new IllegalStateException(
						"Unable to produce test arguments for a parameterized test: " + method );
			}
			return arguments.stream().map( arg -> new ParameterizedTestMethod( method, arg ) )
					.collect( Collectors.toList() );
		}
		else {
			throw new IllegalStateException( "Unsupported test type. " + method );
		}
	}

	private static ParameterizedTestMethodInvoker toBeforeTestMethods(Method method, ExtensionContext extensionContext) {
		if ( method.getParameterCount() > 0 ) {
			throw new IllegalStateException( "ParameterizedSetupBeforeTest methods cannot have parameters. " + method );
		}
		else {
			return new TestMethod( method );
		}
	}


	private static class TestMethod implements ParameterizedTestMethodInvoker {
		protected final Method method;

		private TestMethod(Method method) {
			this.method = method;
			this.method.setAccessible( true );
		}

		@Override
		public String getName() {
			return method.getName() + "()";
		}

		@Override
		public void invoke(Object requiredTestInstance) throws Throwable {
			try {
				method.invoke( requiredTestInstance );
			}
			catch (InvocationTargetException e) {
				// We don't need to wrap an exception in InvocationTargetException.
				// Especially since we might throw an assumption failure exception that needs to be not wrapped for JUnit to correctly process it.
				throw e.getCause();
			}
		}

		@Override
		public String toString() {
			return "TestMethod{" +
					"method=" + method +
					'}';
		}
	}

	private static class ParameterizedTestMethod extends TestMethod {

		private final Object[] arguments;

		private ParameterizedTestMethod(Method method, Object[] arguments) {
			super( method );
			this.arguments = arguments;
		}

		@Override
		public void invoke(Object requiredTestInstance) throws InvocationTargetException, IllegalAccessException {
			method.invoke( requiredTestInstance, arguments );
		}

		@Override
		public String getName() {
			return super.getName() + " : parameters: " + Arrays.toString( arguments );
		}

		@Override
		public String toString() {
			return "ParameterizedTestMethod{" +
					"method=" + method +
					", arguments=" + Arrays.toString( arguments ) +
					'}';
		}
	}
}
