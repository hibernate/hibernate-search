/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.integration.wildfly.cdi.integration;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Initialized;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.BeanManager;
import javax.inject.Inject;
import javax.inject.Singleton;


/**
 * @author Yoann Rodiere
 */
@Singleton
public class CDIObserver {

	@Inject
	private BeanManager beanManager;

	public void onBeanManagerInitialized(@Observes @Initialized(ApplicationScoped.class) Object init) {
		CDIBeanManagerInitializedSynchronizer synchronizer = CDIBeanManagerInitializedSynchronizer.get( beanManager );
		if ( synchronizer != null ) {
			synchronizer.onBeanManagerInitialized();
		}
	}
}
