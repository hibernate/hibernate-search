/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.impl;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.hibernate.search.backend.elasticsearch.document.model.impl.ElasticsearchFieldModel;
import org.hibernate.search.backend.elasticsearch.document.model.impl.ElasticsearchIndexModel;
import org.hibernate.search.backend.elasticsearch.impl.ElasticsearchBackend;
import org.hibernate.search.backend.elasticsearch.index.impl.ElasticsearchIndexManager;
import org.hibernate.search.backend.elasticsearch.logging.impl.Log;
import org.hibernate.search.backend.elasticsearch.search.clause.impl.ElasticsearchClauseFactory;
import org.hibernate.search.backend.elasticsearch.search.clause.impl.ElasticsearchClauseFactoryImpl;
import org.hibernate.search.backend.elasticsearch.search.dsl.impl.QueryTargetContext;
import org.hibernate.search.backend.elasticsearch.search.dsl.impl.SearchContextImpl;
import org.hibernate.search.engine.backend.index.spi.SearchTarget;
import org.hibernate.search.engine.common.spi.SessionContext;
import org.hibernate.search.engine.search.DocumentReference;
import org.hibernate.search.engine.search.SearchQuery;
import org.hibernate.search.engine.search.spi.SearchResultDefinitionContext;
import org.hibernate.search.engine.search.spi.SearchWrappingDefinitionContext;
import org.hibernate.search.util.spi.LoggerFactory;


/**
 * @author Yoann Rodiere
 */
public class ElasticsearchSearchTarget implements SearchTarget {

	private static final Log log = LoggerFactory.make( Log.class );

	private final ElasticsearchBackend backend;

	// Use LinkedHashSet to ensure stable order when generating requests
	private final Set<ElasticsearchIndexManager> indexManagers = new LinkedHashSet<>();

	public ElasticsearchSearchTarget(ElasticsearchBackend backend, ElasticsearchIndexManager indexManager) {
		this.backend = backend;
		this.indexManagers.add( indexManager );
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
	public <R> SearchResultDefinitionContext<R> search(
			SessionContext context, Function<DocumentReference, R> documentReferenceTransformer) {
		// Use LinkedHashSet to ensure stable order when generating requests
		Set<ElasticsearchIndexModel> indexModels = indexManagers.stream().map( ElasticsearchIndexManager::getModel )
				.collect( Collectors.toCollection( LinkedHashSet::new ) );
		QueryTargetContextImpl targetContext = new QueryTargetContextImpl( indexModels );
		return new SearchResultDefinitionContextImpl<>( targetContext, context, documentReferenceTransformer );
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

	private class SearchResultDefinitionContextImpl<R> implements SearchResultDefinitionContext<R> {

		private final QueryTargetContextImpl targetContext;

		private final SessionContext context;

		private final Function<DocumentReference, R> documentReferenceTransformer;

		public SearchResultDefinitionContextImpl(QueryTargetContextImpl targetContext,
				SessionContext context,
				Function<DocumentReference, R> documentReferenceTransformer) {
			this.targetContext = targetContext;
			this.context = context;
			this.documentReferenceTransformer = documentReferenceTransformer;
		}

		@Override
		public <T> SearchWrappingDefinitionContext<SearchQuery<T>> asReferences(Function<R, T> hitTransformer) {
			HitExtractor<T> extractor = new TransformingHitExtractor<>(
					DocumentReferenceHitExtractor.get(),
					documentReferenceTransformer.andThen( hitTransformer )
			);
			ElasticsearchSearchQueryBuilder<T> builder = createSearchQueryBuilder( extractor );
			return new SearchContextImpl<>( targetContext, builder, Function.identity() );
		}

		@Override
		public <T> SearchWrappingDefinitionContext<SearchQuery<T>> asProjections(
				Function<List<?>, T> hitTransformer, String... projections) {
			BitSet projectionFound = new BitSet( projections.length );

			HitExtractor<List<?>> compositeHitExtractor;
			if ( targetContext.getIndexModels().size() == 1 ) {
				ElasticsearchIndexModel indexModel = targetContext.getIndexModels().iterator().next();
				compositeHitExtractor = createProjectionHitExtractor( indexModel, projections, projectionFound );
			}
			else {
				// Use LinkedHashMap to ensure stable order when generating requests
				Map<String, HitExtractor<List<?>>> extractorByIndex = new LinkedHashMap<>();
				for ( ElasticsearchIndexModel indexModel : targetContext.getIndexModels() ) {
					HitExtractor<List<?>> indexHitExtractor = createProjectionHitExtractor(
							indexModel, projections, projectionFound );
					extractorByIndex.put( indexModel.getIndexName(), indexHitExtractor );
				}
				compositeHitExtractor = new IndexSensitiveHitExtractor<>( extractorByIndex );
			}
			if ( projectionFound.cardinality() < projections.length ) {
				projectionFound.flip( 0, projectionFound.length() );
				List<String> unknownProjections = projectionFound.stream()
						.mapToObj( i -> projections[i] )
						.collect( Collectors.toList() );
				throw log.unknownProjectionForSearch( unknownProjections, getIndexNames() );
			}
			HitExtractor<T> hitExtractor = new TransformingHitExtractor<>( compositeHitExtractor, hitTransformer );
			ElasticsearchSearchQueryBuilder<T> builder = createSearchQueryBuilder( hitExtractor );
			return new SearchContextImpl<>( targetContext, builder, Function.identity() );
		}

		private HitExtractor<List<?>> createProjectionHitExtractor(ElasticsearchIndexModel indexModel, String[] projections,
				BitSet projectionFound) {
			List<HitExtractor<?>> extractors = new ArrayList<>( projections.length );
			for ( int i = 0; i < projections.length; ++i ) {
				String absoluteFieldPath = projections[i];
				ElasticsearchFieldModel fieldModel = indexModel.getFieldModel( absoluteFieldPath );
				if ( fieldModel != null ) {
					projectionFound.set( i );
					extractors.add( new SourceHitExtractor( absoluteFieldPath, fieldModel.getFormatter() ) );
				}
				else {
					// Make sure that the result list will have the correct indices and size
					extractors.add( NullHitExtractor.get() );
				}
			}
			return new CompositeHitExtractor( extractors );
		}

		private <T> ElasticsearchSearchQueryBuilder<T> createSearchQueryBuilder(HitExtractor<T> hitExtractor) {
			return new ElasticsearchSearchQueryBuilderImpl<>(
					backend.getQueryOrchestrator(),
					backend.getWorkFactory(),
					indexManagers.stream().map( ElasticsearchIndexManager::getName ).collect( Collectors.toSet() ),
					context,
					hitExtractor
			);
		}

		private List<String> getIndexNames() {
			return targetContext.getIndexModels().stream()
					.map( ElasticsearchIndexModel::getIndexName )
					.collect( Collectors.toList() );
		}
	}

	private static class QueryTargetContextImpl implements QueryTargetContext {

		private final ElasticsearchClauseFactory clauseFactory;

		private final Set<ElasticsearchIndexModel> indexModels;

		public QueryTargetContextImpl(Set<ElasticsearchIndexModel> indexModels) {
			this.clauseFactory = new ElasticsearchClauseFactoryImpl( indexModels );
			this.indexModels = indexModels;
		}

		@Override
		public ElasticsearchClauseFactory getClauseFactory() {
			return clauseFactory;
		}

		public Set<ElasticsearchIndexModel> getIndexModels() {
			return indexModels;
		}

	}

}
