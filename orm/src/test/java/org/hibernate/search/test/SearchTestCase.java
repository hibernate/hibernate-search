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
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import junit.framework.TestCase;
import org.apache.lucene.store.Directory;

import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;

import org.hibernate.annotations.common.util.StringHelper;
import org.hibernate.cfg.Configuration;
import org.hibernate.dialect.Dialect;
import org.hibernate.search.SearchFactory;
import org.hibernate.search.engine.spi.SearchFactoryImplementor;
import org.hibernate.search.test.fwk.SkipLog;
import org.hibernate.testing.FailureExpected;
import org.hibernate.testing.SkipForDialect;

/**
 * Base class for Hibernate Search tests using Hibernate ORM and Junit 3.
 *
 * @author Emmanuel Bernard
 * @author Hardy Ferentschik
 */
public abstract class SearchTestCase extends TestCase implements TestResourceManager {
	private DefaultTestResourceManager testResourceManager;

	public void setUp() throws Exception {
		if ( testResourceManager == null || testResourceManager.needsConfigurationRebuild() ) {
			testResourceManager = new DefaultTestResourceManager( getAnnotatedClasses() );
			configure( testResourceManager.getCfg() );
			testResourceManager.buildConfiguration();
		}
		testResourceManager.openSessionFactory();
	}

	public void tearDown() throws Exception {
		testResourceManager.defaultTearDown();
	}

	@Override
	public final Configuration getCfg() {
		return testResourceManager.getCfg();
	}

	@Override
	public final void openSessionFactory() {
		testResourceManager.openSessionFactory();
	}

	@Override
	public SearchFactory getSearchFactory() {
		return testResourceManager.getSearchFactory();
	}

	@Override
	public final SessionFactory getSessionFactory() {
		return testResourceManager.getSessionFactory();
	}

	@Override
	public final void closeSessionFactory() {
		testResourceManager.closeSessionFactory();
	}

	@Override
	public SearchFactoryImplementor getSearchFactoryImpl() {
		return testResourceManager.getSearchFactoryImpl();
	}

	@Override
	public final Session openSession() throws HibernateException {
		return testResourceManager.openSession();
	}

	@Override
	public final Session getSession() {
		return testResourceManager.getSession();
	}

	@Override
	public File getBaseIndexDir() {
		return testResourceManager.getBaseIndexDir();
	}

	@Override
	public void ensureIndexesAreEmpty() {
		testResourceManager.ensureIndexesAreEmpty();
	}

	@Override
	public Directory getDirectory(Class<?> clazz) {
		return testResourceManager.getDirectory( clazz );
	}

	@Override
	public void forceConfigurationRebuild() {
		if ( testResourceManager == null ) {
			testResourceManager = new DefaultTestResourceManager( getAnnotatedClasses() );
		}
		testResourceManager.forceConfigurationRebuild();
	}

	@Override
	public boolean needsConfigurationRebuild() {
		return testResourceManager.needsConfigurationRebuild();
	}

	protected abstract Class<?>[] getAnnotatedClasses();

	protected void configure(Configuration cfg) {
		testResourceManager.applyDefaultConfiguration( cfg );
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
		catch (FailureExpectedTestPassedException t) {
			throw t;
		}
		catch (Throwable t) {
			if ( t instanceof InvocationTargetException ) {
				t = ( (InvocationTargetException) t ).getTargetException();
			}
			if ( t instanceof IllegalAccessException ) {
				t.fillInStackTrace();
			}
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

	private void reportSkip(Skip skip) {
		reportSkip( skip.reason, skip.testDescription );
	}

	private void reportSkip(String reason, String testDescription) {
		StringBuilder builder = new StringBuilder();
		builder.append( "*** skipping test [" );
		builder.append( fullTestName() );
		builder.append( "] - " );
		builder.append( testDescription );
		builder.append( " : " );
		builder.append( reason );
		SkipLog.LOG.warn( builder.toString() );
	}

	private Skip buildSkip(Dialect dialect, String comment, String jiraKey) {
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

	private <T extends Annotation> T locateAnnotation(Class<T> annotationClass, Method runMethod) {
		T annotation = runMethod.getAnnotation( annotationClass );
		if ( annotation == null ) {
			annotation = getClass().getAnnotation( annotationClass );
		}
		if ( annotation == null ) {
			annotation = runMethod.getDeclaringClass().getAnnotation( annotationClass );
		}
		return annotation;
	}

	private Skip determineSkipByDialect(Dialect dialect, Method runMethod) throws Exception {
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

	private String fullTestName() {
		return this.getClass().getName() + "#" + this.getName();
	}

	private Method findTestMethod() {
		String fName = getName();
		assertNotNull( fName );
		Method runMethod = null;
		try {
			runMethod = getClass().getMethod( fName );
		}
		catch (NoSuchMethodException e) {
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

	private static class Skip {
		private final String reason;
		private final String testDescription;

		public Skip(String reason, String testDescription) {
			this.reason = reason;
			this.testDescription = testDescription;
		}
	}
}
