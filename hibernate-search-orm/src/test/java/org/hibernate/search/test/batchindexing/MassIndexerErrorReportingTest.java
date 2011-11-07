package org.hibernate.search.test.batchindexing;

import junit.framework.Assert;
import org.jboss.byteman.contrib.bmunit.BMRule;
import org.jboss.byteman.contrib.bmunit.BMUnitRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.hibernate.search.Environment;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.engine.spi.SearchFactoryImplementor;
import org.hibernate.search.exception.ErrorHandler;
import org.hibernate.search.test.SearchTestCase;
import org.hibernate.search.test.errorhandling.MockErrorHandler;

@RunWith(BMUnitRunner.class)
public class MassIndexerErrorReportingTest extends SearchTestCase {

	@Test
	@BMRule(targetClass = "org.hibernate.search.batchindexing.impl.IdentifierConsumerEntityProducer",
			targetMethod = "loadList",
			helper = "org.hibernate.search.test.util.BytemanHelper",
			action = "throwNPE(\"Byteman created NPE\")",
			name = "testMassIndexerErrorsReported")
	public void testMassIndexerErrorsReported() throws InterruptedException {
		SearchFactoryImplementor searchFactory = getSearchFactoryImpl();
		ErrorHandler errorHandler = searchFactory.getErrorHandler();
		Assert.assertTrue( errorHandler instanceof MockErrorHandler );
		MockErrorHandler mockErrorHandler = (MockErrorHandler) errorHandler;

		FullTextSession fullTextSession = Search.getFullTextSession( openSession() );
		fullTextSession.beginTransaction();
		Nation france = new Nation( "France", "FR" );
		fullTextSession.save( france );
		Book ceylonBook = new Book();
		ceylonBook.setTitle( "Ceylon in Action" );
		ceylonBook.setFirstPublishedIn( france );
		fullTextSession.save( ceylonBook );
		fullTextSession.getTransaction().commit();

		fullTextSession.createIndexer( Book.class ).startAndWait();

		session.close();
		String errorMessage = mockErrorHandler.getErrorMessage();
		Assert.assertEquals( "Unexpected error during MassIndexer operation", errorMessage );
		Throwable exception = mockErrorHandler.getLastException();
		Assert.assertTrue( exception instanceof org.jboss.byteman.rule.exception.ExecuteException );
	}

	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { Book.class, Nation.class };
	}

	protected void configure(org.hibernate.cfg.Configuration cfg) {
		super.configure( cfg );
		cfg.setProperty( Environment.ERROR_HANDLER, MockErrorHandler.class.getName() );
	}

}
