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

import org.hibernate.search.engine.environment.bean.BeanRetrieval;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.PropertyBinder;
import org.hibernate.search.mapper.pojo.common.annotation.Param;

/**
 * References a {@link PropertyBinder}.
 * <p>
 * References can use either a name, a type, or both.
 */
@Documented
@Target({ }) // Only used as a component in other annotations
@Retention(RetentionPolicy.RUNTIME)
public @interface PropertyBinderRef {

	/**
	 * Reference a property binder by its bean name.
	 * @return The bean name of the property binder.
	 */
	String name() default "";

	/**
	 * Reference a property binder by its bean type.
	 * @return The type of the property binder.
	 */
	Class<? extends PropertyBinder> type() default UndefinedBinderImplementationType.class;

	/**
	 * @return How to retrieve the binder. See {@link BeanRetrieval}.
	 */
	BeanRetrieval retrieval() default BeanRetrieval.ANY;

	/**
	 * @return Params that will be passed to the {@link PropertyBinder}.
	 */
	Param[] params() default { };

	/**
	 * Class used as a marker for the default value of the {@link #type()} attribute.
	 */
	abstract class UndefinedBinderImplementationType implements PropertyBinder {
		private UndefinedBinderImplementationType() {
		}
	}
}
