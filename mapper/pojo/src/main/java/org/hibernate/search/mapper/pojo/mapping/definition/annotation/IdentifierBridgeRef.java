/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping.definition.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.hibernate.search.mapper.pojo.bridge.IdentifierBridge;
import org.hibernate.search.mapper.pojo.bridge.mapping.BridgeBuilder;

/**
 * Reference to the identifier bridge to use for a {@link DocumentId} field.
 * <p>
 * Either a bridge or a bridge builder can be provided, but never both.
 * Reference can be obtained using either a name or a type.
 *
 * @author Yoann Rodiere
 */
@Documented
@Target({}) // Only used as a component in other annotations
@Retention(RetentionPolicy.RUNTIME)
public @interface IdentifierBridgeRef {

	/**
	 * Provide the bridge name to get the bridge reference.
	 *
	 * @return the bridge name
	 */
	String name() default "";

	/**
	 * Provide the bridge type to get the bridge reference.
	 *
	 * @return the bridge type
	 */
	Class<? extends IdentifierBridge<?>> type() default UndefinedBridgeImplementationType.class;

	/**
	 * Provide the builder bridge name to get the bridge reference.
	 *
	 * @return the bridge builder name
	 */
	String builderName() default "";

	/**
	 * Provide the builder bridge type to get the bridge reference.
	 *
	 * @return the bridge builder type
	 */
	Class<? extends BridgeBuilder<? extends IdentifierBridge<?>>> builderType() default UndefinedBuilderImplementationType.class;

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
	abstract class UndefinedBuilderImplementationType implements BridgeBuilder<IdentifierBridge<Object>> {
		private UndefinedBuilderImplementationType() {
		}
	}
}
