/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.search.mapper.orm.bootstrap.impl;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.hibernate.boot.Metadata;
import org.hibernate.boot.spi.BootstrapContext;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.integrator.spi.Integrator;
import org.hibernate.search.mapper.orm.common.impl.HibernateOrmUtils;
import org.hibernate.search.mapper.orm.mapping.impl.HibernateSearchContextProviderService;
import org.hibernate.service.spi.SessionFactoryServiceRegistry;

/**
 * Integrates Hibernate Search into Hibernate Core by registering its needed listeners
 *
 * @author Emmanuel Bernard
 * @author Hardy Ferentschik
 * @author Steve Ebersole
 */
public class HibernateSearchIntegrator implements Integrator {

	@Override
	public void integrate(Metadata metadata, BootstrapContext bootstrapContext,
			SessionFactoryImplementor sessionFactory) {
		Optional<HibernateSearchPreIntegrationService> preIntegrationServiceOptional =
				HibernateOrmUtils.getServiceOrEmpty( bootstrapContext.getServiceRegistry(),
						HibernateSearchPreIntegrationService.class );

		if ( !preIntegrationServiceOptional.isPresent() ) {
			// Hibernate Search is disabled
			return;
		}

		HibernateOrmIntegrationBooterImpl booter =
				new HibernateOrmIntegrationBooterImpl.BuilderImpl( metadata, bootstrapContext )
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

	@Override
	public void disintegrate(SessionFactoryImplementor sessionFactory, SessionFactoryServiceRegistry serviceRegistry) {
		// Nothing to do, Hibernate Search shuts down automatically when the SessionFactory is closed
	}

}
