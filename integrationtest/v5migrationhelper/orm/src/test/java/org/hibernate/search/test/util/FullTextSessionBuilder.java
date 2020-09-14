/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.Session;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.event.spi.EventType;
import org.hibernate.event.spi.LoadEventListener;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.SearchFactory;
import org.hibernate.search.hcore.util.impl.ContextHelper;
import org.hibernate.search.impl.ImplementationFactory;
import org.hibernate.search.test.testsupport.V5MigrationHelperOrmSetupHelper;
import org.hibernate.service.spi.ServiceRegistryImplementor;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

/**
 * Use the builder pattern to provide a SessionFactory.
 * This is meant to use only in-memory index and databases, for those test
 * which need to use several differently configured SessionFactories.
 *
 * @author Sanne Grinovero
 * @author Hardy Ferentschik
 */
public class FullTextSessionBuilder implements AutoCloseable, TestRule {

	private final V5MigrationHelperOrmSetupHelper setupHelper = V5MigrationHelperOrmSetupHelper.create();

	private final Map<String, Object> cfg = new HashMap<>();
	private final Set<Class<?>> annotatedClasses = new HashSet<>();
	private SessionFactoryImplementor sessionFactory;
	private final List<LoadEventListener> additionalLoadEventListeners = new ArrayList<>();

	public FullTextSessionBuilder() {
		cfg.put( AvailableSettings.USE_NEW_ID_GENERATOR_MAPPINGS, "true" ); //As in ORM testsuite
	}

	/**
	 * Override before building any parameter, or add new ones.
	 *
	 * @param key Property name.
	 * @param value Property value.
	 *
	 * @return the same builder (this).
	 */
	public FullTextSessionBuilder setProperty(String key, String value) {
		cfg.put( key, value );
		return this;
	}

	/**
	 * Adds classes to the SessionFactory being built.
	 *
	 * @param annotatedClass The annotated class to add to the configuration.
	 *
	 * @return the same builder (this)
	 */
	public FullTextSessionBuilder addAnnotatedClass(Class annotatedClass) {
		annotatedClasses.add( annotatedClass );
		return this;
	}

	/**
	 * @return a new FullTextSession based upon the built configuration.
	 */
	public FullTextSession openFullTextSession() {
		if ( sessionFactory == null ) {
			build();
		}
		Session session = sessionFactory.openSession();
		return Search.getFullTextSession( session );
	}

	/**
	 * Closes the SessionFactory.
	 * Make sure you close all sessions first
	 */
	@Override
	public void close() {
		if ( sessionFactory == null ) {
			throw new java.lang.IllegalStateException( "sessionFactory not yet built" );
		}
		try {
			sessionFactory.close();
		}
		finally {
			sessionFactory = null;
		}
	}

	/**
	 * Builds the sessionFactory as configured so far.
	 */
	public FullTextSessionBuilder build() {
		V5MigrationHelperOrmSetupHelper.SetupContext setupContext = setupHelper.start();

		setupContext = setupContext.withProperties( cfg );

		setupContext = setupContext.withConfiguration( builder -> builder.addAnnotatedClasses( annotatedClasses ) );

		sessionFactory = setupContext.setup().unwrap( SessionFactoryImplementor.class );

		ServiceRegistryImplementor serviceRegistryImplementor = sessionFactory.getServiceRegistry();
		EventListenerRegistry registry = serviceRegistryImplementor.getService( EventListenerRegistry.class );

		for ( LoadEventListener listener : additionalLoadEventListeners ) {
			registry.getEventListenerGroup( EventType.LOAD ).appendListener( listener );
		}

		return this;
	}

	/**
	 * @return the SearchFactory
	 */
	public SearchFactory getSearchFactory() {
		if ( sessionFactory == null ) {
			build();
		}
		return ImplementationFactory.createSearchFactory( ContextHelper.getSearchIntegratorBySFI( sessionFactory ) );
	}

	public FullTextSessionBuilder addLoadEventListener(LoadEventListener additionalLoadEventListener) {
		additionalLoadEventListeners.add( additionalLoadEventListener );
		return this;
	}

	@Override
	public Statement apply(final Statement base, Description description) {
		Statement wrapped = new Statement() {
			@Override
			public void evaluate() throws Throwable {
				build();
				try {
					base.evaluate();
				}
				finally {
					close();
				}
			}
		};
		return setupHelper.apply( wrapped, description );
	}

}
