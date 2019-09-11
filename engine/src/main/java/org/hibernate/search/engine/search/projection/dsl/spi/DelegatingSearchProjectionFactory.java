/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.projection.dsl.spi;

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.hibernate.search.engine.search.projection.SearchProjection;
import org.hibernate.search.engine.search.projection.dsl.CompositeProjectionOptionsStep;
import org.hibernate.search.engine.search.projection.dsl.DistanceToFieldProjectionOptionsStep;
import org.hibernate.search.engine.search.projection.dsl.DocumentReferenceProjectionOptionsStep;
import org.hibernate.search.engine.search.projection.dsl.EntityProjectionOptionsStep;
import org.hibernate.search.engine.search.projection.dsl.EntityReferenceProjectionOptionsStep;
import org.hibernate.search.engine.search.projection.dsl.FieldProjectionOptionsStep;
import org.hibernate.search.engine.search.projection.dsl.ScoreProjectionOptionsStep;
import org.hibernate.search.engine.search.projection.dsl.SearchProjectionFactory;
import org.hibernate.search.engine.search.projection.dsl.SearchProjectionFactoryExtension;
import org.hibernate.search.engine.search.projection.dsl.SearchProjectionFactoryExtensionIfSupportedStep;
import org.hibernate.search.engine.search.common.ValueConvert;
import org.hibernate.search.engine.spatial.GeoPoint;
import org.hibernate.search.util.common.function.TriFunction;

public class DelegatingSearchProjectionFactory<R, E> implements SearchProjectionFactory<R, E> {

	private final SearchProjectionFactory<R, E> delegate;

	public DelegatingSearchProjectionFactory(SearchProjectionFactory<R, E> delegate) {
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
	public <T> FieldProjectionOptionsStep<T> field(String absoluteFieldPath, Class<T> type, ValueConvert convert) {
		return delegate.field( absoluteFieldPath, type, convert );
	}

	@Override
	public FieldProjectionOptionsStep<Object> field(String absoluteFieldPath, ValueConvert convert) {
		return delegate.field( absoluteFieldPath, convert );
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
	public <T> T extension(SearchProjectionFactoryExtension<T, R, E> extension) {
		return delegate.extension( extension );
	}

	@Override
	public <P> SearchProjectionFactoryExtensionIfSupportedStep<P, R, E> extension() {
		return delegate.extension();
	}
}
