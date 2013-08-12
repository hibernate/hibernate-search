/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2011 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
package org.hibernate.search.test.batchindexing;

import org.hibernate.cfg.Configuration;
import org.hibernate.search.Environment;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.engine.spi.SearchFactoryImplementor;
import org.hibernate.search.test.SearchTestCaseJUnit4;
import org.hibernate.search.test.errorhandling.MockErrorHandler;
import org.hibernate.testing.RequiresDialect;
import org.hibernate.testing.junit4.CustomRunner;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Verifies that {@link org.hibernate.search.MassIndexer#idFetchSize(int)} is applied by checking for errors thrown by
 * the JDBC Dialect. We use this approach especially as we want to make sure that using
 * {@link Integer#MIN_VALUE} is an acceptable option on MySQL as we suggest it on the documentation.
 *
 * @author Sanne Grinovero <sanne@hibernate.org> (C) 2011 Red Hat Inc.
 */
@RunWith(CustomRunner.class) //needed to enable @RequiresDialect functionality
public class FetchSizeConfigurationTest extends SearchTestCaseJUnit4 {

	@Test
	@RequiresDialect(comment = "H2 does not accept negative fetch sizes",
		strictMatching = true, value = org.hibernate.dialect.H2Dialect.class)
	public void testSetFetchSizeOnH2Fails() throws InterruptedException {
		SearchFactoryImplementor searchFactory = getSearchFactoryImpl();
		MockErrorHandler mockErrorHandler = MassIndexerErrorReportingTest.getErrorHandler( searchFactory );

		FullTextSession fullTextSession = MassIndexerErrorReportingTest.prepareSomeData( this );

		fullTextSession.createIndexer( Book.class ).idFetchSize( -1 ).startAndWait();

		getSession().close();
		String errorMessage = mockErrorHandler.getErrorMessage();
		Assert.assertEquals( "HSEARCH000116: Unexpected error during MassIndexer operation", errorMessage );
		Throwable exception = mockErrorHandler.getLastException();
		Assert.assertTrue( exception instanceof org.hibernate.exception.GenericJDBCException );
	}

	@Test
	@RequiresDialect(comment = "MySQL definitely should accept Integer.MIN_VALUE",
		strictMatching = false, value = org.hibernate.dialect.MySQLDialect.class)
	public void testSetFetchSizeOnMySQL() throws InterruptedException {
		SearchFactoryImplementor searchFactory = getSearchFactoryImpl();
		MockErrorHandler mockErrorHandler = MassIndexerErrorReportingTest.getErrorHandler( searchFactory );

		FullTextSession fullTextSession = MassIndexerErrorReportingTest.prepareSomeData( this );

		fullTextSession.createIndexer( Book.class ).idFetchSize( Integer.MIN_VALUE ).startAndWait();

		getSession().close();
		String errorMessage = mockErrorHandler.getErrorMessage();
		Assert.assertEquals( null, errorMessage );
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { Book.class, Nation.class };
	}

	@Override
	protected void configure(Configuration cfg) {
		super.configure( cfg );
		cfg.setProperty( Environment.ERROR_HANDLER, MockErrorHandler.class.getName() );
	}

}
