/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.search.query.collector.impl;

import java.io.IOException;

import org.apache.lucene.index.AtomicReaderContext;
import org.apache.lucene.search.Collector;
import org.hibernate.search.query.fieldcache.impl.FieldLoadingStrategy;

/**
 * {@code FieldCacheCollector} using an internal array to collect extracted values
 * from the {@link org.apache.lucene.search.FieldCache}. Note that the size of the array is as big as the number
 * of {@code Document}s in the index, so if we only need to collect a small amount
 * of values other implementations are likely able to be more conservative
 * on memory requirements.
 *
 * @author Sanne Grinovero <sanne@hibernate.org> (C) 2011 Red Hat Inc.
 */
final class BigArrayFieldCacheCollectorImpl extends FieldCacheCollector {
	private final Object[] valuePerDocumentId;

	private int currentDocBase;
	private final FieldLoadingStrategy cacheLoadingStrategy;

	public BigArrayFieldCacheCollectorImpl(Collector delegate, FieldLoadingStrategy cacheLoadingStrategy, Object[] valueContainer) {
		super( delegate );
		this.cacheLoadingStrategy = cacheLoadingStrategy;
		this.valuePerDocumentId = valueContainer;
	}

	@Override
	public void collect(int doc) throws IOException {
		//warning when changing this method: extremely performance sensitive!
		this.delegate.collect( doc );
		this.valuePerDocumentId[currentDocBase + doc] = cacheLoadingStrategy.collect( doc );
	}

	@Override
	public void setNextReader(AtomicReaderContext context) throws IOException {
		this.currentDocBase = context.docBase;
		this.cacheLoadingStrategy.loadNewCacheValues( context );
		this.delegate.setNextReader( context );
	}

	@Override
	public Object getValue(int docId) {
		return valuePerDocumentId[docId];
	}

}
