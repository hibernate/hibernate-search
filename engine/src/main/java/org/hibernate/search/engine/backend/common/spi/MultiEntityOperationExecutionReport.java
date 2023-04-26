/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.backend.common.spi;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.hibernate.search.util.common.AssertionFailure;

/**
 * @param <R> The type of entity references.
 */
public final class MultiEntityOperationExecutionReport<R> {

	public static <R> MultiEntityOperationExecutionReport.Builder<R> builder() {
		return new MultiEntityOperationExecutionReport.Builder<>();
	}

	public static <R> CompletableFuture<MultiEntityOperationExecutionReport<R>> allOf(
			List<CompletableFuture<MultiEntityOperationExecutionReport<R>>> reportFutures) {
		if ( reportFutures.size() == 1 ) {
			return reportFutures.get( 0 );
		}
		else {
			CompletableFuture<MultiEntityOperationExecutionReport.Builder<R>> reportBuilderFuture =
					CompletableFuture.completedFuture( MultiEntityOperationExecutionReport.builder() );
			for ( CompletableFuture<MultiEntityOperationExecutionReport<R>> future : reportFutures ) {
				reportBuilderFuture = reportBuilderFuture.thenCombine(
						future, MultiEntityOperationExecutionReport.Builder::add
				);
			}
			return reportBuilderFuture.thenApply( MultiEntityOperationExecutionReport.Builder::build );
		}
	}

	private final Throwable throwable;

	private final List<R> failingEntityReferences;

	private MultiEntityOperationExecutionReport(Builder<R> builder) {
		this.failingEntityReferences = builder.failingEntityReferences == null
				? Collections.emptyList() : Collections.unmodifiableList( builder.failingEntityReferences );
		if ( builder.throwable == null && !failingEntityReferences.isEmpty() ) {
			this.throwable = new AssertionFailure(
					"Unknown throwable: missing throwable when reporting the failure."
			);
		}
		else {
			this.throwable = builder.throwable;
		}
	}

	public Optional<Throwable> throwable() {
		return Optional.ofNullable( throwable );
	}

	public List<R> failingEntityReferences() {
		return failingEntityReferences;
	}

	public static final class Builder<R> {

		private Throwable throwable;
		private List<R> failingEntityReferences;

		private Builder() {
		}

		public Builder<R> add(MultiEntityOperationExecutionReport<R> report) {
			report.throwable().ifPresent( this::throwable );
			for ( R failingEntityReference : report.failingEntityReferences() ) {
				failingEntityReference( failingEntityReference );
			}
			return this;
		}

		public Builder<R> throwable(Throwable throwable) {
			if ( this.throwable == null ) {
				this.throwable = throwable;
			}
			else if ( this.throwable != throwable ) {
				this.throwable.addSuppressed( throwable );
			}
			return this;
		}

		public Builder<R> failingEntityReference(R reference) {
			if ( failingEntityReferences == null ) {
				failingEntityReferences = new ArrayList<>();
			}
			failingEntityReferences.add( reference );
			return this;
		}

		public Builder<R> failingEntityReference(EntityReferenceFactory<? extends R> referenceFactory,
				String typeName, Object entityIdentifier) {
			R reference = EntityReferenceFactory.safeCreateEntityReference( referenceFactory,
					typeName, entityIdentifier, this::throwable );
			if ( reference != null ) {
				failingEntityReference( reference );
			}
			return this;
		}

		public MultiEntityOperationExecutionReport<R> build() {
			return new MultiEntityOperationExecutionReport<>( this );
		}

	}

}
