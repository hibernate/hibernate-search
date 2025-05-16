/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.search.definition.binding;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.hibernate.search.engine.common.tree.TreeFilterDefinition;
import org.hibernate.search.engine.environment.bean.BeanHolder;
import org.hibernate.search.engine.environment.bean.BeanReference;
import org.hibernate.search.engine.environment.bean.BeanResolver;
import org.hibernate.search.engine.search.projection.ProjectionCollector;
import org.hibernate.search.engine.search.projection.ProjectionCollectorProviderFactory;
import org.hibernate.search.engine.search.projection.definition.ProjectionDefinition;
import org.hibernate.search.engine.search.projection.dsl.TypedSearchProjectionFactory;
import org.hibernate.search.mapper.pojo.bridge.mapping.annotation.PropertyBinderRef;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.ObjectProjection;
import org.hibernate.search.mapper.pojo.model.PojoModelConstructorParameter;
import org.hibernate.search.mapper.pojo.model.PojoModelValue;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.common.annotation.Incubating;

/**
 * The context passed to {@link ProjectionBinder#bind(ProjectionBindingContext)}.
 * @see ProjectionBinder#bind(ProjectionBindingContext)
 */
@Incubating
public interface ProjectionBindingContext {

	/**
	 * Binds the {@link #constructorParameter()} to the given projection definition.
	 *
	 * @param expectedValueType The expected type of the {@link #constructorParameter()},
	 * which must be compatible with the given projection definition.
	 * Hibernate Search will check that these expectations are met, and throw an exception if they are not.
	 * @param definition A definition of the projection
	 * to bind to the {@link #constructorParameter()}.
	 * @param <P> The type of single projected value.
	 * @param <C> The type of values returned by the projection.
	 * It may be the same as {@code P}, if it is a simple single-valued projection,
	 * or a type of the container if the {@code P} values are wrapped in some type of container (e.g. {@code Optional<..>}, {@code Collection<..>}..).
	 */
	<P, C> void definition(Class<P> expectedValueType, ProjectionDefinition<? extends C> definition);

	/**
	 * Binds the {@link #constructorParameter()} to the given projection definition.
	 *
	 * @param expectedValueType The expected type of the {@link #constructorParameter()},
	 * which must be compatible with the given projection definition.
	 * Hibernate Search will check that these expectations are met, and throw an exception if they are not.
	 * @param definitionHolder A {@link BeanHolder} containing the definition of the projection
	 * to bind to the {@link #constructorParameter()}.
	 * @param <P> The type of single projected value.
	 * @param <C> The type of values returned by the projection.
	 * It may be the same as {@code P}, if it is a simple single-valued projection,
	 * or a type of the container if the {@code P} values are wrapped in some type of container (e.g. {@code Optional<..>}, {@code Collection<..>}..).
	 */
	<P, C> void definition(Class<P> expectedValueType,
			BeanHolder<? extends ProjectionDefinition<? extends C>> definitionHolder);

	/**
	 * Inspects the type of the {@link #constructorParameter()}
	 * to determine if it may be bound to a multi-valued projection.
	 *
	 * @return An optional containing a context that can be used to bind a projection
	 * if the type of the {@link #constructorParameter()} can be bound to a multi-valued projection;
	 * an empty optional otherwise.
	 * @deprecated Use {@link #containerElement()} and various bind methods of this context instead.
	 */
	@Deprecated(since = "8.0")
	@Incubating
	Optional<? extends ProjectionBindingMultiContext> multi();

	/**
	 * @return A bean provider, allowing the retrieval of beans,
	 * including CDI/Spring DI beans when in the appropriate environment.
	 */
	BeanResolver beanResolver();

	/**
	 * @return An entry point allowing to inspect the constructor parameter being bound to a projection.
	 */
	@Incubating
	PojoModelConstructorParameter constructorParameter();

	/**
	 * @return An entry point allowing to inspect the constructor parameter container element being bound to a projection.
	 * Returns non-empty optional only if a {@link #constructorParameter()} can be bound to a container-wrapped projection
	 * (be it a single-valued optional, or some multi-valued collection).
	 */
	@Incubating
	Optional<PojoModelValue<?>> containerElement();

	/**
	 * @param name The name of the parameter.
	 * @return The value provided for this parameter.
	 * @throws SearchException If no value was provided for this parameter.
	 * @see PropertyBinderRef#params()
	 * @see org.hibernate.search.mapper.pojo.mapping.definition.programmatic.MethodParameterMappingStep#projection(BeanReference, Map)
	 * @deprecated Use {@link #param(String, Class)} instead.
	 */
	@Deprecated(since = "7.0")
	default Object param(String name) {
		return param( name, Object.class );
	}

	/**
	 * @param name The name of the parameter.
	 * @param paramType The type of the parameter.
	 * @param <T> The type of the parameter.
	 * @return The value provided for this parameter.
	 * @throws SearchException If no value was provided for this parameter.
	 * @see PropertyBinderRef#params()
	 * @see org.hibernate.search.mapper.pojo.mapping.definition.programmatic.MethodParameterMappingStep#projection(BeanReference, Map)
	 */
	<T> T param(String name, Class<T> paramType);

	/**
	 * @param name The name of the parameter.
	 * @return An optional containing the value provided for this parameter,
	 * or {@code Optional.empty()} if no value was provided for this parameter.
	 * @see PropertyBinderRef#params()
	 * @see org.hibernate.search.mapper.pojo.mapping.definition.programmatic.MethodParameterMappingStep#projection(BeanReference, Map)
	 * @deprecated Use {@link #paramOptional(String, Class)} instead.
	 */
	@Deprecated(since = "7.0")
	default Optional<Object> paramOptional(String name) {
		return paramOptional( name, Object.class );
	}

