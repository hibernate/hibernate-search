/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.hcore.impl;

import java.util.ArrayList;
import java.util.List;
import javax.enterprise.inject.spi.BeanManager;

import org.hibernate.jpa.event.spi.jpa.ExtendedBeanManager;
import org.hibernate.search.hcore.spi.EnvironmentSynchronizer;

/**
 * @author Yoann Rodiere
 */
class ExtendedBeanManagerSynchronizer
		implements EnvironmentSynchronizer, ExtendedBeanManager.LifecycleListener {

	private final List<Runnable> environmentInitializedActions = new ArrayList<>();
	private final List<Runnable> environmentDestroyingActions = new ArrayList<>();

	@Override
	public void whenEnvironmentReady(Runnable action) {
		environmentInitializedActions.add( action );
	}

	@Override
	public void whenEnvironmentDestroying(Runnable action) {
		environmentDestroyingActions.add( action );
	}

	@Override
	public void beanManagerInitialized(BeanManager beanManager) {
		for ( Runnable action : environmentInitializedActions ) {
			action.run();
		}
	}

	@Override
	public void beforeBeanManagerDestroyed(BeanManager beanManager) {
		for ( Runnable action : environmentDestroyingActions ) {
			action.run();
		}
	}
}
