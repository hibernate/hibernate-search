/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.hibernate;

import java.util.List;
import java.util.function.Function;

import org.hibernate.search.engine.search.dsl.SearchContext;

/**
 * @author Yoann Rodiere
 */
public interface HibernateOrmSearchResultDefinitionContext<O>
		extends org.hibernate.search.mapper.orm.jpa.HibernateOrmSearchResultDefinitionContext<O> {

	@Override
	default SearchContext<? extends FullTextQuery<O>> asEntities() {
		return asEntities( Function.identity() );
	}

	@Override
	default SearchContext<? extends FullTextQuery<List<?>>> asProjections(String... projections) {
		return asProjections( Function.identity(), projections );
	}

	@Override
	<T> SearchContext<? extends FullTextQuery<T>> asEntities(Function<O, T> hitTransformer);

	@Override
	<T> SearchContext<? extends FullTextQuery<T>> asProjections(
			Function<List<?>, T> hitTransformer,
			String... projections);

}
