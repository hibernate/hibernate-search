/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat, Inc. and/or its affiliates or third-party contributors as
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
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.sql.Connection;
import java.sql.SQLException;

import junit.framework.TestCase;
import org.apache.lucene.analysis.StopAnalyzer;
import org.apache.lucene.store.Directory;

import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;

import org.hibernate.annotations.common.util.StringHelper;
import org.hibernate.cfg.Configuration;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.jdbc.Work;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.SearchException;
import org.hibernate.search.SearchFactory;
import org.hibernate.search.engine.spi.SearchFactoryImplementor;
import org.hibernate.search.indexes.impl.DirectoryBasedIndexManager;
import org.hibernate.search.indexes.spi.IndexManager;
import org.hibernate.search.test.fwk.SkipLog;
import org.hibernate.search.util.impl.ContextHelper;
import org.hibernate.search.util.impl.FileHelper;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;
import org.hibernate.testing.FailureExpected;
import org.hibernate.testing.SkipForDialect;
import org.hibernate.tool.hbm2ddl.SchemaExport;
import org.junit.After;
import org.junit.Before;

/**
 * Base class for Hibernate Search unit tests.
 *
 * @author Emmanuel Bernard
 * @author Hardy Ferentschik
 */
public abstract class SearchTestCase extends TestCase {

	private static final Log log = LoggerFactory.make();

	private SessionFactory sessions;
	protected Session session;

	private SearchFactoryImplementor searchFactory;

	protected Configuration cfg;
	private static Class<?> lastTestClass;

	@Before
	public void setUp() throws Exception {
		if ( cfg == null || lastTestClass != getClass() ) {
			buildConfiguration();
		}
		lastTestClass = getClass();
		openSessionFactory();
	}

	protected void openSessionFactory() {
		if ( sessions == null ) {
			if ( cfg == null ) {
				throw new IllegalStateException( "configuration was not built" );
			}
			setSessions( cfg.buildSessionFactory( /*new TestInterceptor()*/ ) );
		}
		else {
			throw new IllegalStateException( "there should be no SessionFactory initialized at this point" );
		}
	}

	protected void closeSessionFactory() {
		if ( sessions == null ) {
			throw new IllegalStateException( "there is no SessionFactory to close" );
		}
		else {
			sessions.close();
			sessions = null;
		}
	}

