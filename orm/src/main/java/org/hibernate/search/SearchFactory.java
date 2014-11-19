/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search;

import java.util.Set;

import org.apache.lucene.analysis.Analyzer;
import org.hibernate.search.indexes.IndexReaderAccessor;
import org.hibernate.search.metadata.IndexedTypeDescriptor;
import org.hibernate.search.query.dsl.QueryContextBuilder;
import org.hibernate.search.stat.Statistics;

/**
 * Provide application wide operations as well as access to the underlying Lucene resources.
 *
 * @author Emmanuel Bernard
 * @author Hardy Ferentschik
 * @author Sanne Grinovero
 */
public interface SearchFactory {

	/**
	 * Optimize all indexes
	 */
	void optimize();

	/**
	 * Optimize the index holding {@code entityType}
	 *
	 * @param entityType the entity type (index) to optimize
	 */
	void optimize(Class<?> entityType);

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
	 * @return the IndexReaderAccessor for this SearchFactory
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
	 */
	<T> T unwrap(Class<T> cls);

}
