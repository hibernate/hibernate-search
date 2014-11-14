/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.search.bridge.spi;

import org.hibernate.search.bridge.FieldBridge;
import org.hibernate.search.engine.service.classloading.spi.ClassLoaderService;

/**
 * Service interface to implement to allow custom bridges to be
 * auto discovered.
 *
 * It must have a default constructor and a file named
 * {@code META-INF/services/org.hibernate.search.bridge.spi.BridgeProvider}
 * should contain the fully qualified class name of the bridge provider
 * implementation. When several implementations are present in a given JAR,
 * place one class name per line.
 * This follows the JDK service loader pattern.
 *
 * @author Emmanuel Bernard <emmanuel@hibernate.org>
 */
public interface BridgeProvider {

	/**
	 * Return a {@link org.hibernate.search.bridge.FieldBridge} instance if the provider can
	 * build a bridge for the calling context. {@code null} otherwise.
	 */
	FieldBridge provideFieldBridge(BridgeProviderContext bridgeProviderContext);

	interface BridgeProviderContext {

		/**
		 * Member return type seeking a bridge.
		 */
		Class<?> getReturnType();

		/**
		 * Provides access to the {@code ClassLoaderService}.
		 */
		ClassLoaderService getClassLoaderService();
	}
}
