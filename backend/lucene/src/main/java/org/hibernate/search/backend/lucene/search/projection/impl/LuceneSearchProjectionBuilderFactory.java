/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.projection.impl;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.hibernate.search.backend.lucene.document.model.impl.LuceneIndexSchemaFieldNode;
import org.hibernate.search.backend.lucene.logging.impl.Log;
import org.hibernate.search.backend.lucene.scope.model.impl.IndexSchemaFieldNodeComponentRetrievalStrategy;
import org.hibernate.search.backend.lucene.scope.model.impl.LuceneScopeModel;
import org.hibernate.search.backend.lucene.scope.model.impl.LuceneScopedIndexFieldComponent;
import org.hibernate.search.backend.lucene.types.projection.impl.LuceneFieldProjectionBuilderFactory;
import org.hibernate.search.engine.search.common.ValueConvert;
import org.hibernate.search.engine.search.projection.SearchProjection;
import org.hibernate.search.engine.search.projection.spi.CompositeProjectionBuilder;
import org.hibernate.search.engine.search.projection.spi.DistanceToFieldProjectionBuilder;
import org.hibernate.search.engine.search.projection.spi.DocumentReferenceProjectionBuilder;
import org.hibernate.search.engine.search.projection.spi.EntityProjectionBuilder;
import org.hibernate.search.engine.search.projection.spi.EntityReferenceProjectionBuilder;
import org.hibernate.search.engine.search.projection.spi.FieldProjectionBuilder;
import org.hibernate.search.engine.search.projection.spi.ScoreProjectionBuilder;
import org.hibernate.search.engine.search.projection.spi.SearchProjectionBuilder;
import org.hibernate.search.engine.search.projection.spi.SearchProjectionBuilderFactory;
import org.hibernate.search.engine.spatial.GeoPoint;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.common.function.TriFunction;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;
import org.hibernate.search.util.common.reporting.EventContext;

import org.apache.lucene.document.Document;
import org.apache.lucene.search.Explanation;

public class LuceneSearchProjectionBuilderFactory implements SearchProjectionBuilderFactory {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private static final ProjectionBuilderFactoryRetrievalStrategy PROJECTION_BUILDER_FACTORY_RETRIEVAL_STRATEGY =
			new ProjectionBuilderFactoryRetrievalStrategy();

	private final LuceneScopeModel scopeModel;

	public LuceneSearchProjectionBuilderFactory(LuceneScopeModel scopeModel) {
		this.scopeModel = scopeModel;
	}

	@Override
	public DocumentReferenceProjectionBuilder documentReference() {
		return new LuceneDocumentReferenceProjectionBuilder( scopeModel.getIndexNames() );
	}

	@Override
	public <T> FieldProjectionBuilder<T> field(String absoluteFieldPath, Class<T> expectedType, ValueConvert convert) {
		LuceneScopedIndexFieldComponent<LuceneFieldProjectionBuilderFactory> fieldComponent =
				scopeModel.getSchemaNodeComponent( absoluteFieldPath, PROJECTION_BUILDER_FACTORY_RETRIEVAL_STRATEGY );
		switch ( convert ) {
			case NO:
				break;
			case YES:
			default:
				fieldComponent.getConverterCompatibilityChecker().failIfNotCompatible();
				break;
		}

		return fieldComponent.getComponent()
				.createFieldValueProjectionBuilder( scopeModel.getIndexNames(), absoluteFieldPath, scopeModel.getNestedDocumentPath( absoluteFieldPath ),
						expectedType, convert );
	}

	@Override
	public <E> EntityProjectionBuilder<E> entity() {
		return new LuceneEntityProjectionBuilder<>( scopeModel.getIndexNames() );
	}

	@Override
	public <R> EntityReferenceProjectionBuilder<R> entityReference() {
		return new LuceneEntityReferenceProjectionBuilder<>( scopeModel.getIndexNames() );
	}

	@Override
	public ScoreProjectionBuilder score() {
		return new LuceneScoreProjectionBuilder( scopeModel.getIndexNames() );
	}

	@Override
	public DistanceToFieldProjectionBuilder distance(String absoluteFieldPath, GeoPoint center) {
		return scopeModel
				.getSchemaNodeComponent( absoluteFieldPath, PROJECTION_BUILDER_FACTORY_RETRIEVAL_STRATEGY )
				.getComponent().createDistanceProjectionBuilder( scopeModel.getIndexNames(), absoluteFieldPath, scopeModel.getNestedDocumentPath( absoluteFieldPath ), center );
	}

