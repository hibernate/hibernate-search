/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.errorhandling;

import java.util.Map;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.MassIndexer;
import org.hibernate.search.Search;
import org.hibernate.search.batchindexing.MassIndexerProgressMonitor;
import org.hibernate.search.cfg.Environment;
import org.hibernate.search.exception.ErrorHandler;
import org.hibernate.search.spi.SearchIntegrator;
import org.hibernate.search.test.SearchTestBase;
import org.hibernate.search.test.util.progessmonitor.AssertingMassIndexerProgressMonitor;
import org.hibernate.search.testsupport.TestForIssue;
import org.jboss.byteman.contrib.bmunit.BMRule;
import org.jboss.byteman.contrib.bmunit.BMUnitRunner;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Test to verify the configured ErrorHandler is used for building the Lucene document and for example
 * errors in the bridges are caught.
 *
 * @author Hardy Ferentschik
 */
@TestForIssue(jiraKey = "HSEARCH-1354")
@RunWith(BMUnitRunner.class)
public class ErrorHandlingDuringDocumentCreationTest extends SearchTestBase {

	@Test
	@BMRule(targetClass = "org.hibernate.search.batchindexing.impl.IdentifierConsumerDocumentProducer",
			targetMethod = "index",
			action = "throw new RuntimeException(\"Byteman said: Error in document creation!\")",
			name = "testErrorInBuildingLuceneDocumentGetsCaughtByErrorHandler")
	public void testErrorInBuildingLuceneDocumentGetsCaughtByErrorHandler() throws Exception {
		MockErrorHandler mockErrorHandler = getErrorHandlerAndAssertCorrectTypeIsUsed();
		AssertingMassIndexerProgressMonitor progressMonitor = new AssertingMassIndexerProgressMonitor( 0, 1 );

		indexSingleFooInstance();
		massIndexFooInstances( progressMonitor );

		String errorMessage = mockErrorHandler.getErrorMessage();
		Assert.assertTrue( "Wrong error code: " + errorMessage, errorMessage.startsWith( "HSEARCH000183" ) );
		Throwable exception = mockErrorHandler.getLastException();
		Assert.assertTrue( exception instanceof RuntimeException );
		Assert.assertEquals( "Byteman said: Error in document creation!", exception.getMessage() );
		progressMonitor.assertExpectedProgressMade();
	}

	private void massIndexFooInstances(MassIndexerProgressMonitor monitor) throws InterruptedException {
		FullTextSession fullTextSession = Search.getFullTextSession( openSession() );
		MassIndexer massIndexer = fullTextSession.createIndexer( Foo.class );
		massIndexer.progressMonitor( monitor );
		massIndexer.startAndWait();
		fullTextSession.close();
	}

	private void indexSingleFooInstance() {
		Session session = openSession();
		Transaction transaction = session.beginTransaction();
		session.persist( new Foo() );
		transaction.commit();
		session.close();
	}

	private MockErrorHandler getErrorHandlerAndAssertCorrectTypeIsUsed() {
		SearchIntegrator integrator = getExtendedSearchIntegrator();
		ErrorHandler errorHandler = integrator.getErrorHandler();
		Assert.assertEquals( MockErrorHandler.class, errorHandler.getClass() );
		return (MockErrorHandler) errorHandler;
	}

	@Override
	public Class<?>[] getAnnotatedClasses() {
		return new Class[] { Foo.class };
	}

	@Override
	public void configure(Map<String,Object> cfg) {
		cfg.put( Environment.ERROR_HANDLER, MockErrorHandler.class.getName() );
	}
}
