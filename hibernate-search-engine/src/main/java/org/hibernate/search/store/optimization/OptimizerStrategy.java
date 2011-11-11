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
package org.hibernate.search.store.optimization;

import java.util.Properties;

import org.apache.lucene.index.IndexWriter;

import org.hibernate.search.SearchException;
import org.hibernate.search.SearchFactory;
import org.hibernate.search.indexes.spi.IndexManager;
import org.hibernate.search.store.Workspace;

/**
 * Controls how and when the indexes are optimized.
 * Implementations need to be threadsafe.
 *
 * @author Emmanuel Bernard
 * @author Sanne Grinovero
 */
public interface OptimizerStrategy {

	/**
	 * Invokes optimize on the IndexWriter; This is invoked when
	 * an optimization has been explicitly requested by the user API
	 * using {@link SearchFactory#optimize()} or {@link SearchFactory#optimize(Class)},
	 * or at the start or end of a MassIndexer's work.
	 *
	 * @param writer the index writer
	 *
	 * @return {@code true} if optimisation occurred, {@code false} otherwise
	 *
	 * @throws SearchException in case of IO errors on the index
	 */
	boolean performOptimization(IndexWriter writer);

	/**
	 * To count the amount of operations which where applied to the index.
	 * Invoked once per transaction.
	 *
	 * @param increment operation count
	 */
	void addOperationWithinTransactionCount(long increment);

	/**
	 * Allows the implementation to start an optimization process.
	 * The decision of optimizing or not is up to the implementor.
	 * This is invoked after all changes of a transaction are applied,
	 * but never during stream operation such as those used by
	 * the MassIndexer.
	 *
	 * @param workspace the current work space
	 */
	void optimize(Workspace workspace);

	/**
	 * Initializes the {@code OptimizerStrategy}. Is called once at the initialisation of the strategy.
	 *
	 * @param indexManager the index manager for which this strategy applies
	 * @param indexProperties the configuration properties
	 */
	void initialize(IndexManager indexManager, Properties indexProperties);
}
