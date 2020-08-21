/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.errorhandling;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.hibernate.search.backend.DeleteLuceneWork;
import org.hibernate.search.backend.FlushLuceneWork;
import org.hibernate.search.backend.IndexWorkVisitor;
import org.hibernate.search.backend.IndexingMonitor;
import org.hibernate.search.backend.LuceneWork;
import org.hibernate.search.backend.impl.StreamingOperationExecutorSelector;
import org.hibernate.search.backend.impl.lucene.IndexWriterDelegate;
import org.hibernate.search.backend.impl.lucene.works.LuceneWorkExecutor;
import org.hibernate.search.batchindexing.MassIndexerProgressMonitor;
import org.hibernate.search.cfg.Environment;
import org.hibernate.search.engine.spi.EntityIndexBinding;
import org.hibernate.search.exception.ErrorHandler;
import org.hibernate.search.exception.SearchException;
import org.hibernate.search.exception.impl.LogErrorHandler;
import org.hibernate.search.indexes.spi.IndexManager;
import org.hibernate.search.spi.IndexedTypeIdentifier;
import org.hibernate.search.spi.SearchIntegrator;
import org.hibernate.search.spi.impl.PojoIndexedTypeIdentifier;
import org.hibernate.search.test.SearchTestBase;
import org.hibernate.search.testsupport.junit.SkipOnElasticsearch;
import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;

/**
 * Test to verify the configured ErrorHandler is used in the Lucene
 * backend, and the backend exceptions are logged as expected.
 *
 * @see Environment#ERROR_HANDLER
 *
 * @author Sanne Grinovero
 * @since 3.2
 */
@Category(SkipOnElasticsearch.class) // This test is Lucene-specific. The equivalent for Elasticsearch is ElasticsearchIndexWorkProcessorErrorHandlingTest
public class LuceneErrorHandlingTest extends SearchTestBase {

	private static final IndexedTypeIdentifier testTypeId = new PojoIndexedTypeIdentifier( Foo.class );

	private static final AtomicInteger WORK_COUNTER = new AtomicInteger();

	@Test
	public void testErrorHandling() {
		MockErrorHandler mockErrorHandler = getErrorHandlerAndAssertCorrectTypeIsUsed();
		EntityIndexBinding mappingForEntity = getExtendedSearchIntegrator().getIndexBinding( testTypeId );
		IndexManager indexManager = mappingForEntity.getIndexManagerSelector().all().iterator().next();

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
		Assert.assertEquals( 2, WORK_COUNTER.get() );

		String errorMessage = mockErrorHandler.getErrorMessage();
		Throwable exception = mockErrorHandler.getLastException();

		StringBuilder expectedErrorMessage = new StringBuilder();
		expectedErrorMessage.append( "Exception occurred " ).append( exception ).append( "\n" );
		expectedErrorMessage.append( "Primary Failure:\n" );
		LogErrorHandler.appendFailureMessage( expectedErrorMessage, firstFailure );

		expectedErrorMessage.append( "Subsequent failures:\n" );
		LogErrorHandler.appendFailureMessage( expectedErrorMessage, firstFailure );
		LogErrorHandler.appendFailureMessage( expectedErrorMessage, thirdWork );
		LogErrorHandler.appendFailureMessage( expectedErrorMessage, fourthWork );

		// should verify the errorHandler logs the work which was not processed (third and fourth)
		// and which work was failing
		Assert.assertEquals( expectedErrorMessage.toString() , errorMessage );
		Assert.assertTrue( exception instanceof SearchException );
		Assert.assertEquals( "failed work message", exception.getMessage() );
		Assert.assertEquals( indexManager, mockErrorHandler.getIndexManager() );
	}

	@Test
	public void testNoEntityErrorHandling() {
		MockErrorHandler mockErrorHandler = getErrorHandlerAndAssertCorrectTypeIsUsed();
		EntityIndexBinding mappingForEntity = getExtendedSearchIntegrator().getIndexBinding( testTypeId );
		IndexManager indexManager = mappingForEntity.getIndexManagerSelector().all().iterator().next();

		List<LuceneWork> queue = new ArrayList<LuceneWork>();

		WORK_COUNTER.set( 0 ); // reset work counter
		final NoEntityFailingWork firstFailure = new NoEntityFailingWork();
		queue.add( firstFailure );
		indexManager.performOperations( queue, null );
		Assert.assertEquals( 0, WORK_COUNTER.get() );

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
		Assert.assertEquals( indexManager, mockErrorHandler.getIndexManager() );
	}

