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

public interface PojoScopeWorkspace {

	@Deprecated
	default CompletableFuture<?> mergeSegments() {
		return mergeSegments( OperationSubmitter.BLOCKING );
	}

	CompletableFuture<?> mergeSegments(OperationSubmitter operationSubmitter);

	@Deprecated
	default CompletableFuture<?> purge(Set<String> routingKeys) {
		return purge( routingKeys, OperationSubmitter.BLOCKING );
	}

	CompletableFuture<?> purge(Set<String> routingKeys, OperationSubmitter operationSubmitter);

	@Deprecated
	default CompletableFuture<?> flush() {
		return flush( OperationSubmitter.BLOCKING );
	}

	CompletableFuture<?> flush(OperationSubmitter operationSubmitter);

	@Deprecated
	default CompletableFuture<?> refresh() {
		return refresh( OperationSubmitter.BLOCKING );
	}

	CompletableFuture<?> refresh(OperationSubmitter operationSubmitter);

}
