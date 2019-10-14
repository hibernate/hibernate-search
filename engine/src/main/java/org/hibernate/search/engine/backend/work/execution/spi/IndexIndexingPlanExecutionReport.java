/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.backend.work.execution.spi;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.hibernate.search.engine.backend.common.DocumentReference;
import org.hibernate.search.util.common.AssertionFailure;

public final class IndexIndexingPlanExecutionReport {

	public static IndexIndexingPlanExecutionReport.Builder builder() {
		return new IndexIndexingPlanExecutionReport.Builder();
	}

	public static CompletableFuture<IndexIndexingPlanExecutionReport> allOf(
			List<CompletableFuture<IndexIndexingPlanExecutionReport>> reportFutures) {
		if ( reportFutures.size() == 1 ) {
			return reportFutures.get( 0 );
		}
		else {
			CompletableFuture<IndexIndexingPlanExecutionReport.Builder> reportBuilderFuture =
					CompletableFuture.completedFuture( IndexIndexingPlanExecutionReport.builder() );
			for ( CompletableFuture<IndexIndexingPlanExecutionReport> future : reportFutures ) {
				reportBuilderFuture = reportBuilderFuture.thenCombine(
						future, IndexIndexingPlanExecutionReport.Builder::add
				);
			}
			return reportBuilderFuture.thenApply( IndexIndexingPlanExecutionReport.Builder::build );
		}
	}

	private final Throwable throwable;

	private final List<DocumentReference> failingDocuments;

	private IndexIndexingPlanExecutionReport(Builder builder) {
		this.failingDocuments = builder.failingDocuments == null
				? Collections.emptyList() : Collections.unmodifiableList( builder.failingDocuments );
		if ( builder.throwable == null && !failingDocuments.isEmpty() ) {
			this.throwable = new AssertionFailure(
					"Unknown throwable: missing throwable when reporting the failure."
							+ " There is probably a bug in Hibernate Search, please report it."
			);
		}
		else {
			this.throwable = builder.throwable;
		}
	}

	public Optional<Throwable> getThrowable() {
		return Optional.ofNullable( throwable );
	}

	public List<DocumentReference> getFailingDocuments() {
		return failingDocuments;
	}

	public static final class Builder {

		private Throwable throwable;
		private List<DocumentReference> failingDocuments;

		private Builder() {
		}

		public Builder add(IndexIndexingPlanExecutionReport report) {
			report.getThrowable().ifPresent( this::throwable );
			for ( DocumentReference failingDocument : report.getFailingDocuments() ) {
				failingDocument( failingDocument );
			}
			return this;
		}

		public Builder throwable(Throwable throwable) {
			if ( this.throwable == null ) {
				this.throwable = throwable;
			}
			else if ( this.throwable != throwable ) {
				this.throwable.addSuppressed( throwable );
			}
			return this;
		}

		public Builder failingDocument(DocumentReference reference) {
			if ( failingDocuments == null ) {
				failingDocuments = new ArrayList<>();
			}
			failingDocuments.add( reference );
			return this;
		}

		public IndexIndexingPlanExecutionReport build() {
			return new IndexIndexingPlanExecutionReport( this );
		}
	}

}
