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
import org.hibernate.search.mapper.pojo.bridge.binding.RoutingKeyBindingContext;
import org.hibernate.search.mapper.pojo.bridge.mapping.annotation.declaration.RoutingKeyBridgeMapping;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.RoutingKeyBinder;

/**
 * Reference a bridge for a {@link RoutingKeyBridgeMapping}.
 * <p>
 * Either a bridge or a binder can be referenced, but never both.
 * References can use either a name, a type, or both.
 * <p>
 * If a <b>bridge</b> is referenced directly, using the methods {@link #name()} or {@link #type()},
 * each time the mapped annotation is encountered, an instance of the routing key bridge will be created
 * and applied to the location where the annotation was found.
 * <p>
 * Routing key bridges mapped this way cannot be parameterized:
 * any attribute of the mapped annotation will be ignored.
 * <p>
 * If a <b>binder</b> is referenced, using the methods {@link #binderName()} or {@link #binderType()},
 * each time the mapped annotation is encountered, an instance of the routing key binder will be created.
 * The binder will be passed the annotation through its {@link RoutingKeyBinder#initialize(Annotation)} method,
 * and then the bridge will be created and bound by {@link RoutingKeyBinder#bind(RoutingKeyBindingContext)}.
 * <p>
 * Routing key bridges mapped this way can be parameterized:
 * the binder will be able to take any attribute of the mapped annotation into account
 * in its {@link RoutingKeyBinder#initialize(Annotation)} method.
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
	 * Reference a routing key bridge by the bean name of its binder.
	 * @return The bean name of the routing key binder.
	 */
	String binderName() default "";

	/**
	 * Reference a routing key bridge by the type of its binder.
	 * @return The type of the routing key binder.
	 */
	Class<? extends RoutingKeyBinder<?>> binderType() default UndefinedBinderImplementationType.class;

	/**
	 * Class used as a marker for the default value of the {@link #type()} attribute.
	 */
	abstract class UndefinedBridgeImplementationType implements RoutingKeyBridge {
		private UndefinedBridgeImplementationType() {
		}
	}

	/**
	 * Class used as a marker for the default value of the {@link #binderType()} attribute.
	 */
	abstract class UndefinedBinderImplementationType implements RoutingKeyBinder<Annotation> {
		private UndefinedBinderImplementationType() {
		}
	}
}

