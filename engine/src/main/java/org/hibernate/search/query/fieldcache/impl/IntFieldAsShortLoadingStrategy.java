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
import org.apache.lucene.search.FieldCache.Ints;

/**
 * Loading strategy which loads shorts from int fields.
 *
 * @author Gunnar Morling
 */
public final class IntFieldAsShortLoadingStrategy implements FieldLoadingStrategy {
	private final String fieldName;
	private Ints currentCache;

	public IntFieldAsShortLoadingStrategy(String fieldName) {
		this.fieldName = fieldName;
	}

	@Override
	public void loadNewCacheValues(AtomicReaderContext context) throws IOException {
		final AtomicReader reader = context.reader();
		currentCache = FieldCache.DEFAULT.getInts( reader, fieldName, false );
	}

	@Override
	public Short collect(int relativeDocId) {
		return (short) currentCache.get( relativeDocId );
	}
}
