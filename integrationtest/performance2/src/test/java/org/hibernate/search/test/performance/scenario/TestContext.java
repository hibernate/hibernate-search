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

import static org.hibernate.search.test.performance.util.Util.runGarbageCollectorAndWait;

import java.util.List;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.hibernate.SessionFactory;
import org.hibernate.search.test.performance.task.AbstractTask;

/**
 * @author Tomas Hradec
 */
public class TestContext {

	public static final boolean VERBOSE = true;
	public static final boolean MEASURE_MEMORY = true;
	public static final boolean MEASURE_TASK_TIME = true;
	public static final boolean ASSERT_QUERY_RESULTS = true;
	public static final boolean CHECK_INDEX_STATE = true;
	public static final int MAX_AUTHORS = 1000;
	public static final int THREADS_COUNT = 10;

	public final SessionFactory sf;
	public final TestScenario scenario;
	public final ExecutorService executor = Executors.newFixedThreadPool( THREADS_COUNT );
	public final CountDownLatch startSignal = new CountDownLatch( 1 );

	public final List<AbstractTask> tasks = new CopyOnWriteArrayList<AbstractTask>();
	public final AtomicLong bookIdCounter = new AtomicLong( 0 );
	public final AtomicLong authorIdCounter = new AtomicLong( 0 );
	public final Random bookRandom = new Random();
	public final Random authorRandom = new Random();

	public long startTime;
	public long stopTime;
	public long freeMemory;
	public long totalMemory;

	public TestContext(TestScenario testScenario, SessionFactory sf) {
		this.scenario = testScenario;
		this.sf = sf;

		if ( MEASURE_MEMORY ) {
			runGarbageCollectorAndWait();
			freeMemory = Runtime.getRuntime().freeMemory();
			totalMemory = Runtime.getRuntime().totalMemory();
		}
	}

	public long getRandomBookId() {
		long bookId = bookIdCounter.get();
		if ( bookId > 0 ) {
			return Math.abs( bookRandom.nextLong() ) % bookId;
		}
		return 0;
	}

	public long getRandomAutorId() {
		long authorId = authorIdCounter.get();
		if ( authorId > 0 ) {
			return Math.abs( authorRandom.nextLong() ) % authorIdCounter.get();
		}
		return 0;
	}

	public void startAndWait() {
		try {
			startTime = System.nanoTime();
			startSignal.countDown();
			executor.shutdown();
			executor.awaitTermination( 1, TimeUnit.DAYS );
			stopTime = System.nanoTime();
		}
		catch (InterruptedException e) {
			throw new RuntimeException( e );
		}
	}

}
