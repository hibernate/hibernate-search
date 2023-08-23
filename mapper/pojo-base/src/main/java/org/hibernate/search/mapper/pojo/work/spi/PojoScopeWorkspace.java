/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.work.spi;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

import org.hibernate.search.engine.backend.work.execution.OperationSubmitter;
import org.hibernate.search.engine.backend.work.execution.spi.UnsupportedOperationBehavior;

public interface PojoScopeWorkspace {

	@Deprecated
	default CompletableFuture<?> mergeSegments() {
		return mergeSegments( OperationSubmitter.blocking() );
	}

	@Deprecated
	default CompletableFuture<?> mergeSegments(OperationSubmitter operationSubmitter) {
		return mergeSegments( operationSubmitter, UnsupportedOperationBehavior.FAIL );
	}

	CompletableFuture<?> mergeSegments(OperationSubmitter operationSubmitter,
			UnsupportedOperationBehavior unsupportedOperationBehavior);

	@Deprecated
	default CompletableFuture<?> purge(Set<String> routingKeys) {
		return purge( routingKeys, OperationSubmitter.blocking() );
	}

	@Deprecated
	default CompletableFuture<?> purge(Set<String> routingKeys, OperationSubmitter operationSubmitter) {
		return purge( routingKeys, operationSubmitter, UnsupportedOperationBehavior.FAIL );
	}

	CompletableFuture<?> purge(Set<String> routingKeys, OperationSubmitter operationSubmitter,
			UnsupportedOperationBehavior unsupportedOperationBehavior);

	@Deprecated
	default CompletableFuture<?> flush() {
		return flush( OperationSubmitter.blocking() );
	}

	@Deprecated
	default CompletableFuture<?> flush(OperationSubmitter operationSubmitter) {
		return flush( operationSubmitter, UnsupportedOperationBehavior.FAIL );
	}

	CompletableFuture<?> flush(OperationSubmitter operationSubmitter,
			UnsupportedOperationBehavior unsupportedOperationBehavior);

	@Deprecated
	default CompletableFuture<?> refresh() {
		return refresh( OperationSubmitter.blocking() );
	}

	@Deprecated
	default CompletableFuture<?> refresh(OperationSubmitter operationSubmitter) {
		return refresh( operationSubmitter, UnsupportedOperationBehavior.FAIL );
	}

	CompletableFuture<?> refresh(OperationSubmitter operationSubmitter,
			UnsupportedOperationBehavior unsupportedOperationBehavior);

}
