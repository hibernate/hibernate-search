/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.search.dsl.query;

import org.hibernate.query.Query;
import org.hibernate.search.engine.search.dsl.projection.SearchProjectionFactoryContext;
import org.hibernate.search.engine.search.dsl.query.SearchQueryResultDefinitionContext;
import org.hibernate.search.mapper.pojo.search.PojoReference;

public interface HibernateOrmSearchQueryResultDefinitionContext<O>
		extends SearchQueryResultDefinitionContext<
				PojoReference,
				O,
				SearchProjectionFactoryContext<PojoReference, O>
				> {

	/**
	 * Set the JDBC fetch size for this query.
	 *
	 * @param fetchSize The fetch size. Must be positive or zero.
	 * @return {@code this} for method chaining.
	 * @see Query#setFetchSize(int)
	 */
	HibernateOrmSearchQueryResultDefinitionContext<O> fetchSize(int fetchSize);

}
