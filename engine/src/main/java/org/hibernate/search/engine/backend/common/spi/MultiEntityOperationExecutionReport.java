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

import org.hibernate.search.engine.common.EntityReference;
import org.hibernate.search.util.common.AssertionFailure;

public final class MultiEntityOperationExecutionReport {

	public static MultiEntityOperationExecutionReport.Builder builder() {
		return new MultiEntityOperationExecutionReport.Builder();
	}

	public static CompletableFuture<MultiEntityOperationExecutionReport> allOf(
			List<CompletableFuture<MultiEntityOperationExecutionReport>> reportFutures) {
		if ( reportFutures.size() == 1 ) {
			return reportFutures.get( 0 );
		}
		else {
			CompletableFuture<MultiEntityOperationExecutionReport.Builder> reportBuilderFuture =
					CompletableFuture.completedFuture( MultiEntityOperationExecutionReport.builder() );
			for ( CompletableFuture<MultiEntityOperationExecutionReport> future : reportFutures ) {
				reportBuilderFuture = reportBuilderFuture.thenCombine(
						future, MultiEntityOperationExecutionReport.Builder::add
				);
			}
			return reportBuilderFuture.thenApply( MultiEntityOperationExecutionReport.Builder::build );
		}
	}

	private final Throwable throwable;

	private final List<EntityReference> failingEntityReferences;

	private MultiEntityOperationExecutionReport(Builder builder) {
		this.failingEntityReferences = builder.failingEntityReferences == null
				? Collections.emptyList()
				: Collections.unmodifiableList( builder.failingEntityReferences );
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

	public List<EntityReference> failingEntityReferences() {
		return failingEntityReferences;
	}

	public static final class Builder {

		private Throwable throwable;
		private List<EntityReference> failingEntityReferences;

		private Builder() {
		}

		public Builder add(MultiEntityOperationExecutionReport report) {
			report.throwable().ifPresent( this::throwable );
			for ( EntityReference failingEntityReference : report.failingEntityReferences() ) {
				failingEntityReference( failingEntityReference );
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

		public Builder failingEntityReference(EntityReference reference) {
			if ( failingEntityReferences == null ) {
				failingEntityReferences = new ArrayList<>();
			}
			failingEntityReferences.add( reference );
			return this;
		}

		public Builder failingEntityReference(EntityReferenceFactory referenceFactory,
				String typeName, Object entityIdentifier) {
			EntityReference reference = EntityReferenceFactory.safeCreateEntityReference( referenceFactory,
					typeName, entityIdentifier, this::throwable );
			if ( reference != null ) {
				failingEntityReference( reference );
			}
			return this;
		}

		public MultiEntityOperationExecutionReport build() {
			return new MultiEntityOperationExecutionReport( this );
		}

	}

}
