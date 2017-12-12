/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.index.impl;

import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.hibernate.search.backend.elasticsearch.document.model.impl.ElasticsearchIndexModel;
import org.hibernate.search.backend.elasticsearch.impl.ElasticsearchBackend;
import org.hibernate.search.backend.elasticsearch.orchestration.impl.ElasticsearchWorkOrchestrator;
import org.hibernate.search.backend.elasticsearch.search.dsl.impl.SearchTargetContext;
import org.hibernate.search.backend.elasticsearch.search.dsl.impl.SingleSearchPredicateContainerContext;
import org.hibernate.search.backend.elasticsearch.search.impl.SearchQueryResultDefinitionContextImpl;
import org.hibernate.search.backend.elasticsearch.search.predicate.impl.ElasticsearchSearchPredicateFactory;
import org.hibernate.search.backend.elasticsearch.search.predicate.impl.ElasticsearchSearchPredicateFactoryImpl;
import org.hibernate.search.backend.elasticsearch.work.impl.ElasticsearchWorkFactory;
import org.hibernate.search.engine.backend.index.spi.IndexSearchTarget;
import org.hibernate.search.engine.common.spi.SessionContext;
import org.hibernate.search.engine.search.DocumentReference;
import org.hibernate.search.engine.search.ObjectLoader;
import org.hibernate.search.engine.search.SearchPredicate;
import org.hibernate.search.engine.search.dsl.predicate.SearchPredicateContainerContext;
import org.hibernate.search.engine.search.spi.SearchQueryResultDefinitionContext;


/**
 * @author Yoann Rodiere
 */
class ElasticsearchIndexSearchTarget implements IndexSearchTarget, SearchTargetContext {

	private final ElasticsearchBackend backend;

	private final ElasticsearchSearchPredicateFactory searchPredicateFactory;

	private final Set<ElasticsearchIndexModel> indexModels;

	private final Set<String> indexNames;

	ElasticsearchIndexSearchTarget(ElasticsearchBackend backend, Set<ElasticsearchIndexModel> indexModels) {
		this.backend = backend;
		this.searchPredicateFactory = new ElasticsearchSearchPredicateFactoryImpl( indexModels );
		this.indexModels = indexModels;
		this.indexNames = indexModels.stream()
				.map( ElasticsearchIndexModel::getIndexName )
				.collect( Collectors.toSet() );
	}

	@Override
	public <R, O> SearchQueryResultDefinitionContext<R, O> query(
			SessionContext context,
			Function<DocumentReference, R> documentReferenceTransformer,
			ObjectLoader<R, O> objectLoader) {
		return new SearchQueryResultDefinitionContextImpl<>( this, context,
				documentReferenceTransformer, objectLoader );
	}

	@Override
	public SearchPredicateContainerContext<SearchPredicate> predicate() {
		return new SingleSearchPredicateContainerContext( this );
	}

	@Override
	public Set<String> getIndexNames() {
		return indexNames;
	}

	@Override
	public Set<ElasticsearchIndexModel> getIndexModels() {
		return indexModels;
	}

	@Override
	public ElasticsearchSearchPredicateFactory getSearchPredicateFactory() {
		return searchPredicateFactory;
	}

	@Override
	public ElasticsearchWorkFactory getWorkFactory() {
		return backend.getWorkFactory();
	}

	@Override
	public ElasticsearchWorkOrchestrator getQueryOrchestrator() {
		return backend.getQueryOrchestrator();
	}

	@Override
	public String toString() {
		return new StringBuilder( getClass().getSimpleName() )
				.append( "[" )
				.append( "backend=" ).append( backend )
				.append( ", indexNames=" ).append( indexNames )
				.append( "]")
				.toString();
	}
}
