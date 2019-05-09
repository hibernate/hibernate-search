/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.dsl.projection.impl;

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.hibernate.search.engine.common.dsl.spi.DslExtensionState;
import org.hibernate.search.engine.search.SearchProjection;
import org.hibernate.search.engine.search.dsl.projection.CompositeProjectionContext;
import org.hibernate.search.engine.search.dsl.projection.DistanceToFieldProjectionContext;
import org.hibernate.search.engine.search.dsl.projection.DocumentReferenceProjectionContext;
import org.hibernate.search.engine.search.dsl.projection.FieldProjectionContext;
import org.hibernate.search.engine.search.dsl.projection.EntityProjectionContext;
import org.hibernate.search.engine.search.dsl.projection.ReferenceProjectionContext;
import org.hibernate.search.engine.search.dsl.projection.ScoreProjectionContext;
import org.hibernate.search.engine.search.dsl.projection.SearchProjectionFactoryContext;
import org.hibernate.search.engine.search.dsl.projection.SearchProjectionFactoryContextExtension;
import org.hibernate.search.engine.search.dsl.projection.SearchProjectionFactoryExtensionContext;
import org.hibernate.search.engine.search.projection.ProjectionConverter;
import org.hibernate.search.engine.search.projection.spi.SearchProjectionBuilderFactory;
import org.hibernate.search.engine.spatial.GeoPoint;
import org.hibernate.search.util.common.function.TriFunction;
import org.hibernate.search.util.common.impl.Contracts;


public class DefaultSearchProjectionFactoryContext<R, O> implements SearchProjectionFactoryContext<R, O> {

	private final SearchProjectionBuilderFactory factory;

	public DefaultSearchProjectionFactoryContext(SearchProjectionBuilderFactory factory) {
		this.factory = factory;
	}

	@Override
	public DocumentReferenceProjectionContext documentReference() {
		return new DocumentReferenceProjectionContextImpl( factory );
	}

	@Override
	public <T> FieldProjectionContext<T> field(String absoluteFieldPath, Class<T> clazz, ProjectionConverter projectionConverter) {
		Contracts.assertNotNull( clazz, "clazz" );

		return new FieldProjectionContextImpl<>( factory, absoluteFieldPath, clazz, projectionConverter );
	}

	@Override
	public FieldProjectionContext<Object> field(String absoluteFieldPath, ProjectionConverter projectionConverter) {
		return field( absoluteFieldPath, Object.class, projectionConverter );
	}

	@Override
	public ReferenceProjectionContext<R> reference() {
		return new ReferenceProjectionContextImpl<>( factory );
	}

	@Override
	public EntityProjectionContext<O> entity() {
		return new EntityProjectionContextImpl<>( factory );
	}

	@Override
	public ScoreProjectionContext score() {
		return new ScoreProjectionContextImpl( factory );
	}

	@Override
	public DistanceToFieldProjectionContext distance(String absoluteFieldPath, GeoPoint center) {
		Contracts.assertNotNull( center, "center" );

		return new DistanceToFieldProjectionContextImpl( factory, absoluteFieldPath, center );
	}

	@Override
	public <T> CompositeProjectionContext<T> composite(Function<List<?>, T> transformer,
			SearchProjection<?>... projections) {
		Contracts.assertNotNull( transformer, "transformer" );
		Contracts.assertNotNullNorEmpty( projections, "projections" );

		return new CompositeProjectionContextImpl<>( factory, transformer, projections );
	}

	@Override
	public <P, T> CompositeProjectionContext<T> composite(Function<P, T> transformer, SearchProjection<P> projection) {
		Contracts.assertNotNull( transformer, "transformer" );
		Contracts.assertNotNull( projection, "projection" );

		return new CompositeProjectionContextImpl<>( factory, transformer, projection );
	}

	@Override
	public <P1, P2, T> CompositeProjectionContext<T> composite(BiFunction<P1, P2, T> transformer,
			SearchProjection<P1> projection1, SearchProjection<P2> projection2) {
		Contracts.assertNotNull( transformer, "transformer" );
		Contracts.assertNotNull( projection1, "projection1" );
		Contracts.assertNotNull( projection2, "projection2" );

		return new CompositeProjectionContextImpl<>( factory, transformer, projection1, projection2 );
	}

	@Override
	public <P1, P2, P3, T> CompositeProjectionContext<T> composite(TriFunction<P1, P2, P3, T> transformer,
			SearchProjection<P1> projection1, SearchProjection<P2> projection2, SearchProjection<P3> projection3) {
		Contracts.assertNotNull( transformer, "transformer" );
		Contracts.assertNotNull( projection1, "projection1" );
		Contracts.assertNotNull( projection2, "projection2" );
		Contracts.assertNotNull( projection3, "projection3" );

		return new CompositeProjectionContextImpl<>( factory, transformer, projection1, projection2, projection3 );
	}

	@Override
	public <T> T extension(SearchProjectionFactoryContextExtension<T, R, O> extension) {
		return DslExtensionState.returnIfSupported(
				extension, extension.extendOptional( this, factory )
		);
	}

	@Override
	public <T> SearchProjectionFactoryExtensionContext<T, R, O> extension() {
		return new SearchProjectionFactoryExtensionContextImpl<>( this, factory );
	}
}
