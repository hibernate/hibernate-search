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

import org.hibernate.search.backend.lucene.logging.impl.Log;
import org.hibernate.search.backend.lucene.search.impl.LuceneSearchContext;
import org.hibernate.search.backend.lucene.search.impl.LuceneSearchFieldContext;
import org.hibernate.search.backend.lucene.search.impl.LuceneSearchIndexesContext;
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
import org.hibernate.search.util.common.function.TriFunction;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

import org.apache.lucene.document.Document;
import org.apache.lucene.search.Explanation;

public class LuceneSearchProjectionBuilderFactory implements SearchProjectionBuilderFactory {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final LuceneSearchContext searchContext;
	private final LuceneSearchIndexesContext indexes;

	public LuceneSearchProjectionBuilderFactory(LuceneSearchContext searchContext) {
		this.searchContext = searchContext;
		this.indexes = searchContext.indexes();
	}

	@Override
	public DocumentReferenceProjectionBuilder documentReference() {
		return new LuceneDocumentReferenceProjectionBuilder( indexes.indexNames() );
	}

	@Override
	public <T> FieldProjectionBuilder<T> field(String absoluteFieldPath, Class<T> expectedType, ValueConvert convert) {
		LuceneSearchFieldContext<?> field = indexes.field( absoluteFieldPath );
		// Fail early if the nested structure differs in the case of multi-index search.
		field.nestedPathHierarchy();
		return field.createFieldValueProjectionBuilder( searchContext, expectedType, convert );
	}

	@Override
	public <E> EntityProjectionBuilder<E> entity() {
		return new LuceneEntityProjectionBuilder<>( indexes.indexNames() );
	}

	@Override
	public <R> EntityReferenceProjectionBuilder<R> entityReference() {
		return new LuceneEntityReferenceProjectionBuilder<>( indexes.indexNames() );
	}

	@Override
	public ScoreProjectionBuilder score() {
		return new LuceneScoreProjectionBuilder( indexes.indexNames() );
	}

	@Override
	public DistanceToFieldProjectionBuilder distance(String absoluteFieldPath, GeoPoint center) {
		LuceneSearchFieldContext<?> field = indexes.field( absoluteFieldPath );
		// Fail early if the nested structure differs in the case of multi-index search.
		field.nestedPathHierarchy();
		return field.createDistanceProjectionBuilder( searchContext, center );
	}

	@Override
	public <P> CompositeProjectionBuilder<P> composite(Function<List<?>, P> transformer,
			SearchProjection<?>... projections) {
		List<LuceneSearchProjection<?, ?>> typedProjections = new ArrayList<>( projections.length );
		for ( SearchProjection<?> projection : projections ) {
			typedProjections.add( toImplementation( projection ) );
		}

		return new LuceneCompositeProjectionBuilder<>(
				new LuceneCompositeListProjection<>( indexes.indexNames(), transformer, typedProjections )
		);
	}

	@Override
	public <P1, P> CompositeProjectionBuilder<P> composite(Function<P1, P> transformer,
			SearchProjection<P1> projection) {
		return new LuceneCompositeProjectionBuilder<>(
				new LuceneCompositeFunctionProjection<>( indexes.indexNames(), transformer, toImplementation( projection ) )
		);
	}

	@Override
	public <P1, P2, P> CompositeProjectionBuilder<P> composite(BiFunction<P1, P2, P> transformer,
			SearchProjection<P1> projection1, SearchProjection<P2> projection2) {
		return new LuceneCompositeProjectionBuilder<>(
				new LuceneCompositeBiFunctionProjection<>( indexes.indexNames(), transformer, toImplementation( projection1 ),
						toImplementation( projection2 ) )
		);
	}

	@Override
	public <P1, P2, P3, P> CompositeProjectionBuilder<P> composite(TriFunction<P1, P2, P3, P> transformer,
			SearchProjection<P1> projection1, SearchProjection<P2> projection2, SearchProjection<P3> projection3) {
		return new LuceneCompositeProjectionBuilder<>(
				new LuceneCompositeTriFunctionProjection<>( indexes.indexNames(), transformer, toImplementation( projection1 ),
						toImplementation( projection2 ), toImplementation( projection3 ) )
		);
	}

	public <T> LuceneSearchProjection<?, T> toImplementation(SearchProjection<T> projection) {
		if ( !( projection instanceof LuceneSearchProjection ) ) {
			throw log.cannotMixLuceneSearchQueryWithOtherProjections( projection );
		}
		LuceneSearchProjection<?, T> casted = (LuceneSearchProjection<?, T>) projection;
		if ( !indexes.indexNames().equals( casted.getIndexNames() ) ) {
			throw log.projectionDefinedOnDifferentIndexes( projection, casted.getIndexNames(), indexes.indexNames() );
		}
		return casted;
	}

	public SearchProjectionBuilder<Document> document() {
		return new LuceneDocumentProjectionBuilder( indexes.indexNames() );
	}

	public SearchProjectionBuilder<Explanation> explanation() {
		return new LuceneExplanationProjectionBuilder( indexes.indexNames() );
	}
}
