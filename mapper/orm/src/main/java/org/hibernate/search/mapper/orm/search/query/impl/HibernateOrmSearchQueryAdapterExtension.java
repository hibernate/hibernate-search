/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.search.query.impl;


import java.util.Optional;

import org.hibernate.search.engine.search.loading.spi.SearchLoadingContext;
import org.hibernate.search.engine.search.query.SearchQuery;
import org.hibernate.search.engine.search.query.SearchQueryExtension;
import org.hibernate.search.engine.search.query.spi.SearchQueryImplementor;
import org.hibernate.search.mapper.orm.loading.impl.HibernateOrmSelectionLoadingContext;

final class HibernateOrmSearchQueryAdapterExtension<H> implements
		SearchQueryExtension<HibernateOrmSearchQueryAdapter<H>, H> {
	private static final HibernateOrmSearchQueryAdapterExtension<Object> INSTANCE = new HibernateOrmSearchQueryAdapterExtension<>();

	@SuppressWarnings("unchecked") // The instance works for any H
	static <H> HibernateOrmSearchQueryAdapterExtension<H> get() {
		return (HibernateOrmSearchQueryAdapterExtension<H>) INSTANCE;
	}

	@Override
	public Optional<HibernateOrmSearchQueryAdapter<H>> extendOptional(SearchQuery<H> original, SearchLoadingContext<?> loadingContext) {
		Object unwrapped = loadingContext.unwrap();
		if ( unwrapped instanceof HibernateOrmSelectionLoadingContext ) {
			HibernateOrmSelectionLoadingContext castedLoadingContext = (HibernateOrmSelectionLoadingContext) unwrapped;
			return Optional.of( new HibernateOrmSearchQueryAdapter<>(
					// All SearchQuery implementations should implement SearchQueryImplementor
					(SearchQueryImplementor<H>) original,
					castedLoadingContext.sessionImplementor(),
					castedLoadingContext.loadingOptions()
			) );
		}
		else {
			return Optional.empty();
		}
	}
}
