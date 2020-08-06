/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.bridge.mapping.programmatic;

import org.hibernate.search.mapper.pojo.bridge.RoutingBridge;
import org.hibernate.search.mapper.pojo.bridge.binding.RoutingBindingContext;

/**
 * An object responsible for assigning a {@link RoutingBridge} to an indexed entity.
 * <p>
 * This binder takes advantage of provided metadata
 * to pick, configure and create a {@link RoutingBridge}.
 */
public interface RoutingBinder {

	/**
	 * Configure the mapping of an indexed entity to an index as necessary using the given {@code context}.
	 * @param context A context exposing methods to configure the mapping.
	 */
	void bind(RoutingBindingContext context);

}
