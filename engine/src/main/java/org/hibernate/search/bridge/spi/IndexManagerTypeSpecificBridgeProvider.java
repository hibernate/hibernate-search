/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.bridge.spi;

import org.hibernate.search.indexes.spi.IndexManager;

/**
 * Bridge provider specific to a given {@code IndexManager} type.
 *
 * @author Guillaume Smet
 * @hsearch.experimental : This feature is experimental
 */

public interface IndexManagerTypeSpecificBridgeProvider extends BridgeProvider {

	/**
	 * Returns the {@code IndexManager} type for which we register the bridge
	 *
	 * @return the {@code IndexManager} type for which we register the bridge
	 */
	Class<? extends IndexManager> getIndexManagerType();

}
