/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.bridge.mapping;

import org.hibernate.search.engine.common.spi.BuildContext;

public interface BridgeBuilder<B> {

	/**
	 * Build a bridge.
	 * <p>
	 * <strong>Warning:</strong> this method can be called multiple times and must return a new instance to each call.
	 *
	 * @return A new bridge instance.
	 */
	B build(BuildContext buildContext);

}
