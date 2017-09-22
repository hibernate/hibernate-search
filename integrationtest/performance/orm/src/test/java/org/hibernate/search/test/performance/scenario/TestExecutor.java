/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.performance.scenario;

import java.io.IOException;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import javax.transaction.TransactionManager;

import org.hibernate.Session;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.engine.transaction.jta.platform.spi.JtaPlatform;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.test.performance.model.Book;
import org.hibernate.search.test.performance.util.BatchCallback;
import org.hibernate.search.test.performance.util.BatchSupport;
import org.hibernate.search.util.impl.SearchThreadFactory;

import static org.apache.commons.lang.StringUtils.reverse;
import static org.hibernate.search.test.performance.task.InsertBookTask.PUBLICATION_DATE_ZERO;
import static org.hibernate.search.test.performance.task.InsertBookTask.SUMMARIES;
import static org.hibernate.search.test.performance.util.Util.log;

/**
 * @author Tomas Hradec
 */
class TestExecutor {

	public final void run(TestContext testContext, TestScenarioContext warmupContext, TestScenarioContext measureContext)
			throws IOException {
		initDatabase( testContext );
		performWarmUp( warmupContext );
		performCleanUp( testContext );
		if ( warmupContext.getFailures().isEmpty() ) {
			initIndex( testContext );
			performMeasuring( measureContext );
		}
		TestReporter.printReport( testContext, warmupContext, measureContext );

		// Propagate the very first failure, which is likely to be the most important one
		if ( !warmupContext.getFailures().isEmpty() ) {
			throw new RuntimeException(
					"Warmup phase failed due to an unexpected exception",
					warmupContext.getFailures().iterator().next() );
		}
		else if ( !measureContext.getFailures().isEmpty() ) {
			throw new RuntimeException(
					"Measure phase failed due to an unexpected exception",
					measureContext.getFailures().iterator().next() );
		}
	}

	protected void initDatabase(TestContext ctx) {
		log( "starting initialize database" );

		ctx.initDatabaseStopWatch.start();

		BatchSupport batchSupport = new BatchSupport( ctx.sessionFactory, ctx.initialOffset );
		batchSupport.execute(
				"insert into author(id, name) values(?, ?)",
				ctx.initialAuthorCount,
				new BatchCallback() {
					@Override
					public void initStatement(PreparedStatement ps, long id) throws SQLException {
						ps.setLong( 1, id );
						ps.setString( 2, "author" + id );
					}
				} );
		batchSupport.execute(
				"insert into book(id, title, summary, rating, totalSold, publicationDate) values(?, ?, ?, ?, ?, ?)",
				ctx.initialBookCount,
				new BatchCallback() {
					@Override
					public void initStatement(PreparedStatement ps, long id) throws SQLException {
						ps.setLong( 1, id );
						ps.setString( 2, "title" + id );
						ps.setString( 3, reverse( SUMMARIES[(int) ( id % SUMMARIES.length )] ) );
						ps.setLong( 4, -1 );
						ps.setLong( 5, -1 );
						ps.setDate( 6, new Date( PUBLICATION_DATE_ZERO.getTime() ) );
					}
				} );
		batchSupport.execute(
				"insert into book_author(book_id, authors_id) values(?, ?)",
				ctx.initialBookCount,
				new BatchCallback() {
					@Override
					public void initStatement(PreparedStatement ps, long id) throws SQLException {
						ps.setLong( 1, id );
						ps.setLong( 2, ctx.initialOffset + ( id % ctx.initialAuthorCount ) );
					}
				} );

		ctx.initDatabaseStopWatch.stop();
	}

	protected void initIndex(TestContext ctx) {
		if ( !ctx.initIndex ) {
			log( "skipping index initialization as requested" );
			return;
		}

		log( "starting initialize index" );

		ctx.initIndexStopWatch.start();

		FullTextSession s = Search.getFullTextSession( ctx.sessionFactory.openSession() );
		try {
			s.createIndexer().startAndWait();
		}
		catch (InterruptedException e) {
			throw new RuntimeException( e );
		}
		finally {
			s.close();
		}

		ctx.initIndexStopWatch.stop();
	}

