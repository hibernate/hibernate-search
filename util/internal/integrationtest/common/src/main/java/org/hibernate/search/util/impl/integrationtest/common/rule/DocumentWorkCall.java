/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.common.rule;

import static org.hibernate.search.util.impl.integrationtest.common.assertion.StubDocumentWorkAssert.assertThatDocumentWork;
import static org.junit.Assert.fail;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;

import org.hibernate.search.util.impl.integrationtest.common.stub.backend.index.StubDocumentWork;

class DocumentWorkCall extends Call<DocumentWorkCall> {

	enum WorkPhase {
		/**
		 * Creation of the work, in preparation for its execution.
		 */
		CREATE,

		/**
		 * Actual execution of the work.
		 * Happens after creation, unless the work was discarded.
		 */
		EXECUTE,

		/**
		 * Discarding of the work.
		 * Happens after creation, if the mapper decides to cancel some changes.
		 */
		DISCARD
	}

	private final DocumentKey documentKey;
	private final WorkPhase phase;
	private final StubDocumentWork work;
	private final CompletableFuture<?> completableFuture;

	DocumentWorkCall(String indexName, WorkPhase phase, StubDocumentWork work,
			CompletableFuture<?> completableFuture) {
		this.documentKey = new DocumentKey( indexName, work.getTenantIdentifier(), work.getIdentifier() );
		this.phase = phase;
		this.work = work;
		this.completableFuture = completableFuture;
	}

	DocumentWorkCall(String indexName, WorkPhase phase, StubDocumentWork work) {
		this( indexName, phase, work, null );
	}

	public DocumentKey documentKey() {
		return documentKey;
	}

	public CallBehavior<CompletableFuture<?>> verify(DocumentWorkCall actualCall) {
		String whenThisWorkWasExpected = "when a " + phase + " call for a work on document '" + documentKey + "' was expected";
		if ( !Objects.equals( phase, actualCall.phase ) ) {
			fail( "Incorrect work phase " + whenThisWorkWasExpected + ".\n\tExpected: "
					+ phase + ", actual: " + actualCall.phase
					+ ".\n\tExpected work: " + work + "\n\tActual work: " + actualCall.work );
		}
		assertThatDocumentWork( actualCall.work )
				.as( "Incorrect work " + whenThisWorkWasExpected + ":\n" )
				.matches( work );
		return () -> completableFuture;
	}

	@Override
	protected boolean isSimilarTo(DocumentWorkCall other) {
		return Objects.equals( phase, other.phase )
				&& Objects.equals( documentKey, other.documentKey );
	}

	@Override
	public String toString() {
		return phase + " call for a work on document '" + documentKey + "'";
	}
}
