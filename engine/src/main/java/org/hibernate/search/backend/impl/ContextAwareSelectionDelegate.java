/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat, Inc. and/or its affiliates or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat, Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
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
public interface ContextAwareSelectionDelegate {

	/**
	 * The LuceneWork must be applied to different indexes.
	 *
	 * @param work the work to split.
	 * @param shardingStrategy the Sharding strategy is usually needed to identify affected Directories.
	 * @param context the transactional context where the pending changes are stored
	 */
	void performOperation(LuceneWork work, IndexShardingStrategy shardingStrategy, WorkQueuePerIndexSplitter context);

}
