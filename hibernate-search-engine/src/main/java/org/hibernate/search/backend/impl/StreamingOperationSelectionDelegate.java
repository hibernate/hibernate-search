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

import org.hibernate.search.backend.IndexingMonitor;
import org.hibernate.search.backend.LuceneWork;
import org.hibernate.search.store.IndexShardingStrategy;

/**
 * Sends a single operation to the related backends, considering the sharding strategy.
 * This delegates to {@link org.hibernate.search.indexes.spi.IndexManager#performStreamOperation(LuceneWork, boolean)}
 * so it's suited for streams of many LuceneWork operations which don't need strict ordering.
 * 
 * @author Sanne Grinovero
 */
public interface StreamingOperationSelectionDelegate {
	
	/**
	 * The LuceneWork must be applied to different indexes.
	 * @param work the work to split.
	 * @param shardingStrategy the Sharding strategy is usually needed to identify affected Directories.
	 * @param monitor to receive notification of indexing operations
	 * @param forceAsync if true, the invocation will not block to wait for it being applied.
	 *  When false this will depend on the backend configuration.
	 */
	public void performStreamOperation(LuceneWork work, IndexShardingStrategy shardingStrategy, IndexingMonitor monitor, boolean forceAsync);

}
