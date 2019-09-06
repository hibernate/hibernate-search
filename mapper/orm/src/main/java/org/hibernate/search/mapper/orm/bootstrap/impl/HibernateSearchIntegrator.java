/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.search.mapper.orm.bootstrap.impl;

import java.lang.invoke.MethodHandles;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

import org.hibernate.boot.Metadata;
import org.hibernate.boot.spi.BootstrapContext;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.event.service.spi.DuplicationStrategy;
import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.event.spi.EventType;
import org.hibernate.integrator.spi.Integrator;
import org.hibernate.search.engine.cfg.spi.ConfigurationProperty;
import org.hibernate.search.mapper.orm.automaticindexing.AutomaticIndexingStrategyName;
import org.hibernate.search.mapper.orm.cfg.HibernateOrmMapperSettings;
import org.hibernate.search.mapper.orm.cfg.impl.HibernateOrmConfigurationPropertySource;
import org.hibernate.search.mapper.orm.event.impl.HibernateSearchEventListener;
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
			ConfigurationProperty.forKey( HibernateOrmMapperSettings.Radicals.ENABLED )
					.asBoolean()
					.withDefault( HibernateOrmMapperSettings.Defaults.ENABLED )
					.build();

	private static final ConfigurationProperty<AutomaticIndexingStrategyName> AUTOMATIC_INDEXING_STRATEGY =
			ConfigurationProperty.forKey( HibernateOrmMapperSettings.Radicals.AUTOMATIC_INDEXING_STRATEGY )
					.as( AutomaticIndexingStrategyName.class, AutomaticIndexingStrategyName::of )
					.withDefault( HibernateOrmMapperSettings.Defaults.AUTOMATIC_INDEXING_STRATEGY )
					.build();

	private static final ConfigurationProperty<Boolean> DIRTY_CHECK_ENABLED =
			ConfigurationProperty.forKey( HibernateOrmMapperSettings.Radicals.AUTOMATIC_INDEXING_ENABLE_DIRTY_CHECK )
					.asBoolean()
					.withDefault( HibernateOrmMapperSettings.Defaults.AUTOMATIC_INDEXING_ENABLE_DIRTY_CHECK )
					.build();

	@Override
	public void integrate(Metadata metadata, SessionFactoryImplementor sessionFactory, SessionFactoryServiceRegistry serviceRegistry) {
		ConfigurationService configurationService = serviceRegistry.getService( ConfigurationService.class );
		HibernateOrmConfigurationPropertySource propertySource =
				new HibernateOrmConfigurationPropertySource( configurationService );
		if ( ! ENABLED.get( propertySource ) ) {
			log.debug( "Hibernate Search is disabled through configuration properties." );
			return;
		}

		// TODO When we'll move to Hibernate ORM 6, use the bootstrapContext parameter passed to the integrate() method
		BootstrapContext bootstrapContext = ( (MetadataImplementor) metadata ).getTypeConfiguration()
				.getMetadataBuildingContext().getBootstrapContext();
		HibernateOrmIntegrationBooterImpl booter = new HibernateOrmIntegrationBooterImpl( metadata, bootstrapContext );


		// Orchestrate bootstrap and shutdown
		CompletableFuture<SessionFactoryImplementor> sessionFactoryCreatedFuture = new CompletableFuture<>();
		CompletableFuture<?> sessionFactoryClosingFuture = new CompletableFuture<>();
		CompletableFuture<HibernateSearchContextProviderService> contextFuture =
				booter.orchestrateBootAndShutdown( sessionFactoryCreatedFuture, sessionFactoryClosingFuture );

		// Listen to the session factory lifecycle to boot/shutdown Hibernate Search at the right time
		HibernateSearchSessionFactoryObserver observer = new HibernateSearchSessionFactoryObserver(
				sessionFactoryCreatedFuture,
				sessionFactoryClosingFuture,
				contextFuture
		);
		sessionFactory.addObserver( observer );

		// Listen to Hibernate ORM events to index automatically
		AutomaticIndexingStrategyName automaticIndexingStrategyName =
				AUTOMATIC_INDEXING_STRATEGY.get( propertySource );
		if ( AutomaticIndexingStrategyName.SESSION.equals( automaticIndexingStrategyName ) ) {
			log.debug( "Hibernate Search event listeners activated" );
			HibernateSearchEventListener hibernateSearchEventListener = new HibernateSearchEventListener(
					contextFuture.thenApply( Supplier::get ),
					DIRTY_CHECK_ENABLED.get( propertySource )
			);
			registerHibernateSearchEventListener( hibernateSearchEventListener, serviceRegistry );
		}
		else {
			log.debug( "Hibernate Search event listeners deactivated" );
		}
	}

	@Override
	public void disintegrate(SessionFactoryImplementor sessionFactory, SessionFactoryServiceRegistry serviceRegistry) {
		// Nothing to do, Hibernate Search shuts down automatically when the SessionFactory is closed
	}

	private void registerHibernateSearchEventListener(HibernateSearchEventListener eventListener, SessionFactoryServiceRegistry serviceRegistry) {
		EventListenerRegistry listenerRegistry = serviceRegistry.getService( EventListenerRegistry.class );
		listenerRegistry.addDuplicationStrategy( new KeepIfSameClassDuplicationStrategy( HibernateSearchEventListener.class ) );

		listenerRegistry.appendListeners( EventType.POST_INSERT, eventListener );
		listenerRegistry.appendListeners( EventType.POST_UPDATE, eventListener );
		listenerRegistry.appendListeners( EventType.POST_DELETE, eventListener );
		listenerRegistry.appendListeners( EventType.POST_COLLECTION_RECREATE, eventListener );
		listenerRegistry.appendListeners( EventType.POST_COLLECTION_REMOVE, eventListener );
		listenerRegistry.appendListeners( EventType.POST_COLLECTION_UPDATE, eventListener );
		listenerRegistry.appendListeners( EventType.FLUSH, eventListener );
		listenerRegistry.appendListeners( EventType.AUTO_FLUSH, eventListener );
		listenerRegistry.appendListeners( EventType.CLEAR, eventListener );
	}

	public static class KeepIfSameClassDuplicationStrategy implements DuplicationStrategy {
		private final Class<?> checkClass;

		public KeepIfSameClassDuplicationStrategy(Class<?> checkClass) {
			this.checkClass = checkClass;
		}

		@Override
		public boolean areMatch(Object listener, Object original) {
			// not isAssignableFrom since the user could subclass
			return checkClass == original.getClass() && checkClass == listener.getClass();
		}

		@Override
		public Action getAction() {
			return Action.KEEP_ORIGINAL;
		}
	}

}
