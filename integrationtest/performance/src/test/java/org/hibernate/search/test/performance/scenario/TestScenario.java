/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.performance.scenario;

import static org.apache.commons.lang.StringUtils.reverse;
import static org.hibernate.search.test.performance.task.InsertBookTask.PUBLICATION_DATE_ZERO;
import static org.hibernate.search.test.performance.task.InsertBookTask.SUMMARIES;
import static org.hibernate.search.test.performance.util.CheckerUncaughtExceptions.initUncaughtExceptionHandler;
import static org.hibernate.search.test.performance.util.Util.log;

import java.io.UnsupportedEncodingException;
import java.io.IOException;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Properties;

import javax.transaction.TransactionManager;

import org.apache.commons.lang.time.StopWatch;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.engine.transaction.jta.platform.spi.JtaPlatform;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.test.performance.model.Book;
import org.hibernate.search.test.performance.task.InsertBookTask;
import org.hibernate.search.test.performance.task.QueryBooksByAuthorTask;
import org.hibernate.search.test.performance.task.QueryBooksByAverageRatingTask;
import org.hibernate.search.test.performance.task.QueryBooksByBestRatingTask;
import org.hibernate.search.test.performance.task.QueryBooksByNewestPublishedTask;
import org.hibernate.search.test.performance.task.QueryBooksBySummaryTask;
import org.hibernate.search.test.performance.task.QueryBooksByTitleTask;
import org.hibernate.search.test.performance.task.QueryBooksByTotalSoldTask;
import org.hibernate.search.test.performance.task.UpdateBookRatingTask;
import org.hibernate.search.test.performance.task.UpdateBookTotalSoldTask;
import org.hibernate.search.test.performance.util.BatchCallback;
import org.hibernate.search.test.performance.util.BatchSupport;
import org.hibernate.search.testsupport.TestConstants;

/**
 * @author Tomas Hradec
 */
public abstract class TestScenario {

	private static final Boolean PERFORMANCE_ENABLED = TestConstants.arePerformanceTestsEnabled();

	public final long initialOffset;
	public final long initialAutorCount;
	public final long initialBookCount;
	public final long warmupCyclesCount;
	public final long measuredCyclesCount;

	public final StopWatch initIndexStopWatch = new StopWatch();
	public final StopWatch initDatabaseStopWatch = new StopWatch();
	public final StopWatch warmupStopWatch = new StopWatch();

	public TestScenario() {
		if ( PERFORMANCE_ENABLED ) {
			this.initialAutorCount = 10_000;
			this.initialBookCount = 1_000_000;
			this.warmupCyclesCount = 1_000;
			this.measuredCyclesCount = 5_000;
			this.initialOffset = 1_000_000;
		}
		else {
			this.initialAutorCount = 10;
			this.initialBookCount = 100;
			this.warmupCyclesCount = 1;
			this.measuredCyclesCount = 1;
			this.initialOffset = 100;
		}
	}

	public TestScenario(long initialAutorCount, long initialBookCount, long warmupCyclesCount, long measuredCyclesCount) {
		super();
		this.initialAutorCount = initialAutorCount;
		this.initialBookCount = initialBookCount;
		this.warmupCyclesCount = warmupCyclesCount;
		this.measuredCyclesCount = measuredCyclesCount;
		this.initialOffset = 1_000_000;
	}

	public Properties getHibernateProperties() {
		Properties properties = new Properties();
		properties.setProperty( "hibernate.hbm2ddl.auto", "create" );
		properties.setProperty( "hibernate.search.default.lucene_version", "LUCENE_CURRENT" );
		properties.setProperty( "wildfly.jpa.hibernate.search.module", "none" );
		return properties;
	}

	public final void run(SessionFactory sf) throws IOException {
		initUncaughtExceptionHandler();
		initDatabase( sf );
		performWarmUp( sf );
		performCleanUp( sf );
		initIndex( sf );
		performMeasuring( sf );
	}

