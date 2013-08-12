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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import junit.framework.Assert;
import org.apache.lucene.index.IndexWriter;

import org.hibernate.search.Environment;
import org.hibernate.search.SearchException;
import org.hibernate.search.backend.DeleteLuceneWork;
import org.hibernate.search.backend.IndexingMonitor;
import org.hibernate.search.backend.LuceneWork;
import org.hibernate.search.backend.impl.StreamingSelectionVisitor;
import org.hibernate.search.backend.impl.WorkVisitor;
import org.hibernate.search.backend.impl.lucene.works.LuceneWorkDelegate;
import org.hibernate.search.batchindexing.MassIndexerProgressMonitor;
import org.hibernate.search.engine.spi.EntityIndexBinding;
import org.hibernate.search.engine.spi.SearchFactoryImplementor;
import org.hibernate.search.exception.ErrorHandler;
import org.hibernate.search.exception.impl.LogErrorHandler;
import org.hibernate.search.indexes.spi.IndexManager;
import org.hibernate.search.test.SearchTestCase;

/**
 * Test to verify the configured ErrorHandler is used in the Lucene
 * backend, and the backend exceptions are logged as expected.
 *
 * @see Environment#ERROR_HANDLER
 *
 * @author Sanne Grinovero
 * @since 3.2
 */
public class LuceneErrorHandlingTest extends SearchTestCase {

	static final AtomicInteger WORK_COUNTER = new AtomicInteger();

	public void testErrorHandling() {
		MockErrorHandler mockErrorHandler = getErrorHandlerAndAssertCorrectTypeIsUsed();
		EntityIndexBinding mappingForEntity = getSearchFactoryImpl().getIndexBinding( Foo.class );
		IndexManager indexManager = mappingForEntity.getIndexManagers()[0];

		List<LuceneWork> queue = new ArrayList<LuceneWork>();
		queue.add( new HarmlessWork( "firstWork" ) );
		queue.add( new HarmlessWork( "secondWork" ) );
		WORK_COUNTER.set( 0 ); // reset work counter
		indexManager.performOperations( queue, null );
		Assert.assertEquals( 2, WORK_COUNTER.get() );

		WORK_COUNTER.set( 0 ); // reset work counter
		final FailingWork firstFailure = new FailingWork( "firstFailure" );
		queue.add( firstFailure );
		final HarmlessWork thirdWork = new HarmlessWork( "thirdWork" );
		queue.add( thirdWork );
		final HarmlessWork fourthWork = new HarmlessWork( "fourthWork" );
		queue.add( fourthWork );
		indexManager.performOperations( queue, null );
		Assert.assertEquals( 4, WORK_COUNTER.get() );

		String errorMessage = mockErrorHandler.getErrorMessage();
		Throwable exception = mockErrorHandler.getLastException();

		StringBuilder expectedErrorMessage = new StringBuilder();
		expectedErrorMessage.append( "Exception occurred " ).append( exception ).append( "\n" );
		expectedErrorMessage.append( "Primary Failure:\n" );
		LogErrorHandler.appendFailureMessage( expectedErrorMessage, firstFailure );

		expectedErrorMessage.append( "Subsequent failures:\n" );
		LogErrorHandler.appendFailureMessage( expectedErrorMessage, firstFailure );

		// should verify the errorHandler logs the work which was not processed (third and fourth)
		// and which work was failing
		Assert.assertEquals( expectedErrorMessage.toString() , errorMessage );
		Assert.assertTrue( exception instanceof SearchException );
		Assert.assertEquals( "failed work message", exception.getMessage() );
	}

	private MockErrorHandler getErrorHandlerAndAssertCorrectTypeIsUsed() {
		SearchFactoryImplementor searchFactory = getSearchFactoryImpl();
		ErrorHandler errorHandler = searchFactory.getErrorHandler();
		Assert.assertTrue( errorHandler instanceof MockErrorHandler );
		return (MockErrorHandler)errorHandler;
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

	/**
	 * A LuceneWork which doesn't fail and delegates to a NoOp
	 * operation on the index.
	 */
	static class HarmlessWork extends DeleteLuceneWork {

		public HarmlessWork(String workIdentifier) {
			super( workIdentifier, workIdentifier, Foo.class );
		}

		@Override
		public <T> T getWorkDelegate(WorkVisitor<T> visitor) {
			if ( visitor instanceof StreamingSelectionVisitor ) {
				//during shard-selection visitor this work is applied to
				//all DirectoryProviders as this extends DeleteLuceneWork
				return visitor.getDelegate( this );
			}
			else {
				return (T) new NoOpLuceneWorkDelegate();
			}
		}

		@Override
		public String toString() {
			return "HarmlessWork: " + this.getIdInString();
		}

	}

	static class NoOpLuceneWorkDelegate implements LuceneWorkDelegate {

		public void logWorkDone(LuceneWork work, MassIndexerProgressMonitor monitor) {
		}

		@Override
		public void performWork(LuceneWork work, IndexWriter writer, IndexingMonitor monitor) {
			WORK_COUNTER.incrementAndGet();
		}

	}

	/**
	 * A LuceneWork which will throw a SearchException when applied to
	 * the index, which is the type thrown to wrap real IOExceptions.
	 */
	static class FailingWork extends DeleteLuceneWork {

		public FailingWork(String workIdentifier) {
			super( workIdentifier, workIdentifier, Foo.class );
		}

		@Override
		public <T> T getWorkDelegate(WorkVisitor<T> visitor) {
			if ( visitor instanceof StreamingSelectionVisitor ) {
				//during shard-selection visitor this work is applied to
				//all DirectoryProviders as this extends DeleteLuceneWork
				return visitor.getDelegate( this );
			}
			else {
				return (T) new FailingLuceneWorkDelegate();
			}
		}

		@Override
		public String toString() {
			return "FailingWork: " + this.getIdInString();
		}

	}

	static class FailingLuceneWorkDelegate implements LuceneWorkDelegate {

		public void logWorkDone(LuceneWork work, MassIndexerProgressMonitor monitor) {
		}

		@Override
		public void performWork(LuceneWork work, IndexWriter writer, IndexingMonitor monitor) {
			throw new SearchException( "failed work message" );
		}
	}

}
