/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.bridge.runtime;

import org.hibernate.search.util.common.SearchException;

/**
 * The context passed to
 * {@link org.hibernate.search.mapper.pojo.bridge.RoutingKeyBridge#toRoutingKey(String, Object, Object, RoutingKeyBridgeToRoutingKeyContext)}.
 *
 * @deprecated This is only useful in {@link org.hibernate.search.mapper.pojo.bridge.RoutingKeyBridge},
 * which is deprecated.
 */
@Deprecated
public interface RoutingKeyBridgeToRoutingKeyContext {

	/**
	 * Extend the current context with the given extension,
	 * resulting in an extended context offering more options.
	 *
	 * @param extension The extension to apply.
	 * @param <T> The type of context provided by the extension.
	 * @return The extended context.
	 * @throws SearchException If the extension cannot be applied (wrong underlying technology, ...).
	 */
	<T> T extension(RoutingKeyBridgeToRoutingKeyContextExtension<T> extension);

}
