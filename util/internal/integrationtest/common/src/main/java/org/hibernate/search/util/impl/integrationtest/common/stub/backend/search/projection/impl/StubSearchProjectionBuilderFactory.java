/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.projection.impl;

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.hibernate.search.engine.backend.common.DocumentReference;
import org.hibernate.search.engine.search.common.spi.SearchIndexIdentifierContext;
import org.hibernate.search.engine.search.projection.SearchProjection;
import org.hibernate.search.engine.search.projection.spi.CompositeProjectionBuilder;
import org.hibernate.search.engine.search.projection.spi.DocumentReferenceProjectionBuilder;
import org.hibernate.search.engine.search.projection.spi.EntityProjectionBuilder;
import org.hibernate.search.engine.search.projection.spi.EntityReferenceProjectionBuilder;
import org.hibernate.search.engine.search.projection.spi.IdProjectionBuilder;
import org.hibernate.search.engine.search.projection.spi.ProjectionCompositor;
import org.hibernate.search.engine.search.projection.spi.ScoreProjectionBuilder;
import org.hibernate.search.engine.search.projection.spi.SearchProjectionBuilderFactory;
import org.hibernate.search.util.common.AssertionFailure;
import org.hibernate.search.util.common.function.TriFunction;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.common.impl.StubSearchIndexScope;

public class StubSearchProjectionBuilderFactory implements SearchProjectionBuilderFactory {

	private final StubSearchIndexScope scope;

	public StubSearchProjectionBuilderFactory(StubSearchIndexScope scope) {
		this.scope = scope;
	}

	@Override
	public DocumentReferenceProjectionBuilder documentReference() {
		return new DocumentReferenceProjectionBuilder() {
			@Override
			public SearchProjection<DocumentReference> build() {
				return StubDefaultProjection.get();
			}
		};
	}

	@Override
	public <E> EntityProjectionBuilder<E> entity() {
		return StubEntityProjection::get;
	}

	@Override
	public <R> EntityReferenceProjectionBuilder<R> entityReference() {
		return StubReferenceProjection::get;
	}

	@Override
	public <I> IdProjectionBuilder<I> id(Class<I> identifierType) {
		SearchIndexIdentifierContext identifier = scope.identifier();
		return new StubIdProjection.Builder<>(
				identifier.projectionConverter().withConvertedType( identifierType, identifier ) );
	}

	@Override
	public ScoreProjectionBuilder score() {
		return new ScoreProjectionBuilder() {
			@Override
			public SearchProjection<Float> build() {
				return StubDefaultProjection.get();
			}
		};
	}

	@Override
	public <V> CompositeProjectionBuilder<V> composite(Function<List<?>, V> transformer,
			SearchProjection<?>... projections) {
		StubSearchProjection<?>[] typedProjections = new StubSearchProjection<?>[ projections.length ];
		for ( int i = 0; i < projections.length; i++ ) {
			typedProjections[i] = toImplementation( projections[i] );
		}
		return new StubCompositeProjection.Builder<>( ProjectionCompositor.fromList( projections.length, transformer ),
				typedProjections );
	}

	@Override
	public <P1, V> CompositeProjectionBuilder<V> composite(Function<P1, V> transformer,
			SearchProjection<P1> projection) {
		return new StubCompositeProjection.Builder<>( ProjectionCompositor.from( transformer ),
				toImplementation( projection ) );
	}

	@Override
	public <P1, P2, V> CompositeProjectionBuilder<V> composite(BiFunction<P1, P2, V> transformer,
			SearchProjection<P1> projection1, SearchProjection<P2> projection2) {
		return new StubCompositeProjection.Builder<>( ProjectionCompositor.from( transformer ),
				toImplementation( projection1 ), toImplementation( projection2 ) );
	}

	@Override
	public <P1, P2, P3, V> CompositeProjectionBuilder<V> composite(TriFunction<P1, P2, P3, V> transformer,
			SearchProjection<P1> projection1, SearchProjection<P2> projection2, SearchProjection<P3> projection3) {
		return new StubCompositeProjection.Builder<>( ProjectionCompositor.from( transformer ),
				toImplementation( projection1 ), toImplementation( projection2 ), toImplementation( projection3 ) );
	}

	private <U> StubSearchProjection<U> toImplementation(SearchProjection<U> projection) {
		if ( !( projection instanceof StubSearchProjection ) ) {
			throw new AssertionFailure( "Projection " + projection + " must be a StubSearchProjection" );
		}
		return (StubSearchProjection<U>) projection;
	}

}
