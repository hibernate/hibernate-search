/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.spi;

import org.hibernate.search.engine.spi.EntityIndexBinding;
import org.hibernate.search.indexes.IndexReaderAccessor;
import org.hibernate.search.query.dsl.QueryContextBuilder;
import org.hibernate.search.query.engine.spi.HSQuery;
import org.hibernate.search.query.engine.spi.TimeoutExceptionFactory;
import org.hibernate.search.stat.Statistics;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.search.Query;

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
	 * Returns the set of currently indexed types.
	 *
	 * @return the set of currently indexed types. This might be empty.
	 */
	IndexedTypeSet getIndexedTypeIdentifiers();

	/**
	 * Shuts down all workers and releases all resources.
	 */
	@Override
	void close();

}
