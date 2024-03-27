/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.test;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.jdbc.Work;
import org.hibernate.search.Search;
import org.hibernate.search.SearchFactory;
import org.hibernate.search.backend.lucene.cfg.LuceneBackendSettings;
import org.hibernate.search.backend.lucene.multitenancy.MultiTenancyStrategyName;
import org.hibernate.search.engine.cfg.BackendSettings;
import org.hibernate.search.test.testsupport.V5MigrationHelperOrmSetupHelper;
import org.hibernate.search.test.util.TestConfiguration;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;

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

	private static final Log log = LoggerFactory.make( MethodHandles.lookup() );

	private final TestConfiguration test;
	private final V5MigrationHelperOrmSetupHelper setupHelper;

	/* Each of the following fields needs to be cleaned up on close */
	private SessionFactoryImplementor sessionFactory;
	private Path baseIndexDir;
	private Session session;
	private SearchFactory searchFactory;
	private Map<String, Object> configurationSettings;

	public DefaultTestResourceManager(TestConfiguration test, V5MigrationHelperOrmSetupHelper setupHelper) {
		this.test = test;
		this.setupHelper = setupHelper;
	}

	@Override
	public void openSessionFactory() {
		if ( sessionFactory == null ) {
			sessionFactory = buildSessionFactory();
			Map settings = sessionFactory.getServiceRegistry().getService( ConfigurationService.class ).getSettings();
			baseIndexDir = Paths.get( (String) settings.get( "hibernate.search.backend.directory.root" ) );
		}
		else {
			throw new IllegalStateException( "there should be no SessionFactory initialized at this point" );
		}
	}

	private SessionFactoryImplementor buildSessionFactory() {
		V5MigrationHelperOrmSetupHelper.SetupContext setupContext = setupHelper.start();

		Map<String, Object> settings = getConfigurationSettings();
		setupContext = setupContext.withProperties( settings );

		Set<String> tenantIds = test.multiTenantIds();
		if ( !tenantIds.isEmpty() ) {
			setupContext = setupContext.tenants( tenantIds.toArray( new String[0] ) );
			setupContext = setupContext.withProperty(
					BackendSettings.backendKey( LuceneBackendSettings.MULTI_TENANCY_STRATEGY ),
					MultiTenancyStrategyName.DISCRIMINATOR );
		}

		Class<?>[] annotatedClasses = test.getAnnotatedClasses();
		if ( annotatedClasses != null ) {
			setupContext = setupContext
					.withConfiguration( builder -> builder.addAnnotatedClasses( Arrays.asList( annotatedClasses ) ) );
		}

		return setupContext.setup().unwrap( SessionFactoryImplementor.class );
	}

	private Map<String, Object> getConfigurationSettings() {
		if ( configurationSettings == null ) {
			configurationSettings = new HashMap<>();
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
		deleteRecursive( getBaseIndexDir() );
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

	private static void deleteRecursive(Path path) throws IOException {
		if ( path == null || Files.notExists( path ) ) {
			return;
		}

		Files.walkFileTree( path, new SimpleFileVisitor<Path>() {
			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
				safeDelete( file );
				return FileVisitResult.CONTINUE;
			}

			@Override
			public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
				safeDelete( dir );
				return FileVisitResult.CONTINUE;
			}
		} );
	}

	private static void safeDelete(Path file) {
		try {
			Files.deleteIfExists( file );
		}
		catch (IOException e) {
			log.fileDeleteFailureIgnored( e );
		}
	}

	private static class RollbackWork implements Work {
		@Override
		public void execute(Connection connection) throws SQLException {
			connection.rollback();
		}
	}

}
