/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.lowlevel.impl;

import org.apache.lucene.document.IntPoint;
import org.apache.lucene.document.NumericDocValuesField;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.SortField;

public class LuceneIntegerDomain implements LuceneNumericDomain<Integer> {
	private static final LuceneNumericDomain<Integer> INSTANCE = new LuceneIntegerDomain();

	public static LuceneNumericDomain<Integer> get() {
		return INSTANCE;
	}

	@Override
	public Integer getMinValue() {
		return Integer.MIN_VALUE;
	}

	@Override
	public Integer getMaxValue() {
		return Integer.MAX_VALUE;
	}

	@Override
	public Integer getPreviousValue(Integer value) {
		return Math.addExact( value, -1 );
	}

	@Override
	public Integer getNextValue(Integer value) {
		return Math.addExact( value, 1 );
	}

	@Override
	public Query createExactQuery(String absoluteFieldPath, Integer value) {
		return IntPoint.newExactQuery( absoluteFieldPath, value );
	}

	@Override
	public Query createRangeQuery(String absoluteFieldPath, Integer lowerLimit, Integer upperLimit) {
		return IntPoint.newRangeQuery(
				absoluteFieldPath, lowerLimit, upperLimit
		);
	}

	@Override
	public SortField.Type getSortFieldType() {
		return SortField.Type.INT;
	}

	@Override
	public IndexableField createIndexField(String absoluteFieldPath, Integer numericValue) {
		return new IntPoint( absoluteFieldPath, numericValue );
	}

	@Override
	public IndexableField createDocValuesField(String absoluteFieldPath, Integer numericValue) {
		return new NumericDocValuesField( absoluteFieldPath, numericValue.longValue() );
	}
}
