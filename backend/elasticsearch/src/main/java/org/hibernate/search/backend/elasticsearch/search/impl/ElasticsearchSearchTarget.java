/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.impl;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.hibernate.search.backend.elasticsearch.impl.ElasticsearchBackend;
import org.hibernate.search.backend.elasticsearch.index.impl.ElasticsearchIndexManager;
import org.hibernate.search.backend.elasticsearch.logging.impl.Log;
import org.hibernate.search.backend.elasticsearch.orchestration.impl.ElasticsearchWorkOrchestrator;
import org.hibernate.search.backend.elasticsearch.search.clause.impl.ElasticsearchClauseFactory;
import org.hibernate.search.backend.elasticsearch.search.clause.impl.ElasticsearchClauseFactoryImpl;
import org.hibernate.search.backend.elasticsearch.search.dsl.impl.QueryTargetContext;
import org.hibernate.search.backend.elasticsearch.search.dsl.impl.SearchContextImpl;
import org.hibernate.search.backend.elasticsearch.work.impl.ElasticsearchWorkFactory;
import org.hibernate.search.engine.backend.index.spi.SearchTarget;
import org.hibernate.search.engine.common.spi.SessionContext;
import org.hibernate.search.engine.search.DocumentReference;
import org.hibernate.search.engine.search.SearchQuery;
import org.hibernate.search.engine.search.spi.SearchResultDefinitionContext;
import org.hibernate.search.engine.search.spi.SearchWrappingDefinitionContext;
import org.hibernate.search.util.spi.LoggerFactory;

import com.google.gson.JsonObject;


/**
 * @author Yoann Rodiere
 */
public class ElasticsearchSearchTarget implements SearchTarget {

	private static final Log log = LoggerFactory.make( Log.class );

	private final ElasticsearchBackend backend;

	private final Set<ElasticsearchIndexManager> indexManagers = new HashSet<>();

	public ElasticsearchSearchTarget(ElasticsearchBackend backend, ElasticsearchIndexManager indexManager) {
		this.backend = backend;
		indexManagers.add( indexManager );
	}

	@Override
	public void add(SearchTarget other) {
		if ( ! (other instanceof ElasticsearchSearchTarget) ) {
			throw log.cannotMixElasticsearchSearchTargetWithOtherType( this, other );
		}
		ElasticsearchSearchTarget otherEs = (ElasticsearchSearchTarget) other;
		if ( ! backend.equals( otherEs.backend ) ) {
			throw log.cannotMixElasticsearchSearchTargetWithOtherBackend( this, otherEs );
		}
		indexManagers.addAll( otherEs.indexManagers );
	}

	@Override
	public SearchResultDefinitionContext<DocumentReference> search(SessionContext context) {
		return new SearchResultDefinitionContextImpl( context );
	}

	@Override
	public String toString() {
		return new StringBuilder( getClass().getSimpleName() )
				.append( "[" )
				.append( "backend=" ).append( backend )
				.append( ", indexManagers=" ).append( indexManagers )
				.append( "]")
				.toString();
	}

	private class SearchResultDefinitionContextImpl implements SearchResultDefinitionContext<DocumentReference> {

		private final SessionContext context;

		public SearchResultDefinitionContextImpl(SessionContext context) {
			this.context = context;
		}

		@Override
		public <T> SearchWrappingDefinitionContext<SearchQuery<T>> asReferences(Function<DocumentReference, T> hitTransformer) {
			ElasticsearchSearchQueryFactory<SearchQuery<T>> queryFactory = new SearchQueryFactory<>( hitTransformer );
			QueryTargetContext targetContext = new QueryTargetContextImpl();
			return new SearchContextImpl<>( targetContext, queryFactory );
		}

		@Override
		public <T> SearchWrappingDefinitionContext<SearchQuery<T>> asProjections(Function<List<?>, T> hitTransformer, String... projections) {
			// TODO projections
			throw new UnsupportedOperationException();
		}

		private class SearchQueryFactory<T> implements ElasticsearchSearchQueryFactory<SearchQuery<T>> {

			private final Function<DocumentReference, T> hitTransformer;

			public SearchQueryFactory(Function<DocumentReference, T> hitTransformer) {
				this.hitTransformer = hitTransformer;
			}

			@Override
			public SearchQuery<T> create(JsonObject rootQueryClause) {
				String tenantId = context.getTenantIdentifier();
				if ( tenantId != null ) {
					// TODO handle tenant ID filtering
				}
				ElasticsearchWorkOrchestrator queryOrchestrator = backend.getQueryOrchestrator();
				ElasticsearchWorkFactory workFactory = backend.getWorkFactory();
				Set<String> indexNames = indexManagers.stream().map( ElasticsearchIndexManager::getName ).collect( Collectors.toSet() );
				return new ElasticsearchSearchQuery<>( queryOrchestrator, workFactory, indexNames,
						rootQueryClause, hitTransformer );
			}
		}

	}

	private class QueryTargetContextImpl implements QueryTargetContext {

		private final ElasticsearchClauseFactory clauseFactory;

		public QueryTargetContextImpl() {
			this.clauseFactory = new ElasticsearchClauseFactoryImpl(
					indexManagers.stream().map( ElasticsearchIndexManager::getModel ).collect( Collectors.toSet() )
					);
		}

		@Override
		public ElasticsearchClauseFactory getClauseFactory() {
			return clauseFactory;
		}

	}

}
