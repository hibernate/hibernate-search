/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.spi;

import java.util.Set;

import org.apache.lucene.analysis.Analyzer;
import org.hibernate.search.backend.spi.BatchBackend;
import org.hibernate.search.backend.spi.Worker;
import org.hibernate.search.batchindexing.MassIndexerProgressMonitor;
import org.hibernate.search.engine.service.spi.ServiceManager;
import org.hibernate.search.engine.spi.EntityIndexBinding;
import org.hibernate.search.exception.ErrorHandler;
import org.hibernate.search.indexes.IndexReaderAccessor;
import org.hibernate.search.indexes.spi.IndexManager;
import org.hibernate.search.metadata.IndexedTypeDescriptor;
import org.hibernate.search.query.dsl.QueryContextBuilder;
import org.hibernate.search.query.engine.spi.HSQuery;
import org.hibernate.search.query.engine.spi.TimeoutExceptionFactory;
import org.hibernate.search.stat.Statistics;

/**
 * This contract is considered experimental.
 *
 * This contract gives access to lower level APIs of Hibernate Search for
 * frameworks integrating with it.
 * Frameworks should not expose this as public API though, but expose a simplified view; for
 * example the Hibernate Search ORM module expose the {@code org.hibernate.search.SearchFactory} contract to
 * its clients.
 *
 * @author Emmanuel Bernard
 * @hsearch.experimental
 */
public interface SearchIntegrator extends AutoCloseable {

	/**
	 * Returns the entity to index binding for the given type.
	 *
	 * @param entityType the type for which to retrieve the binding
	 *
	 * @return the entity to index binding for the given type. {@code null} is returned for types which are unindexed or
	 *         unknown.
	 */
	EntityIndexBinding getIndexBinding(Class<?> entityType);

	/**
	 * Add the following classes to the SearchIntegrator. If these classes are new to the SearchIntegrator this
	 * will trigger a reconfiguration.
	 * @param classes the classes to add to the {@link SearchIntegrator}
	 */
	void addClasses(Class<?>... classes);

	/**
	 * Return an Hibernate Search query object.
	 * This object uses fluent APIs to define the query executed.
	 * Offers a few execution approaches:
	 * - return the list of results eagerly
	 * - return the list of results lazily
	 * - get the number of results
	 *
	 * @return an Hibernate Search query object
	 */
	HSQuery createHSQuery();

	/**
	 * @return true if the SearchIntegrator was stopped
	 */
	boolean isStopped();

	/**
	 * Used to catch exceptions in all synchronous operations; but default they are logged, the user
	 * can configure alternative error management means.
	 *
	 * @return the configured ErrorHandler, global to the SearchIntegrator
	 */
	ErrorHandler getErrorHandler();

	/**
	 * Useful if you need to create custom exception types to represent query timeouts.
	 *
	 * @return the configured TimeoutExceptionFactory
	 */
	TimeoutExceptionFactory getDefaultTimeoutExceptionFactory();

	/**
	 * Optimize all indexes
	 */
	void optimize();

	/**
	 * Optimize the index holding {@code entityType}
	 *
	 * @param entityType the entity type (index) to optimize
	 */
	void optimize(Class entityType);

	/**
	 * Retrieve an analyzer instance by its definition name
	 *
	 * @param name the name of the analyzer
	 *
	 * @return analyzer with the specified name
	 *
	 * @throws org.hibernate.search.exception.SearchException if the definition name is unknown
	 */
	Analyzer getAnalyzer(String name);

	/**
	 * Retrieves the scoped analyzer for a given class.
	 *
	 * @param clazz The class for which to retrieve the analyzer.
	 *
	 * @return The scoped analyzer for the specified class.
	 *
	 * @throws java.lang.IllegalArgumentException in case {@code clazz == null} or the specified
	 * class is not an indexed entity.
	 */
	Analyzer getAnalyzer(Class<?> clazz);

	/**
	 * @return return a query builder providing a fluent API to create Lucene queries
	 */
	QueryContextBuilder buildQueryBuilder();

	/**
	 * Retrieve the statistics instance for this factory.
	 *
	 * @return The statistics.
	 */
	Statistics getStatistics();

	/**
	 * Provides access to the IndexReader API
	 *
	 * @return the IndexReaderAccessor for this SearchIntegrator
	 */
	IndexReaderAccessor getIndexReaderAccessor();

	/**
	 * Returns a descriptor for the specified entity type describing its indexed state.
	 *
	 * @param entityType the entity for which to retrieve the descriptor
	 *
	 * @return a non {@code null} {@code IndexedEntityDescriptor}. This method can also be called for non indexed types.
	 *         To determine whether the entity is actually indexed {@link org.hibernate.search.metadata.IndexedTypeDescriptor#isIndexed()} can be used.
	 *
	 * @throws IllegalArgumentException in case {@code entityType} is {@code null}
	 */
	IndexedTypeDescriptor getIndexedTypeDescriptor(Class<?> entityType);

	/**
	 * Returns the set of currently indexed types.
	 *
	 * @return the set of currently indexed types. If no types are indexed the empty set is returned.
	 */
	Set<Class<?>> getIndexedTypes();

	/**
	 * Unwraps some internal Hibernate Search types.
	 * Currently, no public type is accessible. This method should not be used by users.
	 * @param <T> the type of the unwrapped object
	 * @param cls the class of the internal object to unwrap
	 * @return the unwrapped object
	 */
	<T> T unwrap(Class<T> cls);

	/**
	 * Returns the service manager.
	 *
	 * @return Returns the service manager.
	 */
	ServiceManager getServiceManager();

	/**
	 * The Worker is the entry point to apply writes and updates to the indexes.
	 * @return the {@link Worker}
	 */
	Worker getWorker();

	/**
	 * Shuts down all workers and releases all resources.
	 */
	@Override
	void close();

	/**
	 * Get an {@link IndexManager} using the name
	 * @param indexName the name of the {@link IndexManager}
	 * @return the selected {@link IndexManager}
	 */
	IndexManager getIndexManager(String indexName);

	/**
	 * @return the current indexing strategy as specified via {@link org.hibernate.search.cfg.Environment#INDEXING_STRATEGY}.
	 */
	IndexingMode getIndexingMode();

	BatchBackend makeBatchBackend(MassIndexerProgressMonitor progressMonitor);
}
