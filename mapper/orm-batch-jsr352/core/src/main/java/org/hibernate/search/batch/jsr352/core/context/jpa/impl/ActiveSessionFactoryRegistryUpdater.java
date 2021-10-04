/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.batch.jsr352.core.context.jpa.impl;

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
