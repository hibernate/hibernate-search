/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.search.definition.binding;

import org.hibernate.search.engine.environment.bean.BeanHolder;
import org.hibernate.search.engine.search.projection.definition.ProjectionDefinition;
import org.hibernate.search.engine.search.projection.dsl.MultiProjectionTypeReferenceProvider;
import org.hibernate.search.mapper.pojo.model.PojoModelValue;
import org.hibernate.search.util.common.annotation.Incubating;

/**
 * The context returned by {@link ProjectionBindingContext#multi()}.
 * @see ProjectionBindingContext#multi()
 */
@Incubating
public interface ProjectionBindingMultiContext {

	/**
	 * Binds the constructor parameter to the given multi-valued projection definition.
	 *
	 * @param expectedValueType The expected type of elements of the constructor parameter,
	 * which must be compatible with the element type of lists returned by the projection definition.
	 * Hibernate Search will check that these expectations are met, and throw an exception if they are not.
	 * Note this is not the type of the constructor parameter, but of its elements;
	 * i.e. for a constructor parameter of type {@code List<String>},
	 * {@code expectedValueType} should be set to {@code String.class}.
	 * @param definition A definition of the projection to bind to the constructor parameter.
	 * @param <P> The type of values returned by the projection.
	 */
	<C, P> void definition(Class<P> expectedValueType, ProjectionDefinition<? extends C> definition);

	/**
	 * Binds the constructor parameter to the given multi-valued projection definition.
	 *
	 * @param expectedValueType The expected type of elements of the constructor parameter,
	 * which must be compatible with the element type of lists returned by the projection definition.
	 * Hibernate Search will check that these expectations are met, and throw an exception if they are not.
	 * Note this is not the type of the constructor parameter, but of its elements;
	 * i.e. for a constructor parameter of type {@code List<String>},
	 * {@code expectedValueType} should be set to {@code String.class}.
	 * @param definitionHolder A {@link BeanHolder} containing the definition of the projection
	 * to bind to the constructor parameter.
	 * @param <C> The type of collection to collect the result into.
	 * @param <P> The type of values returned by the projection.
	 */
	<C, P> void definition(Class<P> expectedValueType,
			BeanHolder<? extends ProjectionDefinition<? extends C>> definitionHolder);

	/**
	 * @return An entry point allowing to inspect the constructor parameter container element being bound to a projection.
	 */
	@Incubating
	PojoModelValue<?> containerElement();

	/**
	 * @return An entry point allowing to inspect the constructor parameter container being bound to a projection.
	 */
	@Incubating
	PojoModelValue<?> container();

	@Incubating
	MultiProjectionTypeReferenceProvider builtInMultiProjectionTypeReferenceProvider();

}
