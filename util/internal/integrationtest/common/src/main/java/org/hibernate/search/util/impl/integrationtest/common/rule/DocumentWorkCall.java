/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.common.rule;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;

import org.hibernate.search.util.impl.integrationtest.common.stub.backend.index.StubDocumentWork;
import org.hibernate.search.util.impl.integrationtest.common.assertion.StubDocumentWorkAssert;

import org.junit.Assert;

class DocumentWorkCall extends Call<DocumentWorkCall> {

	enum WorkPhase {
		/**
		 * Processing of the work, in preparation for its execution.
		 */
		PROCESS,

		/**
		 * Actual execution of the work.
		 * Happens after processing, unless the work was discarded.
		 */
		EXECUTE,

		/**
		 * Discarding of the work.
		 * Happens after processing, if the mapper decides to cancel some changes.
		 */
		DISCARD
	}

	private final String indexName;
	private final WorkPhase phase;
	private final StubDocumentWork work;
	private final CompletableFuture<?> completableFuture;

	DocumentWorkCall(String indexName, WorkPhase phase, StubDocumentWork work,
			CompletableFuture<?> completableFuture) {
		this.indexName = indexName;
		this.phase = phase;
		this.work = work;
		this.completableFuture = completableFuture;
	}

	DocumentWorkCall(String indexName, WorkPhase phase, StubDocumentWork work) {
		this.indexName = indexName;
		this.phase = phase;
		this.work = work;
		this.completableFuture = null;
	}

	public CallBehavior<CompletableFuture<?>> verify(DocumentWorkCall actualCall) {
		String whenThisWorkWasExpected = "when a " + phase + " call for a document work on index '" + indexName
				+ "', identifier '" + work.getIdentifier() + "' was expected";
		if ( !Objects.equals( phase, actualCall.phase ) ) {
			Assert.fail( "Incorrect work phase " + whenThisWorkWasExpected + ".\n\tExpected: "
					+ phase + ", actual: " + actualCall.phase
					+ ".\n\tExpected work: " + work + "\n\tActual work: " + actualCall.work );
		}
		StubDocumentWorkAssert.assertThat( actualCall.work )
				.as( "Incorrect work " + whenThisWorkWasExpected + ":\n" )
				.matches( work );
		return () -> completableFuture;
	}

	@Override
	protected boolean isSimilarTo(DocumentWorkCall other) {
		return Objects.equals( phase, other.phase )
				&& Objects.equals( indexName, other.indexName )
				&& Objects.equals( work.getTenantIdentifier(), other.work.getTenantIdentifier() )
				&& Objects.equals( work.getIdentifier(), other.work.getIdentifier() );
	}

	@Override
	public String toString() {
		return phase + " call for a work on index '" + indexName + "', identifier '" + work.getIdentifier()
				+ "'; work = " + work;
	}
}
