/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.orchestration.impl;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

import org.hibernate.search.backend.lucene.index.spi.ReaderProvider;
import org.hibernate.search.backend.lucene.work.impl.LuceneReadWork;

/**
 * @author Guillaume Smet
 */
public interface LuceneReadWorkOrchestrator extends AutoCloseable {

	<T> CompletableFuture<T> submit(Set<String> indexNames, Set<ReaderProvider> readerProviders,
			LuceneReadWork<T> work);

	@Override
	default void close() {
	}
}
