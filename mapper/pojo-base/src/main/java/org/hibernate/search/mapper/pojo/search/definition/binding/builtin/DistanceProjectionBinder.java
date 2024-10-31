/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.search.definition.binding.builtin;

import java.util.Optional;
import java.util.function.Function;

import org.hibernate.search.engine.environment.bean.BeanHolder;
import org.hibernate.search.engine.search.projection.ProjectionCollector;
import org.hibernate.search.engine.search.projection.definition.spi.ConstantProjectionDefinition;
import org.hibernate.search.engine.search.projection.definition.spi.DistanceProjectionDefinition;
import org.hibernate.search.engine.search.projection.dsl.DistanceToFieldProjectionOptionsStep;
import org.hibernate.search.engine.search.projection.dsl.SearchProjectionFactory;
import org.hibernate.search.engine.spatial.DistanceUnit;
import org.hibernate.search.engine.spatial.GeoPoint;
import org.hibernate.search.mapper.pojo.logging.impl.ProjectionLog;
import org.hibernate.search.mapper.pojo.model.PojoModelValue;
import org.hibernate.search.mapper.pojo.search.definition.binding.ProjectionBinder;
import org.hibernate.search.mapper.pojo.search.definition.binding.ProjectionBindingContext;
import org.hibernate.search.util.common.impl.Contracts;

/**
 * Binds a constructor parameter to a projection to the distance from the center to a field in the indexed document.
 *
 * @see SearchProjectionFactory#distance(String, GeoPoint)
 * @see org.hibernate.search.mapper.pojo.mapping.definition.annotation.DistanceProjection
 */
public final class DistanceProjectionBinder implements ProjectionBinder {

	/**
	 * Creates a {@link DistanceProjectionBinder} to be passed
	 * to {@link org.hibernate.search.mapper.pojo.mapping.definition.programmatic.MethodParameterMappingStep#projection(ProjectionBinder)}.
	 * <p>
	 * This method requires the projection constructor class to be compiled with the {@code -parameters} flag
	 * to infer the field path from the name of the constructor parameter being bound.
	 * If this compiler flag is not used,
	 * use {@link #create(String)} instead and pass the field path explicitly.
	 *
	 * @param parameterName The name of the parameter representing the {@link GeoPoint center point} from which the distance
	 * to the field value is going to be calculated.
	 *
	 * @return The binder.
	 * @see SearchProjectionFactory#distance(String, GeoPoint)
	 * @see SearchProjectionFactory#withParameters(Function)
	 */
	public static DistanceProjectionBinder create(String parameterName) {
		return create( null, parameterName );
	}

	/**
	 * Creates a {@link DistanceProjectionBinder} to be passed
	 * to {@link org.hibernate.search.mapper.pojo.mapping.definition.programmatic.MethodParameterMappingStep#projection(ProjectionBinder)}.
	 *
	 * @param fieldPath The <a href="../../../../../../engine/search/projection/dsl/SearchProjectionFactory.html#field-paths">path</a>
	 * to the index field whose value will be extracted.
	 * When {@code null}, defaults to the name of the constructor parameter being bound,
	 * if it can be retrieved (requires the class to be compiled with the {@code -parameters} flag;
	 * otherwise a null {@code fieldPath} will lead to a failure).
	 * @param parameterName The name of the parameter representing the {@link GeoPoint center point} from which the distance
	 * to the field value is going to be calculated.
	 *
	 * @return The binder.
	 * @see SearchProjectionFactory#distance(String, GeoPoint)
	 * @see SearchProjectionFactory#withParameters(Function)
	 */
	public static DistanceProjectionBinder create(String fieldPath, String parameterName) {
		return new DistanceProjectionBinder( fieldPath, parameterName );
	}

	private final String fieldPathOrNull;
	private final String parameterName;
	private DistanceUnit unit = DistanceUnit.METERS;

	private DistanceProjectionBinder(String fieldPathOrNull, String parameterName) {
		this.fieldPathOrNull = fieldPathOrNull;
		this.parameterName = parameterName;
	}

	/**
	 *
	 * @param unit The unit of the computed distance (default is meters).
	 * @return {@code this}, for method chaining.
	 * @see DistanceToFieldProjectionOptionsStep#unit(DistanceUnit)
	 */
	public DistanceProjectionBinder unit(DistanceUnit unit) {
		this.unit = unit;
		return this;
	}

	@Override
	public void bind(ProjectionBindingContext context) {
		Contracts.assertNotNullNorEmpty( parameterName, "parameterName" );
		Optional<PojoModelValue<?>> containerElementOptional = context.containerElement();
		String fieldPath = fieldPathOrFail( context );
		Class<?> containerClass;
		if ( containerElementOptional.isPresent() ) {
			PojoModelValue<?> containerElement = containerElementOptional.get();
			if ( !containerElement.rawType().isAssignableFrom( Double.class ) ) {
				throw ProjectionLog.INSTANCE.invalidParameterTypeForDistanceProjectionInProjectionConstructor(
						containerElement.rawType(),
						"SomeContainer<Double>" );
			}
			containerClass = context.constructorParameter().rawType();
		}
		else {
			if ( !context.constructorParameter().rawType().isAssignableFrom( Double.class ) ) {
				throw ProjectionLog.INSTANCE.invalidParameterTypeForDistanceProjectionInProjectionConstructor(
						context.constructorParameter().rawType(), "Double" );
			}
			containerClass = null;
		}
		ProjectionCollector.Provider<Double, ?> collector = context.projectionCollectorProviderFactory()
				.projectionCollectorProvider( containerClass, Double.class );
		bind( context, fieldPath, collector );
	}

	private void bind(ProjectionBindingContext context, String fieldPath,
			ProjectionCollector.Provider<Double, ?> collector) {
		context.definition( Double.class, context.isIncluded( fieldPath )
				? BeanHolder
						.of( new DistanceProjectionDefinition.WrappedValued<>( fieldPath, parameterName, unit, collector ) )
				: ConstantProjectionDefinition.empty( collector ) );
	}

	private String fieldPathOrFail(ProjectionBindingContext context) {
		if ( fieldPathOrNull != null ) {
			return fieldPathOrNull;
		}
		Optional<String> paramName = context.constructorParameter().name();
		if ( paramName.isEmpty() ) {
			throw ProjectionLog.INSTANCE.missingParameterNameForFieldProjectionInProjectionConstructor();
		}
		return paramName.get();
	}
}
