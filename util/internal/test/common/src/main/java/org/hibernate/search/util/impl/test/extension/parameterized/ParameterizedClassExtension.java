/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.util.impl.test.extension.parameterized;

import static java.util.Spliterators.spliteratorUnknownSize;
import static java.util.stream.StreamSupport.stream;
import static org.hibernate.search.util.impl.test.extension.parameterized.ParameterizedClassTestMethodDiscoverer.discover;
import static org.hibernate.search.util.impl.test.extension.parameterized.ParameterizedClassTestMethodDiscoverer.discoverBeforeTest;
import static org.junit.platform.commons.util.AnnotationUtils.isAnnotated;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.hibernate.search.util.impl.test.extension.ExtensionScope;

import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.Extension;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.InvocationInterceptor;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.junit.jupiter.api.extension.ReflectiveInvocationContext;
import org.junit.jupiter.api.extension.TestExecutionExceptionHandler;
import org.junit.jupiter.api.extension.TestTemplateInvocationContext;
import org.junit.jupiter.api.extension.TestTemplateInvocationContextProvider;

final class ParameterizedClassExtension
		implements TestTemplateInvocationContextProvider, ParameterResolver,
		TestExecutionExceptionHandler, InvocationInterceptor {
	private List<Object[]> envArguments;
	private int envIndex = 0;
	private boolean envInitialized = false;
	private boolean envInitializationFailed = false;
	private boolean reinitOnEachTest = false;

	private List<ParameterizedTestMethodInvoker> parameterizedSetupBeforeTestInvokers;

	public static boolean areThereMoreTestsForCurrentConfigurations(ExtensionContext extensionContext) {
		return read( extensionContext, StoreKey.MORE_TESTS_AVAILABLE, boolean.class );
	}

	@Override
	public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext)
			throws ParameterResolutionException {
		return true;
	}

	@Override
	public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext)
			throws ParameterResolutionException {
		return envArguments.get( envIndex )[parameterContext.getIndex()];
	}

	@Override
	public boolean supportsTestTemplate(ExtensionContext context) {
		return isAnnotated( context.getTestMethod(), ParameterizedSetup.class );
	}

	@Override
	public Stream<TestTemplateInvocationContext> provideTestTemplateInvocationContexts(ExtensionContext context) {
		Class<?> testClass = context.getTestClass().orElseThrow();
		// find actual "tests" that we'll invoke via reflection:
		List<ParameterizedTestMethodInvoker> testMethods = discover( testClass, context );

		this.parameterizedSetupBeforeTestInvokers = discoverBeforeTest( testClass, context );

		reinitOnEachTest = context.getTestInstanceLifecycle()
				.map( TestInstance.Lifecycle.PER_METHOD::equals )
				.orElse( Boolean.FALSE );

		if ( testMethods.isEmpty() ) {
			throw new IllegalStateException( "No tests to execute were found." );
		}

		envArguments = new ArrayList<>();
		ParameterizedClassUtils.findParameters( envArguments, context, context.getRequiredTestMethod() );

		Consumer<ExtensionScope> scopeModifier = ExtensionScope.currentScopeModifier( context );

		return stream(
				spliteratorUnknownSize(
						new Iterator<>() {

							Iterator<ParameterizedTestMethodInvoker> test = testMethods.iterator();

							@Override
							public boolean hasNext() {
								if ( test.hasNext() ) {
									return true;
								}
								else {
									envIndex++;
									envInitialized = false;
									envInitializationFailed = false;
								}

								if ( envIndex < envArguments.size() ) {
									test = testMethods.iterator();
									return test.hasNext();
								}
								return false;
							}

							@Override
							public TestTemplateInvocationContext next() {
								ParameterizedTestMethodInvoker testMethod = test.next();
								write( context, StoreKey.TEST_TO_RUN, testMethod );
								write( context, StoreKey.MORE_TESTS_AVAILABLE, !test.hasNext() );

								return new ParameterizedClassTestTemplateInvocationContext(
										testMethod.getName(),
										List.of( new MethodInterceptor( scopeModifier, !envInitialized || reinitOnEachTest ) )
								);
							}
						}, Spliterator.NONNULL
				), false );
	}

	private static void write(ExtensionContext context, StoreKey key, Object value) {
		ExtensionContext.Store store = context.getRoot().getStore( extensionNamespace( context ) );
		store.put( key, value );
	}

	private static <T> T read(ExtensionContext context, StoreKey key, Class<T> clazz) {
		ExtensionContext.Store store = context.getRoot().getStore( extensionNamespace( context ) );
		return store.get( key, clazz );
	}

	private static ExtensionContext.Namespace extensionNamespace(ExtensionContext context) {
		return ExtensionContext.Namespace.create( context.getRequiredTestClass(), ParameterizedClassExtension.class );
	}

	@Override
	public void handleTestExecutionException(ExtensionContext extensionContext, Throwable throwable) throws Throwable {
		envInitializationFailed = !envInitialized;
		throw throwable;
	}

	private enum StoreKey {
		TEST_TO_RUN,
		MORE_TESTS_AVAILABLE;
	}

	private class ParameterizedClassTestTemplateInvocationContext implements TestTemplateInvocationContext {
		private final String name;
		private final List<Extension> extensions;

		private ParameterizedClassTestTemplateInvocationContext(String name, List<Extension> extension) {
			this.name = name;
			this.extensions = extension;
		}

		@Override
		public String getDisplayName(int invocationIndex) {
			// setup is not necessarily parameterized
			if ( envArguments.isEmpty() ) {
				return name;
			}
			return "Configuration #" + envIndex + ": " + Arrays.toString( envArguments.get( envIndex ) ) + " : " + name;
		}

		@Override
		public List<Extension> getAdditionalExtensions() {
			return extensions;
		}
	}

	private class MethodInterceptor implements InvocationInterceptor {

		private final Consumer<ExtensionScope> scopeModifier;
		private final boolean invoke;

		private MethodInterceptor(Consumer<ExtensionScope> scopeModifier, boolean invoke) {
			this.scopeModifier = scopeModifier;
			this.invoke = invoke;
		}

		@Override
		public void interceptTestTemplateMethod(Invocation<Void> invocation,
				ReflectiveInvocationContext<Method> invocationContext, ExtensionContext extensionContext)
				throws Throwable {
			if ( invoke ) {
				scopeModifier.accept( ExtensionScope.PARAMETERIZED_CLASS_SETUP );
				invocation.proceed();
				scopeModifier.accept( ExtensionScope.TEST );
			}
			else {
				invocation.skip();
			}

			for ( ParameterizedTestMethodInvoker beforeTestInvoker : parameterizedSetupBeforeTestInvokers ) {
				beforeTestInvoker.invoke( extensionContext.getRequiredTestInstance() );
			}

			// that's where we actually execute the test:
			ParameterizedTestMethodInvoker testMethod =
					read( extensionContext, StoreKey.TEST_TO_RUN, ParameterizedTestMethodInvoker.class );
			testMethod.invoke( extensionContext.getRequiredTestInstance() );
			envInitialized = !envInitializationFailed;
		}
	}
}
