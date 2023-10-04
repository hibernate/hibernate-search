/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
import java.util.stream.Stream;

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
		TestExecutionExceptionHandler {
	private List<Object[]> envArguments;
	private int envIndex = 0;
	private boolean envInitialized = false;
	private boolean envInitializationFailed = false;
	private boolean reinitOnEachTest = false;

	private List<ParameterizedTestMethodInvoker> parameterizedSetupBeforeTestInvokers;

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

								return new ParameterizedClassTestTemplateInvocationContext(
										testMethod.getName(),
										new MethodInterceptor( !envInitialized || reinitOnEachTest )
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
		if ( !envInitialized ) {
			envInitializationFailed = true;
		}
		else {
			envInitializationFailed = false;
		}

		throw throwable;
	}

	private enum StoreKey {
		TEST_TO_RUN
	}

	private class ParameterizedClassTestTemplateInvocationContext implements TestTemplateInvocationContext {
		private final String name;
		private final MethodInterceptor interceptor;

		private ParameterizedClassTestTemplateInvocationContext(String name, MethodInterceptor interceptor) {
			this.name = name;
			this.interceptor = interceptor;
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
			return List.of( interceptor );
		}
	}

	private class MethodInterceptor implements InvocationInterceptor {

		private final boolean invoke;

		private MethodInterceptor(boolean invoke) {
			this.invoke = invoke;
		}

		@Override
		public void interceptTestTemplateMethod(Invocation<Void> invocation,
				ReflectiveInvocationContext<Method> invocationContext, ExtensionContext extensionContext)
				throws Throwable {
			if ( invoke ) {
				invocation.proceed();
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
