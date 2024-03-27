/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.reporting.spi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.hibernate.search.util.common.SearchException;

import org.junit.jupiter.api.Test;

class RootFailureCollectorTest {

	@Test
	void simple() {
		RootFailureCollector rootFailureCollector = new RootFailureCollector( "RootName" );
		List<String> messages = new ArrayList<>();
		List<Throwable> exceptions = new ArrayList<>();
		for ( int i = 0; i < 30; i++ ) {
			ContextualFailureCollector failureCollector = rootFailureCollector.withContext(
					EventContexts.fromType( "Type #" + i ) );
			if ( i % 3 == 0 ) {
				String message = "Failure with RuntimeException #" + i;
				messages.add( message );
				Throwable exception = new RuntimeException( message );
				exceptions.add( exception );
				failureCollector.add( exception );
			}
			else if ( i % 3 == 1 ) {
				String message = "Failure with SearchException #" + i;
				messages.add( message );
				Throwable exception = new SearchException( message, EventContexts.fromIndexFieldAbsolutePath( "path" + i ) );
				exceptions.add( exception );
				failureCollector.add( exception );
			}
			else {
				String message = "Failure without exception #" + i;
				messages.add( message );
				failureCollector.add( message );
			}
		}
		for ( int i = 0; i < 10; i++ ) {
			ContextualFailureCollector failureCollector = rootFailureCollector.withContext(
					EventContexts.fromType( "Type #" + i ) );
			failureCollector.add( "Error #" + i );
		}
		assertThatThrownBy( rootFailureCollector::checkNoFailure )
				// Check that we mention that some failures are not being reported
				.hasMessageContaining( "Hibernate Search encountered failures during RootName" )
				.hasMessageContainingAll( messages.toArray( CharSequence[]::new ) )
				.satisfies( e -> assertThat( e.getSuppressed() ).containsExactlyInAnyOrderElementsOf( exceptions ) );
	}

	/**
	 * Triggers many more failures than the failure limit.
	 * <p>
	 * Only the first {@value RootFailureCollector#FAILURE_LIMIT} failures should be reported.
	 */
	@Test
	void failureLimit() {
		RootFailureCollector rootFailureCollector = new RootFailureCollector( "RootName" );
		for ( int i = 0; i < RootFailureCollector.FAILURE_LIMIT; i++ ) {
			ContextualFailureCollector failureCollector = rootFailureCollector.withContext(
					EventContexts.fromType( "Type #" + i ) );
			failureCollector.add( "Error #" + i );
		}
		for ( int i = 0; i < 10; i++ ) {
			ContextualFailureCollector failureCollector = rootFailureCollector.withContext(
					EventContexts.fromType( "Type #" + i ) );
			failureCollector.add( "Error #" + i );
		}
		assertThatThrownBy( rootFailureCollector::checkNoFailure )
				// Check that we mention that some failures are not being reported
				.hasMessageContainingAll( "Hibernate Search encountered " + ( RootFailureCollector.FAILURE_LIMIT + 10 )
						+ " failures during RootName",
						"Only the first " + RootFailureCollector.FAILURE_LIMIT + " failures are displayed here",
						"See the TRACE logs for extra failures" )
				// Check that we didn't report failures after the limit was reached
				.message().satisfies( message -> {
					assertThat( countOccurrences( message, "Error #" ) )
							.as( "Number of errors reported" ).isEqualTo( RootFailureCollector.FAILURE_LIMIT );
				} );
	}

	/**
	 * Triggers many more failures than the failure limit
	 * in concurrent tasks that create contextual failure collectors then add a failure.
	 * <p>
	 * This used to lead to a deadlock, because we would:
	 * <ul>
	 *     <li>lock on a child to add a failure</li>
	 *     <li>check on the root whether there are more failures than the limit</li>
	 *     <li>lock on the root to access the children</li>
	 *     <li>try to lock on each child one after the other,
	 *     to render their failures and include that in the "failure limit reached" exception message</li>
	 * </ul>
	 * Do that concurrently from two different children, and you're likely to end up with a deadlock.
	 */
	@Test
	void failureLimit_concurrency() {
		RootFailureCollector rootFailureCollector = new RootFailureCollector( "RootName" );
		List<Runnable> runnables = IntStream.range( 0, RootFailureCollector.FAILURE_LIMIT + 1000 )
				.mapToObj( i -> (Runnable) () -> {
					ContextualFailureCollector failureCollector = rootFailureCollector.withContext(
							EventContexts.fromType( "Type #" + i ) );
					failureCollector.add( "Error #" + i );
				} )
				.collect( Collectors.toList() );
		ForkJoinPool pool = new ForkJoinPool( 10 );
		List<ForkJoinTask<?>> tasks = runnables.stream().map( pool::submit )
				.collect( Collectors.toList() );
		await().atMost( 2, TimeUnit.SECONDS ).until( pool::isQuiescent );
		pool.shutdownNow();
		assertThat( tasks )
				.hasSameSizeAs( runnables )
				.allSatisfy( task -> assertThat( task ).isDone() );
		assertThatThrownBy( rootFailureCollector::checkNoFailure )
				// Check that we mention that some failures are not being reported
				.hasMessageContainingAll( "Hibernate Search encountered " + ( RootFailureCollector.FAILURE_LIMIT + 1000 )
						+ " failures during RootName",
						"Only the first " + RootFailureCollector.FAILURE_LIMIT + " failures are displayed here",
						"See the TRACE logs for extra failures" )
				// Check that we didn't report failures after the limit was reached
				.message().satisfies( message -> {
					assertThat( countOccurrences( message, "Error #" ) )
							.as( "Number of errors reported" ).isEqualTo( RootFailureCollector.FAILURE_LIMIT );
				} );
	}

	private int countOccurrences(String message, String substring) {
		int count = 0;
		int currentIndex = message.indexOf( substring );
		while ( currentIndex >= 0 ) {
			++count;
			currentIndex = message.indexOf( substring, currentIndex + 1 );
		}
		return count;
	}

}
