/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 *  Copyright (c) 2010, Red Hat, Inc. and/or its affiliates or third-party contributors as
 *  indicated by the @author tags or express copyright attribution
 *  statements applied by the authors.  All third-party contributions are
 *  distributed under license by Red Hat, Inc.
 *
 *  This copyrighted material is made available to anyone wishing to use, modify,
 *  copy, or redistribute it subject to the terms and conditions of the GNU
 *  Lesser General Public License, as published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 *  or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 *  for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with this distribution; if not, write to:
 *  Free Software Foundation, Inc.
 *  51 Franklin Street, Fifth Floor
 *  Boston, MA  02110-1301  USA
 */
package org.hibernate.search.jmx;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * @author Hardy Ferentschik
 */
public class StatisticsImpl implements StatisticsImplMBean {
	private AtomicLong searchQueryCount = new AtomicLong();
	private AtomicLong searchExecutionTotalTime = new AtomicLong();
	private AtomicLong searchExecutionMaxTime = new AtomicLong();
	private volatile String queryExecutionMaxTimeQueryString;
	private volatile boolean isStatisticsEnabled;

	private final Lock readLock;
	private final Lock writeLock;

	{
		ReadWriteLock lock = new ReentrantReadWriteLock();
		readLock = lock.readLock();
		writeLock = lock.writeLock();
	}

	public void clear() {
		searchQueryCount.set( 0 );
		searchExecutionTotalTime.set( 0 );
		searchExecutionMaxTime.set( 0 );
		queryExecutionMaxTimeQueryString = "";
	}

	public long getSearchQueryExecutionCount() {
		return searchQueryCount.get();
	}

	public long getSearchQueryTotalTime() {
		return searchExecutionTotalTime.get();
	}

	public long getSearchQueryExecutionMaxTime() {
		return searchExecutionMaxTime.get();
	}

	public long getSearchQueryExecutionAvgTime() {
		writeLock.lock();
		try {
			long avgExecutionTime = 0;
			if ( searchQueryCount.get() > 0 ) {
				avgExecutionTime = searchExecutionTotalTime.get() / searchQueryCount.get();
			}
			return avgExecutionTime;
		}
		finally {
			writeLock.unlock();
		}
	}

	public String getSearchQueryExecutionMaxTimeQueryString() {
		return queryExecutionMaxTimeQueryString;
	}

	public void searchExecuted(String searchString, long time) {
		readLock.lock();
		try {
			boolean isLongestQuery = false;
			for ( long old = searchExecutionMaxTime.get();
				  ( time > old ) && ( isLongestQuery = searchExecutionMaxTime.compareAndSet( old, time ) );
				  old = searchExecutionMaxTime.get() ) {
				;
			}
			if ( isLongestQuery ) {
				queryExecutionMaxTimeQueryString = searchString;
			}
			searchQueryCount.getAndIncrement();
			searchExecutionTotalTime.addAndGet( time );
		}
		finally {
			readLock.unlock();
		}
	}

	public boolean isStatisticsEnabled() {
		return isStatisticsEnabled;
	}

	public void setStatisticsEnabled(boolean b) {
		isStatisticsEnabled = b;
	}
}


