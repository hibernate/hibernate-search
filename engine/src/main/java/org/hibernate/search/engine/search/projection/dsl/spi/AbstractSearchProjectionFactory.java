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

import org.hibernate.search.engine.common.dsl.spi.DslExtensionState;
import org.hibernate.search.engine.search.common.ValueConvert;
import org.hibernate.search.engine.search.projection.SearchProjection;
import org.hibernate.search.engine.search.projection.dsl.CompositeProjectionComponent1Step;
import org.hibernate.search.engine.search.projection.dsl.CompositeProjectionOptionsStep;
import org.hibernate.search.engine.search.projection.dsl.DistanceToFieldProjectionValueStep;
import org.hibernate.search.engine.search.projection.dsl.DocumentReferenceProjectionOptionsStep;
import org.hibernate.search.engine.search.projection.dsl.EntityProjectionOptionsStep;
import org.hibernate.search.engine.search.projection.dsl.EntityReferenceProjectionOptionsStep;
import org.hibernate.search.engine.search.projection.dsl.ExtendedSearchProjectionFactory;
import org.hibernate.search.engine.search.projection.dsl.FieldProjectionValueStep;
import org.hibernate.search.engine.search.projection.dsl.IdProjectionOptionsStep;
import org.hibernate.search.engine.search.projection.dsl.ScoreProjectionOptionsStep;
import org.hibernate.search.engine.search.projection.dsl.SearchProjectionFactoryExtension;
import org.hibernate.search.engine.search.projection.dsl.SearchProjectionFactoryExtensionIfSupportedStep;
import org.hibernate.search.engine.search.projection.dsl.impl.CompositeProjectionComponent1StepImpl;
import org.hibernate.search.engine.search.projection.dsl.impl.CompositeProjectionFinalStep;
import org.hibernate.search.engine.search.projection.dsl.impl.DistanceToFieldProjectionValueStepImpl;
import org.hibernate.search.engine.search.projection.dsl.impl.DocumentReferenceProjectionOptionsStepImpl;
import org.hibernate.search.engine.search.projection.dsl.impl.EntityProjectionOptionsStepImpl;
import org.hibernate.search.engine.search.projection.dsl.impl.EntityReferenceProjectionOptionsStepImpl;
import org.hibernate.search.engine.search.projection.dsl.impl.FieldProjectionValueStepImpl;
import org.hibernate.search.engine.search.projection.dsl.impl.IdProjectionOptionsStepImpl;
import org.hibernate.search.engine.search.projection.dsl.impl.ScoreProjectionOptionsStepImpl;
import org.hibernate.search.engine.search.projection.dsl.impl.SearchProjectionFactoryExtensionStep;
import org.hibernate.search.engine.search.projection.spi.SearchProjectionIndexScope;
import org.hibernate.search.engine.spatial.GeoPoint;
import org.hibernate.search.util.common.function.TriFunction;
import org.hibernate.search.util.common.impl.Contracts;


public abstract class AbstractSearchProjectionFactory<
				S extends ExtendedSearchProjectionFactory<S, R, E>,
				SC extends SearchProjectionIndexScope<?>,
				R,
				E
		>
		implements ExtendedSearchProjectionFactory<S, R, E> {

	protected final SearchProjectionDslContext<SC> dslContext;

	public AbstractSearchProjectionFactory(SearchProjectionDslContext<SC> dslContext) {
		this.dslContext = dslContext;
	}

	@Override
	public DocumentReferenceProjectionOptionsStep<?> documentReference() {
		return new DocumentReferenceProjectionOptionsStepImpl( dslContext );
	}

	@Override
	public <T> FieldProjectionValueStep<?, T> field(String fieldPath, Class<T> clazz, ValueConvert convert) {
		Contracts.assertNotNull( clazz, "clazz" );
		return new FieldProjectionValueStepImpl<>( dslContext, fieldPath, clazz, convert );
	}

	@Override
	public FieldProjectionValueStep<?, Object> field(String fieldPath, ValueConvert convert) {
		return field( fieldPath, Object.class, convert );
	}

	@Override
	public EntityReferenceProjectionOptionsStep<?, R> entityReference() {
		return new EntityReferenceProjectionOptionsStepImpl<>( dslContext );
	}

	@Override
	public <I> IdProjectionOptionsStep<?, I> id(Class<I> identifierType) {
		Contracts.assertNotNull( identifierType, "identifierType" );
		return new IdProjectionOptionsStepImpl<>( dslContext, identifierType );
	}

	@Override
	public EntityProjectionOptionsStep<?, E> entity() {
		return new EntityProjectionOptionsStepImpl<>( dslContext );
	}

	@Override
	public ScoreProjectionOptionsStep<?> score() {
		return new ScoreProjectionOptionsStepImpl( dslContext );
	}

	@Override
	public DistanceToFieldProjectionValueStep<?, Double> distance(String fieldPath, GeoPoint center) {
		Contracts.assertNotNull( center, "center" );
		return new DistanceToFieldProjectionValueStepImpl( dslContext, fieldPath, center );
	}

	@Override
	public CompositeProjectionComponent1Step composite() {
		return new CompositeProjectionComponent1StepImpl( dslContext );
	}

	@Override
	public <T> CompositeProjectionOptionsStep<?, T> composite(Function<List<?>, T> transformer,
			SearchProjection<?>... projections) {
		Contracts.assertNotNull( transformer, "transformer" );
		Contracts.assertNotNullNorEmpty( projections, "projections" );

		return new CompositeProjectionFinalStep<>( dslContext, transformer, projections );
	}

	@Override
	public <P, T> CompositeProjectionOptionsStep<?, T> composite(Function<P, T> transformer, SearchProjection<P> projection) {
		Contracts.assertNotNull( transformer, "transformer" );
		Contracts.assertNotNull( projection, "projection" );

		return new CompositeProjectionFinalStep<>( dslContext, transformer, projection );
	}

	@Override
	public <P1, P2, T> CompositeProjectionOptionsStep<?, T> composite(BiFunction<P1, P2, T> transformer,
			SearchProjection<P1> projection1, SearchProjection<P2> projection2) {
		Contracts.assertNotNull( transformer, "transformer" );
		Contracts.assertNotNull( projection1, "projection1" );
		Contracts.assertNotNull( projection2, "projection2" );

		return new CompositeProjectionFinalStep<>( dslContext, transformer, projection1, projection2 );
	}

	@Override
	public <P1, P2, P3, T> CompositeProjectionOptionsStep<?, T> composite(TriFunction<P1, P2, P3, T> transformer,
			SearchProjection<P1> projection1, SearchProjection<P2> projection2, SearchProjection<P3> projection3) {
		Contracts.assertNotNull( transformer, "transformer" );
		Contracts.assertNotNull( projection1, "projection1" );
		Contracts.assertNotNull( projection2, "projection2" );
		Contracts.assertNotNull( projection3, "projection3" );

		return new CompositeProjectionFinalStep<>( dslContext, transformer, projection1, projection2, projection3 );
	}

	@Override
	public <T> T extension(SearchProjectionFactoryExtension<T, R, E> extension) {
		return DslExtensionState.returnIfSupported(
				extension, extension.extendOptional( this )
		);
	}

	@Override
	public <T> SearchProjectionFactoryExtensionIfSupportedStep<T, R, E> extension() {
		return new SearchProjectionFactoryExtensionStep<>( this );
	}

	@Override
	public final String toAbsolutePath(String relativeFieldPath) {
		return dslContext.scope().toAbsolutePath( relativeFieldPath );
	}
}
