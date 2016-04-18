/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.bridge.spi;

import org.hibernate.search.indexes.spi.IndexManager;

/**
 * Bridge provider specific to a given backend.
 *
 * @author Guillaume Smet
 */
public interface BackendSpecificBridgeProvider extends BridgeProvider {

	/**
	 * Returns the backend for which we register the bridge
	 *
	 * @return the backend for which we register the bridge
	 */
	Class<? extends IndexManager> getBackend();

}
