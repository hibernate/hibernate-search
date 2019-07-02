/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.bridge.mapping.programmatic;

import java.lang.annotation.Annotation;

import org.hibernate.search.engine.environment.bean.BeanHolder;
import org.hibernate.search.mapper.pojo.bridge.RoutingKeyBridge;

/**
 * A builder of {@link RoutingKeyBridge}.
 *
 * @param <A> The type of annotations accepted by the {@link #initialize(Annotation)} method.
 * @see RoutingKeyBridge
 */
public interface RoutingKeyBridgeBuilder<A extends Annotation> {

	/**
	 * Initializes the parameters of this builder with the attributes of the given annotation.
	 * @param annotation An annotation to extract parameters from.
	 */
	default void initialize(A annotation) {
	}

	/**
	 * Build a bridge.
	 * <p>
	 * <strong>Warning:</strong> this method can be called multiple times and must return a new instance to each call.
	 *
	 * @param buildContext A object providing access to other components involved in the build process.
	 * @return A new bridge instance, enclosed in a {@link BeanHolder}.
	 * Use {@link BeanHolder#of(Object)} if you don't need any particular closing behavior.
	 */
	// FIXME use method parameter overload to avoid conflict between this build() method and the ones from other builders
	BeanHolder<? extends RoutingKeyBridge> buildForRoutingKey(BridgeBuildContext buildContext);

}
