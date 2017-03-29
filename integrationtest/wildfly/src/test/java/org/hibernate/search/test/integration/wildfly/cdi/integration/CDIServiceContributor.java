/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.integration.wildfly.cdi.integration;

import java.util.Map;

import javax.enterprise.inject.spi.BeanManager;

import org.hibernate.boot.registry.StandardServiceInitiator;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.search.hcore.spi.BeanResolver;
import org.hibernate.search.hcore.spi.EnvironmentSynchronizer;
import org.hibernate.service.spi.ServiceContributor;
import org.hibernate.service.spi.ServiceRegistryImplementor;


/**
 * @author Yoann Rodiere
 */
public class CDIServiceContributor implements ServiceContributor {

	@Override
	public void contribute(StandardServiceRegistryBuilder serviceRegistryBuilder) {
		serviceRegistryBuilder.addInitiator( new BeanManagerSynchronizerInitiator() );
		serviceRegistryBuilder.addInitiator( new BeanResolverInitiator() );
	}

	private static BeanManager getBeanManager(Map<?, ?> configurationValues) {
		return (BeanManager) configurationValues.get( AvailableSettings.CDI_BEAN_MANAGER );
	}

	private static class BeanManagerSynchronizerInitiator implements StandardServiceInitiator<EnvironmentSynchronizer> {

		@Override
		public Class<EnvironmentSynchronizer> getServiceInitiated() {
			return EnvironmentSynchronizer.class;
		}

		@Override
		@SuppressWarnings("rawtypes")
		public EnvironmentSynchronizer initiateService(Map configurationValues, ServiceRegistryImplementor registry) {
			BeanManager beanManager = getBeanManager( configurationValues );
			if ( beanManager != null ) {
				return CDIBeanManagerSynchronizer.get( beanManager );
			}
			else {
				return null;
			}
		}

	}

	private static class BeanResolverInitiator implements StandardServiceInitiator<BeanResolver> {

		@Override
		public Class<BeanResolver> getServiceInitiated() {
			return BeanResolver.class;
		}

		@Override
		@SuppressWarnings("rawtypes")
		public BeanResolver initiateService(Map configurationValues, ServiceRegistryImplementor registry) {
			BeanManager beanManager = getBeanManager( configurationValues );
			if ( beanManager != null ) {
				return new CDIBeanResolver( beanManager );
			}
			else {
				return null;
			}
		}

	}
}
