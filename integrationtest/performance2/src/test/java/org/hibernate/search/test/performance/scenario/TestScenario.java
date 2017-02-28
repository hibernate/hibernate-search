/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2013, Red Hat, Inc. and/or its affiliates or third-party contributors as
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
package org.hibernate.search.test.performance.scenario;

import static org.apache.commons.lang.StringUtils.reverse;
import static org.hibernate.search.test.performance.task.InsertBookTask.PUBLICATION_DATE_ZERO;
import static org.hibernate.search.test.performance.task.InsertBookTask.SUMMARIES;
import static org.hibernate.search.test.performance.util.CheckerUncaughtExceptions.initUncaughtExceptionHandler;
import static org.hibernate.search.test.performance.util.Util.log;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Properties;

import org.apache.commons.lang.time.StopWatch;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
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

/**
 * @author Tomas Hradec
 */
public abstract class TestScenario {

	public final long initialOffset = 1000 * 1000;
	public final long initialAutorCount;
	public final long initialBookCount;
	public final long warmupCyclesCount;
	public final long measuredCyclesCount;

	public final StopWatch initIndexStopWatch = new StopWatch();
	public final StopWatch initDatabaseStopWatch = new StopWatch();
	public final StopWatch warmupStopWatch = new StopWatch();

	public TestScenario() {
		this.initialAutorCount = 10 * 1000;
		this.initialBookCount = 1000 * 1000;
		this.warmupCyclesCount = 100;
		this.measuredCyclesCount = 1000;
	}

	public TestScenario(long initialAutorCount, long initialBookCount, long warmupCyclesCount, long measuredCyclesCount) {
		super();
		this.initialAutorCount = initialAutorCount;
		this.initialBookCount = initialBookCount;
		this.warmupCyclesCount = warmupCyclesCount;
		this.measuredCyclesCount = measuredCyclesCount;
	}

	public Properties getHibernateProperties() {
		Properties properties = new Properties();
		properties.setProperty( "hibernate.hbm2ddl.auto", "create" );
		properties.setProperty( "hibernate.search.default.lucene_version", "LUCENE_CURRENT" );
		return properties;
	}

	public final void run(SessionFactory sf) {
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

		BatchSupport batchSupport = new BatchSupport( sf );
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

		FullTextSession s = Search.getFullTextSession( sf.openSession() );
		try {
			Transaction tx = s.beginTransaction();
			s.createSQLQuery( "delete from book_author where book_id < :id" ).setParameter( "id", initialOffset ).executeUpdate();
			s.createSQLQuery( "delete from book where id < :id" ).setParameter( "id", initialOffset ).executeUpdate();
			s.createSQLQuery( "delete from author where id < :id" ).setParameter( "id", initialOffset ).executeUpdate();

			s.purgeAll( Book.class );
			s.flush();
			s.flushToIndexes();
			tx.commit();
		}
		finally {
			s.close();
		}
	}

	protected void performMeasuring(SessionFactory sf) {
		log( "starting measuring" );

		TestContext ctx = new TestContext( this, sf );
		scheduleTasksAndStart( ctx, measuredCyclesCount );

		TestReporter.printReport( ctx );
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

}
