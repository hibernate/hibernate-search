/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.dsl.projection.spi;

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.hibernate.search.engine.search.SearchProjection;
import org.hibernate.search.engine.search.dsl.projection.CompositeProjectionOptionsStep;
import org.hibernate.search.engine.search.dsl.projection.DistanceToFieldProjectionOptionsStep;
import org.hibernate.search.engine.search.dsl.projection.DocumentReferenceProjectionOptionsStep;
import org.hibernate.search.engine.search.dsl.projection.EntityProjectionOptionsStep;
import org.hibernate.search.engine.search.dsl.projection.EntityReferenceProjectionOptionsStep;
import org.hibernate.search.engine.search.dsl.projection.FieldProjectionOptionsStep;
import org.hibernate.search.engine.search.dsl.projection.ScoreProjectionOptionsStep;
import org.hibernate.search.engine.search.dsl.projection.SearchProjectionFactoryContext;
import org.hibernate.search.engine.search.dsl.projection.SearchProjectionFactoryContextExtension;
import org.hibernate.search.engine.search.dsl.projection.SearchProjectionFactoryContextExtensionStep;
import org.hibernate.search.engine.search.projection.ProjectionConverter;
import org.hibernate.search.engine.spatial.GeoPoint;
import org.hibernate.search.util.common.function.TriFunction;

public class DelegatingSearchProjectionFactoryContext<R, E> implements SearchProjectionFactoryContext<R, E> {

	private final SearchProjectionFactoryContext<R, E> delegate;

	public DelegatingSearchProjectionFactoryContext(SearchProjectionFactoryContext<R, E> delegate) {
		this.delegate = delegate;
	}

	@Override
	public DocumentReferenceProjectionOptionsStep documentReference() {
		return delegate.documentReference();
	}

	@Override
	public EntityReferenceProjectionOptionsStep<R> entityReference() {
		return delegate.entityReference();
	}

	@Override
	public EntityProjectionOptionsStep<E> entity() {
		return delegate.entity();
	}

	@Override
	public <T> FieldProjectionOptionsStep<T> field(String absoluteFieldPath, Class<T> type, ProjectionConverter projectionConverter) {
		return delegate.field( absoluteFieldPath, type, projectionConverter );
	}

	@Override
	public FieldProjectionOptionsStep<Object> field(String absoluteFieldPath, ProjectionConverter projectionConverter) {
		return delegate.field( absoluteFieldPath, projectionConverter );
	}

	@Override
	public ScoreProjectionOptionsStep score() {
		return delegate.score();
	}

	@Override
	public DistanceToFieldProjectionOptionsStep distance(String absoluteFieldPath, GeoPoint center) {
		return delegate.distance( absoluteFieldPath, center );
	}

	@Override
	public <T> CompositeProjectionOptionsStep<T> composite(Function<List<?>, T> transformer,
			SearchProjection<?>... projections) {
		return delegate.composite( transformer, projections );
	}

	@Override
	public <P, T> CompositeProjectionOptionsStep<T> composite(Function<P, T> transformer, SearchProjection<P> projection) {
		return delegate.composite( transformer, projection );
	}

	@Override
	public <P1, P2, T> CompositeProjectionOptionsStep<T> composite(BiFunction<P1, P2, T> transformer,
			SearchProjection<P1> projection1, SearchProjection<P2> projection2) {
		return delegate.composite( transformer, projection1, projection2 );
	}

	@Override
	public <P1, P2, P3, T> CompositeProjectionOptionsStep<T> composite(TriFunction<P1, P2, P3, T> transformer,
			SearchProjection<P1> projection1, SearchProjection<P2> projection2, SearchProjection<P3> projection3) {
		return delegate.composite( transformer, projection1, projection2, projection3 );
	}

	@Override
	public <T> T extension(SearchProjectionFactoryContextExtension<T, R, E> extension) {
		return delegate.extension( extension );
	}

	@Override
	public <P> SearchProjectionFactoryContextExtensionStep<P, R, E> extension() {
		return delegate.extension();
	}
}
