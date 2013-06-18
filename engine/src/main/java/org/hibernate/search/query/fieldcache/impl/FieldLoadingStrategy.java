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

package org.hibernate.search.query.fieldcache.impl;

import java.io.IOException;

import org.apache.lucene.index.IndexReader;

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
	 * @param reader the {@code IndexReader} for which to load the new cache values
	 * @throws java.io.IOException in case an error occurs reading the cache values from the index
	 */
	void loadNewCacheValues(IndexReader reader) throws IOException;

	/**
	 * The collector wants to pick a specific element from the cache.
	 * Only at this point we convert primitives into an object if needed.
	 *
	 * @param relativeDocId the doc id relative to the current reader
	 * @return the cached field value for the document with the relative id {@code relativeDocId}.
	 */
	Object collect(int relativeDocId);
}
