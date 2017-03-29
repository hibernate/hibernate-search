/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.integration.wildfly.cdi.integration;

import java.util.concurrent.CompletableFuture;

import javax.enterprise.context.spi.Contextual;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.BeanManager;
import javax.inject.Singleton;

import org.hibernate.search.hcore.spi.EnvironmentSynchronizer;

/**
 * @author Yoann Rodiere
 */
public class CDIBeanManagerSynchronizer implements EnvironmentSynchronizer {

	private static final Contextual<CDIBeanManagerSynchronizer> CONTEXTUAL = new Contextual<CDIBeanManagerSynchronizer>() {
		@Override
		public CDIBeanManagerSynchronizer create(CreationalContext<CDIBeanManagerSynchronizer> creationalContext) {
			return new CDIBeanManagerSynchronizer();
		}
		@Override
		public void destroy(CDIBeanManagerSynchronizer instance, CreationalContext<CDIBeanManagerSynchronizer> creationalContext) {
			creationalContext.release();
		}
	};

	/**
	 * Retrieve the {@link CDIBeanManagerSynchronizer} from a bean manager.
	 * <p>
	 * We cannot use classic ways to retrieve a CDI bean or an Extension from the bean manager
	 * because WildFly starts Hibernate ORM *before* initializing CDI, and the methods we need
	 * in the bean manager are not available when we create the {@link CDIBeanManagerSynchronizer}.
	 * <p>
	 * Thus we work it around by abusing the Context API...
	 */
	public static CDIBeanManagerSynchronizer get(BeanManager beanManager) {
		CreationalContext<CDIBeanManagerSynchronizer> creationalContext = beanManager.createCreationalContext( CONTEXTUAL );
		return beanManager.getContext( Singleton.class ).get( CONTEXTUAL, creationalContext );
	}

	private CDIBeanManagerSynchronizer() {
		// Private constructor; use get(BeanManager) instead
	}

	private final CompletableFuture<Void> beanManagerInitialization = new CompletableFuture<>();

	@Override
	public void whenEnvironmentReady(Runnable runnable) {
		beanManagerInitialization.thenRun( runnable );
	}

	public void onBeanManagerInitialized() {
		beanManagerInitialization.complete( null );
	}
}
