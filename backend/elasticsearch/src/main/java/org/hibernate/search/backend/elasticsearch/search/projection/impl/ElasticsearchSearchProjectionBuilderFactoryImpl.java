/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.projection.impl;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.hibernate.search.backend.elasticsearch.document.model.impl.ElasticsearchIndexSchemaFieldNode;
import org.hibernate.search.backend.elasticsearch.logging.impl.Log;
import org.hibernate.search.backend.elasticsearch.search.impl.ElasticsearchSearchTargetModel;
import org.hibernate.search.backend.elasticsearch.search.impl.IndexSchemaFieldNodeComponentRetrievalStrategy;
import org.hibernate.search.backend.elasticsearch.types.projection.impl.ElasticsearchFieldProjectionBuilderFactory;
import org.hibernate.search.engine.search.SearchProjection;
import org.hibernate.search.engine.search.projection.spi.CompositeSearchProjectionBuilder;
import org.hibernate.search.engine.search.projection.spi.DistanceToFieldSearchProjectionBuilder;
import org.hibernate.search.engine.search.projection.spi.DocumentReferenceSearchProjectionBuilder;
import org.hibernate.search.engine.search.projection.spi.FieldSearchProjectionBuilder;
import org.hibernate.search.engine.search.projection.spi.ObjectSearchProjectionBuilder;
import org.hibernate.search.engine.search.projection.spi.ReferenceSearchProjectionBuilder;
import org.hibernate.search.engine.search.projection.spi.ScoreSearchProjectionBuilder;
import org.hibernate.search.engine.search.projection.spi.SearchProjectionBuilderFactory;
import org.hibernate.search.engine.spatial.GeoPoint;
import org.hibernate.search.util.EventContext;
import org.hibernate.search.util.SearchException;
import org.hibernate.search.util.function.TriFunction;
import org.hibernate.search.util.impl.common.LoggerFactory;

public class ElasticsearchSearchProjectionBuilderFactoryImpl implements SearchProjectionBuilderFactory {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private static final ProjectionBuilderFactoryRetrievalStrategy PROJECTION_BUILDER_FACTORY_RETRIEVAL_STRATEGY =
			new ProjectionBuilderFactoryRetrievalStrategy();

	private final SearchProjectionBackendContext searchProjectionBackendContext;

	private final ElasticsearchSearchTargetModel searchTargetModel;

	public ElasticsearchSearchProjectionBuilderFactoryImpl(SearchProjectionBackendContext searchProjectionBackendContext,
			ElasticsearchSearchTargetModel searchTargetModel) {
		this.searchProjectionBackendContext = searchProjectionBackendContext;
		this.searchTargetModel = searchTargetModel;
	}

	@Override
	public DocumentReferenceSearchProjectionBuilder documentReference() {
		return searchProjectionBackendContext.getDocumentReferenceProjectionBuilder();
	}

	@Override
	public <T> FieldSearchProjectionBuilder<T> field(String absoluteFieldPath, Class<T> expectedType) {
		return searchTargetModel
				.getSchemaNodeComponent( absoluteFieldPath, PROJECTION_BUILDER_FACTORY_RETRIEVAL_STRATEGY )
				.createFieldValueProjectionBuilder( absoluteFieldPath, expectedType );
	}

	@Override
	public <O> ObjectSearchProjectionBuilder<O> object() {
		return searchProjectionBackendContext.getObjectProjectionBuilder();
	}

	@Override
	public <R> ReferenceSearchProjectionBuilder<R> reference() {
		return searchProjectionBackendContext.getReferenceProjectionBuilder();
	}

	@Override
	public ScoreSearchProjectionBuilder score() {
		return searchProjectionBackendContext.getScoreProjectionBuilder();
	}

	@Override
	public DistanceToFieldSearchProjectionBuilder distance(String absoluteFieldPath, GeoPoint center) {
		return searchTargetModel
				.getSchemaNodeComponent( absoluteFieldPath, PROJECTION_BUILDER_FACTORY_RETRIEVAL_STRATEGY )
				.createDistanceProjectionBuilder( absoluteFieldPath, center );
	}

	@Override
	public <T> CompositeSearchProjectionBuilder<T> composite(Function<List<?>, T> transformer,
			SearchProjection<?>... projections) {
		List<ElasticsearchSearchProjection<?>> typedProjections = new ArrayList<>( projections.length );
		for ( SearchProjection<?> projection : projections ) {
			typedProjections.add( toImplementation( projection ) );
		}

		return new CompositeSearchProjectionBuilderImpl<>(
				new CompositeListSearchProjectionImpl<>( transformer, typedProjections )
		);
	}

	@Override
	public <P, T> CompositeSearchProjectionBuilder<T> composite(Function<P, T> transformer,
			SearchProjection<P> projection) {
		return new CompositeSearchProjectionBuilderImpl<>(
				new CompositeFunctionSearchProjectionImpl<>( transformer, toImplementation( projection ) )
		);
	}

	@Override
	public <P1, P2, T> CompositeSearchProjectionBuilder<T> composite(BiFunction<P1, P2, T> transformer,
			SearchProjection<P1> projection1, SearchProjection<P2> projection2) {
		return new CompositeSearchProjectionBuilderImpl<>(
				new CompositeBiFunctionSearchProjectionImpl<>( transformer, toImplementation( projection1 ),
						toImplementation( projection2 ) )
		);
	}

	@Override
	public <P1, P2, P3, T> CompositeSearchProjectionBuilder<T> composite(TriFunction<P1, P2, P3, T> transformer,
			SearchProjection<P1> projection1, SearchProjection<P2> projection2, SearchProjection<P3> projection3) {
		return new CompositeSearchProjectionBuilderImpl<>(
				new CompositeTriFunctionSearchProjectionImpl<>( transformer, toImplementation( projection1 ),
						toImplementation( projection2 ), toImplementation( projection3 ) )
		);
	}

	public <T> ElasticsearchSearchProjection<T> toImplementation(SearchProjection<T> projection) {
		if ( !( projection instanceof ElasticsearchSearchProjection ) ) {
			throw log.cannotMixElasticsearchSearchQueryWithOtherProjections( projection );
		}
		return (ElasticsearchSearchProjection<T>) projection;
	}

	private static class ProjectionBuilderFactoryRetrievalStrategy
			implements IndexSchemaFieldNodeComponentRetrievalStrategy<ElasticsearchFieldProjectionBuilderFactory> {

		@Override
		public ElasticsearchFieldProjectionBuilderFactory extractComponent(ElasticsearchIndexSchemaFieldNode<?> schemaNode) {
			return schemaNode.getProjectionBuilderFactory();
		}

		@Override
		public boolean areCompatible(ElasticsearchFieldProjectionBuilderFactory component1,
				ElasticsearchFieldProjectionBuilderFactory component2) {
			return component1.isDslCompatibleWith( component2 );
		}

		@Override
		public SearchException createCompatibilityException(String absoluteFieldPath,
				ElasticsearchFieldProjectionBuilderFactory component1,
				ElasticsearchFieldProjectionBuilderFactory component2,
				EventContext context) {
			return log.conflictingFieldTypesForProjection( absoluteFieldPath, component1, component2, context );
		}
	}
}
