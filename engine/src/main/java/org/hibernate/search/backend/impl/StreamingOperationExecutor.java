/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.impl;

import org.hibernate.search.backend.IndexingMonitor;
import org.hibernate.search.backend.LuceneWork;
import org.hibernate.search.store.IndexShardingStrategy;

/**
 * Sends a single operation to the related backends, considering the sharding strategy.
 * This delegates to {@link org.hibernate.search.indexes.spi.IndexManager#performStreamOperation(LuceneWork, IndexingMonitor, boolean)}
 * so it's suited for streams of many LuceneWork operations which don't need strict ordering.
 *
 * @author Sanne Grinovero
 */
public interface StreamingOperationExecutor {

	/**
	 * The LuceneWork must be applied to different indexes.
	 *
	 * @param work the work to split.
	 * @param shardingStrategy the Sharding strategy is usually needed to identify affected Directories.
	 * @param monitor to receive notification of indexing operations
	 * @param forceAsync if true, the invocation will not block to wait for it being applied.
	 *  When false this will depend on the backend configuration.
	 */
	void performStreamOperation(LuceneWork work, IndexShardingStrategy shardingStrategy, IndexingMonitor monitor, boolean forceAsync);

}
