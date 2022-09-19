/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.orm.cdi;

import java.util.ArrayList;
import java.util.List;

import jakarta.enterprise.inject.se.SeContainer;
import jakarta.enterprise.inject.se.SeContainerInitializer;

import org.hibernate.resource.beans.container.spi.ExtendedBeanManager;

class StubExtendedBeanManager implements ExtendedBeanManager {

	private SeContainer cdiContainer;

	private final List<LifecycleListener> lifecycleListeners = new ArrayList<>();

	@Override
	public void registerLifecycleListener(LifecycleListener lifecycleListener) {
		this.lifecycleListeners.add( lifecycleListener );
	}

	public void simulateBoot(Class<?>... beanClasses) {
		SeContainerInitializer cdiInitializer = SeContainerInitializer.newInstance()
				.disableDiscovery()
				.addBeanClasses( beanClasses );
		this.cdiContainer = cdiInitializer.initialize();
		for ( LifecycleListener lifecycleListener : lifecycleListeners ) {
			lifecycleListener.beanManagerInitialized( cdiContainer.getBeanManager() );
		}
	}

	public void simulateCancelledBoot(Class<?>... beanClasses) {
		SeContainerInitializer cdiInitializer = SeContainerInitializer.newInstance()
				.disableDiscovery()
				.addBeanClasses( beanClasses );
		this.cdiContainer = cdiInitializer.initialize();
		// Let's say some other bean fails to initialize: we will effectively cancel Hibernate Search's boot.
		// Notify Hibernate Search of the destruction before we even notify of initialization.
		for ( LifecycleListener lifecycleListener : lifecycleListeners ) {
			lifecycleListener.beforeBeanManagerDestroyed( cdiContainer.getBeanManager() );
		}
		cdiContainer.close();
		cdiContainer = null;
	}

	public void simulateShutdown() {
		for ( LifecycleListener lifecycleListener : lifecycleListeners ) {
			lifecycleListener.beforeBeanManagerDestroyed( cdiContainer.getBeanManager() );
		}
		cdiContainer.close();
		cdiContainer = null;
	}

	public void cleanUp() {
		if ( cdiContainer != null ) {
			cdiContainer.close();
			cdiContainer = null;
		}
	}
}
