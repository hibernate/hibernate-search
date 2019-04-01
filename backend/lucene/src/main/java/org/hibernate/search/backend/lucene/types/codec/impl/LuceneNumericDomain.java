/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.codec.impl;

import org.apache.lucene.document.DoubleDocValuesField;
import org.apache.lucene.document.DoublePoint;
import org.apache.lucene.document.FloatDocValuesField;
import org.apache.lucene.document.FloatPoint;
import org.apache.lucene.document.IntPoint;
import org.apache.lucene.document.LongPoint;
import org.apache.lucene.document.NumericDocValuesField;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.SortField;

public abstract class LuceneNumericDomain<E> {

	public abstract E getMinValue();

	public abstract E getMaxValue();

	public abstract E getPreviousValue(E value);

	public abstract E getNextValue(E value);

	public abstract Query createExactQuery(String absoluteFieldPath, E value);

	public abstract Query createRangeQuery(String absoluteFieldPath, E lowerLimit, E upperLimit);

	public abstract SortField.Type getSortFieldType();

	abstract IndexableField createIndexField(String absoluteFieldPath, E numericValue);

	abstract IndexableField createDocValuesField(String absoluteFieldPath, E numericValue);

	public static final LuceneNumericDomain<Integer> INTEGER = new LuceneNumericDomain<Integer>() {
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
		IndexableField createIndexField(String absoluteFieldPath, Integer numericValue) {
			return new IntPoint( absoluteFieldPath, numericValue );
		}

		@Override
		IndexableField createDocValuesField(String absoluteFieldPath, Integer numericValue) {
			return new NumericDocValuesField( absoluteFieldPath, numericValue.longValue() );
		}
	};

	public static final LuceneNumericDomain<Long> LONG = new LuceneNumericDomain<Long>() {
		@Override
		public Long getMinValue() {
			return Long.MIN_VALUE;
		}

		@Override
		public Long getMaxValue() {
			return Long.MAX_VALUE;
		}

		@Override
		public Long getPreviousValue(Long value) {
			return Math.addExact( value, -1L );
		}

		@Override
		public Long getNextValue(Long value) {
			return Math.addExact( value, 1L );
		}

		@Override
		public Query createExactQuery(String absoluteFieldPath, Long value) {
			return LongPoint.newExactQuery( absoluteFieldPath, value );
		}

		@Override
		public Query createRangeQuery(String absoluteFieldPath, Long lowerLimit, Long upperLimit) {
			return LongPoint.newRangeQuery(
					absoluteFieldPath, lowerLimit, upperLimit
			);
		}

		@Override
		public SortField.Type getSortFieldType() {
			return SortField.Type.LONG;
		}

		@Override
		IndexableField createIndexField(String absoluteFieldPath, Long numericValue) {
			return new LongPoint( absoluteFieldPath, numericValue );
		}

		@Override
		IndexableField createDocValuesField(String absoluteFieldPath, Long numericValue) {
			return new NumericDocValuesField( absoluteFieldPath, numericValue );
		}
	};

	public static final LuceneNumericDomain<Float> FLOAT = new LuceneNumericDomain<Float>() {
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
		IndexableField createIndexField(String absoluteFieldPath, Float numericValue) {
			return new FloatPoint( absoluteFieldPath, numericValue );
		}

		@Override
		IndexableField createDocValuesField(String absoluteFieldPath, Float numericValue) {
			return new FloatDocValuesField( absoluteFieldPath, numericValue );
		}
	};

	public static final LuceneNumericDomain<Double> DOUBLE = new LuceneNumericDomain<Double>() {
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
		IndexableField createIndexField(String absoluteFieldPath, Double numericValue) {
			return new DoublePoint( absoluteFieldPath, numericValue );
		}

		@Override
		IndexableField createDocValuesField(String absoluteFieldPath, Double numericValue) {
			return new DoubleDocValuesField( absoluteFieldPath, numericValue );
		}
	};
}
