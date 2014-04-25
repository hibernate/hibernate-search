/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2012, Red Hat, Inc. and/or its affiliates or third-party contributors as
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
package org.hibernate.search.test.batchindexing;

import org.hibernate.search.cfg.Environment;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.engine.spi.SearchFactoryImplementor;
import org.hibernate.search.exception.ErrorHandler;
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
		SearchFactoryImplementor searchFactory = getSearchFactoryImpl();
		MockErrorHandler mockErrorHandler = getErrorHandler( searchFactory );

		FullTextSession fullTextSession = prepareSomeData( this );

		fullTextSession.createIndexer( Book.class ).startAndWait();

		getSession().close();
		String errorMessage = mockErrorHandler.getErrorMessage();
		Assert.assertEquals( "HSEARCH000212: An exception occurred while the MassIndexer was transforming identifiers to Lucene Documents", errorMessage );
		Throwable exception = mockErrorHandler.getLastException();
		Assert.assertTrue( exception instanceof NullPointerException );
		Assert.assertEquals( "Byteman created NPE", exception.getMessage() );
	}

	static MockErrorHandler getErrorHandler(SearchFactoryImplementor searchFactory) {
		ErrorHandler errorHandler = searchFactory.getErrorHandler();
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
