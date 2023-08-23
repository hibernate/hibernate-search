/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.backend.work.execution.spi;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

import org.hibernate.search.engine.backend.work.execution.OperationSubmitter;

/**
 * The entry point for explicit index operations on a single index.
 */
public interface IndexWorkspace {

	/**
	 * Merge all segments of the index into a single one.
	 * @param operationSubmitter The behavior to adopt when submitting the operation to a full queue/executor.
	 * @param unsupportedOperationBehavior The behavior to adopt if the operation is not supported in this index.
	 * @return A completion stage for the executed operation, or a completed stage if the operation is not supported.
	 */
	CompletableFuture<?> mergeSegments(OperationSubmitter operationSubmitter,
			UnsupportedOperationBehavior unsupportedOperationBehavior);

	/**
	 * Delete documents that were indexed with any of the given routing keys,
	 * or all documents if the set of routing keys is empty.
	 *
	 * @param routingKeys The set of routing keys.
	 * If non-empty, only documents that were indexed with these routing keys will be deleted.
	 * If empty, documents will be deleted regardless of their routing key.
	 * @param operationSubmitter The behavior to adopt when submitting the operation to a full queue/executor.
	 * @param unsupportedOperationBehavior The behavior to adopt if the operation is not supported in this index.
	 *
	 * @return A completion stage for the executed operation.
	 */
	CompletableFuture<?> purge(Set<String> routingKeys, OperationSubmitter operationSubmitter,
			UnsupportedOperationBehavior unsupportedOperationBehavior);

	/**
	 * Flush to disk the changes to the index that were not committed yet.
	 * <p>
	 * In the case of backends with a transaction log (Elasticsearch),
	 * also apply operations from the transaction log that were not applied yet.
	 * @param operationSubmitter The behavior to adopt when submitting the operation to a full queue/executor.
	 * @param unsupportedOperationBehavior The behavior to adopt if the operation is not supported in this index.
	 * @return A completion stage for the executed operation, or a completed stage if the operation is not supported.
	 */
	CompletableFuture<?> flush(OperationSubmitter operationSubmitter,
			UnsupportedOperationBehavior unsupportedOperationBehavior);

	/**
	 * Refresh the indexes so that all changes executed so far will be visible in search queries.
	 * @param operationSubmitter The behavior to adopt when submitting the operation to a full queue/executor.
	 * @param unsupportedOperationBehavior The behavior to adopt if the operation is not supported in this index.
	 * @return A completion stage for the executed operation, or a completed stage if the operation is not supported.
	 */
	CompletableFuture<?> refresh(OperationSubmitter operationSubmitter,
			UnsupportedOperationBehavior unsupportedOperationBehavior);

}
