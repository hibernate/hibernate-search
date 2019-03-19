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
import org.hibernate.search.backend.lucene.search.impl.IndexSchemaFieldNodeComponentRetrievalStrategy;
import org.hibernate.search.backend.lucene.search.impl.LuceneSearchScopeModel;
import org.hibernate.search.backend.lucene.types.projection.impl.LuceneFieldProjectionBuilderFactory;
import org.hibernate.search.engine.search.SearchProjection;
import org.hibernate.search.engine.search.predicate.DslConverter;
import org.hibernate.search.engine.search.projection.spi.ProjectionConverter;
import org.hibernate.search.engine.search.projection.spi.CompositeProjectionBuilder;
import org.hibernate.search.engine.search.projection.spi.DistanceToFieldProjectionBuilder;
import org.hibernate.search.engine.search.projection.spi.DocumentReferenceProjectionBuilder;
import org.hibernate.search.engine.search.projection.spi.FieldProjectionBuilder;
import org.hibernate.search.engine.search.projection.spi.ObjectProjectionBuilder;
import org.hibernate.search.engine.search.projection.spi.ReferenceProjectionBuilder;
import org.hibernate.search.engine.search.projection.spi.ScoreProjectionBuilder;
import org.hibernate.search.engine.search.projection.spi.SearchProjectionBuilder;
import org.hibernate.search.engine.search.projection.spi.SearchProjectionBuilderFactory;
import org.hibernate.search.engine.spatial.GeoPoint;
import org.hibernate.search.util.common.reporting.EventContext;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.common.function.TriFunction;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

import org.apache.lucene.document.Document;
import org.apache.lucene.search.Explanation;

public class LuceneSearchProjectionBuilderFactory implements SearchProjectionBuilderFactory {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private static final ProjectionBuilderFactoryRetrievalStrategy PROJECTION_BUILDER_FACTORY_RETRIEVAL_STRATEGY =
			new ProjectionBuilderFactoryRetrievalStrategy();

	private final LuceneSearchScopeModel scopeModel;

	public LuceneSearchProjectionBuilderFactory(LuceneSearchScopeModel scopeModel) {
		this.scopeModel = scopeModel;
	}

	@Override
	public DocumentReferenceProjectionBuilder documentReference() {
		return LuceneDocumentReferenceProjectionBuilder.get();
	}

	@Override
	public <T> FieldProjectionBuilder<T> field(String absoluteFieldPath, Class<T> expectedType, ProjectionConverter projectionConverter) {
		return scopeModel
				.getSchemaNodeComponent( absoluteFieldPath, PROJECTION_BUILDER_FACTORY_RETRIEVAL_STRATEGY )
				.createFieldValueProjectionBuilder( absoluteFieldPath, expectedType, projectionConverter );
	}

	@Override
	public <O> ObjectProjectionBuilder<O> object() {
		return LuceneObjectProjectionBuilder.get();
	}

	@Override
	public <R> ReferenceProjectionBuilder<R> reference() {
		return LuceneReferenceProjectionBuilder.get();
	}

	@Override
	public ScoreProjectionBuilder score() {
		return LuceneScoreProjectionBuilder.get();
	}

	@Override
	public DistanceToFieldProjectionBuilder distance(String absoluteFieldPath, GeoPoint center) {
		return scopeModel
				.getSchemaNodeComponent( absoluteFieldPath, PROJECTION_BUILDER_FACTORY_RETRIEVAL_STRATEGY )
				.createDistanceProjectionBuilder( absoluteFieldPath, center );
	}

	@Override
	public <T> CompositeProjectionBuilder<T> composite(Function<List<?>, T> transformer,
			SearchProjection<?>... projections) {
		List<LuceneSearchProjection<?, ?>> typedProjections = new ArrayList<>( projections.length );
		for ( SearchProjection<?> projection : projections ) {
			typedProjections.add( toImplementation( projection ) );
		}

		return new LuceneCompositeProjectionBuilder<>(
				new LuceneCompositeListProjection<>( transformer, typedProjections )
		);
	}

	@Override
	public <P, T> CompositeProjectionBuilder<T> composite(Function<P, T> transformer,
			SearchProjection<P> projection) {
		return new LuceneCompositeProjectionBuilder<>(
				new LuceneCompositeFunctionProjection<>( transformer, toImplementation( projection ) )
		);
	}

	@Override
	public <P1, P2, T> CompositeProjectionBuilder<T> composite(BiFunction<P1, P2, T> transformer,
			SearchProjection<P1> projection1, SearchProjection<P2> projection2) {
		return new LuceneCompositeProjectionBuilder<>(
				new LuceneCompositeBiFunctionProjection<>( transformer, toImplementation( projection1 ),
						toImplementation( projection2 ) )
		);
	}

	@Override
	public <P1, P2, P3, T> CompositeProjectionBuilder<T> composite(TriFunction<P1, P2, P3, T> transformer,
			SearchProjection<P1> projection1, SearchProjection<P2> projection2, SearchProjection<P3> projection3) {
		return new LuceneCompositeProjectionBuilder<>(
				new LuceneCompositeTriFunctionProjection<>( transformer, toImplementation( projection1 ),
						toImplementation( projection2 ), toImplementation( projection3 ) )
		);
	}

	@SuppressWarnings("unchecked")
	public <T> LuceneSearchProjection<?, T> toImplementation(SearchProjection<T> projection) {
		if ( !( projection instanceof LuceneSearchProjection ) ) {
			throw log.cannotMixLuceneSearchQueryWithOtherProjections( projection );
		}
		return (LuceneSearchProjection<?, T>) projection;
	}

	public SearchProjectionBuilder<Document> document() {
		return LuceneDocumentProjectionBuilder.get();
	}

	public SearchProjectionBuilder<Explanation> explanation() {
		return LuceneExplanationProjectionBuilder.get();
	}

	private static class ProjectionBuilderFactoryRetrievalStrategy
			implements IndexSchemaFieldNodeComponentRetrievalStrategy<LuceneFieldProjectionBuilderFactory> {

		@Override
		public LuceneFieldProjectionBuilderFactory extractComponent(LuceneIndexSchemaFieldNode<?> schemaNode) {
			return schemaNode.getProjectionBuilderFactory();
		}

		@Override
		public boolean areCompatible(LuceneFieldProjectionBuilderFactory component1,
				LuceneFieldProjectionBuilderFactory component2, DslConverter dslConverter) {
			// TODO HSEARCH-3257 handle dslConverter option
			return component1.isDslCompatibleWith( component2 );
		}

		@Override
		public SearchException createCompatibilityException(String absoluteFieldPath,
				LuceneFieldProjectionBuilderFactory component1, LuceneFieldProjectionBuilderFactory component2,
				EventContext context) {
			return log.conflictingFieldTypesForProjection( absoluteFieldPath, component1, component2, context );
		}
	}
}
