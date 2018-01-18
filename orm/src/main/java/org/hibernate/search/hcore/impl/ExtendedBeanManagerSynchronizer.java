/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.hcore.impl;

import java.util.concurrent.CompletableFuture;
import javax.enterprise.inject.spi.BeanManager;

import org.hibernate.jpa.event.spi.jpa.ExtendedBeanManager;
import org.hibernate.search.hcore.spi.EnvironmentSynchronizer;

/**
 * @author Yoann Rodiere
 */
class ExtendedBeanManagerSynchronizer
		implements EnvironmentSynchronizer, ExtendedBeanManager.LifecycleListener {

	private final CompletableFuture<Void> environmentInitialized = new CompletableFuture<>();

	private final CompletableFuture<Void> environmentDestroying = new CompletableFuture<>();

	@Override
	public void whenEnvironmentReady(Runnable runnable) {
		environmentInitialized.thenRun( runnable );
	}

	@Override
	public void whenEnvironmentDestroying(Runnable runnable) {
		environmentDestroying.thenRun( runnable );
	}

	@Override
	public void beanManagerInitialized(BeanManager beanManager) {
		environmentInitialized.complete( null );
	}

	@Override
	public void beforeBeanManagerDestroyed(BeanManager beanManager) {
		environmentDestroying.complete( null );
	}
}
