/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.query.impl;

import java.util.Set;
import java.util.stream.Collectors;

import org.hibernate.search.backend.elasticsearch.document.model.impl.ElasticsearchIndexModel;
import org.hibernate.search.backend.elasticsearch.impl.ElasticsearchBackend;
import org.hibernate.search.backend.elasticsearch.logging.impl.Log;
import org.hibernate.search.backend.elasticsearch.search.dsl.impl.ElasticsearchSearchPredicateCollector;
import org.hibernate.search.backend.elasticsearch.search.dsl.impl.ElasticsearchSingleSearchPredicateCollector;
import org.hibernate.search.backend.elasticsearch.search.predicate.impl.SearchPredicateFactoryImpl;
import org.hibernate.search.engine.search.SearchPredicate;
import org.hibernate.search.engine.search.dsl.spi.SearchPredicateContributor;
import org.hibernate.search.engine.search.predicate.spi.SearchPredicateFactory;
import org.hibernate.search.engine.search.query.spi.SearchQueryFactory;
import org.hibernate.search.util.spi.LoggerFactory;

public class ElasticsearchSearchTargetContextImpl implements ElasticsearchSearchTargetContext {

	private static final Log log = LoggerFactory.make( Log.class );

	private final SearchPredicateFactory<ElasticsearchSearchPredicateCollector> searchPredicateFactory;
	private final SearchQueryFactory<ElasticsearchSearchPredicateCollector> searchQueryFactory;

	public ElasticsearchSearchTargetContextImpl(ElasticsearchBackend backend, Set<ElasticsearchIndexModel> indexModels) {
		this.searchPredicateFactory = new SearchPredicateFactoryImpl( indexModels );
		Set<String> indexNames = indexModels.stream()
				.map( ElasticsearchIndexModel::getIndexName )
				.collect( Collectors.toSet() );
		this.searchQueryFactory = new SearchQueryFactoryImpl( backend, indexModels, indexNames );
	}

	@Override
	public SearchPredicate toSearchPredicate(SearchPredicateContributor<ElasticsearchSearchPredicateCollector> contributor) {
		ElasticsearchSingleSearchPredicateCollector collector = new ElasticsearchSingleSearchPredicateCollector();
		contributor.contribute( collector );
		return new ElasticsearchSearchPredicate( collector.toJson() );
	}

	@Override
	public SearchPredicateContributor<ElasticsearchSearchPredicateCollector> toContributor(SearchPredicate predicate) {
		if ( !( predicate instanceof ElasticsearchSearchPredicate ) ) {
			throw log.cannotMixElasticsearchSearchQueryWithOtherPredicates( predicate );
		}
		return (ElasticsearchSearchPredicate) predicate;
	}

	@Override
	public SearchPredicateFactory<ElasticsearchSearchPredicateCollector> getSearchPredicateFactory() {
		return searchPredicateFactory;
	}

	@Override
	public SearchQueryFactory<ElasticsearchSearchPredicateCollector> getSearchQueryFactory() {
		return searchQueryFactory;
	}
}
