/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.store;

import java.util.Set;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.index.IndexWriter;
import org.hibernate.search.backend.LuceneWork;
import org.hibernate.search.engine.spi.DocumentBuilderIndexedEntity;

/**
 * @author Sanne Grinovero <sanne@hibernate.org> (C) 2011 Red Hat Inc.
 */
public interface Workspace {

	DocumentBuilderIndexedEntity getDocumentBuilder(Class<?> entity);

	Analyzer getAnalyzer(String name);

	/**
	 * If optimization has not been forced give a chance to configured OptimizerStrategy
	 * to optimize the index.
	 */
	void optimizerPhase();

	/**
	 * Used by OptimizeLuceneWork to start an optimization process of the index.
	 *
	 * @param writer the IndexWriter to use for optimization
	 * @see org.hibernate.search.backend.OptimizeLuceneWork
	 * @see org.hibernate.search.engine.SearchFactory#optimize()
	 * @see org.hibernate.search.engine.SearchFactory#optimize(Class)
	 */
	void performOptimization(IndexWriter writer);

	/**
	 * Gets the IndexWriter, opening one if needed.
	 *
	 * @return a new IndexWriter or an already open one, or null if an error happened.
	 */
	IndexWriter getIndexWriter();

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
	 *
	 * @return true if it's safe to do the single term operation.
	 */
	boolean areSingleTermDeletesSafe();

	/**
	 * Some workspaces need this to determine for example the kind of flush operations which are safe
	 * to apply. Generally used for statistics.
	 * @param work the LuceneWork which was just processed
	 */
	void notifyWorkApplied(LuceneWork work);

}
