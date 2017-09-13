/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.integration.wildfly.cdi.integration;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import javax.enterprise.inject.spi.BeanManager;

import org.hibernate.search.hcore.spi.EnvironmentSynchronizer;

/**
 * @author Yoann Rodiere
 */
public class CDIEnvironmentSynchronizer implements EnvironmentSynchronizer {

	private final CompletableFuture<BeanManager> beanManagerCreated = new CompletableFuture<>();

	private final CompletableFuture<Void> environmentInitialized = new CompletableFuture<>();

	public CDIEnvironmentSynchronizer() {
		beanManagerCreated.thenAccept( beanManager -> {
			CDIBeanManagerInitializedSynchronizer.get( beanManager )
					.whenBeanManagerInitialized( () -> environmentInitialized.complete( null ) );
		} );
	}

	void whenBeanManagerCreated(Consumer<BeanManager> consumer) {
		beanManagerCreated.thenAccept( consumer );
	}

	@Override
	public void whenEnvironmentReady(Runnable runnable) {
		environmentInitialized.thenRun( runnable );
	}

	public void onBeanManagerCreated(BeanManager beanManager) {
		beanManagerCreated.complete( beanManager );
	}

}
