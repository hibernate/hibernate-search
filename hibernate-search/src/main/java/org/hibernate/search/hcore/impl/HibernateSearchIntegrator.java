/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.search.hcore.impl;

import org.hibernate.cfg.Configuration;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.event.service.spi.DuplicationStrategy;
import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.event.spi.EventType;
import org.hibernate.integrator.spi.Integrator;
import org.hibernate.internal.util.config.ConfigurationHelper;
import org.hibernate.metamodel.source.MetadataImplementor;
import org.hibernate.search.event.impl.FullTextIndexEventListener;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;
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

	private FullTextIndexEventListener listener;

	@Override
	public void integrate(
			Configuration configuration,
			SessionFactoryImplementor sessionFactory,
			SessionFactoryServiceRegistry serviceRegistry) {
		final boolean registerListeners = ConfigurationHelper.getBoolean(
				AUTO_REGISTER,
				configuration.getProperties(),
				true
		);
		if ( !registerListeners ) {
			log.debug( "Skipping Hibernate Search event listener auto registration" );
			return;
		}

		listener = new FullTextIndexEventListener( FullTextIndexEventListener.Installation.SINGLE_INSTANCE );

		EventListenerRegistry listenerRegistry = serviceRegistry.getService( EventListenerRegistry.class );
		//TODO if the event is duplicated, do not initialize the newly created listener
		listenerRegistry.addDuplicationStrategy( new DuplicationStrategyImpl( FullTextIndexEventListener.class ) );

		listenerRegistry.getEventListenerGroup( EventType.POST_INSERT ).appendListener( listener );
		listenerRegistry.getEventListenerGroup( EventType.POST_UPDATE ).appendListener( listener );
		listenerRegistry.getEventListenerGroup( EventType.POST_DELETE ).appendListener( listener );
		listenerRegistry.getEventListenerGroup( EventType.POST_COLLECTION_RECREATE ).appendListener( listener );
		listenerRegistry.getEventListenerGroup( EventType.POST_COLLECTION_REMOVE ).appendListener( listener );
		listenerRegistry.getEventListenerGroup( EventType.POST_COLLECTION_UPDATE ).appendListener( listener );
		listenerRegistry.getEventListenerGroup( EventType.FLUSH ).appendListener( listener );

		listener.initialize( configuration );
	}

	@Override
	public void integrate(MetadataImplementor metadata, SessionFactoryImplementor sessionFactory, SessionFactoryServiceRegistry serviceRegistry) {
		// todo - HSEARCH-856
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

	@Override
	public void disintegrate(SessionFactoryImplementor sessionFactory, SessionFactoryServiceRegistry serviceRegistry) {
		if ( listener != null ) {
			listener.cleanup();
		}
	}
}
