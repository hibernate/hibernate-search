/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search;

import java.util.Set;

import org.hibernate.SessionFactory;
import org.hibernate.search.backend.lucene.LuceneBackend;
import org.hibernate.search.backend.lucene.index.LuceneIndexManager;
import org.hibernate.search.engine.backend.Backend;
import org.hibernate.search.engine.backend.index.IndexManager;
import org.hibernate.search.mapper.orm.entity.SearchIndexedEntity;
import org.hibernate.search.mapper.orm.mapping.SearchMapping;
import org.hibernate.search.mapper.orm.scope.SearchScope;
import org.hibernate.search.mapper.orm.session.SearchSession;
import org.hibernate.search.mapper.orm.work.SearchWorkspace;
import org.hibernate.search.query.dsl.FacetContext;
import org.hibernate.search.query.dsl.QueryBuilder;
import org.hibernate.search.query.dsl.QueryContextBuilder;
import org.hibernate.search.query.dsl.sort.SortContext;

import org.apache.lucene.analysis.Analyzer;

/**
 * Provide application wide operations as well as access to the underlying Lucene resources.
 *
 * @author Emmanuel Bernard
 * @author Hardy Ferentschik
 * @author Sanne Grinovero
 * @deprecated Instead of using Hibernate Search 5 APIs, get a {@link SearchMapping}
 * using {@link org.hibernate.search.mapper.orm.Search#mapping(jakarta.persistence.EntityManagerFactory)},
 * or {@link org.hibernate.search.mapper.orm.Search#mapping(SessionFactory)}.
 * See the individual methods for the replacement of each method.
 * Refer to the <a href="https://hibernate.org/search/documentation/migrate/6.0/">migration guide</a> for more information.
 */
public interface SearchFactory {

	/**
	 * Optimize all indexes
	 * @deprecated Instead of using Hibernate Search 5 APIs, get a {@link SearchScope}
	 * using {@link SearchSession#scope(Class)}
	 * or {@link SearchMapping#scope(Class)},
	 * then a {@link SearchWorkspace} using {@link SearchScope#workspace()},
	 * then call {@link SearchWorkspace#mergeSegments()} to "optimize" all indexes in scope.
	 */
	@Deprecated
	void optimize();

	/**
	 * Optimize the index holding {@code entityType}
	 *
	 * @param entityType the entity type (index) to optimize
	 * @deprecated Instead of using Hibernate Search 5 APIs, get a {@link SearchScope}
	 * using {@link SearchSession#scope(Class)}
	 * or {@link SearchMapping#scope(Class)},
	 * then a {@link SearchWorkspace} using {@link SearchScope#workspace()},
	 * then call {@link SearchWorkspace#mergeSegments()} to "optimize" all indexes in scope.
	 */
	@Deprecated
	void optimize(Class<?> entityType);

	/**
	 * Retrieve an analyzer instance by its definition name
	 *
	 * @param name the name of the analyzer
	 *
	 * @return analyzer with the specified name
	 *
	 * @throws org.hibernate.search.util.common.SearchException if the definition name is unknown
	 * @deprecated Instead of using Hibernate Search 5 APIs, get a {@link SearchMapping}
	 * using {@link org.hibernate.search.mapper.orm.Search#mapping(jakarta.persistence.EntityManagerFactory)},
	 * or {@link org.hibernate.search.mapper.orm.Search#mapping(SessionFactory)},
	 * then get access to the backend using {@link SearchMapping#backend()},
	 * then convert it to a {@link LuceneBackend} using {@link Backend#unwrap(Class)},
	 * then get the analyzer using {@link LuceneBackend#analyzer(String)}.
	 * Refer to the <a href="https://hibernate.org/search/documentation/migrate/6.0/">migration guide</a> for more information.
	 */
	@Deprecated
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
	 * @deprecated Instead of using Hibernate Search 5 APIs, get a {@link SearchMapping}
	 * using {@link org.hibernate.search.mapper.orm.Search#mapping(jakarta.persistence.EntityManagerFactory)},
	 * or {@link org.hibernate.search.mapper.orm.Search#mapping(SessionFactory)},
	 * then get access to the entity metadata using {@link SearchMapping#indexedEntity(Class)},
	 * then get the corresponding index using {@link SearchIndexedEntity#indexManager()},
	 * then convert it to a {@link LuceneIndexManager} using {@link IndexManager#unwrap(Class)},
	 * then get the analyzer using {@link LuceneIndexManager#searchAnalyzer()}.
	 * Refer to the <a href="https://hibernate.org/search/documentation/migrate/6.0/">migration guide</a> for more information.
	 */
	@Deprecated
	Analyzer getAnalyzer(Class<?> clazz);

	/**
	 * @return return a query builder providing a fluent API to create Lucene queries
	 * @deprecated See the deprecation note on {@link QueryBuilder} for predicates ("queries")
	 * {@link SortContext} for sorts, {@link FacetContext} for aggregations ("facets").
	 */
	@Deprecated
	QueryContextBuilder buildQueryBuilder();

	/**
	 * Returns the set of currently indexed types.
	 *
	 * @return the set of currently indexed types. If no types are indexed the empty set is returned.
	 * @deprecated Instead of using Hibernate Search 5 APIs, get a {@link SearchMapping}
	 * using {@link org.hibernate.search.mapper.orm.Search#mapping(jakarta.persistence.EntityManagerFactory)},
	 * or {@link org.hibernate.search.mapper.orm.Search#mapping(SessionFactory)},
	 * then get access to the entity metadata using {@link SearchMapping#allIndexedEntities()}.
	 */
	@Deprecated
	Set<Class<?>> getIndexedTypes();

	/**
	 * Unwraps some internal Hibernate Search types.
	 * Currently, no public type is accessible. This method should not be used by users.
	 *
	 * @param <T> the type of the unwrapped class
	 * @param cls the type to unwrap
	 * @return the unwrapped object
	 */
	<T> T unwrap(Class<T> cls);

}
