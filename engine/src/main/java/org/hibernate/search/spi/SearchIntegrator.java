/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.spi;

import java.util.function.Predicate;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.search.Query;
import org.hibernate.search.backend.spi.BatchBackend;
import org.hibernate.search.backend.spi.OperationDispatcher;
import org.hibernate.search.backend.spi.Worker;
import org.hibernate.search.batchindexing.MassIndexerProgressMonitor;
import org.hibernate.search.engine.service.spi.ServiceManager;
import org.hibernate.search.engine.spi.EntityIndexBinding;
import org.hibernate.search.exception.ErrorHandler;
import org.hibernate.search.indexes.IndexReaderAccessor;
import org.hibernate.search.indexes.serialization.spi.LuceneWorkSerializer;
import org.hibernate.search.indexes.spi.IndexManager;
import org.hibernate.search.metadata.IndexedTypeDescriptor;
import org.hibernate.search.query.dsl.QueryContextBuilder;
import org.hibernate.search.query.engine.spi.HSQuery;
import org.hibernate.search.query.engine.spi.TimeoutExceptionFactory;
import org.hibernate.search.stat.Statistics;

/**
 * This contract gives access to lower level APIs of Hibernate Search for
 * frameworks integrating with it.
 * Frameworks should not expose this as public API though, but expose a simplified view; for
 * example the Hibernate Search ORM module expose the {@code org.hibernate.search.SearchFactory} contract to
 * its clients.
 *
 * @author Emmanuel Bernard
 * @author Sanne Grinovero
 */
public interface SearchIntegrator extends AutoCloseable {

	/**
	 * Returns a map of all known entity index binding (indexed entities) keyed against the indexed type
	 *
	 * @return a map of all known entity index binding (indexed entities) keyed against the indexed type. The empty
	 * map is returned if there are no indexed types.
	 */
	IndexedTypeMap<EntityIndexBinding> getIndexBindings();

	/**
	 * Returns the entity to index binding for the given type.
	 * @param entityType the type for which to retrieve the binding
	 * @return the entity to index binding for the given type. {@code null} is returned for types which are unindexed or
	 *         unknown.
	 */
	EntityIndexBinding getIndexBinding(IndexedTypeIdentifier entityType);

	/**
	 * Add the following classes to the SearchIntegrator. If these classes are new to the SearchIntegrator this
	 * will trigger a reconfiguration.
	 * @param classes the classes to add to the {@link SearchIntegrator}
	 */
	void addClasses(Class<?>... classes);

	/**
	 * Return an Hibernate Search query object.
	 * <p>This method DOES support non-Lucene backends (e.g. Elasticsearch).
	 * <p>The returned object uses fluent APIs to define additional query settings.
	 * <p>Be aware that some backends may not implement {@link HSQuery#luceneQuery(Query)},
	 * in which case the query provided here cannot be overridden.
	 *
	 * @param fullTextQuery the full-text engine query
	 * @param entityTypes the targeted entity types
	 * @return an Hibernate Search query object
	 */
	HSQuery createHSQuery(Query fullTextQuery, Class<?>... entityTypes);

	/**
	 * Return an Hibernate Search query object.
	 * <p>This method DOES support non-Lucene backends (e.g. Elasticsearch).
	 * <p>The returned object uses fluent APIs to define additional query settings.
	 * <p>Be aware that some backends may not implement {@link HSQuery#luceneQuery(Query)},
	 * in which case the query provided here cannot be overridden.
	 *
	 * @param fullTextQuery the full-text engine query
	 * @param types the targeted entity types, mapped to (potentially null) overridden metadata
	 * @return an Hibernate Search query object
	 */
	HSQuery createHSQuery(Query fullTextQuery, IndexedTypeMap<CustomTypeMetadata> types);

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
	void optimize(IndexedTypeIdentifier entityType);

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
	 * Retrieves the scoped analyzer for a given indexed type.
	 *
	 * @param typeId The indexed type identifier for which to retrieve the analyzer.
	 *
	 * @return The scoped analyzer for the specified class.
	 *
	 * @throws java.lang.IllegalArgumentException in case {@code clazz == null} or the specified
	 * class is not an indexed entity.
	 */
	Analyzer getAnalyzer(IndexedTypeIdentifier typeId);

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
	 * @param typeId the identification of the indexed entity for which to retrieve the descriptor
	 *
	 * @return a non {@code null} {@code IndexedEntityDescriptor}. This method can also be called for non indexed types.
	 *         To determine whether the entity is actually indexed {@link org.hibernate.search.metadata.IndexedTypeDescriptor#isIndexed()} can be used.
	 *
	 * @throws IllegalArgumentException in case {@code entityType} is {@code null}
	 */
	IndexedTypeDescriptor getIndexedTypeDescriptor(IndexedTypeIdentifier typeId);

	/**
	 * Returns the set of currently indexed types.
	 *
	 * @return the set of currently indexed types. This might be empty.
	 */
	IndexedTypeSet getIndexedTypeIdentifiers();

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

	LuceneWorkSerializer getWorkSerializer();

	/**
	 * @param indexManagerFilter A predicate allowing to exclude index managers from
	 * dispatching. Works will not be applied to these index managers.
	 * @return An operation dispatcher allowing to insert works retrieved from
	 * remote sources (e.g. JMS or JGroups slaves), but only for index managers
	 * verifying the given predicate.
	 * This allows JMS or JGroups integrations to perform checks on index managers that
	 * wouldn't exist before the dispatch, in the case of dynamic sharding in particular.
	 *
	 * @hsearch.experimental Operation dispatchers are under active development.
	 * You should be prepared for incompatible changes in future releases.
	 */
	OperationDispatcher createRemoteOperationDispatcher(Predicate<IndexManager> indexManagerFilter);

}
