/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.search.query.fieldcache.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.lucene.index.AtomicReaderContext;
import org.apache.lucene.index.SortedSetDocValues;
import org.apache.lucene.search.FieldCache;
import org.apache.lucene.util.BytesRef;

/**
 * A {@code FieldLoadingStrategy} which uses {@link SortedSetDocValues} to load fields with multiple values.
 *
 * @author Hardy Ferentschik
 */
public final class MultiStringFieldLoadingStrategy implements FieldLoadingStrategy {

	private final String fieldName;
	private SortedSetDocValues sortedSetDocValues;

	public MultiStringFieldLoadingStrategy(String fieldName) {
		this.fieldName = fieldName;
	}

	@Override
	public void loadNewCacheValues(AtomicReaderContext context) throws IOException {
		// FieldCache.getDocTermOrd allows to get all terms for a given multi value field per document. SortedSetDocValues
		// allows to retrieve the term values via iterator like API
		// See also https://issues.apache.org/jira/browse/LUCENE-3354
		sortedSetDocValues = FieldCache.DEFAULT.getDocTermOrds( context.reader(), fieldName );
	}

	@Override
	public String[] collect(int relativeDocId) {
		// use the loaded SortedSetDocValues to retrieve all values for the field
		sortedSetDocValues.setDocument( relativeDocId );
		List<String> values = new ArrayList<String>();

		long ordinal = sortedSetDocValues.nextOrd();
		while ( ordinal != SortedSetDocValues.NO_MORE_ORDS ) {
			BytesRef bytesRef = sortedSetDocValues.lookupOrd( ordinal );
			sortedSetDocValues.lookupOrd( ordinal );
			values.add( bytesRef.utf8ToString() );
			ordinal = sortedSetDocValues.nextOrd();
		}

		return values.toArray( new String[values.size()] );
	}

}
