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
package org.hibernate.search.test.errorhandling;

import java.io.IOException;

import org.hibernate.Session;
import org.hibernate.Transaction;

import org.hibernate.search.Environment;
import org.hibernate.search.engine.spi.SearchFactoryImplementor;
import org.hibernate.search.exception.ErrorHandler;
import org.hibernate.search.test.SearchTestCaseJUnit4;
import org.jboss.byteman.contrib.bmunit.BMRule;
import org.jboss.byteman.contrib.bmunit.BMUnitRunner;
import org.junit.Assert;
import org.junit.Test;
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
public class ConcurrentMergeErrorHandledTest extends SearchTestCaseJUnit4 {

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
		SearchFactoryImplementor searchFactory = getSearchFactoryImpl();
		ErrorHandler errorHandler = searchFactory.getErrorHandler();
		Assert.assertTrue( errorHandler instanceof MockErrorHandler );
		return (MockErrorHandler) errorHandler;
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { Foo.class };
	}

	@Override
	protected void configure(org.hibernate.cfg.Configuration cfg) {
		super.configure( cfg );
		cfg.setProperty( Environment.ERROR_HANDLER, MockErrorHandler.class.getName() );
	}
}
