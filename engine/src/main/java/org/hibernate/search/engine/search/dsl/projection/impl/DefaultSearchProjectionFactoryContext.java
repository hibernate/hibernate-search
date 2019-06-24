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
import org.hibernate.search.engine.search.projection.spi.SearchProjectionBuilderFactory;
import org.hibernate.search.engine.spatial.GeoPoint;
import org.hibernate.search.util.common.function.TriFunction;
import org.hibernate.search.util.common.impl.Contracts;


public class DefaultSearchProjectionFactoryContext<R, E> implements SearchProjectionFactoryContext<R, E> {

	private final SearchProjectionBuilderFactory factory;

	public DefaultSearchProjectionFactoryContext(SearchProjectionBuilderFactory factory) {
		this.factory = factory;
	}

	@Override
	public DocumentReferenceProjectionOptionsStep documentReference() {
		return new DocumentReferenceProjectionOptionsStepImpl( factory );
	}

	@Override
	public <T> FieldProjectionOptionsStep<T> field(String absoluteFieldPath, Class<T> clazz, ProjectionConverter projectionConverter) {
		Contracts.assertNotNull( clazz, "clazz" );

		return new FieldProjectionOptionsStepImpl<>( factory, absoluteFieldPath, clazz, projectionConverter );
	}

	@Override
	public FieldProjectionOptionsStep<Object> field(String absoluteFieldPath, ProjectionConverter projectionConverter) {
		return field( absoluteFieldPath, Object.class, projectionConverter );
	}

	@Override
	public EntityReferenceProjectionOptionsStep<R> entityReference() {
		return new EntityReferenceProjectionOptionsStepImpl<>( factory );
	}

	@Override
	public EntityProjectionOptionsStep<E> entity() {
		return new EntityProjectionOptionsStepImpl<>( factory );
	}

	@Override
	public ScoreProjectionOptionsStep score() {
		return new ScoreProjectionOptionsStepImpl( factory );
	}

	@Override
	public DistanceToFieldProjectionOptionsStep distance(String absoluteFieldPath, GeoPoint center) {
		Contracts.assertNotNull( center, "center" );

		return new DistanceToFieldProjectionOptionsStepImpl( factory, absoluteFieldPath, center );
	}

	@Override
	public <T> CompositeProjectionOptionsStep<T> composite(Function<List<?>, T> transformer,
			SearchProjection<?>... projections) {
		Contracts.assertNotNull( transformer, "transformer" );
		Contracts.assertNotNullNorEmpty( projections, "projections" );

		return new CompositeProjectionOptionsStepImpl<>( factory, transformer, projections );
	}

	@Override
	public <P, T> CompositeProjectionOptionsStep<T> composite(Function<P, T> transformer, SearchProjection<P> projection) {
		Contracts.assertNotNull( transformer, "transformer" );
		Contracts.assertNotNull( projection, "projection" );

		return new CompositeProjectionOptionsStepImpl<>( factory, transformer, projection );
	}

	@Override
	public <P1, P2, T> CompositeProjectionOptionsStep<T> composite(BiFunction<P1, P2, T> transformer,
			SearchProjection<P1> projection1, SearchProjection<P2> projection2) {
		Contracts.assertNotNull( transformer, "transformer" );
		Contracts.assertNotNull( projection1, "projection1" );
		Contracts.assertNotNull( projection2, "projection2" );

		return new CompositeProjectionOptionsStepImpl<>( factory, transformer, projection1, projection2 );
	}

	@Override
	public <P1, P2, P3, T> CompositeProjectionOptionsStep<T> composite(TriFunction<P1, P2, P3, T> transformer,
			SearchProjection<P1> projection1, SearchProjection<P2> projection2, SearchProjection<P3> projection3) {
		Contracts.assertNotNull( transformer, "transformer" );
		Contracts.assertNotNull( projection1, "projection1" );
		Contracts.assertNotNull( projection2, "projection2" );
		Contracts.assertNotNull( projection3, "projection3" );

		return new CompositeProjectionOptionsStepImpl<>( factory, transformer, projection1, projection2, projection3 );
	}

	@Override
	public <T> T extension(SearchProjectionFactoryContextExtension<T, R, E> extension) {
		return DslExtensionState.returnIfSupported(
				extension, extension.extendOptional( this, factory )
		);
	}

	@Override
	public <T> SearchProjectionFactoryContextExtensionStep<T, R, E> extension() {
		return new SearchProjectionFactoryContextExtensionStepImpl<>( this, factory );
	}
}
