/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.search.impl;

import java.util.List;
import java.util.function.Function;

import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.search.mapper.orm.hibernate.FullTextQuery;
import org.hibernate.search.mapper.orm.hibernate.HibernateOrmSearchResultDefinitionContext;
import org.hibernate.search.mapper.orm.impl.FullTextQueryImpl;
import org.hibernate.search.mapper.pojo.mapping.spi.PojoSearchTarget;
import org.hibernate.search.mapper.pojo.mapping.spi.PojoSessionContext;
import org.hibernate.search.mapper.pojo.search.PojoReference;
import org.hibernate.search.engine.search.ObjectLoader;
import org.hibernate.search.engine.search.dsl.SearchContext;
import org.hibernate.search.engine.search.dsl.SearchResultDefinitionContext;

public class HibernateOrmSearchResultDefinitionContextImpl<O>
		implements HibernateOrmSearchResultDefinitionContext<O> {
	private final PojoSearchTarget<O> searchTarget;
	private final PojoSessionContext sessionContext;
	private final SessionImplementor sessionImplementor;
	private final ObjectLoaderBuilder<O> objectLoaderBuilder;

	public HibernateOrmSearchResultDefinitionContextImpl(
			PojoSearchTarget<O> searchTarget,
			PojoSessionContext sessionContext,
			SessionImplementor sessionImplementor) {
		this.searchTarget = searchTarget;
		this.sessionContext = sessionContext;
		this.sessionImplementor = sessionImplementor;
		this.objectLoaderBuilder = new ObjectLoaderBuilder<>( sessionImplementor, searchTarget.getTargetedIndexedTypes() );
	}

	@Override
	public SearchContext<? extends FullTextQuery<O>> asEntities() {
		MutableObjectLoadingOptions loadingOptions = new MutableObjectLoadingOptions();
		return this.<O>search( objectLoaderBuilder.build( loadingOptions ) )
				.asObjects()
				.asWrappedQuery( q -> new FullTextQueryImpl<>( q, sessionImplementor, loadingOptions ) );
	}

	@Override
	public <T> SearchContext<? extends FullTextQuery<T>> asEntities(Function<O, T> hitTransformer) {
		MutableObjectLoadingOptions loadingOptions = new MutableObjectLoadingOptions();
		return this.<T>search( objectLoaderBuilder.build( loadingOptions, hitTransformer ) )
				.asObjects()
				.asWrappedQuery( q -> new FullTextQueryImpl<>( q, sessionImplementor, loadingOptions ) );
	}

	@Override
	public <T> SearchContext<? extends FullTextQuery<T>> asProjections(
			Function<List<?>, T> hitTransformer, String... projections) {
		MutableObjectLoadingOptions loadingOptions = new MutableObjectLoadingOptions();
		return search( objectLoaderBuilder.build( loadingOptions ) )
				.asProjections( hitTransformer, projections )
				.asWrappedQuery( q -> new FullTextQueryImpl<>( q, sessionImplementor, loadingOptions ) );
	}

	private <T> SearchResultDefinitionContext<PojoReference, T> search(ObjectLoader<PojoReference, T> objectLoader) {
		return searchTarget.search( sessionContext, objectLoader );
	}
}
