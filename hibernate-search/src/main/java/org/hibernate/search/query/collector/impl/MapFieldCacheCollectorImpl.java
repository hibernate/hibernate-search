/* 
 * JBoss, Home of Professional Open Source
 * Copyright 2011 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */

package org.hibernate.search.query.collector.impl;

import java.io.IOException;
import java.util.Map;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.Collector;

import org.hibernate.search.query.fieldcache.impl.FieldLoadingStrategy;

import static org.hibernate.search.util.impl.CollectionHelper.newHashMap;

/**
 * This Collector uses an internal Map to store collected values from the FieldCache.
 * Beware that when this map grows too much the put operation becomes a performance bottleneck,
 * so for large resultsets you should use BigArrayFieldCacheCollectorImpl instead.
 *
 * @author Sanne Grinovero <sanne@hibernate.org> (C) 2011 Red Hat Inc.
 * @see org.hibernate.search.query.collector.BigArrayFieldCacheCollectorImpl
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
	public void setNextReader(IndexReader reader, int docBase) throws IOException {
		this.currentDocBase = docBase;
		this.collectorStrategy.loadNewCacheValues( reader );
		this.delegate.setNextReader( reader, docBase );
	}

	public Object getValue(int docId) {
		return valuePerDocumentId.get( docId );
	}
}
