/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.loading.mapping.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.hibernate.search.engine.environment.bean.BeanRetrieval;
import org.hibernate.search.mapper.pojo.common.annotation.Param;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.SearchEntity;

/**
 * References a loading binder to use for an {@link SearchEntity entity}.
 */
@Documented
@Target({ }) // Only used as a component in other annotations
@Retention(RetentionPolicy.RUNTIME)
public @interface EntityLoadingBinderRef {

	/**
	 * Reference a binder by its bean name.
	 * @return The bean name of the configurer.
	 */
	String name() default "";

	/**
	 * Reference a binder by its type.
	 * @return The type of the binder.
	 */
	Class<?> type() default UndefinedImplementationType.class;

	/**
	 * @return How to retrieve the binder. See {@link BeanRetrieval}.
	 */
	BeanRetrieval retrieval() default BeanRetrieval.ANY;

	/**
	 * @return Params that will be passed to the binder.
	 */
	Param[] params() default { };

	/**
	 * Class used as a marker for the default value of the {@link #type()} attribute.
	 */
	abstract class UndefinedImplementationType {
		private UndefinedImplementationType() {
		}
	}
}
