/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.reporting.spi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.hibernate.search.util.common.SearchException;

import org.junit.Test;

public class RootFailureCollectorTest {

	/**
	 * Triggers many more failures than the failure limit.
	 * <p>
	 * Only the first {@value RootFailureCollector#FAILURE_LIMIT} failures should be reported.
	 */
	@Test
	public void failureLimit() {
		RootFailureCollector rootFailureCollector = new RootFailureCollector( "RootName" );
		for ( int i = 0; i < RootFailureCollector.FAILURE_LIMIT; i++ ) {
			ContextualFailureCollector failureCollector = rootFailureCollector.withContext(
					EventContexts.fromType( "Type #" + i ) );
			failureCollector.add( "Error #" + i );
		}
		for ( int i = 0; i < 10; i++ ) {
			ContextualFailureCollector failureCollector = rootFailureCollector.withContext(
					EventContexts.fromType( "Type #" + i ) );
			int finalI = i;
			assertThatThrownBy( () -> failureCollector.add( "Error #" + finalI ) )
					.isInstanceOf( SearchException.class )
					.hasMessageContainingAll( "Hibernate Search encountered failures during RootName",
							"Stopped collecting failures after " + RootFailureCollector.FAILURE_LIMIT + " failures",
							"Currently at " + ( RootFailureCollector.FAILURE_LIMIT + i + 1 ) + " failures and counting",
							"Failures:" );
		}
		// Check that we didn't report failures after the limit was reached
		assertThatThrownBy( rootFailureCollector::checkNoFailure )
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