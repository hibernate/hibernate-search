/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.common.rule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;

import org.hibernate.search.util.common.impl.Throwables;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.index.StubDocumentWork;

import org.awaitility.core.ThrowingRunnable;

public final class BackendIndexingWorkExpectations {

	public static BackendIndexingWorkExpectations sync() {
		return new BackendIndexingWorkExpectations( true, null, StubDocumentWork.Type.ADD );
	}

	public static BackendIndexingWorkExpectations async(String threadNamePattern) {
		return async( threadNamePattern, StubDocumentWork.Type.ADD );
	}

	public static BackendIndexingWorkExpectations async(String threadNamePattern, StubDocumentWork.Type addWorkType) {
		return new BackendIndexingWorkExpectations( false, Pattern.compile( threadNamePattern ), addWorkType );
	}

	private final boolean sync;
	private final Pattern expectedThreadNamePattern;
	final StubDocumentWork.Type addWorkType;

	private BackendIndexingWorkExpectations(boolean sync, Pattern expectedThreadNamePattern,
			StubDocumentWork.Type addWorkType) {
		this.sync = sync;
		this.expectedThreadNamePattern = expectedThreadNamePattern;
		this.addWorkType = addWorkType;
	}

	public boolean allowDuplicateIndexing() {
		return !sync;
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
			await( "Waiting for indexing assertions" )
					.pollDelay( Duration.ZERO )
					// Most of the time the assertions
					// are only about in-memory state (i.e. the CallQueues in BackendMock),
					// so it's fine to poll aggressively every 5ms.
					.pollInterval( Duration.ofMillis( 5 ) )
					.atMost( Duration.ofSeconds( 30 ) )
					.untilAsserted( assertions );
		}
	}

	public void awaitBackgroundIndexingCompletion(CompletableFuture<?> completion) {
		if ( sync ) {
			return;
		}
		else {
			await( "Waiting for background process completion" )
					.pollDelay( Duration.ZERO )
					// We're only waiting for in-memory state to change,
					// so it's fine to poll aggressively every 5ms.
					.pollInterval( Duration.ofMillis( 5 ) )
					.atMost( Duration.ofSeconds( 30 ) )
					.until( completion::isDone );
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
