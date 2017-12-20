/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.hcore.impl;

import java.util.Map;

import org.hibernate.boot.registry.StandardServiceInitiator;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.resource.beans.container.spi.ExtendedBeanManager;
import org.hibernate.search.hcore.spi.EnvironmentSynchronizer;
import org.hibernate.service.spi.ServiceContributor;
import org.hibernate.service.spi.ServiceRegistryImplementor;


/**
 * @author Yoann Rodiere
 */
public final class ExtendedBeanManagerSynchronizerContributor implements ServiceContributor {

	@Override
	public void contribute(StandardServiceRegistryBuilder serviceRegistryBuilder) {
		serviceRegistryBuilder.addInitiator( new ExtendedBeanManagerSynchronizerInitiator() );
	}

	private static class ExtendedBeanManagerSynchronizerInitiator implements StandardServiceInitiator<EnvironmentSynchronizer> {

		@Override
		public Class<EnvironmentSynchronizer> getServiceInitiated() {
			return EnvironmentSynchronizer.class;
		}

		@Override
		@SuppressWarnings("rawtypes")
		public EnvironmentSynchronizer initiateService(Map configurationValues, ServiceRegistryImplementor registry) {
			Object unknown = configurationValues.get( AvailableSettings.CDI_BEAN_MANAGER );
			if ( unknown instanceof ExtendedBeanManager ) {
				ExtendedBeanManager extendedBeanManager = (ExtendedBeanManager) unknown;
				ExtendedBeanManagerSynchronizer synchronizer = new ExtendedBeanManagerSynchronizer();
				extendedBeanManager.registerLifecycleListener( synchronizer );
				return synchronizer;
			}
			else {
				return null;
			}
		}

	}

}
