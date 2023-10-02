/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.test.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.Session;
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
import org.hibernate.search.util.impl.test.extension.AbstractScopeTrackingExtension;
import org.hibernate.search.util.impl.test.extension.ExtensionScope;
import org.hibernate.service.spi.ServiceRegistryImplementor;

import org.junit.jupiter.api.extension.ExtensionContext;

/**
 * Use the builder pattern to provide a SessionFactory.
 * This is meant to use only in-memory index and databases, for those test
 * which need to use several differently configured SessionFactories.
 *
 * @author Sanne Grinovero
 * @author Hardy Ferentschik
 */
public class FullTextSessionBuilder extends AbstractScopeTrackingExtension {

	private final V5MigrationHelperOrmSetupHelper setupHelper = V5MigrationHelperOrmSetupHelper.create();

	private final Map<String, Object> cfg = new HashMap<>();
	private final Set<Class<?>> annotatedClasses = new HashSet<>();
	private final Map<ExtensionScope, SessionFactoryImplementor> sessionFactory = new HashMap<>();
	private final List<LoadEventListener> additionalLoadEventListeners = new ArrayList<>();

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
		if ( sessionFactory() == null ) {
			build();
		}
		Session session = sessionFactory().openSession();
		return Search.getFullTextSession( session );
	}

	/**
	 * Builds the sessionFactory as configured so far.
	 */
	public FullTextSessionBuilder build() {
		V5MigrationHelperOrmSetupHelper.SetupContext setupContext = setupHelper.start();

		setupContext = setupContext.withProperties( cfg );

		setupContext = setupContext.withConfiguration( builder -> builder.addAnnotatedClasses( annotatedClasses ) );

		currentSessionFactory( setupContext.setup().unwrap( SessionFactoryImplementor.class ) );

		ServiceRegistryImplementor serviceRegistryImplementor = sessionFactory().getServiceRegistry();
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
		if ( sessionFactory() == null ) {
			build();
		}
		return ImplementationFactory.createSearchFactory( ContextHelper.getSearchIntegratorBySFI( sessionFactory() ) );
	}

	public FullTextSessionBuilder addLoadEventListener(LoadEventListener additionalLoadEventListener) {
		additionalLoadEventListeners.add( additionalLoadEventListener );
		return this;
	}

	@Override
	protected void actualAfterEach(ExtensionContext extensionContext) throws Exception {
		currentSessionFactory( null );
		setupHelper.afterEach( extensionContext );
	}

	@Override
	protected void actualBeforeEach(ExtensionContext extensionContext) throws Exception {
		setupHelper.beforeEach( extensionContext );
	}

	@Override
	protected void actualAfterAll(ExtensionContext extensionContext) throws Exception {
		currentSessionFactory( null );
		setupHelper.afterAll( extensionContext );
	}

	@Override
	protected void actualBeforeAll(ExtensionContext extensionContext) throws Exception {
		setupHelper.beforeAll( extensionContext );
	}

	private SessionFactoryImplementor sessionFactory() {
		Collection<SessionFactoryImplementor> values = sessionFactory.values();
		return values.isEmpty() ? null : values.iterator().next();
	}

	private void currentSessionFactory(SessionFactoryImplementor sessionFactory) {
		SessionFactoryImplementor removed = this.sessionFactory.put( currentScope(), sessionFactory );
		if ( removed != null ) {
			removed.close();
		}
	}
}
