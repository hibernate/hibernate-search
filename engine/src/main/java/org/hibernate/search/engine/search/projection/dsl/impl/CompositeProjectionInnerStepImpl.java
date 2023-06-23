/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.projection.dsl.impl;

import org.hibernate.search.engine.search.projection.SearchProjection;
import org.hibernate.search.engine.search.projection.definition.impl.DefaultProjectionDefinitionContext;
import org.hibernate.search.engine.search.projection.dsl.CompositeProjectionFrom1AsStep;
import org.hibernate.search.engine.search.projection.dsl.CompositeProjectionFrom2AsStep;
import org.hibernate.search.engine.search.projection.dsl.CompositeProjectionFrom3AsStep;
import org.hibernate.search.engine.search.projection.dsl.CompositeProjectionFromAsStep;
import org.hibernate.search.engine.search.projection.dsl.CompositeProjectionInnerStep;
import org.hibernate.search.engine.search.projection.dsl.CompositeProjectionValueStep;
import org.hibernate.search.engine.search.projection.dsl.ProjectionFinalStep;
import org.hibernate.search.engine.search.projection.dsl.SearchProjectionFactory;
import org.hibernate.search.engine.search.projection.dsl.spi.SearchProjectionDslContext;
import org.hibernate.search.engine.search.projection.spi.CompositeProjectionBuilder;
import org.hibernate.search.engine.search.projection.spi.ProjectionTypeKeys;
import org.hibernate.search.util.common.impl.Contracts;

public class CompositeProjectionInnerStepImpl implements CompositeProjectionInnerStep {

	private final SearchProjectionDslContext<?> dslContext;
	private final SearchProjectionFactory<?, ?> projectionFactory;
	private final CompositeProjectionBuilder builder;
	private final String objectFieldPath;

	public CompositeProjectionInnerStepImpl(SearchProjectionDslContext<?> dslContext,
			SearchProjectionFactory<?, ?> projectionFactory) {
		this.dslContext = dslContext;
		this.projectionFactory = projectionFactory;
		this.builder = dslContext.scope().projectionBuilders().composite();
		this.objectFieldPath = null;
	}

	public CompositeProjectionInnerStepImpl(SearchProjectionDslContext<?> dslContext,
			SearchProjectionFactory<?, ?> projectionFactory, String objectFieldPath) {
		this.dslContext = dslContext;
		this.projectionFactory = projectionFactory;
		this.builder = dslContext.scope().fieldQueryElement( objectFieldPath, ProjectionTypeKeys.OBJECT );
		this.objectFieldPath = objectFieldPath;
	}

	@Override
	public <V> CompositeProjectionValueStep<?, V> as(Class<V> objectClass) {
		SearchProjectionFactory<?, ?> projectionFactoryWithCorrectRoot = objectFieldPath == null
				? projectionFactory
				: projectionFactory.withRoot( objectFieldPath );
		return dslContext.scope().projectionRegistry().composite( objectClass )
				.apply( projectionFactoryWithCorrectRoot, this,
						// TODO HSEARCH-4806/HSEARCH-4807 pass an actual context with parameters
						DefaultProjectionDefinitionContext.INSTANCE );
	}

	@Override
	public <V1> CompositeProjectionFrom1AsStep<V1> from(SearchProjection<V1> projection) {
		return new CompositeProjectionFrom1AsStepImpl<>( builder, projection );
	}

	@Override
	public <V1, V2> CompositeProjectionFrom2AsStep<V1, V2> from(SearchProjection<V1> projection1,
			SearchProjection<V2> projection2) {
		return new CompositeProjectionFrom2AsStepImpl<>( builder, projection1, projection2 );
	}

	@Override
	public <V1, V2, V3> CompositeProjectionFrom3AsStep<V1, V2, V3> from(SearchProjection<V1> projection1,
			SearchProjection<V2> projection2, SearchProjection<V3> projection3) {
		return new CompositeProjectionFrom3AsStepImpl<>( builder, projection1, projection2, projection3 );
	}

	@Override
	public CompositeProjectionFromAsStep from(SearchProjection<?>... projections) {
		Contracts.assertNotNullNorEmpty( projections, "projections" );
		return new CompositeProjectionFromAnyNumberAsStep( builder, projections );
	}

	@Override
	public final CompositeProjectionFromAsStep from(ProjectionFinalStep<?>... dslFinalSteps) {
		Contracts.assertNotNullNorEmpty( dslFinalSteps, "dslFinalSteps" );
		SearchProjection<?>[] projections = new SearchProjection<?>[dslFinalSteps.length];
		for ( int i = 0; i < dslFinalSteps.length; i++ ) {
			projections[i] = dslFinalSteps[i].toProjection();
		}
		return from( projections );
	}

}
