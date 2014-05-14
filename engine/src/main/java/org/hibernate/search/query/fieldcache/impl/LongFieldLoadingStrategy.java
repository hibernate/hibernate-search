/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.search.query.fieldcache.impl;

import java.io.IOException;

import org.apache.lucene.index.AtomicReader;
import org.apache.lucene.index.AtomicReaderContext;
import org.apache.lucene.search.FieldCache;
import org.apache.lucene.search.FieldCache.Longs;

/**
 * We need a collection of similar implementations, one per each FieldCache.DEFAULT.accessmethod
 * to be able to deal with arrays of primitive values without autoboxing all of them.
 *
 * @author Sanne Grinovero <sanne@hibernate.org> (C) 2011 Red Hat Inc.
 * @see FieldLoadingStrategy
 */
public final class LongFieldLoadingStrategy implements FieldLoadingStrategy {
	private final String fieldName;
	private Longs currentCache;

	public LongFieldLoadingStrategy(String fieldName) {
		this.fieldName = fieldName;
	}

	@Override
	public void loadNewCacheValues(AtomicReaderContext context) throws IOException {
		final AtomicReader reader = context.reader();
		currentCache = FieldCache.DEFAULT.getLongs( reader, fieldName, false );
	}

	@Override
	public Long collect(int relativeDocId) {
		return currentCache.get( relativeDocId );
	}
}
