/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.util;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.apache.lucene.analysis.core.StopAnalyzer;
import org.hibernate.Session;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.event.spi.EventType;
import org.hibernate.event.spi.LoadEventListener;
import org.hibernate.internal.SessionFactoryImpl;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.SearchFactory;
import org.hibernate.search.hcore.util.impl.ContextHelper;
import org.hibernate.search.impl.ImplementationFactory;
import org.hibernate.search.cfg.SearchMapping;
import org.hibernate.search.testsupport.TestConstants;
import org.hibernate.search.util.impl.FileHelper;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.service.ServiceRegistryBuilder;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.hibernate.testing.cache.CachingRegionFactory;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

/**
 * Use the builder pattern to provide a SessionFactory.
 * This is meant to use only ram-based index and databases, for those test
 * which need to use several differently configured SessionFactories.
 *
 * @author Sanne Grinovero
 * @author Hardy Ferentschik
 */
public class FullTextSessionBuilder implements AutoCloseable, TestRule {

	private static final Log log = org.hibernate.search.util.logging.impl.LoggerFactory.make();

	private File indexRootDirectory;
	private final Properties cfg = new Properties();
	private final Set<Class<?>> annotatedClasses = new HashSet<Class<?>>();
	private SessionFactoryImplementor sessionFactory;
	private boolean usingFileSystem = false;
	private final List<LoadEventListener> additionalLoadEventListeners = new ArrayList<LoadEventListener>();

	public FullTextSessionBuilder() {
		cfg.setProperty( "hibernate.search.lucene_version", TestConstants.getTargetLuceneVersion().toString() );
		cfg.setProperty( Environment.HBM2DDL_AUTO, "create-drop" );

		//cache:
		cfg.setProperty( Environment.USE_SECOND_LEVEL_CACHE, "true" );
		cfg.setProperty(
				Environment.CACHE_REGION_FACTORY,
				CachingRegionFactory.class.getCanonicalName()
		);
		cfg.setProperty( Environment.USE_QUERY_CACHE, "true" );

		//search specific:
		cfg.setProperty(
				org.hibernate.search.cfg.Environment.ANALYZER_CLASS,
				StopAnalyzer.class.getName()
		);
		cfg.setProperty( "hibernate.search.default.directory_provider", "ram" );
		usingFileSystem = false;
	}

	/**
	 * Store indexes permanently in FSDirectory. Helper to automatically cleanup
	 * the filesystem when the builder is closed; alternatively you could just use
	 * properties directly and clean the filesystem explicitly.
	 *
	 * @param testClass needed to locate an appropriate temporary directory
	 * @return the same builder (this).
	 */
	public FullTextSessionBuilder useFileSystemDirectoryProvider(Class<?> testClass) {
		indexRootDirectory = new File( TestConstants.getIndexDirectory( testClass ) );
		log.debugf( "Using %s as index directory.", indexRootDirectory.getAbsolutePath() );
		cfg.setProperty( "hibernate.search.default.directory_provider", "filesystem" );
		cfg.setProperty( "hibernate.search.default.indexBase", indexRootDirectory.getAbsolutePath() );
		usingFileSystem = true;
		return this;
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
		cfg.setProperty( key, value );
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
	public void close() {
		if ( sessionFactory == null ) {
			throw new java.lang.IllegalStateException( "sessionFactory not yet built" );
		}
		try {
			sessionFactory.close();
		}
		finally {
			if ( usingFileSystem ) {
				cleanupFilesystem();
			}
		}
		sessionFactory = null;
	}

	/**
	 * Builds the sessionFactory as configured so far.
	 */
	public FullTextSessionBuilder build() {
		Configuration hibConfiguration = new Configuration();
		for ( Class<?> annotatedClass : annotatedClasses ) {
			hibConfiguration.addAnnotatedClass( annotatedClass );
		}
		hibConfiguration.getProperties().putAll( cfg );

		ServiceRegistryBuilder registryBuilder = new ServiceRegistryBuilder();
		registryBuilder.applySettings( hibConfiguration.getProperties() );

		final ServiceRegistry serviceRegistry = ServiceRegistryTools.build( registryBuilder );
		SessionFactoryImpl sessionFactoryImpl = (SessionFactoryImpl) hibConfiguration.buildSessionFactory(
				serviceRegistry
		);
		ServiceRegistryImplementor serviceRegistryImplementor = sessionFactoryImpl.getServiceRegistry();
		EventListenerRegistry registry = serviceRegistryImplementor.getService( EventListenerRegistry.class );

		for ( LoadEventListener listener : additionalLoadEventListeners ) {
			registry.getEventListenerGroup( EventType.LOAD ).appendListener( listener );
		}

		sessionFactory = sessionFactoryImpl;
		return this;
	}

	/**
	 * @return the SearchFactory
	 */
	public SearchFactory getSearchFactory() {
		if ( sessionFactory == null ) {
			build();
		}
		return ImplementationFactory.createSearchFactory( ContextHelper.getSearchintegratorBySFI( sessionFactory ) );
	}

	/**
	 * Defines a programmatic configuration to be used by Search
	 *
	 * @return the enabled SearchMapping. change it to define the mapping programmatically.
	 */
	public SearchMapping fluentMapping() {
		SearchMapping mapping = (SearchMapping) cfg.get( org.hibernate.search.cfg.Environment.MODEL_MAPPING );
		if ( mapping == null ) {
			mapping = new SearchMapping();
			cfg.put( org.hibernate.search.cfg.Environment.MODEL_MAPPING, mapping );
		}
		return mapping;
	}

	public void cleanupFilesystem() {
		FileHelper.delete( indexRootDirectory );
	}

	public FullTextSessionBuilder addLoadEventListener(LoadEventListener additionalLoadEventListener) {
		additionalLoadEventListeners.add( additionalLoadEventListener );
		return this;
	}

	@Override
	public Statement apply(final Statement base, Description description) {
		return new Statement() {
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
	}

}
