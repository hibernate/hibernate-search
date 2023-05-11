/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.search.definition.binding;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.hibernate.search.engine.environment.bean.BeanHolder;
import org.hibernate.search.engine.environment.bean.BeanReference;
import org.hibernate.search.engine.environment.bean.BeanResolver;
import org.hibernate.search.engine.search.projection.definition.ProjectionDefinition;
import org.hibernate.search.mapper.pojo.bridge.mapping.annotation.PropertyBinderRef;
import org.hibernate.search.mapper.pojo.model.PojoModelConstructorParameter;
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
	 * @param <P> The type of values returned by the projection.
	 */
	<P> void definition(Class<P> expectedValueType, ProjectionDefinition<? extends P> definition);

	/**
	 * Binds the {@link #constructorParameter()} to the given projection definition.
	 *
	 * @param expectedValueType The expected type of the {@link #constructorParameter()},
	 * which must be compatible with the given projection definition.
	 * Hibernate Search will check that these expectations are met, and throw an exception if they are not.
	 * @param definitionHolder A {@link BeanHolder} containing the definition of the projection
	 * to bind to the {@link #constructorParameter()}.
	 * @param <P> The type of values returned by the projection.
	 */
	<P> void definition(Class<P> expectedValueType, BeanHolder<? extends ProjectionDefinition<? extends P>> definitionHolder);

	/**
	 * Inspects the type of the {@link #constructorParameter()}
	 * to determine if it may be bound to a multi-valued projection.
	 *
	 * @return An optional containing a context that can be used to bind a projection
	 * if the type of the {@link #constructorParameter()} can be bound to a multi-valued projection;
	 * an empty optional otherwise.
	 */
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
	 * @param name The name of the parameter.
	 * @return The value provided for this parameter.
	 * @throws SearchException If no value was provided for this parameter.
	 * @see PropertyBinderRef#params()
	 * @see org.hibernate.search.mapper.pojo.mapping.definition.programmatic.MethodParameterMappingStep#projection(BeanReference, Map)
	 */
	Object param(String name);

	/**
	 * @param name The name of the parameter.
	 * @return An optional containing the value provided for this parameter,
	 * or {@code Optional.empty()} if no value was provided for this parameter.
	 * @see PropertyBinderRef#params()
	 * @see org.hibernate.search.mapper.pojo.mapping.definition.programmatic.MethodParameterMappingStep#projection(BeanReference, Map)
	 */
	Optional<Object> paramOptional(String name);

	/**
	 * @param fieldPath The (relative) path to an object field in the indexed document.
	 * @param projectedType A type expected to have a corresponding projection mapping
	 * (e.g. using {@link org.hibernate.search.mapper.pojo.mapping.definition.annotation.ProjectionConstructor})
	 * @return A single-valued object projection definition for the given type.
	 * @throws SearchException If mapping the given type to a projection definition fails.
	 * @see org.hibernate.search.engine.search.projection.dsl.SearchProjectionFactory#object(String)
	 * @see org.hibernate.search.engine.search.projection.dsl.CompositeProjectionInnerStep#as(Class)
	 */
	@Incubating
	<T> BeanHolder<? extends ProjectionDefinition<T>> createObjectDefinition(String fieldPath, Class<T> projectedType);

	/**
	 * @param fieldPath The (relative) path to an object field in the indexed document.
	 * @param projectedType A type expected to have a corresponding projection mapping
	 * (e.g. using {@link org.hibernate.search.mapper.pojo.mapping.definition.annotation.ProjectionConstructor})
	 * @return A multi-valued object projection definition for the given type.
	 * @throws SearchException If mapping the given type to a projection definition fails.
	 * @see org.hibernate.search.engine.search.projection.dsl.SearchProjectionFactory#object(String)
	 * @see org.hibernate.search.engine.search.projection.dsl.CompositeProjectionInnerStep#as(Class)
	 */
	@Incubating
	<T> BeanHolder<? extends ProjectionDefinition<List<T>>> createObjectDefinitionMulti(String fieldPath,
			Class<T> projectedType);

}
