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
package org.hibernate.search.test.performance.task;

import static org.hibernate.search.test.performance.scenario.TestContext.MEASURE_TASK_TIME;

import java.util.concurrent.atomic.AtomicLong;

import org.hibernate.Transaction;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.test.performance.scenario.TestContext;

/**
 * @author Tomas Hradec
 */
public abstract class AbstractTask implements Runnable {

	protected final TestContext ctx;

	private final AtomicLong counter = new AtomicLong( 0 );
	private final AtomicLong timer = new AtomicLong( 0 );

	public AbstractTask(TestContext ctx) {
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
		if ( MEASURE_TASK_TIME ) {
			startTime = System.nanoTime();
		}

		FullTextSession s = Search.getFullTextSession( ctx.sf.openSession() );
		Transaction tx = s.beginTransaction();
		try {
			execute( s );
			tx.commit();
		}
		catch (RuntimeException e) {
			tx.rollback();
			throw e;
		}
		finally {
			s.close();
		}

		if ( MEASURE_TASK_TIME ) {
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
