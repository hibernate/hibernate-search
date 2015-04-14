/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.impl;

import org.hibernate.search.backend.LuceneWork;
import org.hibernate.search.store.IndexShardingStrategy;

/**
 * Visitor interface to apply the configured sharding strategy to a list of LuceneWork;
 * this list is usually the set of operations to be applied in a transactional context.
 *
 * @author Sanne Grinovero
 */
public interface TransactionalOperationExecutor {

	/**
	 * The LuceneWork must be applied to different indexes.
	 *
	 * @param work the work to split.
	 * @param shardingStrategy the Sharding strategy is usually needed to identify affected Directories.
	 * @param context the transactional context where the pending changes are stored
	 */
	void performOperation(LuceneWork work, IndexShardingStrategy shardingStrategy, WorkQueuePerIndexSplitter context);

}
