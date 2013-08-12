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

	@Override
	public void clear() {
		delegate.clear();
	}

	@Override
	public long getSearchQueryExecutionCount() {
		return delegate.getSearchQueryExecutionCount();
	}

	@Override
	public long getSearchQueryTotalTime() {
		return delegate.getSearchQueryTotalTime();
	}

	@Override
	public long getSearchQueryExecutionMaxTime() {
		return delegate.getSearchQueryExecutionMaxTime();
	}

	@Override
	public long getSearchQueryExecutionAvgTime() {
		return delegate.getSearchQueryExecutionAvgTime();
	}

	@Override
	public String getSearchQueryExecutionMaxTimeQueryString() {
		return delegate.getSearchQueryExecutionMaxTimeQueryString();
	}

	@Override
	public long getObjectLoadingTotalTime() {
		return delegate.getObjectLoadingTotalTime();
	}

	@Override
	public long getObjectLoadingExecutionMaxTime() {
		return delegate.getObjectLoadingExecutionMaxTime();
	}

	@Override
	public long getObjectLoadingExecutionAvgTime() {
		return delegate.getObjectLoadingExecutionAvgTime();
	}

	@Override
	public long getObjectsLoadedCount() {
		return delegate.getObjectsLoadedCount();
	}

	@Override
	public boolean isStatisticsEnabled() {
		return delegate.isStatisticsEnabled();
	}

	@Override
	public void setStatisticsEnabled(boolean b) {
		delegate.setStatisticsEnabled( b );
	}

	@Override
	public String getSearchVersion() {
		return delegate.getSearchVersion();
	}

	@Override
	public Set<String> getIndexedClassNames() {
		return delegate.getIndexedClassNames();
	}

	@Override
	public int getNumberOfIndexedEntities(String entity) {
		return delegate.getNumberOfIndexedEntities( entity );
	}

	@Override
	public Map<String, Integer> indexedEntitiesCount() {
		return delegate.indexedEntitiesCount();
	}
}


