/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.bridge;

import org.hibernate.search.bridge.impl.ExtendedBridgeProvider.ExtendedBridgeProviderContext;

/**
 * A {@link FieldBridge} might want to take care of specific containers as whoel types.
 * <p>
 * For example, the type {@code byte[]} is technically an array of bytes, but some bridges might want to treat it as a
 * blob.
 *
 * @author Davide D'Alto
 */
public interface ContainerAwareBridge {

	/**
	 * Check if the type should be treated as a container or as a whole.
	 * <p>
	 * If the type is a container, the bridge will be applied to all the element of the container, otherwise the bridge
	 * is applied to the container as a whole.
	 *
	 * @param context the current context for the bridge
	 * @return {@code true} if the propertye should be treated as a container; {@code false} otherwise
	 */
	boolean isContainer(ExtendedBridgeProviderContext context);
}
