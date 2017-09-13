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

public class CDIBeanManagerInitializedSynchronizer {

	private static final Contextual<CDIBeanManagerInitializedSynchronizer> CONTEXTUAL = new Contextual<CDIBeanManagerInitializedSynchronizer>() {
		@Override
		public CDIBeanManagerInitializedSynchronizer create(CreationalContext<CDIBeanManagerInitializedSynchronizer> creationalContext) {
			return new CDIBeanManagerInitializedSynchronizer();
		}

		@Override
		public void destroy(
				CDIBeanManagerInitializedSynchronizer instance,
				CreationalContext<CDIBeanManagerInitializedSynchronizer> creationalContext) {
			creationalContext.release();
		}
	};

	/**
	 * Retrieve the {@link CDIBeanManagerInitializedSynchronizer} from a bean manager.
	 * <p>
	 * We cannot use classic ways to retrieve a CDI bean or an Extension from the bean manager
	 * because WildFly starts Hibernate ORM *before* initializing CDI, and the methods we need
	 * in the bean manager are not available when we create the {@link CDIBeanManagerInitializedSynchronizer}.
	 * <p>
	 * Thus we work it around by abusing the Context API...
	 */
	public static CDIBeanManagerInitializedSynchronizer get(BeanManager beanManager) {
		CreationalContext<CDIBeanManagerInitializedSynchronizer> creationalContext =
				beanManager.createCreationalContext( CONTEXTUAL );
		return beanManager.getContext( Singleton.class ).get( CONTEXTUAL, creationalContext );
	}

	private final CompletableFuture<Void> beanManagerInitialized = new CompletableFuture<>();

	private CDIBeanManagerInitializedSynchronizer() {
	}

	public void whenBeanManagerInitialized(Runnable runnable) {
		beanManagerInitialized.thenRun( runnable );
	}

	public void onBeanManagerInitialized() {
		beanManagerInitialized.complete( null );
	}
}
