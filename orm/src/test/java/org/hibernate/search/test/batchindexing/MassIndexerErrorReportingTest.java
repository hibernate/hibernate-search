/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.batchindexing;

import org.hibernate.search.cfg.Environment;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.exception.ErrorHandler;
import org.hibernate.search.spi.SearchIntegrator;
import org.hibernate.search.test.SearchTestBase;
import org.hibernate.search.test.errorhandling.MockErrorHandler;
import org.jboss.byteman.contrib.bmunit.BMRule;
import org.jboss.byteman.contrib.bmunit.BMUnitRunner;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(BMUnitRunner.class)
public class MassIndexerErrorReportingTest extends SearchTestBase {

	@Test
	@BMRule(targetClass = "org.hibernate.search.batchindexing.impl.IdentifierConsumerDocumentProducer",
			targetMethod = "loadList",
			action = "throw new NullPointerException(\"Byteman created NPE\")",
			name = "testMassIndexerErrorsReported")
	public void testMassIndexerErrorsReported() throws InterruptedException {
		SearchIntegrator integrator = getExtendedSearchIntegrator();
		MockErrorHandler mockErrorHandler = getErrorHandler( integrator );

		FullTextSession fullTextSession = prepareSomeData( this );

		fullTextSession.createIndexer( Book.class ).startAndWait();

		getSession().close();
		String errorMessage = mockErrorHandler.getErrorMessage();
		Assert.assertEquals( "HSEARCH000212: An exception occurred while the MassIndexer was transforming identifiers to Lucene Documents", errorMessage );
		Throwable exception = mockErrorHandler.getLastException();
		Assert.assertTrue( exception instanceof NullPointerException );
		Assert.assertEquals( "Byteman created NPE", exception.getMessage() );
	}

	static MockErrorHandler getErrorHandler(SearchIntegrator integrator) {
		ErrorHandler errorHandler = integrator.getErrorHandler();
		Assert.assertTrue( errorHandler instanceof MockErrorHandler );
		MockErrorHandler mockErrorHandler = (MockErrorHandler) errorHandler;
		return mockErrorHandler;
	}

	static FullTextSession prepareSomeData(SearchTestBase testCase) {
		FullTextSession fullTextSession = Search.getFullTextSession( testCase.openSession() );
		fullTextSession.beginTransaction();
		Nation france = new Nation( "France", "FR" );
		fullTextSession.save( france );
		Book ceylonBook = new Book();
		ceylonBook.setTitle( "Ceylon in Action" );
		ceylonBook.setFirstPublishedIn( france );
		fullTextSession.save( ceylonBook );
		fullTextSession.getTransaction().commit();
		return fullTextSession;
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { Book.class, Nation.class };
	}

	@Override
	protected void configure(org.hibernate.cfg.Configuration cfg) {
		super.configure( cfg );
		cfg.setProperty( Environment.ERROR_HANDLER, MockErrorHandler.class.getName() );
	}

}
