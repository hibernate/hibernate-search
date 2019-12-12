/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.search.query.impl;


import java.util.Optional;

import org.hibernate.search.engine.search.loading.context.spi.LoadingContext;
import org.hibernate.search.engine.search.query.SearchQuery;
import org.hibernate.search.engine.search.query.SearchQueryExtension;
import org.hibernate.search.engine.search.query.spi.SearchQueryImplementor;
import org.hibernate.search.mapper.orm.search.loading.context.impl.HibernateOrmLoadingContext;

final class HibernateOrmSearchQueryAdapterExtension<H> implements
		SearchQueryExtension<HibernateOrmSearchQueryAdapter<H>, H> {
	private static final HibernateOrmSearchQueryAdapterExtension<Object> INSTANCE = new HibernateOrmSearchQueryAdapterExtension<>();

	@SuppressWarnings("unchecked") // The instance works for any H
	static <H> HibernateOrmSearchQueryAdapterExtension<H> get() {
		return (HibernateOrmSearchQueryAdapterExtension<H>) INSTANCE;
	}

	@Override
	public Optional<HibernateOrmSearchQueryAdapter<H>> extendOptional(SearchQuery<H> original, LoadingContext<?, ?> loadingContext) {
		if ( loadingContext instanceof HibernateOrmLoadingContext ) {
			HibernateOrmLoadingContext<?> castedLoadingContext = (HibernateOrmLoadingContext<?>) loadingContext;
			return Optional.of( new HibernateOrmSearchQueryAdapter<>(
					// All SearchQuery implementations should implement SearchQueryImplementor
					(SearchQueryImplementor<H>) original,
					castedLoadingContext.getSessionImplementor(),
					castedLoadingContext.getLoadingOptions()
			) );
		}
		else {
			return Optional.empty();
		}
	}
}