	@Override
	public <P> CompositeProjectionBuilder<P> composite(Function<List<?>, P> transformer,
			SearchProjection<?>... projections) {
		List<LuceneSearchProjection<?, ?>> typedProjections = new ArrayList<>( projections.length );
		for ( SearchProjection<?> projection : projections ) {
			typedProjections.add( toImplementation( projection ) );
		}

		return new LuceneCompositeProjectionBuilder<>(
				new LuceneCompositeListProjection<>( scopeModel.getIndexNames(), transformer, typedProjections )
		);
	}

	@Override
	public <P1, P> CompositeProjectionBuilder<P> composite(Function<P1, P> transformer,
			SearchProjection<P1> projection) {
		return new LuceneCompositeProjectionBuilder<>(
				new LuceneCompositeFunctionProjection<>( scopeModel.getIndexNames(), transformer, toImplementation( projection ) )
		);
	}

	@Override
	public <P1, P2, P> CompositeProjectionBuilder<P> composite(BiFunction<P1, P2, P> transformer,
			SearchProjection<P1> projection1, SearchProjection<P2> projection2) {
		return new LuceneCompositeProjectionBuilder<>(
				new LuceneCompositeBiFunctionProjection<>( scopeModel.getIndexNames(), transformer, toImplementation( projection1 ),
						toImplementation( projection2 ) )
		);
	}

	@Override
	public <P1, P2, P3, P> CompositeProjectionBuilder<P> composite(TriFunction<P1, P2, P3, P> transformer,
			SearchProjection<P1> projection1, SearchProjection<P2> projection2, SearchProjection<P3> projection3) {
		return new LuceneCompositeProjectionBuilder<>(
				new LuceneCompositeTriFunctionProjection<>( scopeModel.getIndexNames(), transformer, toImplementation( projection1 ),
						toImplementation( projection2 ), toImplementation( projection3 ) )
		);
	}

	@SuppressWarnings("unchecked")
	public <T> LuceneSearchProjection<?, T> toImplementation(SearchProjection<T> projection) {
		if ( !( projection instanceof LuceneSearchProjection ) ) {
			throw log.cannotMixLuceneSearchQueryWithOtherProjections( projection );
		}
		LuceneSearchProjection<?, T> casted = (LuceneSearchProjection<?, T>) projection;
		if ( !scopeModel.getIndexNames().equals( casted.getIndexNames() ) ) {
			throw log.projectionDefinedOnDifferentIndexes( projection, casted.getIndexNames(), scopeModel.getIndexNames() );
		}
		return casted;
	}

	public SearchProjectionBuilder<Document> document() {
		return new LuceneDocumentProjectionBuilder( scopeModel.getIndexNames() );
	}

	public SearchProjectionBuilder<Explanation> explanation() {
		return new LuceneExplanationProjectionBuilder( scopeModel.getIndexNames() );
	}

	private static class ProjectionBuilderFactoryRetrievalStrategy
			implements IndexSchemaFieldNodeComponentRetrievalStrategy<LuceneFieldProjectionBuilderFactory> {

		@Override
		public LuceneFieldProjectionBuilderFactory extractComponent(LuceneIndexSchemaFieldNode<?> schemaNode) {
			return schemaNode.getType().getProjectionBuilderFactory();
		}

		@Override
		public boolean hasCompatibleCodec(LuceneFieldProjectionBuilderFactory component1, LuceneFieldProjectionBuilderFactory component2) {
			return component1.hasCompatibleCodec( component2 );
		}

		@Override
		public boolean hasCompatibleConverter(LuceneFieldProjectionBuilderFactory component1, LuceneFieldProjectionBuilderFactory component2) {
			return component1.hasCompatibleConverter( component2 );
		}

		@Override
		public boolean hasCompatibleAnalyzer(LuceneFieldProjectionBuilderFactory component1, LuceneFieldProjectionBuilderFactory component2) {
			// analyzers are not involved in a projection clause
			return true;
		}

		@Override
		public SearchException createCompatibilityException(String absoluteFieldPath,
				LuceneFieldProjectionBuilderFactory component1, LuceneFieldProjectionBuilderFactory component2,
				EventContext context) {
			return log.conflictingFieldTypesForProjection( absoluteFieldPath, component1, component2, context );
		}
	}
}
