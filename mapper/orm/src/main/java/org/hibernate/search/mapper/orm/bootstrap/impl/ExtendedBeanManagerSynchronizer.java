/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.orm.bootstrap.impl;

import java.util.ArrayList;
import java.util.List;

import jakarta.enterprise.inject.spi.BeanManager;

import org.hibernate.resource.beans.container.spi.ExtendedBeanManager;
import org.hibernate.search.mapper.orm.spi.EnvironmentSynchronizer;

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
