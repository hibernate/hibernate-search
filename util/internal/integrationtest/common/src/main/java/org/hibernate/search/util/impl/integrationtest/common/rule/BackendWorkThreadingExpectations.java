/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.common.rule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.awaitility.pollinterval.IterativePollInterval.iterative;

import java.time.Duration;
import java.util.regex.Pattern;

import org.hibernate.search.util.common.impl.Throwables;

import org.awaitility.core.ThrowingRunnable;

public final class BackendWorkThreadingExpectations {

	public static BackendWorkThreadingExpectations sync() {
		return new BackendWorkThreadingExpectations( true, null );
	}

	public static BackendWorkThreadingExpectations async(String threadNamePattern) {
		return new BackendWorkThreadingExpectations( false, Pattern.compile( threadNamePattern ) );
	}

	private final boolean sync;
	private final Pattern expectedThreadNamePattern;

	private BackendWorkThreadingExpectations(boolean sync, Pattern expectedThreadNamePattern) {
		this.sync = sync;
		this.expectedThreadNamePattern = expectedThreadNamePattern;
	}

	public void awaitIndexingAssertions(ThrowingRunnable assertions) {
		if ( sync ) {
			try {
				assertions.run();
			}
			catch (Throwable t) {
				throw Throwables.toRuntimeException( t );
			}
		}
		else {
			await().pollDelay( Duration.ZERO )
					.pollInterval( iterative( duration -> duration.multipliedBy( 2 ), Duration.ofMillis( 5 ) ) )
					.atMost( Duration.ofSeconds( 5 ) )
					.untilAsserted( assertions );
		}
	}

	void checkCurrentThread(Object work) {
		if ( expectedThreadNamePattern == null ) {
			return;
		}
		assertThat( Thread.currentThread().getName() )
				.as( "Name of current thread when executing work " + work )
				.matches( expectedThreadNamePattern );
	}
}
