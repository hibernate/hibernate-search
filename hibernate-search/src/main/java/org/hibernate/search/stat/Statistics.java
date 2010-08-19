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
package org.hibernate.search.stat;

/**
 * @author Hardy Ferentschik
 */
public interface Statistics {
	/**
	 * Reset all statistics.
	 */
	void clear();

	/**
	 * Get global number of executed search queries
	 *
	 * @return search query execution count
	 */
	long getSearchQueryExecutionCount();

	/**
	 * Get the total search time in milliseconds.
	 */
	long getSearchQueryTotalTime();	

	/**
	 * Get the time in milliseconds of the slowest search.
	 */
	long getSearchQueryExecutionMaxTime();

	/**
	 * Get the average search time in milliseconds.
	 */
	long getSearchQueryExecutionAvgTime();

	/**
	 * Get the query string for the slowest query.
	 */
	String getSearchQueryExecutionMaxTimeQueryString();

	void searchExecuted(String searchString, long time);

	/**
	 * Are statistics logged
	 */
	public boolean isStatisticsEnabled();

	/**
	 * Enable statistics logs (this is a dynamic parameter)
	 */
	public void setStatisticsEnabled(boolean b);
}


