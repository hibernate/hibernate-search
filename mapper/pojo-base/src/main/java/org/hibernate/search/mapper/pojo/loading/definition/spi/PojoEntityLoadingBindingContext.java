/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.loading.definition.spi;

import java.util.Optional;

import org.hibernate.search.engine.environment.bean.BeanResolver;
import org.hibernate.search.mapper.pojo.loading.spi.PojoMassLoadingStrategy;
import org.hibernate.search.mapper.pojo.loading.spi.PojoSelectionLoadingStrategy;
import org.hibernate.search.mapper.pojo.model.PojoModelElement;
import org.hibernate.search.util.common.SearchException;

public interface PojoEntityLoadingBindingContext {

	/**
	 * @return The entity type being bound to loading strategies.
	 */
	PojoModelElement entityType();

	/**
	 * @return The type of identifiers for the entity type being bound to loading strategies.
	 */
	PojoModelElement identifierType();

	/**
	 * @param expectedEntitySuperType An expected entity supertype that the strategy can handle.
	 * @param strategy The strategy for selection loading, used in particular during search.
	 * @param <E> An expected entity supertype that the strategy can handle.
	 */
	<E> void selectionLoadingStrategy(Class<E> expectedEntitySuperType, PojoSelectionLoadingStrategy<? super E> strategy);

	/**
	 * @param expectedEntitySuperType An expected entity supertype that the strategy can handle.
	 * @param strategy The strategy for mass loading, used in particular during mass indexing.
	 * @param <E> An expected entity supertype that the strategy can handle.
	 */
	<E> void massLoadingStrategy(Class<E> expectedEntitySuperType, PojoMassLoadingStrategy<? super E, ?> strategy);

	/**
	 * @return A bean provider, allowing the retrieval of beans,
	 * including CDI/Spring DI beans when in the appropriate environment.
	 */
	BeanResolver beanResolver();

	/**
	 * @param name The name of the param
	 * @param paramType The type of the parameter.
	 * @param <T> The type of the parameter.
	 * @return Get a param defined for the binder by the given name
	 * @throws SearchException if it does not exist a param having such name
	 */
	<T> T param(String name, Class<T> paramType);

	/**
	 * @param name The name of the param
	 * @param paramType The type of the parameter.
	 * @param <T> The type of the parameter.
	 * @return Get an optional param defined for the binder by the given name,
	 * a param having such name may either exist or not.
	 */
	<T> Optional<T> paramOptional(String name, Class<T> paramType);

}
