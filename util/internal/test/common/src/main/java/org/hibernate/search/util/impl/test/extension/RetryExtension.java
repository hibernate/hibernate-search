/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.util.impl.test.extension;

import static java.util.Spliterators.spliteratorUnknownSize;
import static java.util.stream.StreamSupport.stream;
import static org.junit.platform.commons.util.AnnotationUtils.findAnnotation;
import static org.junit.platform.commons.util.AnnotationUtils.isAnnotated;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Iterator;
import java.util.List;
import java.util.Spliterator;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.Extension;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestExecutionExceptionHandler;
import org.junit.jupiter.api.extension.TestTemplateInvocationContext;
import org.junit.jupiter.api.extension.TestTemplateInvocationContextProvider;

import org.opentest4j.TestAbortedException;

public final class RetryExtension
		implements TestTemplateInvocationContextProvider, BeforeEachCallback,
		TestExecutionExceptionHandler {

	@Target({ ElementType.METHOD })
	@Retention(RetentionPolicy.RUNTIME)
	@TestTemplate
	@ExtendWith(RetryExtension.class)
	public @interface TestWithRetry {
		int retries() default 3;

		int timeout() default 0;

		TimeUnit timeoutUnits() default TimeUnit.SECONDS;
	}

	private enum StoreKey {
		FAILED,
		LAST_ITERATION;
	}

	@Override
	public boolean supportsTestTemplate(ExtensionContext context) {
		return isAnnotated( context.getTestMethod(), TestWithRetry.class );
	}

	@Override
	public Stream<TestTemplateInvocationContext> provideTestTemplateInvocationContexts(ExtensionContext context) {
		TestWithRetry retry = context.getTestMethod()
				.flatMap( method -> findAnnotation( method, TestWithRetry.class ) )
				.orElseThrow( IllegalStateException::new );

		int retries = retry.retries();
		int timeout = retry.timeout();
		TimeUnit timeUnit = retry.timeoutUnits();

		return stream( spliteratorUnknownSize(
				new Iterator<TestTemplateInvocationContext>() {
					private int iteration = 0;

					@Override
					public boolean hasNext() {
						Boolean failed = read( context, StoreKey.FAILED, Boolean.class );
						return !( iteration != 0 && !Boolean.TRUE.equals( failed ) ) && iteration < retries;
					}

					@Override
					public TestTemplateInvocationContext next() {
						Boolean failed = read( context, StoreKey.FAILED, Boolean.class );
						if ( Boolean.TRUE.equals( failed ) ) {
							try {
								timeUnit.sleep( timeout );
							}
							catch (InterruptedException e) {
								Thread.currentThread().interrupt();
							}
						}
						iteration++;
						save( context, StoreKey.LAST_ITERATION, iteration == retries );

						return new TestTemplateInvocationContext() {

							@Override
							public String getDisplayName(int invocationIndex) {
								return TestTemplateInvocationContext.super.getDisplayName( invocationIndex );
							}

							@Override
							public List<Extension> getAdditionalExtensions() {
								return TestTemplateInvocationContext.super.getAdditionalExtensions();
							}
						};
					}
				}, Spliterator.NONNULL
		), false );
	}

	@Override
	public void beforeEach(ExtensionContext context) {
		save( context, StoreKey.FAILED, false );
	}

	@Override
	public void handleTestExecutionException(ExtensionContext context, Throwable throwable) throws Throwable {
		if ( !TestAbortedException.class.isAssignableFrom( throwable.getClass() ) ) {
			save( context, StoreKey.FAILED, true );

			if ( Boolean.TRUE.equals( read( context, StoreKey.LAST_ITERATION, Boolean.class ) ) ) {
				throw throwable;
			}
			throw new TestAbortedException( "Attempt failed: " + throwable.getMessage(), throwable );
		}
	}

	private void save(ExtensionContext context, StoreKey key, Object value) {
		ExtensionContext.Store store = context.getRoot().getStore(
				ExtensionContext.Namespace.create( context.getRequiredTestMethod() )
		);
		store.put( key, value );
	}

	private <T> T read(ExtensionContext context, StoreKey key, Class<T> clazz) {
		ExtensionContext.Store store = context.getRoot().getStore(
				ExtensionContext.Namespace.create( context.getRequiredTestMethod() )
		);
		return store.get( key, clazz );
	}
}
