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

import org.hibernate.search.mapper.pojo.bridge.ValueBridge;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.BridgeBuilder;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FullTextField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.KeywordField;

/**
 * A reference to the value bridge to use in a {@code @*Field} annotation,
 * for example in {@link GenericField#valueBridge()}, {@link KeywordField#valueBridge()},
 * or {@link FullTextField#valueBridge()}.
 * <p>
 * Either a bridge or a bridge builder can be provided, but never both.
 * Reference can be obtained using either a name or a type.
 */
@Documented
@Target({}) // Only used as a component in other annotations
@Retention(RetentionPolicy.RUNTIME)
public @interface ValueBridgeRef {

	/**
	 * Reference a value bridge by its bean name.
	 * @return The bean name of the value bridge.
	 */
	String name() default "";

	/**
	 * Reference a value bridge by its type.
	 * @return The type of the value bridge.
	 */
	Class<? extends ValueBridge> type() default UndefinedBridgeImplementationType.class;

	/**
	 * Reference a value bridge by the bean name of its builder.
	 * @return The bean name of the value bridge builder.
	 */
	String builderName() default "";

	/**
	 * Reference a value bridge by the type of its builder.
	 * @return The type of the value bridge builder.
	 */
	Class<? extends BridgeBuilder<? extends ValueBridge<?, ?>>> builderType() default UndefinedBuilderImplementationType.class;

	/**
	 * Class used as a marker for the default value of the {@link #type()} attribute.
	 */
	abstract class UndefinedBridgeImplementationType implements ValueBridge<Object, Object> {
		private UndefinedBridgeImplementationType() {
		}
	}

	/**
	 * Class used as a marker for the default value of the {@link #builderType()} attribute.
	 */
	abstract class UndefinedBuilderImplementationType implements BridgeBuilder<ValueBridge<Object, Object>> {
		private UndefinedBuilderImplementationType() {
		}
	}
}
