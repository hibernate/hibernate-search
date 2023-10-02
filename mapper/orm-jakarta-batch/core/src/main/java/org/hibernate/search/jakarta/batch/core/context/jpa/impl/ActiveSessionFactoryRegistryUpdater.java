/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.jakarta.batch.core.context.jpa.impl;

import org.hibernate.boot.Metadata;
import org.hibernate.boot.spi.BootstrapContext;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.integrator.spi.Integrator;
import org.hibernate.service.spi.SessionFactoryServiceRegistry;

/**
 * @author Yoann Rodiere
 */
public class ActiveSessionFactoryRegistryUpdater implements Integrator {

	@Override
	public void integrate(Metadata metadata, BootstrapContext bootstrapContext,
			SessionFactoryImplementor sessionFactory) {
		ActiveSessionFactoryRegistry.getInstance().register( sessionFactory );
	}

	@Override
	public void disintegrate(SessionFactoryImplementor sessionFactory, SessionFactoryServiceRegistry serviceRegistry) {
		ActiveSessionFactoryRegistry.getInstance().unregister( sessionFactory );
	}

}
