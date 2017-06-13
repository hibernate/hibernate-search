/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test;

import java.io.IOException;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.apache.lucene.analysis.core.StopAnalyzer;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.SessionFactoryBuilder;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.jdbc.Work;
import org.hibernate.search.Search;
import org.hibernate.search.SearchFactory;
import org.hibernate.search.cfg.Environment;
import org.hibernate.search.engine.integration.impl.ExtendedSearchIntegrator;
import org.hibernate.search.hcore.util.impl.ContextHelper;
import org.hibernate.search.test.util.MultitenancyTestHelper;
import org.hibernate.search.test.util.TestConfiguration;
import org.hibernate.search.testsupport.TestConstants;
import org.hibernate.search.util.impl.FileHelper;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;
import org.hibernate.service.spi.ServiceRegistryImplementor;

/**
 * Manages bootstrap and teardown of an Hibernate SessionFactory for purposes of
 * testing.
 *
 * It enforces some cleanup rules, and sets various configuration settings by default.
 * This class also takes care of schema creation for tests requiring multi-tenancy,
 * which is normally not supported by the HBM2DDL_AUTO setting.
 *
 * @author Sanne Grinovero
 * @since 5.4
 */
public final class DefaultTestResourceManager implements TestResourceManager {

	private static final Log log = LoggerFactory.make();

	private final TestConfiguration test;
	private final Path baseIndexDir;

	/* Each of the following fields needs to be cleaned up on close */
	private SessionFactoryImplementor sessionFactory;
	private MultitenancyTestHelper multitenancy;
	private Session session;
	private SearchFactory searchFactory;
	private Map<String,Object> configurationSettings;

	public DefaultTestResourceManager(TestConfiguration test, Class<?> currentTestModuleClass) {
		this.test = test;
		this.baseIndexDir = createBaseIndexDir( currentTestModuleClass );
	}

	@Override
	public void openSessionFactory() {
		if ( sessionFactory == null ) {
			sessionFactory = buildSessionFactory();
		}
		else {
			throw new IllegalStateException( "there should be no SessionFactory initialized at this point" );
		}
	}

	private SessionFactoryImplementor buildSessionFactory() {
		multitenancy = new MultitenancyTestHelper( test.multiTenantIds() );
		Map<String, Object> settings = getConfigurationSettings();
		multitenancy.forceConfigurationSettings( settings );

		StandardServiceRegistryBuilder registryBuilder = new StandardServiceRegistryBuilder()
			.applySettings( settings );

		multitenancy.enableIfNeeded( registryBuilder );

		ServiceRegistryImplementor serviceRegistry = (ServiceRegistryImplementor) registryBuilder.build();

		MetadataSources ms = new MetadataSources( serviceRegistry );
		Class<?>[] annotatedClasses = test.getAnnotatedClasses();
		if ( annotatedClasses != null ) {
			for ( Class<?> entity : annotatedClasses ) {
				ms.addAnnotatedClass( entity );
			}
		}

		Metadata metadata = ms.buildMetadata();
		multitenancy.exportSchema( serviceRegistry, metadata, settings );

		final SessionFactoryBuilder sfb = metadata.getSessionFactoryBuilder();
		return (SessionFactoryImplementor) sfb.build();
	}

	private Map<String,Object> getConfigurationSettings() {
		if ( configurationSettings == null ) {
			configurationSettings = new HashMap<>();
			configurationSettings.put( "hibernate.search.lucene_version", TestConstants.getTargetLuceneVersion().toString() );
			configurationSettings.put( "hibernate.search.default.directory_provider", "local-heap" );
			configurationSettings.put( "hibernate.search.default.indexBase", getBaseIndexDir().toAbsolutePath().toString() );
			configurationSettings.put( Environment.ANALYZER_CLASS, StopAnalyzer.class.getName() );
			configurationSettings.put( "hibernate.search.default.indexwriter.merge_factor", "100" );
			configurationSettings.put( "hibernate.search.default.indexwriter.max_buffered_docs", "1000" );
			configurationSettings.put( org.hibernate.cfg.Environment.HBM2DDL_AUTO, "create-drop" );
			test.configure( configurationSettings );
		}
		return configurationSettings;
	}

	@Override
	public void closeSessionFactory() {
		if ( sessionFactory != null ) {
			sessionFactory.close();
			sessionFactory = null;
		}
		if ( multitenancy != null ) {
			multitenancy.close();
			multitenancy = null;
		}
		//Make sure we don't reuse the settings across SessionFactories
		configurationSettings = null;
		session = null;
		searchFactory = null;
	}

	@Override
	public Session openSession() {
		if ( session != null && session.isOpen() ) {
			throw new IllegalStateException( "Previously opened Session wasn't closed!" );
		}
		session = getSessionFactory().openSession();
		return session;
	}

	@Override
	public Session getSession() {
		return session;
	}

	@Override
	public SessionFactory getSessionFactory() {
		if ( sessionFactory == null ) {
			throw new IllegalStateException( "SessionFactory should be already defined at this point" );
		}
		return sessionFactory;
	}

	@Override
	public void ensureIndexesAreEmpty() throws IOException {
		FileHelper.delete( getBaseIndexDir() );
	}

	@Override
	public SearchFactory getSearchFactory() {
		if ( searchFactory == null ) {
			//Don't use this#openSession() as that would interfere with our sanity
			//verification for the tests to not open additional session instances.
			try ( Session session = getSessionFactory().openSession() ) {
				searchFactory = Search.getFullTextSession( session ).getSearchFactory();
			}
		}
		return searchFactory;
	}

	@Override
	public ExtendedSearchIntegrator getExtendedSearchIntegrator() {
		return ContextHelper.getSearchIntegratorBySFI( sessionFactory );
	}

	@Override
	public Path getBaseIndexDir() {
		return baseIndexDir;
	}

	public void defaultTearDown() throws Exception {
		close();
		cleanUp();
	}

	/**
	 * Close all open resources (streams, sessions, session factories, ...)
	 * @throws Exception
	 */
	public void close() {
		handleUnclosedResources();
		closeSessionFactory();
	}

	/**
	 * Clean up any side-effects of the test (temporary files in particular).
	 * <p>
	 * If multiple managers share the same files, this must be executed after
	 * <em>every</em> manager has been {@link #closeResources() closed}.
	 *
	 * @throws Exception
	 */
	public void cleanUp() throws IOException {
		ensureIndexesAreEmpty();
	}

	private void handleUnclosedResources() {
		if ( session != null && session.isOpen() ) {
			if ( session.isConnected() ) {
				session.doWork( new RollbackWork() );
			}
			session.close();
			session = null;
			log.debug( "Closing open session. Make sure to close sessions explicitly in your tests!" );
		}
		else {
			session = null;
		}
		searchFactory = null;
	}

	private Path createBaseIndexDir(Class<?> currentTestModuleClass) {
		// Appending UUID to be extra-sure no directory is ever reused across the test suite as Windows might not be
		// able to delete the files after usage. See also
		// http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4715154
		return TestConstants.getIndexDirectory( TestConstants.getTempTestDataDir(), currentTestModuleClass ).resolve(
				UUID.randomUUID().toString().substring( 0, 8 )
			);
	}

	private static class RollbackWork implements Work {
		@Override
		public void execute(Connection connection) throws SQLException {
			connection.rollback();
		}
	}

}
