/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.bridge.mapping.annotation;

import java.lang.annotation.Annotation;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.hibernate.search.mapper.pojo.bridge.TypeBridge;
import org.hibernate.search.mapper.pojo.bridge.binding.TypeBindingContext;
import org.hibernate.search.mapper.pojo.bridge.mapping.annotation.declaration.TypeBridgeMapping;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.TypeBridgeBuilder;

/**
 * Reference a type bridge for a {@link TypeBridgeMapping}.
 * <p>
 * Either a bridge or a bridge builder can be provided, but never both.
 * Reference can be obtained using either a name or a type.
 * <p>
 * If a <b>direct bridge</b> is provided, using the methods {@link #name()} or {@link #type()},
 * each time the mapped annotation is encountered, an instance of the type bridge will be created
 * and applied to the location where the annotation was found.
 * <p>
 * Type bridges mapped this way cannot be parameterized:
 * any attribute of the mapped annotation will be ignored.
 * <p>
 * If an <b>annotation bridge builder</b> is provided, using the methods {@link #builderName()} or {@link #builderType()},
 * each time the mapped annotation is encountered, an instance of the type bridge builder will be created.
 * The builder will be passed the annotation through its {@link TypeBridgeBuilder#initialize(Annotation)} method,
 * and then the bridge will be created and bound by {@link TypeBridgeBuilder#bind(TypeBindingContext)}.
 * <p>
 * Type bridges mapped this way can be parameterized:
 * the bridge will be able to take any attribute of the mapped annotation into account
 * in its {@link TypeBridgeBuilder#initialize(Annotation)} method.
 *
 */
@Documented
@Target({}) // Only used as a component in other annotations
@Retention(RetentionPolicy.RUNTIME)
public @interface TypeBridgeRef {

	/**
	 * Reference a type bridge by its bean name.
	 * @return The bean name of the type bridge.
	 */
	String name() default "";

	/**
	 * Reference a type bridge by its type.
	 * @return The type of the type bridge.
	 */
	Class<? extends TypeBridge> type() default UndefinedBridgeImplementationType.class;

	/**
	 * Reference a type bridge by the bean name of its builder.
	 * @return The bean name of the type bridge builder.
	 */
	String builderName() default "";

	/**
	 * Reference a type bridge by the type of its builder.
	 * @return The type of the type bridge builder.
	 */
	Class<? extends TypeBridgeBuilder<?>> builderType() default UndefinedBuilderImplementationType.class;

	/**
	 * Class used as a marker for the default value of the {@link #type()} attribute.
	 */
	abstract class UndefinedBridgeImplementationType implements TypeBridge {
		private UndefinedBridgeImplementationType() {
		}
	}

	/**
	 * Class used as a marker for the default value of the {@link #builderType()} attribute.
	 */
	abstract class UndefinedBuilderImplementationType implements TypeBridgeBuilder<Annotation> {
		private UndefinedBuilderImplementationType() {
		}
	}
}

