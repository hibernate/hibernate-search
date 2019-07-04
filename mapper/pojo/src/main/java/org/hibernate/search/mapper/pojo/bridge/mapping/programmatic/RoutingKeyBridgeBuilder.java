/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.bridge.mapping.programmatic;

import java.lang.annotation.Annotation;

import org.hibernate.search.mapper.pojo.bridge.RoutingKeyBridge;
import org.hibernate.search.mapper.pojo.bridge.binding.RoutingKeyBindingContext;
import org.hibernate.search.mapper.pojo.bridge.runtime.RoutingKeyBridgeToRoutingKeyContext;

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
	 * Binds a type to routing keys.
	 * <p>
	 * The context passed in parameter provides various information about the type being bound.
	 * Implementations are expected to take advantage of that information
	 * and to call one of the {@code setBridge(...)} methods on the context
	 * to set the bridge.
	 * <p>
	 * Implementations are also expected to declare dependencies, i.e. the properties
	 * that will later be used in the
	 * {@link RoutingKeyBridge#toRoutingKey(String, Object, Object, RoutingKeyBridgeToRoutingKeyContext)} method,
	 * using {@link RoutingKeyBindingContext#getDependencies()}.
	 * Failing that, Hibernate Search will not reindex entities properly when an indexed property is modified.
	 *
	 * @param context A context object providing information about the type being bound,
	 * and expecting a call to one of its {@code setBridge(...)} methods.
	 */
	void bind(RoutingKeyBindingContext context);

}
