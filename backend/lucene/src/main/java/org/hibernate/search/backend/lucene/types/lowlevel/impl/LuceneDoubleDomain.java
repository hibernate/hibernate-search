/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.lowlevel.impl;

import org.apache.lucene.document.DoubleDocValuesField;
import org.apache.lucene.document.DoublePoint;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.SortField;

public class LuceneDoubleDomain implements LuceneNumericDomain<Double> {
	private static final LuceneNumericDomain<Double> INSTANCE = new LuceneDoubleDomain();

	public static LuceneNumericDomain<Double> get() {
		return INSTANCE;
	}

	@Override
	public Double getMinValue() {
		return Double.MIN_VALUE;
	}

	@Override
	public Double getMaxValue() {
		return Double.MAX_VALUE;
	}

	@Override
	public Double getPreviousValue(Double value) {
		return Math.nextDown( value );
	}

	@Override
	public Double getNextValue(Double value) {
		return Math.nextUp( value );
	}

	@Override
	public Query createExactQuery(String absoluteFieldPath, Double value) {
		return DoublePoint.newExactQuery( absoluteFieldPath, value );
	}

	@Override
	public Query createRangeQuery(String absoluteFieldPath, Double lowerLimit, Double upperLimit) {
		return DoublePoint.newRangeQuery(
				absoluteFieldPath, lowerLimit, upperLimit
		);
	}

	@Override
	public SortField.Type getSortFieldType() {
		return SortField.Type.DOUBLE;
	}

	@Override
	public IndexableField createIndexField(String absoluteFieldPath, Double numericValue) {
		return new DoublePoint( absoluteFieldPath, numericValue );
	}

	@Override
	public IndexableField createDocValuesField(String absoluteFieldPath, Double numericValue) {
		return new DoubleDocValuesField( absoluteFieldPath, numericValue );
	}
}
