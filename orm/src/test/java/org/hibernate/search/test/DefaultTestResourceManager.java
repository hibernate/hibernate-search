/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2013, Red Hat, Inc. and/or its affiliates or third-party contributors as
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
package org.hibernate.search.test;

import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.UUID;

import org.apache.lucene.analysis.StopAnalyzer;
import org.apache.lucene.store.Directory;

import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;

import org.hibernate.cfg.Configuration;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.jdbc.Work;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.SearchException;
import org.hibernate.search.SearchFactory;
import org.hibernate.search.engine.spi.SearchFactoryImplementor;
import org.hibernate.search.indexes.impl.DirectoryBasedIndexManager;
import org.hibernate.search.indexes.spi.IndexManager;
import org.hibernate.search.util.impl.ContextHelper;
import org.hibernate.search.util.impl.FileHelper;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;

/**
 * Test utility class for managing ORM and Search test resources.
 *
 * @author Emmanuel Bernard
 * @author Hardy Ferentschik
 */
public final class DefaultTestResourceManager implements TestResourceManager {

	private static final Log log = LoggerFactory.make();

	private final Class<?>[] annotatedClasses;
	private final File baseIndexDir;

	private Configuration cfg;
	private SessionFactory sessionFactory;
	private Session session;
	private SearchFactoryImplementor searchFactory;
	private boolean needsConfigurationRebuild;

	public DefaultTestResourceManager(Class<?>[] annotatedClasses) {
		this.annotatedClasses = annotatedClasses;
		this.cfg = new Configuration();
		this.baseIndexDir = createBaseIndexDir();
		this.needsConfigurationRebuild = true;
	}

	@Override
	public void openSessionFactory() {
		if ( sessionFactory == null ) {
			if ( cfg == null ) {
				throw new IllegalStateException( "configuration was not built" );
			}
			setSessionFactory( cfg.buildSessionFactory( /*new TestInterceptor()*/ ) );
		}
		else {
			throw new IllegalStateException( "there should be no SessionFactory initialized at this point" );
		}
	}

	@Override
	public void closeSessionFactory() {
		if ( sessionFactory == null ) {
			throw new IllegalStateException( "there is no SessionFactory to close" );
		}
		else {
			sessionFactory.close();
			sessionFactory = null;
		}
	}

	@Override
	public Configuration getCfg() {
		return cfg;
	}

	@Override
	public Session openSession() {
		session = getSessionFactory().openSession();
		return session;
	}

	@Override
	public Session getSession() {
		return session;
	}

	@Override
	public SessionFactory getSessionFactory() {
		if ( cfg == null ) {
			throw new IllegalStateException( "Configuration should be already defined at this point" );
		}
		if ( sessionFactory == null ) {
			throw new IllegalStateException( "SessionFactory should be already defined at this point" );
		}
		return sessionFactory;
	}

	@Override
	public Directory getDirectory(Class<?> clazz) {
		SearchFactoryImplementor searchFactoryBySFI = ContextHelper.getSearchFactoryBySFI( (SessionFactoryImplementor) sessionFactory );
		IndexManager[] indexManagers = searchFactoryBySFI.getIndexBinding( clazz ).getIndexManagers();
		DirectoryBasedIndexManager indexManager = (DirectoryBasedIndexManager) indexManagers[0];
		return indexManager.getDirectoryProvider().getDirectory();
	}

	@Override
	public void ensureIndexesAreEmpty() {
		if ( "jms".equals( getCfg().getProperty( "hibernate.search.worker.backend" ) ) ) {
			log.debug( "JMS based test. Skipping index emptying" );
			return;
		}
		FileHelper.delete( getBaseIndexDir() );
	}

	@Override
	public SearchFactory getSearchFactory() {
		if ( searchFactory == null ) {
			Session session = openSession();
			FullTextSession fullTextSession = Search.getFullTextSession( session );
			searchFactory = (SearchFactoryImplementor) fullTextSession.getSearchFactory();
			fullTextSession.close();
		}
		return searchFactory;
	}

	@Override
	public SearchFactoryImplementor getSearchFactoryImpl() {
		FullTextSession s = Search.getFullTextSession( openSession() );
		s.close();
		SearchFactory searchFactory = s.getSearchFactory();
		return (SearchFactoryImplementor) searchFactory;
	}

	@Override
	public File getBaseIndexDir() {
		return baseIndexDir;
	}

	@Override
	public void forceConfigurationRebuild() {
		this.needsConfigurationRebuild = true;
		this.cfg = new Configuration();
	}

	@Override
	public boolean needsConfigurationRebuild() {
		return this.needsConfigurationRebuild;
	}

	public void defaultTearDown() throws Exception {
		handleUnclosedResources();
		closeSessionFactory();
		ensureIndexesAreEmpty();
	}

	public void applyDefaultConfiguration(Configuration cfg) {
		cfg.setProperty( "hibernate.search.lucene_version", TestConstants.getTargetLuceneVersion().name() );
		cfg.setProperty( "hibernate.search.default.directory_provider", "ram" );
		cfg.setProperty( "hibernate.search.default.indexBase", getBaseIndexDir().getAbsolutePath() );
		cfg.setProperty( org.hibernate.search.Environment.ANALYZER_CLASS, StopAnalyzer.class.getName() );

		cfg.setProperty( "hibernate.search.default.indexwriter.merge_factor", "100" );
		cfg.setProperty( "hibernate.search.default.indexwriter.max_buffered_docs", "1000" );

		cfg.setProperty( org.hibernate.cfg.Environment.HBM2DDL_AUTO, "create-drop" );
	}

	public void handleUnclosedResources() {
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
	}

	public void setSessionFactory(SessionFactory sessionFactory) {
		this.sessionFactory = sessionFactory;
	}

	public void buildConfiguration() {
		try {
			for ( Class<?> aClass : annotatedClasses ) {
				getCfg().addAnnotatedClass( aClass );
			}
		}
		catch (HibernateException e) {
			e.printStackTrace();
			throw e;
		}
		catch (SearchException e) {
			e.printStackTrace();
			throw e;
		}
		catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException( e );
		}
		needsConfigurationRebuild = false;
	}

	private File createBaseIndexDir() {
		// Make sure no directory is ever reused across the test suite as Windows might not be able
		// to delete the files after usage. See also
		// http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4715154
		String shortTestName = this.getClass().getSimpleName() + "-" + UUID.randomUUID().toString().substring( 0, 8 );

		// the constructor File(File, String) is broken too, see :
		// http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=5066567
		// So make sure to use File(String, String) in this case as TestConstants works with absolute paths!
		return new File( TestConstants.getIndexDirectory( this.getClass() ), shortTestName );
	}

	private static class RollbackWork implements Work {

		@Override
		public void execute(Connection connection) throws SQLException {
			connection.rollback();
		}
	}
}
