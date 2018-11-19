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
import org.hibernate.search.backend.lucene.search.impl.LuceneSearchTargetModel;
import org.hibernate.search.backend.lucene.types.projection.impl.LuceneFieldProjectionBuilderFactory;
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

public class LuceneSearchProjectionBuilderFactoryImpl implements SearchProjectionBuilderFactory {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private static final ProjectionBuilderFactoryRetrievalStrategy PROJECTION_BUILDER_FACTORY_RETRIEVAL_STRATEGY =
			new ProjectionBuilderFactoryRetrievalStrategy();

	private final LuceneSearchTargetModel searchTargetModel;

	public LuceneSearchProjectionBuilderFactoryImpl(LuceneSearchTargetModel searchTargetModel) {
		this.searchTargetModel = searchTargetModel;
	}

	@Override
	public DocumentReferenceSearchProjectionBuilder documentReference() {
		return DocumentReferenceSearchProjectionBuilderImpl.get();
	}

	@Override
	public <T> FieldSearchProjectionBuilder<T> field(String absoluteFieldPath, Class<T> expectedType) {
		return searchTargetModel
				.getSchemaNodeComponent( absoluteFieldPath, PROJECTION_BUILDER_FACTORY_RETRIEVAL_STRATEGY )
				.createFieldValueProjectionBuilder( absoluteFieldPath, expectedType );
	}

	@Override
	public <O> ObjectSearchProjectionBuilder<O> object() {
		return ObjectSearchProjectionBuilderImpl.get();
	}

	@Override
	public <R> ReferenceSearchProjectionBuilder<R> reference() {
		return ReferenceSearchProjectionBuilderImpl.get();
	}

	@Override
	public ScoreSearchProjectionBuilder score() {
		return ScoreSearchProjectionBuilderImpl.get();
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
		List<LuceneSearchProjection<?>> typedProjections = new ArrayList<>( projections.length );
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

	public <T> LuceneSearchProjection<T> toImplementation(SearchProjection<T> projection) {
		if ( !( projection instanceof LuceneSearchProjection ) ) {
			throw log.cannotMixLuceneSearchQueryWithOtherProjections( projection );
		}
		return (LuceneSearchProjection<T>) projection;
	}

	private static class ProjectionBuilderFactoryRetrievalStrategy
			implements IndexSchemaFieldNodeComponentRetrievalStrategy<LuceneFieldProjectionBuilderFactory> {

		@Override
		public LuceneFieldProjectionBuilderFactory extractComponent(LuceneIndexSchemaFieldNode<?> schemaNode) {
			return schemaNode.getProjectionBuilderFactory();
		}

		@Override
		public boolean areCompatible(LuceneFieldProjectionBuilderFactory component1,
				LuceneFieldProjectionBuilderFactory component2) {
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
