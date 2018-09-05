/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.performance.task;

import java.util.concurrent.atomic.AtomicLong;

import org.hibernate.Transaction;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.test.performance.scenario.TestScenarioContext;

/**
 * @author Tomas Hradec
 */
public abstract class AbstractTask implements Runnable {

	protected final TestScenarioContext ctx;

	private final AtomicLong counter = new AtomicLong( 0 );
	private final AtomicLong timer = new AtomicLong( 0 );

	public AbstractTask(TestScenarioContext ctx) {
		this.ctx = ctx;
		this.ctx.tasks.add( this );
	}

	@Override
	public final void run() {
		try {
			ctx.startSignal.await();
		}
		catch (InterruptedException e) {
			throw new RuntimeException( e );
		}

		counter.incrementAndGet();

		long startTime = 0;
		if ( ctx.testContext.measureTaskTime ) {
			startTime = System.nanoTime();
		}

		FullTextSession s = Search.getFullTextSession( ctx.testContext.sessionFactory.openSession() );
		Transaction tx = s.beginTransaction();
		try {
			execute( s );
			tx.commit();
		}
		catch (RuntimeException e) {
			tx.rollback();
			ctx.reportFailure( e );
			throw e;
		}
		finally {
			s.close();
		}

		if ( ctx.testContext.measureTaskTime ) {
			long stopTime = System.nanoTime();
			timer.addAndGet( stopTime - startTime );
		}
	}

	protected abstract void execute(FullTextSession fts);

	public final long getCounterValue() {
		return counter.get();
	}

	public final long getTimerValue() {
		return timer.get();
	}

}
