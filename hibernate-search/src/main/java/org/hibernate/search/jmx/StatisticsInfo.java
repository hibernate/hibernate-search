/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat, Inc. and/or its affiliates or third-party contributors as
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

// $Id:$
package org.hibernate.search.jmx;

import java.util.Map;
import java.util.Set;

import org.hibernate.search.stat.Statistics;

/**
 * @author Hardy Ferentschik
 */
public class StatisticsInfo implements StatisticsInfoMBean {
	private final Statistics delegate;

	public StatisticsInfo(Statistics delegate) {
		this.delegate = delegate;
	}

	public void clear() {
		delegate.clear();
	}

	public long getSearchQueryExecutionCount() {
		return delegate.getSearchQueryExecutionCount();
	}

	public long getSearchQueryTotalTime() {
		return delegate.getSearchQueryTotalTime();
	}

	public long getSearchQueryExecutionMaxTime() {
		return delegate.getSearchQueryExecutionMaxTime();
	}

	public long getSearchQueryExecutionAvgTime() {
		return delegate.getSearchQueryExecutionAvgTime();
	}

	public String getSearchQueryExecutionMaxTimeQueryString() {
		return delegate.getSearchQueryExecutionMaxTimeQueryString();
	}

	public long getObjectLoadingTotalTime() {
		return delegate.getObjectLoadingTotalTime();
	}

	public long getObjectLoadingExecutionMaxTime() {
		return delegate.getObjectLoadingExecutionMaxTime();
	}

	public long getObjectLoadingExecutionAvgTime() {
		return delegate.getObjectLoadingExecutionAvgTime();
	}

	public long getObjectsLoadedCount() {
		return delegate.getObjectsLoadedCount();
	}

	public boolean isStatisticsEnabled() {
		return delegate.isStatisticsEnabled();
	}

	public void setStatisticsEnabled(boolean b) {
		delegate.setStatisticsEnabled( b );
	}

	public String getSearchVersion() {
		return delegate.getSearchVersion();
	}

	public Set<String> getIndexedClassNames() {
		return delegate.getIndexedClassNames();
	}

	public int getNumberOfIndexedEntities(String entity) {
		return delegate.getNumberOfIndexedEntities( entity );
	}

	public Map<String, Integer> indexedEntitiesCount() {
		return delegate.indexedEntitiesCount();
	}
}