	protected void initDatabase(SessionFactory sf) {
		log( "starting initialize database" );

		initDatabaseStopWatch.start();

		BatchSupport batchSupport = new BatchSupport( sf, initialOffset );
		batchSupport.execute( "insert into author(id, name) values(?, ?)", initialAutorCount,
				new BatchCallback() {
					@Override
					public void initStatement(PreparedStatement ps, long id) throws SQLException {
						ps.setLong( 1, id );
						ps.setString( 2, "autor" + id );
					}
				} );
		batchSupport.execute( "insert into book(id, title, summary, rating, totalSold, publicationDate) values(?, ?, ?, ?, ?, ?)", initialBookCount,
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
		batchSupport.execute( "insert into book_author(book_id, authors_id) values(?, ?)", initialBookCount,
				new BatchCallback() {
					@Override
					public void initStatement(PreparedStatement ps, long id) throws SQLException {
						ps.setLong( 1, id );
						ps.setLong( 2, initialOffset + ( id % initialAutorCount ) );
					}
				} );

		initDatabaseStopWatch.stop();
	}

	protected void initIndex(SessionFactory sf) {
		log( "starting initialize index" );

		initIndexStopWatch.start();

		FullTextSession s = Search.getFullTextSession( sf.openSession() );
		try {
			s.createIndexer().startAndWait();
		}
		catch (InterruptedException e) {
			throw new RuntimeException( e );
		}
		finally {
			s.close();
		}

		initIndexStopWatch.stop();
	}

	protected void performWarmUp(SessionFactory sf) {
		log( "starting warm up phase" );

		warmupStopWatch.start();

		TestContext ctx = new TestContext( this, sf );
		scheduleTasksAndStart( ctx, warmupCyclesCount );

		warmupStopWatch.stop();
	}

	protected void performCleanUp(SessionFactory sf) {
		log( "starting clean up phase" );
		try ( Session s = sf.openSession() ) {
			final SessionImplementor session = (SessionImplementor) s;
			FullTextSession fulltextSession = Search.getFullTextSession( s );
			beginTransaction( session );
			s.createNativeQuery( "delete from book_author where book_id < :id" ).setParameter( "id", initialOffset ).executeUpdate();
			s.createNativeQuery( "delete from book where id < :id" ).setParameter( "id", initialOffset ).executeUpdate();
			s.createNativeQuery( "delete from author where id < :id" ).setParameter( "id", initialOffset ).executeUpdate();

			fulltextSession.purgeAll( Book.class );
			fulltextSession.flushToIndexes();
			commitTransaction( session );
		}
	}

	protected void performMeasuring(SessionFactory sf) throws IOException {
		log( "starting measuring" );

		TestContext ctx = new TestContext( this, sf );
		scheduleTasksAndStart( ctx, measuredCyclesCount );

		try {
			TestReporter.printReport( ctx );
		}
		catch (UnsupportedEncodingException e) {
			throw new RuntimeException( e );
		}
	}

	protected void scheduleTasksAndStart(TestContext ctx, long cyclesCount) {
		InsertBookTask insertBookTask = new InsertBookTask( ctx );
		UpdateBookRatingTask updateBookRatingTask = new UpdateBookRatingTask( ctx );
		UpdateBookTotalSoldTask updateBookTotalSoldTask = new UpdateBookTotalSoldTask( ctx );
		QueryBooksByAuthorTask queryBooksByAuthorTask = new QueryBooksByAuthorTask( ctx );
		QueryBooksByAverageRatingTask queryBooksByAverageRatingTask = new QueryBooksByAverageRatingTask( ctx );
		QueryBooksByBestRatingTask queryBooksByBestRatingTask = new QueryBooksByBestRatingTask( ctx );
		QueryBooksByNewestPublishedTask queryBooksByNewestPublishedTask = new QueryBooksByNewestPublishedTask( ctx );
		QueryBooksBySummaryTask queryBooksBySummaryTask = new QueryBooksBySummaryTask( ctx );
		QueryBooksByTitleTask queryBooksByTitleTask = new QueryBooksByTitleTask( ctx );
		QueryBooksByTotalSoldTask queryBooksByTotalSoldTask = new QueryBooksByTotalSoldTask( ctx );

		for ( int i = 0; i < cyclesCount; i++ ) {
			ctx.executor.execute( insertBookTask );
			ctx.executor.execute( updateBookRatingTask );
			ctx.executor.execute( updateBookTotalSoldTask );
			ctx.executor.execute( queryBooksByAuthorTask );
			ctx.executor.execute( queryBooksByAverageRatingTask );
			ctx.executor.execute( queryBooksByBestRatingTask );
			ctx.executor.execute( queryBooksByNewestPublishedTask );
			ctx.executor.execute( queryBooksBySummaryTask );
			ctx.executor.execute( queryBooksByTitleTask );
			ctx.executor.execute( queryBooksByTotalSoldTask );
		}

		ctx.startAndWait();
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
