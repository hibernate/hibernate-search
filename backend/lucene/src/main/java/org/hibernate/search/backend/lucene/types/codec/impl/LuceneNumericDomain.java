/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.codec.impl;

import org.apache.lucene.document.IntPoint;
import org.apache.lucene.document.LongPoint;
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
	};

}
