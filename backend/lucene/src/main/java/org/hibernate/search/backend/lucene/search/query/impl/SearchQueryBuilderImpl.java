/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.query.impl;

import java.util.List;
import java.util.function.Function;

import org.hibernate.search.backend.lucene.multitenancy.impl.MultiTenancyStrategy;
import org.hibernate.search.backend.lucene.orchestration.impl.LuceneQueryWorkOrchestrator;
import org.hibernate.search.backend.lucene.search.impl.LuceneQueries;
import org.hibernate.search.backend.lucene.search.impl.LuceneSearchQueryElementCollector;
import org.hibernate.search.backend.lucene.search.impl.LuceneSearchTargetModel;
import org.hibernate.search.backend.lucene.work.impl.LuceneWorkFactory;
import org.hibernate.search.engine.common.spi.SessionContext;
import org.hibernate.search.engine.search.SearchQuery;
import org.hibernate.search.engine.search.query.spi.HitAggregator;
import org.hibernate.search.engine.search.query.spi.SearchQueryBuilder;

import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;

class SearchQueryBuilderImpl<C, T>
		implements SearchQueryBuilder<T, LuceneSearchQueryElementCollector> {

	private final LuceneWorkFactory workFactory;
	private final LuceneQueryWorkOrchestrator queryOrchestrator;
	private final MultiTenancyStrategy multiTenancyStrategy;

	private final LuceneSearchTargetModel searchTargetModel;
	private final String tenantId;

	private final ReusableDocumentStoredFieldVisitor storedFieldVisitor;
	private final HitExtractor<? super C> hitExtractor;
	private final HitAggregator<C, List<T>> hitAggregator;
	private final LuceneSearchQueryElementCollector elementCollector;

	SearchQueryBuilderImpl(
			LuceneWorkFactory workFactory,
			LuceneQueryWorkOrchestrator queryOrchestrator,
			MultiTenancyStrategy multiTenancyStrategy,
			LuceneSearchTargetModel searchTargetModel,
			SessionContext sessionContext,
			ReusableDocumentStoredFieldVisitor storedFieldVisitor,
			HitExtractor<? super C> hitExtractor,
			HitAggregator<C, List<T>> hitAggregator) {
		this.workFactory = workFactory;
		this.queryOrchestrator = queryOrchestrator;
		this.multiTenancyStrategy = multiTenancyStrategy;

		this.searchTargetModel = searchTargetModel;
		this.tenantId = sessionContext.getTenantIdentifier();

		this.elementCollector = new LuceneSearchQueryElementCollector();
		this.storedFieldVisitor = storedFieldVisitor;
		this.hitExtractor = hitExtractor;
		this.hitAggregator = hitAggregator;
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
		SearchResultExtractor<T> searchResultExtractor = new SearchResultExtractorImpl<>( storedFieldVisitor, hitExtractor, hitAggregator );

		BooleanQuery.Builder luceneQueryBuilder = new BooleanQuery.Builder();
		luceneQueryBuilder.add( elementCollector.toLuceneQueryPredicate(), Occur.MUST );
		luceneQueryBuilder.add( LuceneQueries.mainDocumentQuery(), Occur.FILTER );

		return new LuceneSearchQuery<T>( queryOrchestrator, workFactory,
				searchTargetModel.getIndexNames(), searchTargetModel.getReaderProviders(),
				multiTenancyStrategy.decorateLuceneQuery( luceneQueryBuilder.build(), tenantId ),
				elementCollector.toLuceneSort(),
				hitExtractor, searchResultExtractor );
	}

	@Override
	public <Q> Q build(Function<SearchQuery<T>, Q> searchQueryWrapperFactory) {
		return searchQueryWrapperFactory.apply( build() );
	}
}
