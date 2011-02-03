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
package org.hibernate.search;

import java.util.concurrent.Future;

import org.hibernate.CacheMode;
import org.hibernate.search.batchindexing.IdentifierLoadingStrategy;
import org.hibernate.search.batchindexing.MassIndexerProgressMonitor;

/**
 * A MassIndexer is useful to rebuild the indexes from the
 * data contained in the database.
 * This process is expensive: all indexed entities and their
 * indexedEmbedded properties are scrolled from database.
 * 
 * @author Sanne Grinovero
 */
public interface MassIndexer {
	
	/**
	 * Set the number of threads to be used to load
	 * the root entities.
	 * @param numberOfThreads
	 * @return <tt>this</tt> for method chaining
	 */
	MassIndexer threadsToLoadObjects(int numberOfThreads);
	
	/**
	 * Sets the batch size used to load the root entities.
	 * @param batchSize
	 * @return <tt>this</tt> for method chaining
	 */
	MassIndexer batchSizeToLoadObjects(int batchSize);
	
	/**
	 * Sets the number of threads used to load the lazy collections
	 * related to the indexed entities.
	 * @param numberOfThreads
	 * @return <tt>this</tt> for method chaining
	 */
	MassIndexer threadsForSubsequentFetching(int numberOfThreads);
	
	/**
	 * <p>Sets the number of threads to be used to analyze the documents
	 * and write to the index.</p><p>This overrides the global property
	 * <tt>hibernate.search.batchbackend.concurrent_writers</tt>.</p><p>
	 * Might be ignored by <code>BatchBackend</code> implementations other
	 * than <code>org.hibernate.search.backend.impl.batchlucene.LuceneBatchBackend</code></p>
	 * @see org.hibernate.search.backend.impl.batchlucene.LuceneBatchBackend
	 * @param numberOfThreads
	 * @return <tt>this</tt> for method chaining
	 */
	MassIndexer threadsForIndexWriter(int numberOfThreads);
	
	/**
	 * Override the default <code>MassIndexerProgressMonitor</code>.
	 * 
	 * @param monitor this instance will receive updates about the massindexing progress.
	 * @return <tt>this</tt> for method chaining
	 */
	MassIndexer progressMonitor(MassIndexerProgressMonitor monitor);
	
	/**
	 * Sets the cache interaction mode for the data loading tasks.
	 * Defaults to <tt>CacheMode.IGNORE</tt>.
	 * @return <tt>this</tt> for method chaining
	 */
	MassIndexer cacheMode(CacheMode cacheMode);
	
	/**
	 * If index optimization has to be started at the end
	 * of the indexing process.
	 * Defaults to <tt>true</tt>.
	 * @param optimize
	 * @return <tt>this</tt> for method chaining
	 */
	MassIndexer optimizeOnFinish(boolean optimize);
	
	/**
	 * If index optimization should be run before starting,
	 * after the purgeAll. Has no effect if <tt>purgeAll</tt> is set to false.
	 * Defaults to <tt>true</tt>.
	 * @param optimize
	 * @return <tt>this</tt> for method chaining
	 */
	MassIndexer optimizeAfterPurge(boolean optimize);
	
	/**
	 * If all entities should be removed from the index before starting
	 * using purgeAll. Set it to false only if you know there are no
	 * entities in the index: otherwise search results may be duplicated.
	 * Defaults to true.
	 * @param purgeAll
	 * @return <tt>this</tt> for method chaining
	 */
	MassIndexer purgeAllOnStart(boolean purgeAll);
	
	/**
	 * Will stop indexing after having indexed a set amount of objects.
	 * As a results the index will not be consistent
	 * with the database: use only for testing on an (undefined) subset of database data.
	 * @param maximum
	 * @return <tt>this</tt> for method chaining
	 */
	MassIndexer limitIndexedObjectsTo(long maximum);
	
	/**
	 * Define a custom HQL query which will be used to count the entities to be indexed.
	 * If using this option, defining {@link #primaryKeySelectingQuery(String)} too is mandatory and it won't be possible
	 * to choose a custom <tt>IdentifierLoadingStrategy</tt>.
	 * You can pass values for named parameters by using {@link #queryParameter(String, Object)}
	 * @see #primaryKeySelectingQuery(String)
	 * @see #idLoadingStrategy(IdentifierLoadingStrategy)
	 * @param entitiesCountHQL the HQL counting the entities to be indexed.
	 * @return <tt>this</tt> for method chaining
	 */
	MassIndexer countQuery(String entitiesCountHQL);
	
	/**
	 * Define a custom HQL query to select all primary keys of entities to be indexed.
	 * The number of selected entities must match the value returned by {@link #countQuery(String)}.
	 * If using this option, defining {@link #countQuery(String)} is mandatory and it won't be possible
	 * to choose a custom IdentifierLoadingStrategy using {@link #idLoadingStrategy(IdentifierLoadingStrategy)}.
	 * You can pass values for named parameters by using {@link #queryParameter(String, Object)}
	 * @param primaryKeysSelectingHQL the HQL selecting all primary keys of objects to be indexed 
	 * @return <tt>this</tt> for method chaining
	 */
	MassIndexer primaryKeySelectingQuery(String primaryKeysSelectingHQL);
	
	/**
	 * When defining a custom entitiesCountHQL and primaryKeysSelectingHQL via {@link #countQuery(String)}
	 * and {@link #primaryKeySelectingQuery(String)} doesn't suffice, you can implement a custom {@link #idLoadingStrategy(IdentifierLoadingStrategy)}
	 * to use instead. When overriding the <tt>primaryKeysSelectingHQL</tt> you can't also define custom queries.
	 * @param customIdLoadingStrategy
	 * @return <tt>this</tt> for method chaining
	 */
	MassIndexer idLoadingStrategy(IdentifierLoadingStrategy customIdLoadingStrategy);
	
	/**
	 * Define named parameters to be used on queries defined via {@link #countQuery(String)}
	 * and/or {@link #primaryKeySelectingQuery(String)}.
	 * It's allowed to reuse the same parameter names across the two queries, the same object instance
	 * will be passed as parameter to both queries.
	 * @param name query parameter name
	 * @param value
	 * @return <tt>this</tt> for method chaining
	 */
	MassIndexer queryParameter(String name, Object value);

	/**
	 * Starts the indexing process in background (asynchronous).
	 * Can be called only once.
	 * @return a Future to control task canceling.
	 * get() will block until completion.
	 * cancel() is currently not implemented.
	 */
	Future<?> start();
	
	/**
	 * Starts the indexing process, and then block until it's finished.
	 * Can be called only once.
	 * @throws InterruptedException if the current thread is interrupted
	 * while waiting.
	 */
	void startAndWait() throws InterruptedException;
	
}
