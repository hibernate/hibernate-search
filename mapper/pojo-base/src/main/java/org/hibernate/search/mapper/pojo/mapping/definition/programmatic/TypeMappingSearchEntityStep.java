/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.mapping.definition.programmatic;

import java.util.Collections;
import java.util.Map;

import org.hibernate.search.engine.environment.bean.BeanReference;
import org.hibernate.search.mapper.pojo.loading.mapping.annotation.EntityLoadingBinderRef;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.SearchEntity;
import org.hibernate.search.util.common.annotation.Incubating;

/**
 * The step in a mapping definition where a type's entity metadata can be configured more precisely.
 */
@Incubating
public interface TypeMappingSearchEntityStep {

	/**
	 * @param entityName The name of the entity type.
	 * Defaults to the JPA entity name for the Hibernate ORM mapper,
	 * or failing that to the {@link Class#getSimpleName() simple class name}.
	 * @return {@code this}, for method chaining.
	 * @see SearchEntity#name()
	 */
	TypeMappingSearchEntityStep name(String entityName);

	/**
	 * Defines a binder for loading of entities of this type.
	 * <p>
	 * Note: this is unnecessary when using the Hibernate ORM mapper,
	 * which contributes this information automatically.
	 * <p>
	 * To pass some parameters to the binder,
	 * use the method {@link #loadingBinder(BeanReference, Map)} instead.
	 *
	 * @param binder A loading binder to apply to the entity type.
	 * @return {@code this}, for method chaining.
	 * @see SearchEntity#loadingBinder()
	 */
	default TypeMappingSearchEntityStep loadingBinder(Object binder) {
		return loadingBinder( BeanReference.ofInstance( binder ), Collections.emptyMap() );
	}

	/**
	 * Defines a binder for loading of entities of this type.
	 * <p>
	 * Note: this is unnecessary when using the Hibernate ORM mapper,
	 * which contributes this information automatically.
	 * <p>
	 * To pass some parameters to the binder,
	 * use the method {@link #loadingBinder(BeanReference, Map)} instead.
	 *
	 * @param binderRef A {@link BeanReference} to the loading binder to apply to the entity type.
	 * @return {@code this}, for method chaining.
	 * @see SearchEntity#loadingBinder()
	 */
	default TypeMappingSearchEntityStep loadingBinder(BeanReference<?> binderRef) {
		return loadingBinder( binderRef, Collections.emptyMap() );
	}

	/**
	 * Defines a binder for loading of entities of this type.
	 * <p>
	 * Note: this is unnecessary when using the Hibernate ORM mapper,
	 * which contributes this information automatically.
	 * <p>
	 * With this method it is possible to pass a set of parameters to the binder.
	 *
	 * @param binderRef A {@link BeanReference} to the loading binder to apply to the entity type.
	 * @param params The parameters to pass to the binder.
	 * @return {@code this}, for method chaining.
	 * @see SearchEntity#loadingBinder()
	 * @see EntityLoadingBinderRef#params()
	 */
	TypeMappingSearchEntityStep loadingBinder(BeanReference<?> binderRef, Map<String, Object> params);

}
