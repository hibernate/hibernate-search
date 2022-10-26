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

	CompletableFuture<?> mergeSegments(OperationSubmitter operationSubmitter);

	CompletableFuture<?> purge(Set<String> routingKeys, OperationSubmitter operationSubmitter);

	CompletableFuture<?> flush(OperationSubmitter operationSubmitter);

	CompletableFuture<?> refresh(OperationSubmitter operationSubmitter);

}
