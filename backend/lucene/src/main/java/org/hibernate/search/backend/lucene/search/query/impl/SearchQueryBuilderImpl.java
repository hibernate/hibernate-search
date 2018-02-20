/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.query.impl;

import java.util.List;
import java.util.function.Function;

import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.hibernate.search.backend.lucene.orchestration.impl.LuceneQueryWorkOrchestrator;
import org.hibernate.search.backend.lucene.search.impl.LuceneQueries;
import org.hibernate.search.backend.lucene.search.impl.LuceneSearchQueryElementCollector;
import org.hibernate.search.backend.lucene.search.impl.LuceneSearchTargetModel;
import org.hibernate.search.backend.lucene.work.impl.LuceneWorkFactory;
import org.hibernate.search.engine.common.spi.SessionContext;
import org.hibernate.search.engine.search.SearchQuery;
import org.hibernate.search.engine.search.query.spi.HitAggregator;
import org.hibernate.search.engine.search.query.spi.SearchQueryBuilder;

class SearchQueryBuilderImpl<C, T>
		implements SearchQueryBuilder<T, LuceneSearchQueryElementCollector> {

	private final LuceneQueryWorkOrchestrator queryOrchestrator;
	private final LuceneWorkFactory workFactory;
	private final LuceneSearchTargetModel searchTargetModel;
	private final HitExtractor<? super C> hitExtractor;
	private final HitAggregator<C, List<T>> hitAggregator;
	private final LuceneSearchQueryElementCollector elementCollector;

	public SearchQueryBuilderImpl(
			LuceneQueryWorkOrchestrator queryOrchestrator,
			LuceneWorkFactory workFactory,
			LuceneSearchTargetModel searchTargetModel,
			SessionContext context,
			HitExtractor<? super C> hitExtractor,
			HitAggregator<C, List<T>> hitAggregator) {
		this.hitExtractor = hitExtractor;
		this.hitAggregator = hitAggregator;
		String tenantId = context.getTenantIdentifier();
		if ( tenantId != null ) {
			// TODO handle tenant ID filtering
		}
		this.queryOrchestrator = queryOrchestrator;
		this.workFactory = workFactory;
		this.searchTargetModel = searchTargetModel;
		this.elementCollector = new LuceneSearchQueryElementCollector();
	}

	@Override
	public LuceneSearchQueryElementCollector getQueryElementCollector() {
		return elementCollector;
	}

	@Override
	public void addRoutingKey(String routingKey) {
		// TODO see what to do with the routing key
		throw new UnsupportedOperationException( "Routing keys are not supported by the Lucene backend yet." );
	}

	private SearchQuery<T> build() {
		SearchResultExtractor<T> searchResultExtractor = new SearchResultExtractorImpl<>( hitExtractor, hitAggregator );

		BooleanQuery.Builder luceneQueryBuilder = new BooleanQuery.Builder();
		luceneQueryBuilder.add( elementCollector.toLuceneQueryPredicate(), Occur.MUST );
		luceneQueryBuilder.add( LuceneQueries.mainDocumentQuery(), Occur.FILTER );

		return new LuceneSearchQuery<T>( queryOrchestrator, workFactory,
				searchTargetModel.getIndexNames(), searchTargetModel.getReaderProviders(),
				luceneQueryBuilder.build(), elementCollector.toLuceneSort(),
				hitExtractor, searchResultExtractor );
	}

	@Override
	public <Q> Q build(Function<SearchQuery<T>, Q> searchQueryWrapperFactory) {
		return searchQueryWrapperFactory.apply( build() );
	}
}
