/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.search.query.collector.impl;

import java.io.IOException;
import java.util.Map;

import org.apache.lucene.index.AtomicReaderContext;
import org.apache.lucene.search.Collector;
import org.hibernate.search.query.fieldcache.impl.FieldLoadingStrategy;

import static org.hibernate.search.util.impl.CollectionHelper.newHashMap;

/**
 * This Collector uses an internal Map to store collected values from the FieldCache.
 * Beware that when this map grows too much the put operation becomes a performance bottleneck,
 * so for large resultsets you should use BigArrayFieldCacheCollectorImpl instead.
 *
 * @author Sanne Grinovero <sanne@hibernate.org> (C) 2011 Red Hat Inc.
 * @see org.hibernate.search.query.collector.impl.BigArrayFieldCacheCollectorImpl
 */
final class MapFieldCacheCollectorImpl extends FieldCacheCollector {

	private final FieldLoadingStrategy collectorStrategy;
	private final Map<Integer, Object> valuePerDocumentId = newHashMap();

	private int currentDocBase;

	public MapFieldCacheCollectorImpl(Collector delegate, FieldLoadingStrategy collectorStrategy) {
		super( delegate );
		this.collectorStrategy = collectorStrategy;
	}

	@Override
	public void collect(int doc) throws IOException {
		//warning when changing this method: extremely performance sensitive!
		this.delegate.collect( doc );
		Object collected = collectorStrategy.collect( doc );
		this.valuePerDocumentId.put( currentDocBase + doc, collected );
	}

	@Override
	public void setNextReader(AtomicReaderContext context) throws IOException {
		this.currentDocBase = context.docBase;
		this.collectorStrategy.loadNewCacheValues( context );
		this.delegate.setNextReader( context );
	}

	@Override
	public Object getValue(int docId) {
		return valuePerDocumentId.get( docId );
	}
}
