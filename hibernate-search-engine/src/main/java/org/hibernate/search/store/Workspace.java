/* 
 * Hibernate, Relational Persistence for Idiomatic Java
 * 
 * JBoss, Home of Professional Open Source
 * Copyright 2011 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
package org.hibernate.search.store;

import java.util.Set;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.index.IndexWriter;
import org.hibernate.search.SearchFactory;
import org.hibernate.search.backend.OptimizeLuceneWork;
import org.hibernate.search.engine.spi.DocumentBuilderIndexedEntity;

/**
 * @author Sanne Grinovero <sanne@hibernate.org> (C) 2011 Red Hat Inc.
 */
public interface Workspace {

	<T> DocumentBuilderIndexedEntity<?> getDocumentBuilder(Class<T> entity);

	Analyzer getAnalyzer(String name);

	/**
	 * If optimization has not been forced give a chance to configured OptimizerStrategy
	 * to optimize the index.
	 */
	void optimizerPhase();

	/**
	 * Used by OptimizeLuceneWork to start an optimization process of the index.
	 * @param writer the IndexWriter to use for optimization
	 * @see OptimizeLuceneWork
	 * @see SearchFactory#optimize()
	 * @see SearchFactory#optimize(Class)
	 */
	void performOptimization(IndexWriter writer);

	/**
	 * Gets the IndexWriter, opening one if needed.
	 * @return a new IndexWriter or an already open one, or null if an error happened.
	 */
	IndexWriter getIndexWriter();

	/**
	 * Increment the counter of modification operations done on the index.
	 * Used (currently only) by the OptimizerStrategy.
	 * @param modCount the increment to add to the counter.
	 */
	void incrementModificationCounter(int modCount);

	/**
	 * @return The unmodifiable set of entity types being indexed
	 * in the underlying IndexManager backing this Workspace.
	 */
	Set<Class<?>> getEntitiesInIndexManager();

	/**
	 * Invoked after all changes of a transaction are applied.
	 * Must be invoked strictly once after every {@link #getIndexWriter()} in a finally block
	 * as implementations might rely on counters to release the IndexWriter.
	 * 
	 * @param someFailureHappened usually false, set to true if errors
	 * where caught while using the IndexWriter
	 * @param streaming if no immediate visibility of the change is required (hint for performance)
	 */
	void afterTransactionApplied(boolean someFailureHappened, boolean streaming);

	/**
	 * Makes sure eventually pending changes are made visible to IndexReaders.
	 */
	void flush();

	/**
	 * Return true if it's safe to perform index delete operations using only the identifier term.
	 * This can be more efficient but can not work if there are multiple indexed types in the same
	 * index possibly sharing the same id term, or if the index might contain entity types we don't
	 * know.
	 * @return true if it's safe to do the single term operation.
	 */
	boolean areSingleTermDeletesSafe();

}