	private MockErrorHandler getErrorHandlerAndAssertCorrectTypeIsUsed() {
		SearchIntegrator integrator = getExtendedSearchIntegrator();
		ErrorHandler errorHandler = integrator.getErrorHandler();
		Assert.assertTrue( errorHandler instanceof MockErrorHandler );
		return (MockErrorHandler)errorHandler;
	}

	@Override
	public Class<?>[] getAnnotatedClasses() {
		return new Class[] { Foo.class };
	}

	@Override
	public void configure(Map<String,Object> cfg) {
		cfg.put( Environment.ERROR_HANDLER, MockErrorHandler.class.getName() );
	}

	/**
	 * A LuceneWork which doesn't fail and delegates to a NoOp
	 * operation on the index.
	 */
	static class HarmlessWork extends DeleteLuceneWork {

		public HarmlessWork(String workIdentifier) {
			super( workIdentifier, workIdentifier, testTypeId );
		}

		@Override
		public <P, R> R acceptIndexWorkVisitor(IndexWorkVisitor<P, R> visitor, P p) {
			if ( visitor instanceof StreamingOperationExecutorSelector ) {
				//during shard-selection visitor this work is applied to
				//all DirectoryProviders as this extends DeleteLuceneWork
				return visitor.visitDeleteWork( this, p );
			}
			else {
				return (R) new NoOpLuceneWorkDelegate();
			}
		}

		@Override
		public String toString() {
			return "HarmlessWork: " + this.getIdInString();
		}

	}

	static class NoOpLuceneWorkDelegate implements LuceneWorkExecutor {

		public void logWorkDone(LuceneWork work, MassIndexerProgressMonitor monitor) {
		}

		@Override
		public void performWork(LuceneWork work, IndexWriterDelegate delegate, IndexingMonitor monitor) {
			WORK_COUNTER.incrementAndGet();
		}

	}

	/**
	 * A LuceneWork which will throw a SearchException when applied to
	 * the index, which is the type thrown to wrap real IOExceptions.
	 */
	static class FailingWork extends DeleteLuceneWork {

		public FailingWork(String workIdentifier) {
			super( workIdentifier, workIdentifier, testTypeId );
		}

		@Override
		public <P, R> R acceptIndexWorkVisitor(IndexWorkVisitor<P, R> visitor, P p) {
			if ( visitor instanceof StreamingOperationExecutorSelector ) {
				//during shard-selection visitor this work is applied to
				//all DirectoryProviders as this extends DeleteLuceneWork
				return visitor.visitDeleteWork( this, p );
			}
			else {
				return (R) new FailingLuceneWorkDelegate();
			}
		}

		@Override
		public String toString() {
			return "FailingWork: " + this.getIdInString();
		}

	}

	static class FailingLuceneWorkDelegate implements LuceneWorkExecutor {

		public void logWorkDone(LuceneWork work, MassIndexerProgressMonitor monitor) {
		}

		@Override
		public void performWork(LuceneWork work, IndexWriterDelegate delegate, IndexingMonitor monitor) {
			throw new SearchException( "failed work message" );
		}
	}

	/**
	 * A LuceneWork with no attached entity type which will throw a
	 * SearchException when applied to the index, which is the type
	 * thrown to wrap real IOExceptions.
	 */
	static class NoEntityFailingWork extends FlushLuceneWork {

		public NoEntityFailingWork() {
			super( null, null );
		}

		@SuppressWarnings("unchecked")
		@Override
		public <P, R> R acceptIndexWorkVisitor(IndexWorkVisitor<P, R> visitor, P p) {
			return (R) new FailingLuceneWorkDelegate();
		}

		@Override
		public String toString() {
			return "NoEntityFailingWork";
		}

	}

}
