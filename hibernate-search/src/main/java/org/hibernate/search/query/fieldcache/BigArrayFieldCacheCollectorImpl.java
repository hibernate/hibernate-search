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

package org.hibernate.search.query.fieldcache;

import java.io.IOException;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.Collector;

/**
 * FieldCacheCollector using an internal array to collect extracted values
 * from the FieldCache. Note that the size of the array is as big as the number
 * of Documents in the index, so if we only need to collect a small amount
 * of values other implementations are likely able to be more conservative
 * on memory requirements.
 * 
 * @author Sanne Grinovero <sanne@hibernate.org> (C) 2011 Red Hat Inc.
 */
final class BigArrayFieldCacheCollectorImpl<T> extends FieldCacheCollector<T> {
	
	private final T[] valuePerDocumentId;
	
	private int currentDocBase;
	private T[] currentCache;
	private final FieldLoadingStrategy<T> cacheLoadingStrategy;

	public BigArrayFieldCacheCollectorImpl(Collector delegate, FieldLoadingStrategy<T> cacheLoadingStrategy, T[] valueContainer) {
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
	public void setNextReader(IndexReader reader, int docBase) throws IOException {
		this.currentDocBase = docBase;
		this.cacheLoadingStrategy.loadNewCacheValues( reader );
		this.delegate.setNextReader( reader, docBase );
	}

	public T getValue(int docId) {
		return valuePerDocumentId[docId];
	}
	
}
