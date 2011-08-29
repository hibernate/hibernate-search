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
package org.hibernate.search.backend.spi;

import java.util.Properties;
import java.util.List;
import java.util.concurrent.locks.Lock;

import org.hibernate.search.backend.LuceneWork;
import org.hibernate.search.backend.impl.lucene.NRTWorkspaceImpl;
import org.hibernate.search.indexes.impl.DirectoryBasedIndexManager;
import org.hibernate.search.spi.WorkerBuildContext;

/**
 * Interface for different types of queue processors. Implementations need a no-arg constructor.
 *
 * @author Emmanuel Bernard
 * @author Sanne Grinovero
 */
public interface BackendQueueProcessor {

	/**
	 * Used at startup, called once as first method.
	 * @param props all configuration properties
	 * @param context context giving access to required meta data
	 * @param indexManager the index it is related to.
	 */
	void initialize(Properties props, WorkerBuildContext context, DirectoryBasedIndexManager indexManager);

	/**
	 * Used to shutdown and eventually release resources.
	 * No other method should be used after this one.
	 */
	void close();

	/**
	 * Applies a list of operations to the index. A single list might be processed by applying
	 * elements in parallel threads, but no work should be started on a new workList until the previous
	 * one was fully processed.
	 * Work could be applied asynchronously according to capabilities and configuration of implementor.
	 * @param workList
	 */
	void applyWork(List<LuceneWork> workList);

	/**
	 * Applies a single operation on the index, and different operations can be applied in parallel,
	 * even in parallel to a workList instance being processed by {@link #applyWork(List)}
	 * @param singleOperation
	 */
	void applyStreamWork(LuceneWork singleOperation);

	/**
	 * @return a Lock instance which will block index modifications when acquired
	 */
	Lock getExclusiveWriteLock();

}
