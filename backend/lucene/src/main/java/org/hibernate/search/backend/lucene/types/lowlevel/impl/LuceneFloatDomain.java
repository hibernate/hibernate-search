/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.lowlevel.impl;

import org.apache.lucene.document.FloatDocValuesField;
import org.apache.lucene.document.FloatPoint;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.SortField;

public class LuceneFloatDomain implements LuceneNumericDomain<Float> {
	private static final LuceneNumericDomain<Float> INSTANCE = new LuceneFloatDomain();

	public static LuceneNumericDomain<Float> get() {
		return INSTANCE;
	}

	@Override
	public Float getMinValue() {
		return Float.MIN_VALUE;
	}

	@Override
	public Float getMaxValue() {
		return Float.MAX_VALUE;
	}

	@Override
	public Float getPreviousValue(Float value) {
		return Math.nextDown( value );
	}

	@Override
	public Float getNextValue(Float value) {
		return Math.nextUp( value );
	}

	@Override
	public Query createExactQuery(String absoluteFieldPath, Float value) {
		return FloatPoint.newExactQuery( absoluteFieldPath, value );
	}

	@Override
	public Query createRangeQuery(String absoluteFieldPath, Float lowerLimit, Float upperLimit) {
		return FloatPoint.newRangeQuery(
				absoluteFieldPath, lowerLimit, upperLimit
		);
	}

	@Override
	public SortField.Type getSortFieldType() {
		return SortField.Type.FLOAT;
	}

	@Override
	public IndexableField createIndexField(String absoluteFieldPath, Float numericValue) {
		return new FloatPoint( absoluteFieldPath, numericValue );
	}

	@Override
	public IndexableField createDocValuesField(String absoluteFieldPath, Float numericValue) {
		return new FloatDocValuesField( absoluteFieldPath, numericValue );
	}
}