	/**
	 * @param name The name of the parameter.
	 * @param paramType The type of the parameter.
	 * @param <T> The type of the parameter.
	 * @return An optional containing the value provided for this parameter,
	 * or {@code Optional.empty()} if no value was provided for this parameter.
	 * @see PropertyBinderRef#params()
	 * @see org.hibernate.search.mapper.pojo.mapping.definition.programmatic.MethodParameterMappingStep#projection(BeanReference, Map)
	 */
	<T> Optional<T> paramOptional(String name, Class<T> paramType);

	/**
	 * @param fieldPath The (relative) path to an object field in the indexed document.
	 * @param projectedType A type expected to have a corresponding projection mapping
	 * (e.g. using {@link org.hibernate.search.mapper.pojo.mapping.definition.annotation.ProjectionConstructor}).
	 * @param filter The filter to apply to determine which nested index field projections should be included in the projection.
	 * See {@link ObjectProjection#includePaths()}, {@link ObjectProjection#excludePaths()},
	 * {@link ObjectProjection#includeDepth()}, ...
	 * @return A single-valued object projection definition for the given type.
	 * @throws SearchException If mapping the given type to a projection definition fails.
	 * @see TypedSearchProjectionFactory#object(String)
	 * @see org.hibernate.search.engine.search.projection.dsl.CompositeProjectionInnerStep#as(Class)
	 */
	@Incubating
	default <T> BeanHolder<? extends ProjectionDefinition<T>> createObjectDefinition(String fieldPath, Class<T> projectedType,
			TreeFilterDefinition filter) {
		return createObjectDefinition( fieldPath, projectedType, filter, ProjectionCollector.nullable() );
	}

	/**
	 * @param fieldPath The (relative) path to an object field in the indexed document.
	 * @param projectedType A type expected to have a corresponding projection mapping
	 * (e.g. using {@link org.hibernate.search.mapper.pojo.mapping.definition.annotation.ProjectionConstructor}).
	 * @param filter The filter to apply to determine which nested index field projections should be included in the projection.
	 * See {@link ObjectProjection#includePaths()}, {@link ObjectProjection#excludePaths()},
	 * {@link ObjectProjection#includeDepth()}, ...
	 * @return A multi-valued object projection definition for the given type.
	 * @throws SearchException If mapping the given type to a projection definition fails.
	 * @see TypedSearchProjectionFactory#object(String)
	 * @see org.hibernate.search.engine.search.projection.dsl.CompositeProjectionInnerStep#as(Class)
	 * @deprecated Use {@link #createObjectDefinition(String, Class, TreeFilterDefinition, ProjectionCollector.Provider)} instead.
	 */
	@Deprecated(since = "8.0")
	@Incubating
	default <T> BeanHolder<? extends ProjectionDefinition<List<T>>> createObjectDefinitionMulti(String fieldPath,
			Class<T> projectedType, TreeFilterDefinition filter) {
		return createObjectDefinition( fieldPath, projectedType, filter, ProjectionCollector.list() );
	}

	/**
	 * @param fieldPath The (relative) path to an object field in the indexed document.
	 * @param projectedType A type expected to have a corresponding projection mapping
	 * (e.g. using {@link org.hibernate.search.mapper.pojo.mapping.definition.annotation.ProjectionConstructor}).
	 * @param filter The filter to apply to determine which nested index field projections should be included in the projection.
	 * See {@link ObjectProjection#includePaths()}, {@link ObjectProjection#excludePaths()},
	 * {@link ObjectProjection#includeDepth()}, ...
	 * @return A container-wrapped object projection definition for the given type.
	 * @throws SearchException If mapping the given type to a projection definition fails.
	 * @see TypedSearchProjectionFactory#object(String)
	 * @see org.hibernate.search.engine.search.projection.dsl.CompositeProjectionInnerStep#as(Class)
	 */
	@Incubating
	<C, T> BeanHolder<? extends ProjectionDefinition<C>> createObjectDefinition(String fieldPath,
			Class<T> projectedType, TreeFilterDefinition filter, ProjectionCollector.Provider<T, C> collector);

	/**
	 * @param projectedType A type expected to have a corresponding projection mapping
	 * (e.g. using {@link org.hibernate.search.mapper.pojo.mapping.definition.annotation.ProjectionConstructor})
	 * @return A composite projection definition for the given type.
	 * @throws SearchException If mapping the given type to a projection definition fails.
	 * @see TypedSearchProjectionFactory#composite()
	 * @see org.hibernate.search.engine.search.projection.dsl.CompositeProjectionInnerStep#as(Class)
	 */
	@Incubating
	<T> BeanHolder<? extends ProjectionDefinition<T>> createCompositeDefinition(Class<T> projectedType);

	/**
	 * @param fieldPath The (relative) path to an object field in the indexed document.
	 * @return {@code true} if the field with the given path is included according to surrounding
	 * {@link TreeFilterDefinition filters}
	 * (see {@link ObjectProjection#includePaths()}, {@link ObjectProjection#excludePaths()},
	 * {@link ObjectProjection#includeDepth()}, ...).
	 * {@code false} otherwise.
	 * Projections on excluded fields should be replaced with a constant projection
	 * returning {@code null} or an empty list, as appropriate.
	 */
	boolean isIncluded(String fieldPath);

	/**
	 * @return An instance of a projection collector provider factory capable of supplying a collector provider
	 * based on a container and component types.
	 */
	@Incubating
	ProjectionCollectorProviderFactory projectionCollectorProviderFactory();

}
