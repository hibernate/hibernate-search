/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2014, Red Hat, Inc. and/or its affiliates or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat, Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */

package org.hibernate.search.bridge.spi;

import org.hibernate.search.bridge.FieldBridge;
import org.hibernate.search.engine.service.spi.ServiceManager;

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
		 * Provides access to the {@code ServiceManager} and gives access to
		 * Hibernate Search services like the {@code ClassLoaderService}.
		 */
		ServiceManager getServiceManager();
	}
}
