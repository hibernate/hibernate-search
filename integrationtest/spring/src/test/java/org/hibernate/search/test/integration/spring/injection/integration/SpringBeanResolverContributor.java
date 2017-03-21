/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.integration.spring.injection.integration;

import java.util.Map;

import org.hibernate.boot.registry.StandardServiceInitiator;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.search.hcore.spi.BeanResolver;
import org.hibernate.service.spi.ServiceContributor;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.springframework.beans.factory.BeanFactory;


/**
 * @author Yoann Rodiere
 */
public class SpringBeanResolverContributor implements ServiceContributor {

	public static final String BEAN_FACTORY = "hibernate.spring.bean_factory";

	@Override
	public void contribute(StandardServiceRegistryBuilder serviceRegistryBuilder) {
		serviceRegistryBuilder.addInitiator( new Initiator() );
	}

	private static class Initiator implements StandardServiceInitiator<BeanResolver> {

		@Override
		public Class<BeanResolver> getServiceInitiated() {
			return BeanResolver.class;
		}

		@Override
		@SuppressWarnings("rawtypes")
		public BeanResolver initiateService(Map configurationValues, ServiceRegistryImplementor registry) {
			BeanFactory beanFactory = (BeanFactory) configurationValues.get( BEAN_FACTORY );
			if ( beanFactory == null ) {
				return null;
			}
			else {
				return new SpringBeanResolver( beanFactory );
			}
		}

	}
}
