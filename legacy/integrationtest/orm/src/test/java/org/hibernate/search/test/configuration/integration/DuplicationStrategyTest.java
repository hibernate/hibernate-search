/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.configuration.integration;

import java.util.Collections;

import org.hibernate.SessionFactory;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.internal.BootstrapContextImpl;
import org.hibernate.boot.internal.MetadataBuilderImpl;
import org.hibernate.boot.internal.SessionFactoryOptionsBuilder;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.spi.MetadataBuildingOptions;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.event.service.internal.EventListenerServiceInitiator;
import org.hibernate.event.service.spi.EventListenerGroup;
import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.event.spi.EventType;
import org.hibernate.event.spi.PostInsertEventListener;
import org.hibernate.search.event.impl.FullTextIndexEventListener;
import org.hibernate.search.hcore.impl.HibernateSearchIntegrator;
import org.hibernate.service.internal.SessionFactoryServiceRegistryImpl;
import org.hibernate.service.spi.ServiceRegistryImplementor;

import org.junit.Test;

import static org.junit.Assert.assertTrue;

/**
 * @author Hardy Ferentschik
 */
public class DuplicationStrategyTest {

	@Test
	public void testMultipleRegistrationOfEventListenerKeepsOriginalListener() {

		final StandardServiceRegistry serviceRegistry = new StandardServiceRegistryBuilder().build();
		final Metadata metadata = new MetadataSources( serviceRegistry ).buildMetadata();
		final SessionFactory sessionFactory = metadata.getSessionFactoryBuilder().build();
		final SessionFactoryImplementor sessionFactoryImplementor = (SessionFactoryImplementor) sessionFactory;
		final MetadataBuildingOptions buildingOptions = new MetadataBuilderImpl.MetadataBuildingOptionsImpl( serviceRegistry );
		final BootstrapContextImpl ctx = new BootstrapContextImpl( serviceRegistry, buildingOptions );
		final SessionFactoryOptionsBuilder sfOptions = new SessionFactoryOptionsBuilder( serviceRegistry, ctx );
		SessionFactoryServiceRegistryImpl sessionFactoryServiceRegistry = new SessionFactoryServiceRegistryImpl(
				(ServiceRegistryImplementor)serviceRegistry, Collections.emptyList(), Collections.emptyList(),
				sessionFactoryImplementor, ctx, sfOptions
		);

		EventListenerRegistry eventListenerRegistry = EventListenerServiceInitiator.INSTANCE.initiateService( sessionFactoryServiceRegistry );

		EventListenerGroup<PostInsertEventListener> eventEventListenerGroup = eventListenerRegistry.getEventListenerGroup(
				EventType.POST_INSERT
		);
		//Remove all default listeners:
		eventListenerRegistry.setListeners( EventType.POST_INSERT, new PostInsertEventListener[]{} );

		assertTrue( "We should start off with no listeners", eventEventListenerGroup.count() == 0 );

		FullTextIndexEventListener firstFullTextIndexEventListener = new FullTextIndexEventListener();

		eventListenerRegistry.setListeners( EventType.POST_INSERT, firstFullTextIndexEventListener );
		eventListenerRegistry.addDuplicationStrategy(
				new HibernateSearchIntegrator.DuplicationStrategyImpl(
						FullTextIndexEventListener.class
				)
		);
		eventListenerRegistry.appendListeners( EventType.POST_INSERT, new FullTextIndexEventListener() );

		assertTrue(
				"We should only have one listener, but we have " + eventEventListenerGroup.count(),
				eventEventListenerGroup.count() == 1
		);
		assertTrue(
				"The instances should match",
				firstFullTextIndexEventListener == eventEventListenerGroup.listeners().iterator().next()
		);
	}

}


