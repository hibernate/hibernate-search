/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.errorhandling;

import java.io.IOException;
import java.util.Map;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.search.cfg.Environment;
import org.hibernate.search.exception.ErrorHandler;
import org.hibernate.search.spi.SearchIntegrator;
import org.hibernate.search.test.SearchTestBase;
import org.hibernate.search.testsupport.junit.SkipOnElasticsearch;
import org.jboss.byteman.contrib.bmunit.BMRule;
import org.jboss.byteman.contrib.bmunit.BMUnitRunner;
import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

/**
 * This test uses Byteman. Byteman is activated at the invocation of the test
 * in this class, and it will have the IndexWriter fail during segments merge,
 * which means the commit on the index from our part.
 * The tricky issue is that the merger works in a separate thread and some
 * inner private classes are involved.
 *
 * The Byteman rules are defined in a resources file ConcurrentMergeErrorTest.bytemanrules
 *
 * The goal of the test is to make sure we can catch and report the errors
 * thrown by the merger via whatever is configured as Environment.ERROR_HANDLER.
 *
 * @author Sanne Grinovero
 * @see Environment#ERROR_HANDLER
 */
@RunWith(BMUnitRunner.class)
@Category(SkipOnElasticsearch.class) // Merge schedulers are specific to the Lucene backend
public class ConcurrentMergeErrorHandledTest extends SearchTestBase {

	@Test
	@BMRule(targetClass = "org.apache.lucene.index.ConcurrentMergeScheduler",
			targetMethod = "merge",
			action = "throw new IOException(\"Byteman said: your disk is full!\")",
			name = "testLuceneMergerErrorHandling")
	public void testLuceneMergerErrorHandling() {
		MockErrorHandler mockErrorHandler = getErrorHandlerAndAssertCorrectTypeIsUsed();

		indexSingleFooInstance();

		String errorMessage = mockErrorHandler.getErrorMessage();
		Assert.assertEquals( "HSEARCH000117: IOException on the IndexWriter", errorMessage );
		Throwable exception = mockErrorHandler.getLastException();
		Assert.assertTrue( exception instanceof IOException );
		Assert.assertEquals( "Byteman said: your disk is full!", exception.getMessage() );
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
		Assert.assertTrue( errorHandler instanceof MockErrorHandler );
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
