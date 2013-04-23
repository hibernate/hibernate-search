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
package org.hibernate.search.store.impl;

import java.util.Properties;
import java.io.Serializable;

import org.apache.lucene.document.Document;
import org.hibernate.annotations.common.AssertionFailure;
import org.hibernate.search.filter.FullTextFilterImplementor;
import org.hibernate.search.indexes.spi.IndexManager;
import org.hibernate.search.store.IndexShardingStrategy;

/**
 * @author Emmanuel Bernard
 */
public class NotShardedStrategy implements IndexShardingStrategy {

	private IndexManager[] directoryProvider;

	public void initialize(Properties properties, IndexManager[] providers) {
		this.directoryProvider = providers;
		if ( directoryProvider.length > 1) {
			throw new AssertionFailure("Using SingleDirectoryProviderSelectionStrategy with multiple DirectryProviders");
		}
	}

	public IndexManager[] getIndexManagersForAllShards() {
		return directoryProvider;
	}

	public IndexManager getIndexManagerForAddition(Class<?> entity, Serializable id, String idInString, Document document) {
		return directoryProvider[0];
	}

	public IndexManager[] getIndexManagersForDeletion(Class<?> entity, Serializable id, String idInString) {
		return directoryProvider;
	}

	public IndexManager[] getIndexManagersForQuery(FullTextFilterImplementor[] fullTextFilters) {
		return directoryProvider;
	}

}
