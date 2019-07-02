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

import org.hibernate.search.mapper.pojo.bridge.IdentifierBridge;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.IdentifierBridgeBuilder;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;

/**
 * A reference to the identifier bridge to use for a {@link DocumentId}.
 * <p>
 * Either a bridge or a bridge builder can be provided, but never both.
 * Reference can be obtained using either a name or a type.
 */
@Documented
@Target({}) // Only used as a component in other annotations
@Retention(RetentionPolicy.RUNTIME)
public @interface IdentifierBridgeRef {

	/**
	 * Reference an identifier bridge by its bean name.
	 * @return The bean name of the  identifier bridge.
	 */
	String name() default "";

	/**
	 * Reference an identifier bridge by its type.
	 * @return The type of the  identifier bridge.
	 */
	Class<? extends IdentifierBridge> type() default UndefinedBridgeImplementationType.class;

	/**
	 * Reference an identifier bridge by the bean name of its builder.
	 * @return The bean name of the  identifier bridge builder.
	 */
	String builderName() default "";

	/**
	 * Reference an identifier bridge by the type of its builder.
	 * @return The type of the  identifier bridge builder.
	 */
	Class<? extends IdentifierBridgeBuilder> builderType() default UndefinedBuilderImplementationType.class;

	/**
	 * Class used as a marker for the default value of the {@link #type()} attribute.
	 */
	abstract class UndefinedBridgeImplementationType implements IdentifierBridge<Object> {
		private UndefinedBridgeImplementationType() {
		}
	}

	/**
	 * Class used as a marker for the default value of the {@link #builderType()} attribute.
	 */
	abstract class UndefinedBuilderImplementationType implements IdentifierBridgeBuilder {
		private UndefinedBuilderImplementationType() {
		}
	}
}