	protected void handleUnclosedResources() {
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

	protected void closeResources() {
	}

	protected String[] getXmlFiles() {
		return new String[] { };
	}

	protected void setCfg(Configuration cfg) {
		this.cfg = cfg;
	}

	protected Configuration getCfg() {
		return cfg;
	}

	public Session openSession() throws HibernateException {
		session = getSessions().openSession();
		return session;
	}

	protected void setSessions(SessionFactory sessions) {
		this.sessions = sessions;
	}

	protected SessionFactory getSessions() {
		if ( cfg == null ) {
			throw new IllegalStateException( "Configuration should be already defined at this point" );
		}
		if ( sessions == null ) {
			throw new IllegalStateException( "SessionFactory should be already defined at this point" );
		}
		return sessions;
	}

	protected void configure(Configuration cfg) {
		cfg.setProperty( "hibernate.search.lucene_version", TestConstants.getTargetLuceneVersion().name() );
		cfg.setProperty( "hibernate.search.default.directory_provider", "ram" );
		cfg.setProperty( "hibernate.search.default.indexBase", getBaseIndexDir().getAbsolutePath() );
		cfg.setProperty( org.hibernate.search.Environment.ANALYZER_CLASS, StopAnalyzer.class.getName() );

		cfg.setProperty( "hibernate.search.default.indexwriter.merge_factor", "100" );
		cfg.setProperty( "hibernate.search.default.indexwriter.max_buffered_docs", "1000" );
	}

	protected Directory getDirectory(Class<?> clazz) {
		SearchFactoryImplementor searchFactoryBySFI = ContextHelper.getSearchFactoryBySFI( (SessionFactoryImplementor) sessions );
		IndexManager[] indexManagers = searchFactoryBySFI.getIndexBindingForEntity( clazz ).getIndexManagers();
		DirectoryBasedIndexManager indexManager = (DirectoryBasedIndexManager) indexManagers[0];
		return indexManager.getDirectoryProvider().getDirectory();
	}

	@After
	public void tearDown() throws Exception {
		//runSchemaDrop();
		handleUnclosedResources();
		closeResources();
		closeSessionFactory();
		ensureIndexesAreEmpty();
	}

	protected abstract Class<?>[] getAnnotatedClasses();

	protected boolean recreateSchema() {
		return true;
	}

	protected void runSchemaGeneration() {
		SchemaExport export = new SchemaExport( cfg );
		export.create( true, true );
	}

	protected void runSchemaDrop() {
		SchemaExport export = new SchemaExport( cfg );
		export.drop( true, true );
	}

	protected void ensureIndexesAreEmpty() {
		if ( "jms".equals( getCfg().getProperty( "hibernate.search.worker.backend" ) ) ) {
			log.debug( "JMS based test. Skipping index emptying" );
			return;
		}
		FileHelper.delete( getBaseIndexDir() );
	}

	protected SearchFactory getSearchFactory() {
		if ( searchFactory == null ) {
			Session session = openSession();
			FullTextSession fullTextSession = Search.getFullTextSession( session );
			searchFactory = (SearchFactoryImplementor) fullTextSession.getSearchFactory();
			fullTextSession.close();
		}
		return searchFactory;
	}

	protected File getBaseIndexDir() {
		// Make sure no directory is ever reused across the testsuite as Windows might not be able
		// to delete the files after usage. See also
		// http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4715154
		String shortTestName = this.getClass().getSimpleName() + "." + this.getName();

		// the constructor File(File, String) is broken too, see :
		// http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=5066567
		// So make sure to use File(String, String) in this case as TestConstants works with absolute paths!
		File indexPath = new File( TestConstants.getIndexDirectory( this.getClass() ), shortTestName );
		return indexPath;
	}

	protected void buildConfiguration() {
		if ( cfg != null ) {
			throw new IllegalStateException( "Configuration was already built" );
		}
		try {
			setCfg( new Configuration() );
			configure( cfg );
			if ( recreateSchema() ) {
				cfg.setProperty( org.hibernate.cfg.Environment.HBM2DDL_AUTO, "create-drop" );
			}
			for ( String aPackage : getAnnotatedPackages() ) {
				getCfg().addPackage( aPackage );
			}
			for ( Class<?> aClass : getAnnotatedClasses() ) {
				getCfg().addAnnotatedClass( aClass );
			}
			for ( String xmlFile : getXmlFiles() ) {
				InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream( xmlFile );
				getCfg().addInputStream( is );
			}
		}
		catch ( HibernateException e ) {
			e.printStackTrace();
			throw e;
		}
		catch ( SearchException e ) {
			e.printStackTrace();
			throw e;
		}
		catch ( Exception e ) {
			e.printStackTrace();
			throw new RuntimeException( e );
		}
	}

	protected String[] getAnnotatedPackages() {
		return new String[] { };
	}

	protected SearchFactoryImplementor getSearchFactoryImpl() {
		FullTextSession s = Search.getFullTextSession( openSession() );
		s.close();
		SearchFactory searchFactory = s.getSearchFactory();
		return (SearchFactoryImplementor) searchFactory;
	}

	private static class RollbackWork implements Work {

		public void execute(Connection connection) throws SQLException {
			connection.rollback();
		}
	}

	private void reportSkip(Skip skip) {
		reportSkip( skip.reason, skip.testDescription );
	}

	protected void reportSkip(String reason, String testDescription) {
		StringBuilder builder = new StringBuilder();
		builder.append( "*** skipping test [" );
		builder.append( fullTestName() );
		builder.append( "] - " );
		builder.append( testDescription );
		builder.append( " : " );
		builder.append( reason );
		SkipLog.LOG.warn( builder.toString() );
	}

	protected Dialect getDialect() {
		return Dialect.getDialect();
	}

	protected Skip buildSkip(Dialect dialect, String comment, String jiraKey) {
		StringBuilder buffer = new StringBuilder();
		buffer.append( "skipping database-specific test [" );
		buffer.append( fullTestName() );
		buffer.append( "] for dialect [" );
		buffer.append( dialect.getClass().getName() );
		buffer.append( ']' );

		if ( StringHelper.isNotEmpty( comment ) ) {
			buffer.append( "; " ).append( comment );
		}

		if ( StringHelper.isNotEmpty( jiraKey ) ) {
			buffer.append( " (" ).append( jiraKey ).append( ')' );
		}

		return new Skip( buffer.toString(), null );
	}

	protected <T extends Annotation> T locateAnnotation(Class<T> annotationClass, Method runMethod) {
		T annotation = runMethod.getAnnotation( annotationClass );
		if ( annotation == null ) {
			annotation = getClass().getAnnotation( annotationClass );
		}
		if ( annotation == null ) {
			annotation = runMethod.getDeclaringClass().getAnnotation( annotationClass );
		}
		return annotation;
	}

	protected final Skip determineSkipByDialect(Dialect dialect, Method runMethod) throws Exception {
		// skips have precedence, so check them first
		SkipForDialect skipForDialectAnn = locateAnnotation( SkipForDialect.class, runMethod );
		if ( skipForDialectAnn != null ) {
			for ( Class<? extends Dialect> dialectClass : skipForDialectAnn.value() ) {
				if ( skipForDialectAnn.strictMatching() ) {
					if ( dialectClass.equals( dialect.getClass() ) ) {
						return buildSkip( dialect, skipForDialectAnn.comment(), skipForDialectAnn.jiraKey() );
					}
				}
				else {
					if ( dialectClass.isInstance( dialect ) ) {
						return buildSkip( dialect, skipForDialectAnn.comment(), skipForDialectAnn.jiraKey() );
					}
				}
			}
		}
		return null;
	}

	protected static class Skip {
		private final String reason;
		private final String testDescription;

		public Skip(String reason, String testDescription) {
			this.reason = reason;
			this.testDescription = testDescription;
		}
	}

	@Override
	protected void runTest() throws Throwable {
		Method runMethod = findTestMethod();
		FailureExpected failureExpected = locateAnnotation( FailureExpected.class, runMethod );
		try {
			super.runTest();
			if ( failureExpected != null ) {
				throw new FailureExpectedTestPassedException();
			}
		}
		catch ( FailureExpectedTestPassedException t ) {
			closeResources();
			throw t;
		}
		catch ( Throwable t ) {
			if ( t instanceof InvocationTargetException ) {
				t = ( (InvocationTargetException) t ).getTargetException();
			}
			if ( t instanceof IllegalAccessException ) {
				t.fillInStackTrace();
			}
			closeResources();
			if ( failureExpected != null ) {
				StringBuilder builder = new StringBuilder();
				if ( StringHelper.isNotEmpty( failureExpected.message() ) ) {
					builder.append( failureExpected.message() );
				}
				else {
					builder.append( "ignoring @FailureExpected test" );
				}
				builder.append( " (" )
						.append( failureExpected.jiraKey() )
						.append( ")" );
				SkipLog.LOG.warn( builder.toString(), t );
			}
			else {
				throw t;
			}
		}
	}

	@Override
	public void runBare() throws Throwable {
		Method runMethod = findTestMethod();

		final Skip skip = determineSkipByDialect( Dialect.getDialect(), runMethod );
		if ( skip != null ) {
			reportSkip( skip );
			return;
		}

		setUp();
		try {
			runTest();
		}
		finally {
			tearDown();
		}
	}

	public String fullTestName() {
		return this.getClass().getName() + "#" + this.getName();
	}

	private Method findTestMethod() {
		String fName = getName();
		assertNotNull( fName );
		Method runMethod = null;
		try {
			runMethod = getClass().getMethod( fName );
		}
		catch ( NoSuchMethodException e ) {
			fail( "Method \"" + fName + "\" not found" );
		}
		if ( !Modifier.isPublic( runMethod.getModifiers() ) ) {
			fail( "Method \"" + fName + "\" should be public" );
		}
		return runMethod;
	}

	private static class FailureExpectedTestPassedException extends Exception {
		public FailureExpectedTestPassedException() {
			super( "Test marked as @FailureExpected, but did not fail!" );
		}
	}
}
