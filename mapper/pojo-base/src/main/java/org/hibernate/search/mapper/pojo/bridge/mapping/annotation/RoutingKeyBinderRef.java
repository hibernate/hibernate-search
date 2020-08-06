/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.bridge.mapping.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * References a {@link org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.RoutingKeyBinder}.
 * <p>
 * References can use either a name, a type, or both.
 *
 * @deprecated Only useful in {@link org.hibernate.search.mapper.pojo.mapping.definition.annotation.RoutingKeyBinding},
 * which is deprecated.
 */
@Documented
@Target({}) // Only used as a component in other annotations
@Retention(RetentionPolicy.RUNTIME)
@Deprecated
public @interface RoutingKeyBinderRef {

	/**
	 * Reference a routing key binder by its bean name.
	 * @return The bean name of the routing key binder.
	 */
	String name() default "";

	/**
	 * Reference a routing key binder by its bean type.
	 * @return The type of the routing key binder.
	 */
	Class<? extends org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.RoutingKeyBinder> type() default UndefinedBinderImplementationType.class;

	/**
	 * Class used as a marker for the default value of the {@link #type()} attribute.
	 */
	abstract class UndefinedBinderImplementationType implements org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.RoutingKeyBinder {
		private UndefinedBinderImplementationType() {
		}
	}
}

