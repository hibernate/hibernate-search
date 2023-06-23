/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.search.definition.mapping.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.hibernate.search.engine.environment.bean.BeanRetrieval;
import org.hibernate.search.mapper.pojo.common.annotation.Param;
import org.hibernate.search.mapper.pojo.search.definition.binding.ProjectionBinder;
import org.hibernate.search.mapper.pojo.search.definition.binding.ProjectionBindingContext;

/**
 * References a {@link ProjectionBinder}.
 * <p>
 * References can use either a name, a type, or both.
 */
@Documented
@Target({ }) // Only used as a component in other annotations
@Retention(RetentionPolicy.RUNTIME)
public @interface ProjectionBinderRef {

	/**
	 * Reference a projection binder by its bean name.
	 *
	 * @return The bean name of the projection binder.
	 */
	String name() default "";

	/**
	 * Reference a projection binder by its bean type.
	 *
	 * @return The type of the projection binder.
	 */
	Class<? extends ProjectionBinder> type() default UndefinedImplementationType.class;

	/**
	 * @return How to retrieve the projection definition. See {@link BeanRetrieval}.
	 */
	BeanRetrieval retrieval() default BeanRetrieval.ANY;

	/**
	 * @return Params that will get passed to the {@link ProjectionBinder}.
	 *
	 * @see ProjectionBindingContext#param(String, Class)
	 * @see ProjectionBindingContext#paramOptional(String, Class)
	 */
	Param[] params() default { };

	/**
	 * Class used as a marker for the default value of the {@link #type()} attribute.
	 */
	abstract class UndefinedImplementationType implements ProjectionBinder {
		private UndefinedImplementationType() {
		}
	}
}
