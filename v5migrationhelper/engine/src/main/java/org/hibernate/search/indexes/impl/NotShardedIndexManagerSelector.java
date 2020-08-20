/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.indexes.impl;

import java.io.Serializable;
import java.util.Collections;
import java.util.Set;

import org.apache.lucene.document.Document;
import org.hibernate.search.filter.FullTextFilterImplementor;
import org.hibernate.search.indexes.spi.IndexManager;
import org.hibernate.search.indexes.spi.IndexManagerSelector;
import org.hibernate.search.spi.IndexedTypeIdentifier;

/**
 * @author Emmanuel Bernard
 */
public class NotShardedIndexManagerSelector implements IndexManagerSelector {

	private final IndexManager indexManager;
	private final Set<IndexManager> indexManagerAsSet;

	public NotShardedIndexManagerSelector(IndexManager indexManager) {
		this.indexManager = indexManager;
		this.indexManagerAsSet = Collections.singleton( indexManager );
	}

	@Override
	public Set<IndexManager> all() {
		return indexManagerAsSet;
	}

	@Override
	public IndexManager forNew(IndexedTypeIdentifier typeId, Serializable id, String idInString, Document document) {
		return indexManager;
	}

	@Override
	public Set<IndexManager> forExisting(IndexedTypeIdentifier typeId, Serializable id, String idInString) {
		return indexManagerAsSet;
	}

	@Override
	public Set<IndexManager> forFilters(FullTextFilterImplementor[] fullTextFilters) {
		return indexManagerAsSet;
	}

}
