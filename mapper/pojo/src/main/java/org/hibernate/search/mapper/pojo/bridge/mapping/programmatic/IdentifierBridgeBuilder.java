/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.bridge.mapping.programmatic;

import org.hibernate.search.mapper.pojo.bridge.IdentifierBridge;
import org.hibernate.search.mapper.pojo.bridge.binding.IdentifierBindingContext;

/**
 * A builder of {@link IdentifierBridge}.
 *
 * @see IdentifierBridge
 */
public interface IdentifierBridgeBuilder {

	/**
	 * Binds a POJO property to a document identifier.
	 * <p>
	 * The context passed in parameter provides various information about the identifier being bound.
	 * Implementations are expected to take advantage of that information
	 * and to call one of the {@code setBridge(...)} methods on the context
	 * to set the bridge.
	 *
	 * @param context A context object providing information about the identifier being bound,
	 * and expecting a call to one of its {@code setBridge(...)} methods.
	 */
	void bind(IdentifierBindingContext<?> context);

}
