/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat, Inc. and/or its affiliates or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat, Inc.
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

	@Override
	public void integrate(
			Configuration configuration,
			SessionFactoryImplementor sessionFactory,
			SessionFactoryServiceRegistry serviceRegistry) {

		if ( !hibernateSearchNeedsToBeEnabled( configuration ) ) {
			return;
		}

		FullTextIndexEventListener fullTextIndexEventListener = new FullTextIndexEventListener();
		registerHibernateSearchEventListener( fullTextIndexEventListener, serviceRegistry );

		HibernateSearchSessionFactoryObserver observer = new HibernateSearchSessionFactoryObserver(
				configuration,
				fullTextIndexEventListener
		);
		sessionFactory.addObserver( observer );
	}

	@Override
	public void integrate(MetadataImplementor metadata, SessionFactoryImplementor sessionFactory, SessionFactoryServiceRegistry serviceRegistry) {
		// todo - HSEARCH-856
	}

	@Override
	public void disintegrate(SessionFactoryImplementor sessionFactory, SessionFactoryServiceRegistry serviceRegistry) {
	}

	private boolean hibernateSearchNeedsToBeEnabled(Configuration configuration) {
		final boolean enableHibernateSearch = ConfigurationHelper.getBoolean(
				AUTO_REGISTER,
				configuration.getProperties(),
				true
		);
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
