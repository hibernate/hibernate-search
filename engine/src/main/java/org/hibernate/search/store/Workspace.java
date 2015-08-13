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
import org.hibernate.search.backend.impl.CommitPolicy;
import org.hibernate.search.engine.spi.DocumentBuilderIndexedEntity;

/**
 * @deprecated This interface will be moved and should be considered non-public API [HSEARCH-1915]
 *
 * @author Sanne Grinovero (C) 2011 Red Hat Inc.
 */
@Deprecated
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
	 * @see org.hibernate.search.spi.SearchIntegrator#optimize()
	 * @see org.hibernate.search.spi.SearchIntegrator#optimize(Class)
	 */
	void performOptimization(IndexWriter writer);

	/**
	 * Gets the IndexWriter, opening one if needed.
	 *
	 * @return a new IndexWriter or an already open one, or null if an error happened.
	 */
	IndexWriter getIndexWriter();

	/**
	 * @return The set of entity types being indexed
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
	 * Returns true if one and only one entity type is stored in the targeted index.
	 * This allows to use delete by the identifier term which is much faster.
	 *
	 * This method should really be named {@code isSingleClassInIndex} but that's a public contract.
	 * @return true if one and only one entity type is stored in the targeted index.
	 */
	boolean areSingleTermDeletesSafe();

	/**
	 * Returns true if either the configuration guarantees that one can use delete by term on all indexes
	 * or if we ensure that entity types stored in the index return positive to {@link org.hibernate.search.cfg.spi.IdUniquenessResolver}
	 * and that the document id and JPA id are the same property.
	 *
	 * This allows to use delete by identifier term in a safe way and is much more efficient.
	 * If unsure, we will delete taking the class field into account to avoid unwanted document deletions.
	 *
	 * The method should really be named {@code isDeleteByTermEnforcedOrSafe} but that's a public contract.
	 * @return true if either the configuration guarantees that one can use delete by term on all indexes
	 */
	boolean isDeleteByTermEnforced();

	/**
	 * Some workspaces need this to determine for example the kind of flush operations which are safe
	 * to apply. Generally used for statistics.
	 * @param work the LuceneWork which was just processed
	 */
	void notifyWorkApplied(LuceneWork work);

	/**
	 * Get the commit policy applied to the workspace
	 * @return {@link org.hibernate.search.backend.impl.CommitPolicy}
	 */
	CommitPolicy getCommitPolicy();

	/**
	 * Returns the name of the index this workspace is being used for.
	 * @return the name of the index
	 */
	String getIndexName();

}
