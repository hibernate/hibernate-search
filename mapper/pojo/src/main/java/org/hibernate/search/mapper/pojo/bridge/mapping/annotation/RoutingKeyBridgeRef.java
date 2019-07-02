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

import org.hibernate.search.mapper.pojo.bridge.RoutingKeyBridge;
import org.hibernate.search.mapper.pojo.bridge.mapping.annotation.declaration.RoutingKeyBridgeMapping;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.BridgeBuildContext;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.RoutingKeyBridgeBuilder;

/**
 * Reference a bridge for a {@link RoutingKeyBridgeMapping}.
 * <p>
 * Either a bridge or a bridge builder can be provided, but never both.
 * Reference can be obtained using either a name or a type.
 * <p>
 * If a <b>direct bridge</b> is provided, using the methods {@link #name()} or {@link #type()},
 * each time the mapped annotation is encountered, an instance of the routing key bridge will be created
 * and applied to the location where the annotation was found.
 * <p>
 * Routing key bridges mapped this way cannot be parameterized:
 * any attribute of the mapped annotation will be ignored.
 * <p>
 * If an <b>annotation bridge builder</b> is provided, using the methods {@link #builderName()} or {@link #builderType()},
 * each time the mapped annotation is encountered, an instance of the routing key bridge builder will be created.
 * The builder will be passed the annotation through its {@link RoutingKeyBridgeBuilder#initialize(Annotation)} method,
 * and then the bridge will be retrieved by calling {@link RoutingKeyBridgeBuilder#buildForRoutingKey(BridgeBuildContext)}.
 * <p>
 * Routing key bridges mapped this way can be parameterized:
 * the bridge will be able to take any attribute of the mapped annotation into account
 * in its {@link RoutingKeyBridgeBuilder#initialize(Annotation)} method.
 *
 */
@Documented
@Target({}) // Only used as a component in other annotations
@Retention(RetentionPolicy.RUNTIME)
public @interface RoutingKeyBridgeRef {

	/**
	 * Reference a routing key bridge by its bean name.
	 * @return The bean name of the routing key bridge.
	 */
	String name() default "";

	/**
	 * Reference a routing key bridge by its type.
	 * @return The type of the routing key bridge.
	 */
	Class<? extends RoutingKeyBridge> type() default UndefinedBridgeImplementationType.class;

	/**
	 * Reference a routing key bridge by the bean name of its builder.
	 * @return The bean name of the routing key bridge builder.
	 */
	String builderName() default "";

	/**
	 * Reference a routing key bridge by the type of its builder.
	 * @return The type of the routing key bridge builder.
	 */
	Class<? extends RoutingKeyBridgeBuilder<?>> builderType() default UndefinedBuilderImplementationType.class;

	/**
	 * Class used as a marker for the default value of the {@link #type()} attribute.
	 */
	abstract class UndefinedBridgeImplementationType implements RoutingKeyBridge {
		private UndefinedBridgeImplementationType() {
		}
	}

	/**
	 * Class used as a marker for the default value of the {@link #builderType()} attribute.
	 */
	abstract class UndefinedBuilderImplementationType implements RoutingKeyBridgeBuilder<Annotation> {
		private UndefinedBuilderImplementationType() {
		}
	}
}

