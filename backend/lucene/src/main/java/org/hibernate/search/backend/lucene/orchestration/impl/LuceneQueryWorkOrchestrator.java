/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.orchestration.impl;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.hibernate.search.backend.lucene.work.impl.LuceneQueryWork;

/**
 * @author Guillaume Smet
 */
public interface LuceneQueryWorkOrchestrator extends AutoCloseable {

	<T> CompletableFuture<T> submit(LuceneQueryWork<T> work);

	CompletableFuture<?> submit(List<LuceneQueryWork<?>> work);

	@Override
	default void close() {
	}
}
