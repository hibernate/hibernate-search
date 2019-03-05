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
import org.hibernate.search.engine.search.dsl.projection.CompositeProjectionContext;
import org.hibernate.search.engine.search.dsl.projection.DistanceToFieldProjectionContext;
import org.hibernate.search.engine.search.dsl.projection.DocumentReferenceProjectionContext;
import org.hibernate.search.engine.search.dsl.projection.FieldProjectionContext;
import org.hibernate.search.engine.search.dsl.projection.ObjectProjectionContext;
import org.hibernate.search.engine.search.dsl.projection.ReferenceProjectionContext;
import org.hibernate.search.engine.search.dsl.projection.ScoreProjectionContext;
import org.hibernate.search.engine.search.dsl.projection.SearchProjectionFactoryContext;
import org.hibernate.search.engine.search.dsl.projection.SearchProjectionFactoryContextExtension;
import org.hibernate.search.engine.search.dsl.projection.SearchProjectionFactoryExtensionContext;
import org.hibernate.search.engine.spatial.GeoPoint;
import org.hibernate.search.util.common.function.TriFunction;

public class DelegatingSearchProjectionFactoryContext<R, O> implements SearchProjectionFactoryContext<R, O> {

	private final SearchProjectionFactoryContext<R, O> delegate;

	public DelegatingSearchProjectionFactoryContext(SearchProjectionFactoryContext<R, O> delegate) {
		this.delegate = delegate;
	}

	@Override
	public DocumentReferenceProjectionContext documentReference() {
		return delegate.documentReference();
	}

	@Override
	public ReferenceProjectionContext<R> reference() {
		return delegate.reference();
	}

	@Override
	public ObjectProjectionContext<O> object() {
		return delegate.object();
	}

	@Override
	public <T> FieldProjectionContext<T> field(String absoluteFieldPath, Class<T> type) {
		return delegate.field( absoluteFieldPath, type );
	}

	@Override
	public FieldProjectionContext<Object> field(String absoluteFieldPath) {
		return delegate.field( absoluteFieldPath );
	}

	@Override
	public <T> FieldProjectionContext<T> rawField(String absoluteFieldPath, Class<T> type) {
		return delegate.rawField( absoluteFieldPath, type );
	}

	@Override
	public FieldProjectionContext<Object> rawField(String absoluteFieldPath) {
		return delegate.rawField( absoluteFieldPath );
	}

	@Override
	public ScoreProjectionContext score() {
		return delegate.score();
	}

	@Override
	public DistanceToFieldProjectionContext distance(String absoluteFieldPath, GeoPoint center) {
		return delegate.distance( absoluteFieldPath, center );
	}

	@Override
	public <T> CompositeProjectionContext<T> composite(Function<List<?>, T> transformer,
			SearchProjection<?>... projections) {
		return delegate.composite( transformer, projections );
	}

	@Override
	public <P, T> CompositeProjectionContext<T> composite(Function<P, T> transformer, SearchProjection<P> projection) {
		return delegate.composite( transformer, projection );
	}

	@Override
	public <P1, P2, T> CompositeProjectionContext<T> composite(BiFunction<P1, P2, T> transformer,
			SearchProjection<P1> projection1, SearchProjection<P2> projection2) {
		return delegate.composite( transformer, projection1, projection2 );
	}

	@Override
	public <P1, P2, P3, T> CompositeProjectionContext<T> composite(TriFunction<P1, P2, P3, T> transformer,
			SearchProjection<P1> projection1, SearchProjection<P2> projection2, SearchProjection<P3> projection3) {
		return delegate.composite( transformer, projection1, projection2, projection3 );
	}

	@Override
	public <T> T extension(SearchProjectionFactoryContextExtension<T, R, O> extension) {
		return delegate.extension( extension );
	}

	@Override
	public <P> SearchProjectionFactoryExtensionContext<P, R, O> extension() {
		return delegate.extension();
	}
}
