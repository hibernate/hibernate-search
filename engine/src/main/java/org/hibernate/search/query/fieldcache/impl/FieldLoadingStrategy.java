/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.search.query.fieldcache.impl;

import java.io.IOException;

import org.apache.lucene.index.AtomicReaderContext;

/**
 * Using as composition in implementations of {@link org.hibernate.search.query.collector.impl.FieldCacheCollector},
 * so that we can reuse different loading strategies in different kinds
 * of Collectors.
 *
 * @author Sanne Grinovero <sanne@hibernate.org> (C) 2011 Red Hat Inc.
 * @see org.hibernate.search.query.collector.impl.BigArrayFieldCacheCollectorImpl
 * @see org.hibernate.search.query.collector.impl.MapFieldCacheCollectorImpl
 */
public interface FieldLoadingStrategy {
	/**
	 * A new IndexReader is opened - implementations usually need this to
	 * load the next array of cached data.
	 *
	 * @param atomicReaderContext the {@code AtomicReaderContext} for which to load the new cache values
	 * @throws java.io.IOException in case an error occurs reading the cache values from the index
	 */
	void loadNewCacheValues(AtomicReaderContext atomicReaderContext) throws IOException;

	/**
	 * The collector wants to pick a specific element from the cache.
	 * Only at this point we convert primitives into an object if needed.
	 *
	 * @param relativeDocId the doc id relative to the current reader
	 * @return the cached field value for the document with the relative id {@code relativeDocId}.
	 */
	Object collect(int relativeDocId);
}
