/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.query;

import java.util.function.Consumer;

import org.hibernate.search.engine.search.query.SearchQuery;
import org.hibernate.search.engine.search.query.dsl.SearchQueryOptionsStep;

/**
 * Define whether or not to check whether objects are already present in the second level cache or the persistence context.
 *
 * In most cases, no presence check is necessary.
 *
 * @author Emmanuel Bernard
 * @deprecated Instead of using Hibernate Search 5 APIs, get a {@code org.hibernate.search.mapper.orm.session.SearchSession}
 * using {@code org.hibernate.search.mapper.orm.Search#session(org.hibernate.Session)},
 * create a {@link SearchQuery} with {@code org.hibernate.search.mapper.orm.session.SearchSession#search(Class)},
 * and define your loading options using {@link SearchQueryOptionsStep#loading(Consumer)}.
 * To set the equivalent to {@link ObjectLookupMethod} in Hibernate Search 6,
 * use {@code org.hibernate.search.mapper.orm.search.loading.dsl.SearchLoadingOptionsStep#cacheLookupStrategy(org.hibernate.search.mapper.orm.search.loading.EntityLoadingCacheLookupStrategy)}
 * Refer to the <a href="https://hibernate.org/search/documentation/migrate/6.0/">migration guide</a> for more information.
 */
@Deprecated
public enum ObjectLookupMethod {

	/**
	 * Skip checking (default)
	 */
	SKIP,

	/**
	 * Check whether an object is already in the persistence context before initializing it
	 */
	PERSISTENCE_CONTEXT,

	/**
	 * Check whether an object is already either in the persistence context or in the second level cache before loading it.
	 */
	SECOND_LEVEL_CACHE

}
