/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping.definition.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.hibernate.search.engine.backend.document.model.Store;

/**
 * @author Yoann Rodiere
 */
@Documented
@Target({ ElementType.TYPE, ElementType.METHOD, ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
@Repeatable( Field.List.class )
public @interface Field {

	/**
	 * @return The name of the index field.
	 */
	String name() default "";

	/**
	 * @return A reference to the function bridge to use for this field.
	 * Cannot be used in the same {@code @Field} annotation as {@link #functionBridgeBuilder()}:
	 * either a bridge or a bridge builder can be provided, but never both.
	 */
	FunctionBridgeBeanReference functionBridge() default @FunctionBridgeBeanReference;

	/**
	 * @return A reference to the builder to use to build a function bridge for this field.
	 * Cannot be used in the same {@code @Field} annotation as {@link #functionBridge()}:
	 * either a bridge or a bridge builder can be provided, but never both.
	 */
	FunctionBridgeBuilderBeanReference functionBridgeBuilder() default @FunctionBridgeBuilderBeanReference;

	Store store() default Store.DEFAULT;

	// TODO index, analyze, norms, termVector
	// TODO analyzer, normalizer
	// TODO indexNullAs? => Maybe we should rather use "missing" queries?

	@Documented
	@Target({ ElementType.TYPE, ElementType.METHOD, ElementType.FIELD })
	@Retention(RetentionPolicy.RUNTIME)
	@interface List {
		Field[] value();
	}

}
