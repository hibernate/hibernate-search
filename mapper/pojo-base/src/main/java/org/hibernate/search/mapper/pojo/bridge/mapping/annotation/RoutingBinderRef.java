/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.bridge.mapping.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.hibernate.search.engine.environment.bean.BeanRetrieval;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.RoutingBinder;
import org.hibernate.search.mapper.pojo.common.annotation.Param;

/**
 * References a {@link RoutingBinder}.
 * <p>
 * References can use either a name, a type, or both.
 */
@Documented
@Target({ }) // Only used as a component in other annotations
@Retention(RetentionPolicy.RUNTIME)
public @interface RoutingBinderRef {

	/**
	 * Reference a routing binder by its bean name.
	 * @return The bean name of the routing binder.
	 */
	String name() default "";

	/**
	 * Reference a routing binder by its bean type.
	 * @return The bean type of the routing binder.
	 */
	Class<? extends RoutingBinder> type() default UndefinedRoutingBinderImplementationType.class;

	/**
	 * @return How to retrieve the binder. See {@link BeanRetrieval}.
	 */
	BeanRetrieval retrieval() default BeanRetrieval.ANY;

	/**
	 * @return Params that will be passed to the {@link RoutingBinder}.
	 */
	Param[] params() default { };

	/**
	 * Class used as a marker for the default value of the {@link #type()} attribute.
	 */
	abstract class UndefinedRoutingBinderImplementationType implements RoutingBinder {
		private UndefinedRoutingBinderImplementationType() {
		}
	}
}
