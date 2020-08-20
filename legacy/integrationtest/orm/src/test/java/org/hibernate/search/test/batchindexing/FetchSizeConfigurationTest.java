/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.batchindexing;

import java.util.Map;

import org.hibernate.search.cfg.Environment;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.spi.SearchIntegrator;
import org.hibernate.search.test.SearchTestBase;
import org.hibernate.search.test.errorhandling.MockErrorHandler;
import org.hibernate.testing.RequiresDialect;
import org.junit.Assert;
import org.junit.Test;

/**
 * Verifies that {@link org.hibernate.search.MassIndexer#idFetchSize(int)} is applied by checking for errors thrown by
 * the JDBC Dialect. We use this approach especially as we want to make sure that using
 * {@link Integer#MIN_VALUE} is an acceptable option on MySQL as we suggest it on the documentation.
 *
 * @author Sanne Grinovero (C) 2011 Red Hat Inc.
 */
public class FetchSizeConfigurationTest extends SearchTestBase {

	@Test
	@RequiresDialect(comment = "H2 does not accept negative fetch sizes",
		strictMatching = true, value = org.hibernate.dialect.H2Dialect.class)
	public void testSetFetchSizeOnH2Fails() throws InterruptedException {
		SearchIntegrator searchIntegrator = getExtendedSearchIntegrator();
		MockErrorHandler mockErrorHandler = MassIndexerErrorReportingTest.getErrorHandler( searchIntegrator );

		FullTextSession fullTextSession = MassIndexerErrorReportingTest.prepareSomeData( this );

		fullTextSession.createIndexer( Book.class ).idFetchSize( -1 ).startAndWait();

		getSession().close();
		String errorMessage = mockErrorHandler.getErrorMessage();
		Assert.assertEquals( "HSEARCH000211: An exception occurred while the MassIndexer was fetching the primary identifiers list", errorMessage );
		Throwable exception = mockErrorHandler.getLastException();
		Assert.assertTrue( exception instanceof org.hibernate.exception.GenericJDBCException );
	}

	@Test
	@RequiresDialect(comment = "MySQL definitely should accept Integer.MIN_VALUE",
		strictMatching = false, value = org.hibernate.dialect.MySQLDialect.class)
	public void testSetFetchSizeOnMySQL() throws InterruptedException {
		SearchIntegrator searchIntegrator = getExtendedSearchIntegrator();
		MockErrorHandler mockErrorHandler = MassIndexerErrorReportingTest.getErrorHandler( searchIntegrator );

		FullTextSession fullTextSession = MassIndexerErrorReportingTest.prepareSomeData( this );

		fullTextSession.createIndexer( Book.class ).idFetchSize( Integer.MIN_VALUE ).startAndWait();

		getSession().close();
		String errorMessage = mockErrorHandler.getErrorMessage();
		Assert.assertEquals( null, errorMessage );
	}

	@Override
	public Class<?>[] getAnnotatedClasses() {
		return new Class[] { Book.class, Nation.class };
	}

	@Override
	public void configure(Map<String,Object> cfg) {
		cfg.put( Environment.ERROR_HANDLER, MockErrorHandler.class.getName() );
	}

}
