/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */

package org.hibernate.search.mapper.orm.bootstrap.impl;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.hibernate.boot.Metadata;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.integrator.spi.Integrator;
import org.hibernate.search.mapper.orm.common.impl.HibernateOrmUtils;
import org.hibernate.search.mapper.orm.mapping.impl.HibernateSearchContextProviderService;

/**
 * Integrates Hibernate Search into Hibernate Core by registering its needed listeners
 *
 * @author Emmanuel Bernard
 * @author Hardy Ferentschik
 * @author Steve Ebersole
 */
public class HibernateSearchIntegrator implements Integrator {

	@Override
	public void integrate(Metadata metadata,
			Context context,
			SessionFactoryImplementor sessionFactory) {
		Optional<HibernateSearchPreIntegrationService> preIntegrationServiceOptional =
				HibernateOrmUtils.getServiceOrEmpty( sessionFactory.getServiceRegistry(),
						HibernateSearchPreIntegrationService.class );

		if ( !preIntegrationServiceOptional.isPresent() ) {
			// Hibernate Search is disabled
			return;
		}

		HibernateOrmIntegrationBooterImpl booter =
				new HibernateOrmIntegrationBooterImpl.BuilderImpl( metadata, sessionFactory.getServiceRegistry(),
						context.getClassDetailsRegistry() )
						.build();
		// Orchestrate bootstrap and shutdown
		CompletableFuture<SessionFactoryImplementor> sessionFactoryCreatedFuture = new CompletableFuture<>();
		CompletableFuture<?> sessionFactoryClosingFuture = new CompletableFuture<>();
		CompletableFuture<HibernateSearchContextProviderService> contextFuture =
				booter.orchestrateBootAndShutdown( sessionFactoryCreatedFuture, sessionFactoryClosingFuture );

		// Listen to the session factory lifecycle to boot/shutdown Hibernate Search at the right time
		HibernateSearchSessionFactoryObserver observer = new HibernateSearchSessionFactoryObserver(
				contextFuture, sessionFactoryCreatedFuture, sessionFactoryClosingFuture
		);
		sessionFactory.addObserver( observer );
	}
}
