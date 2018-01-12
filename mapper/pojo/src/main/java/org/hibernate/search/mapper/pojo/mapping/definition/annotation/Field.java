/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping.definition.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author Yoann Rodiere
 */
@Documented
@Target({ ElementType.TYPE, ElementType.METHOD, ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
// TODO repeatable
public @interface Field {

	/**
	 * @return The name of the index field.
	 */
	String name() default "";

	/**
	 * @return A reference to the function bridge to use for this field.
	 * Cannot be used in the same {@code @Field} annotation as {@link #bridgeBuilder()}:
	 * either a bridge or a bridge builder can be provided, but never both.
	 */
	FunctionBridgeBeanReference bridge() default @FunctionBridgeBeanReference;

	/**
	 * @return A reference to the builder to use to build a function bridge for this field.
	 * Cannot be used in the same {@code @Field} annotation as {@link #bridge()}:
	 * either a bridge or a bridge builder can be provided, but never both.
	 */
	FunctionBridgeBuilderBeanReference bridgeBuilder() default @FunctionBridgeBuilderBeanReference;

	// TODO index, analyze, store, norms, termVector
	// TODO analyzer, normalizer
	// TODO indexNullAs? => Maybe we should rather use "missing" queries?

}
