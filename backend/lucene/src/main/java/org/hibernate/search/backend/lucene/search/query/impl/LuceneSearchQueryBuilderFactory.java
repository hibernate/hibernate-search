/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.query.impl;

import org.hibernate.search.backend.lucene.search.common.impl.LuceneSearchIndexScope;
import org.hibernate.search.backend.lucene.search.projection.impl.LuceneSearchProjection;
import org.hibernate.search.engine.backend.session.spi.BackendSessionContext;
import org.hibernate.search.engine.search.loading.spi.SearchLoadingContextBuilder;
import org.hibernate.search.engine.search.projection.SearchProjection;
import org.hibernate.search.engine.search.query.spi.SearchQueryBuilderFactory;

public class LuceneSearchQueryBuilderFactory
		implements SearchQueryBuilderFactory {

	private final SearchBackendContext searchBackendContext;

	private final LuceneSearchIndexScope scope;

	public LuceneSearchQueryBuilderFactory(SearchBackendContext searchBackendContext,
			LuceneSearchIndexScope scope) {
		this.searchBackendContext = searchBackendContext;
		this.scope = scope;
	}

	@Override
	public <P> LuceneSearchQueryBuilder<P> select(
			BackendSessionContext sessionContext, SearchLoadingContextBuilder<?, ?, ?> loadingContextBuilder,
			SearchProjection<P> projection) {
		return searchBackendContext.createSearchQueryBuilder(
				scope, sessionContext, loadingContextBuilder, LuceneSearchProjection.from( scope, projection )
		);
	}
}