	private void performWarmUp(TestScenarioContext ctx) {
		log( "starting warm up phase" );

		scheduleTasksAndStart( ctx, ctx.testContext.warmupCyclesCount );
	}

	private void performCleanUp(TestContext ctx) {
		log( "starting clean up phase" );
		try ( Session s = ctx.sessionFactory.openSession() ) {
			final SessionImplementor session = (SessionImplementor) s;
			FullTextSession fulltextSession = Search.getFullTextSession( s );
			beginTransaction( session );
			s.createNativeQuery( "delete from book_author where book_id < :id" ).setParameter( "id", ctx.initialOffset ).executeUpdate();
			s.createNativeQuery( "delete from book where id < :id" ).setParameter( "id", ctx.initialOffset ).executeUpdate();
			s.createNativeQuery( "delete from author where id < :id" ).setParameter( "id", ctx.initialOffset ).executeUpdate();

			fulltextSession.purgeAll( Book.class );
			fulltextSession.flushToIndexes();
			commitTransaction( session );
		}
	}

	private void performMeasuring(TestScenarioContext ctx) throws IOException {
		log( "starting measuring" );

		scheduleTasksAndStart( ctx, ctx.testContext.measuredCyclesCount );
	}

	private void scheduleTasksAndStart(TestScenarioContext ctx, long cyclesCount) {
		ExecutorService executor = newAutoStoppingErrorReportingThreadPool( ctx );
		for ( int i = 0; i < cyclesCount; i++ ) {
			for ( Runnable task : ctx.tasks ) {
				executor.execute( task );
			}
		}

		try {
			ctx.executionStopWatch.start();
			ctx.startSignal.countDown();
			executor.shutdown();
			executor.awaitTermination( 1, TimeUnit.DAYS );
			ctx.executionStopWatch.stop();
		}
		catch (InterruptedException e) {
			throw new RuntimeException( e );
		}
	}

	private ExecutorService newAutoStoppingErrorReportingThreadPool(TestScenarioContext ctx) {
		int nThreads = ctx.testContext.threadCount;
		ThreadFactory threadFactory = new SearchThreadFactory( ctx.scenario.getClass().getSimpleName() ) {
			@Override
			public Thread newThread(Runnable r) {
				Thread t = super.newThread( r );
				// Just ignore uncaught exceptions, we'll report them through other means (see below)
				t.setUncaughtExceptionHandler( (thread, throwable) -> { } );
				return t;
			}
		};
		return new ThreadPoolExecutor(
				nThreads, nThreads,
				0L, TimeUnit.MILLISECONDS,
				new LinkedBlockingQueue<>(), threadFactory
				) {
			@Override
			protected void afterExecute(Runnable r, Throwable t) {
				super.afterExecute( r, t );
				if ( t != null ) {
					ctx.reportFailure( t );
					shutdown();
				}
			}
		};
	}

	private void commitTransaction(SessionImplementor session) {
		if ( session.getTransactionCoordinator().getTransactionCoordinatorBuilder().isJta() ) {
			TransactionManager transactionManager = lookupTransactionManager( session );
			try {
				transactionManager.commit();
			}
			catch (Exception e) {
				throw new RuntimeException( e );
			}
		}
		else {
			session.getTransaction().commit();
		}
	}

	private void beginTransaction(SessionImplementor session) {
		if ( session.getTransactionCoordinator().getTransactionCoordinatorBuilder().isJta() ) {
			TransactionManager transactionManager = lookupTransactionManager( session );
			try {
				transactionManager.begin();
			}
			catch (Exception e) {
				throw new RuntimeException( e );
			}
		}
		else {
			session.getTransaction().begin();
		}
	}

	private static TransactionManager lookupTransactionManager(SessionImplementor session) {
		return session
				.getSessionFactory()
				.getServiceRegistry()
				.getService( JtaPlatform.class )
				.retrieveTransactionManager();
	}

}
