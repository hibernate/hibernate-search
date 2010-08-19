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

import org.hibernate.search.jmx.StatisticsImplMBean;
import org.hibernate.search.stat.Statistics;

/**
 * @author Hardy Ferentschik
 */
public class StatisticsImpl implements Statistics, StatisticsImplMBean {
	private AtomicLong searchQueryCount = new AtomicLong();
	private AtomicLong searchExecutionAvgTime = new AtomicLong();
	private AtomicLong searchExecutionTotalTime = new AtomicLong();
	private AtomicLong searchExecutionMaxTime = new AtomicLong();
	private AtomicLong searchExecutionMinTime = new AtomicLong();
	private volatile String queryExecutionMaxTimeQueryString;
	private volatile boolean isStatisticsEnabled;


	public void clear() {
		searchQueryCount.set( 0 );
	}

	public long getSearchQueryExecutionCount() {
		return searchQueryCount.get();
	}

	public long getSearchQueryExecutionMaxTime() {
		return searchExecutionMaxTime.get();
	}

	public long getSearchQueryExecutionMinTime() {
		return searchExecutionMinTime.get();
	}

	public long getSearchQueryExecutionAvgTime() {
		return searchExecutionAvgTime.get();
	}

	public String getSearchQueryExecutionMaxTimeQueryString() {
		return queryExecutionMaxTimeQueryString;
	}

	public void searchExecuted(String searchString, long time) {
		searchQueryCount.getAndIncrement();
		boolean isLongestQuery = false;
		for ( long old = searchExecutionMaxTime.get();
			  ( time > old ) && ( isLongestQuery = !searchExecutionMaxTime.compareAndSet( old, time ) );
			  old = searchExecutionMaxTime.get() ) {
			;
		}
		if ( isLongestQuery ) {
			queryExecutionMaxTimeQueryString = searchString;
		}

		boolean isShortestQuery = false;
		for ( long old = searchExecutionMinTime.get();
			  ( time < old ) && ( isShortestQuery = !searchExecutionMinTime.compareAndSet( old, time ) );
			  old = searchExecutionMinTime.get() ) {
			;
		}

		searchExecutionTotalTime.getAndAdd( time );
		searchExecutionAvgTime.getAndSet( searchExecutionTotalTime.get() / searchQueryCount.get() );
	}

	public boolean isStatisticsEnabled() {
		return isStatisticsEnabled;
	}

	public void setStatisticsEnabled(boolean b) {
		isStatisticsEnabled = b;
	}
}


