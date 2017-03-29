/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.search.hcore.impl;

import org.hibernate.boot.Metadata;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.engine.jndi.spi.JndiService;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.event.service.spi.DuplicationStrategy;
import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.event.spi.EventType;
import org.hibernate.integrator.spi.Integrator;
import org.hibernate.search.event.impl.FullTextIndexEventListener;
import org.hibernate.search.hcore.spi.EnvironmentSynchronizer;
import org.hibernate.search.hcore.spi.BeanResolver;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;
import org.hibernate.service.spi.ServiceBinding;
import org.hibernate.service.spi.SessionFactoryServiceRegistry;

/**
 * Integrates Hibernate Search into Hibernate Core by registering its needed listeners
 *
 * @author Emmanuel Bernard
 * @author Hardy Ferentschik
 * @author Steve Ebersole
 */
public class HibernateSearchIntegrator implements Integrator {

	private static final Log log = LoggerFactory.make();

	public static final String AUTO_REGISTER = "hibernate.search.autoregister_listeners";

	@Override
	public void integrate(Metadata metadata, SessionFactoryImplementor sessionFactory, SessionFactoryServiceRegistry serviceRegistry) {
		ConfigurationService configurationService = serviceRegistry.getService( ConfigurationService.class );
		JndiService namingService = serviceRegistry.getService( JndiService.class );

		if ( ! hibernateSearchNeedsToBeEnabled( configurationService ) ) {
			return;
		}

		FullTextIndexEventListener fullTextIndexEventListener = new FullTextIndexEventListener();
		registerHibernateSearchEventListener( fullTextIndexEventListener, serviceRegistry );

		ClassLoaderService hibernateOrmClassLoaderService = serviceRegistry.getService( ClassLoaderService.class );
		ServiceBinding<EnvironmentSynchronizer> environmentSynchronizerBinding = serviceRegistry.locateServiceBinding( EnvironmentSynchronizer.class );
		ServiceBinding<BeanResolver> hibernateOrmBeanResolverBinding = serviceRegistry.locateServiceBinding( BeanResolver.class );
		HibernateSearchSessionFactoryObserver observer = new HibernateSearchSessionFactoryObserver(
				metadata,
				configurationService,
				fullTextIndexEventListener,
				hibernateOrmClassLoaderService,
				environmentSynchronizerBinding == null ? null : serviceRegistry.getService( EnvironmentSynchronizer.class ),
				hibernateOrmBeanResolverBinding == null ? null : serviceRegistry.getService( BeanResolver.class ),
				namingService
		);

		sessionFactory.addObserver( observer );
	}

	@Override
	public void disintegrate(SessionFactoryImplementor sessionFactory, SessionFactoryServiceRegistry serviceRegistry) {
	}

	private boolean hibernateSearchNeedsToBeEnabled(ConfigurationService configurationService) {
		final Boolean enableHibernateSearch = configurationService.getSetting( AUTO_REGISTER, org.hibernate.engine.config.spi.StandardConverters.BOOLEAN, true );
		if ( !enableHibernateSearch ) {
			log.debug( "Skipping Hibernate Search event listener auto registration" );
		}
		return enableHibernateSearch;
	}

	private void registerHibernateSearchEventListener(FullTextIndexEventListener eventListener, SessionFactoryServiceRegistry serviceRegistry) {
		EventListenerRegistry listenerRegistry = serviceRegistry.getService( EventListenerRegistry.class );
		listenerRegistry.addDuplicationStrategy( new DuplicationStrategyImpl( FullTextIndexEventListener.class ) );

		listenerRegistry.appendListeners( EventType.POST_INSERT, eventListener );
		listenerRegistry.appendListeners( EventType.POST_UPDATE, eventListener );
		listenerRegistry.appendListeners( EventType.POST_DELETE, eventListener );
		listenerRegistry.appendListeners( EventType.POST_COLLECTION_RECREATE, eventListener );
		listenerRegistry.appendListeners( EventType.POST_COLLECTION_REMOVE, eventListener );
		listenerRegistry.appendListeners( EventType.POST_COLLECTION_UPDATE, eventListener );
		listenerRegistry.appendListeners( EventType.FLUSH, eventListener );
	}

	public static class DuplicationStrategyImpl implements DuplicationStrategy {
		private final Class checkClass;

		public DuplicationStrategyImpl(Class checkClass) {
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
