/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.search.mapper.orm.bootstrap.impl;

import java.lang.invoke.MethodHandles;
import java.util.concurrent.CompletableFuture;

import org.hibernate.boot.Metadata;
import org.hibernate.boot.spi.BootstrapContext;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.integrator.spi.Integrator;
import org.hibernate.search.engine.Version;
import org.hibernate.search.engine.cfg.spi.ConfigurationProperty;
import org.hibernate.search.engine.cfg.ConfigurationPropertySource;

import org.hibernate.search.mapper.orm.cfg.HibernateOrmMapperSettings;
import org.hibernate.search.engine.cfg.spi.ConfigurationPropertyChecker;
import org.hibernate.search.mapper.orm.mapping.impl.HibernateSearchContextProviderService;
import org.hibernate.search.mapper.orm.logging.impl.Log;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;
import org.hibernate.service.spi.SessionFactoryServiceRegistry;

/**
 * Integrates Hibernate Search into Hibernate Core by registering its needed listeners
 *
 * @author Emmanuel Bernard
 * @author Hardy Ferentschik
 * @author Steve Ebersole
 */
public class HibernateSearchIntegrator implements Integrator {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private static final ConfigurationProperty<Boolean> ENABLED =
			ConfigurationProperty.forKey( HibernateOrmMapperSettings.ENABLED )
					.asBoolean()
					.withDefault( HibernateOrmMapperSettings.Defaults.ENABLED )
					.build();

	@Override
	public void integrate(Metadata metadata, SessionFactoryImplementor sessionFactory,
			SessionFactoryServiceRegistry serviceRegistry) {
		log.version( Version.versionString() );

		ConfigurationPropertyChecker propertyChecker = ConfigurationPropertyChecker.create();
		ConfigurationPropertySource propertySource = HibernateOrmIntegrationBooterImpl.getPropertySource(
				serviceRegistry, propertyChecker
		);

		if ( ! ENABLED.get( propertySource ) ) {
			log.debug( "Hibernate Search is disabled through configuration properties." );
			return;
		}

		// TODO When we'll move to Hibernate ORM 6, use the bootstrapContext parameter passed to the integrate() method
		BootstrapContext bootstrapContext = ( (MetadataImplementor) metadata ).getTypeConfiguration()
				.getMetadataBuildingContext().getBootstrapContext();
		HibernateOrmIntegrationBooterImpl booter = new HibernateOrmIntegrationBooterImpl.BuilderImpl( metadata, bootstrapContext )
				.configurationPropertySource( propertySource, propertyChecker )
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
