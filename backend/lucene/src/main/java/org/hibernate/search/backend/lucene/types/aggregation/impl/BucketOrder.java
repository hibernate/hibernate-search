/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.types.aggregation.impl;

import java.util.Comparator;

public enum BucketOrder {
	COUNT_ASC {
		@Override
		<F> Comparator<Bucket<F>> toBucketComparatorInternal(Comparator<F> termComparator) {
			return (left, right) -> {
				int order = Long.compare( left.count(), right.count() );
				if ( order != 0 ) {
					return order;
				}
				order = termComparator.compare( left.term(), right.term() );
				return order;
			};
		}

		@Override
		Comparator<LongBucket> toLongBucketComparatorInternal() {
			return Comparator.comparingLong( LongBucket::count ).thenComparingLong( LongBucket::term );
		}
	},
	COUNT_DESC {
		@Override
		<F> Comparator<Bucket<F>> toBucketComparatorInternal(Comparator<F> termComparator) {
			return (left, right) -> {
				int order = Long.compare( right.count(), left.count() ); // reversed, because desc
				if ( order != 0 ) {
					return order;
				}
				order = termComparator.compare( left.term(), right.term() );
				return order;
			};
		}

		@Override
		Comparator<LongBucket> toLongBucketComparatorInternal() {
			return (left, right) -> {
				int order = Long.compare( right.count(), left.count() ); // reversed, because desc
				if ( order != 0 ) {
					return order;
				}
				order = Long.compare( left.term(), right.term() );
				return order;
			};
		}
	},
	TERM_ASC {
		@Override
		<F> Comparator<Bucket<F>> toBucketComparatorInternal(Comparator<F> termComparator) {
			return (left, right) -> termComparator.compare( left.term(), right.term() );
		}

		@Override
		Comparator<LongBucket> toLongBucketComparatorInternal() {
			return Comparator.comparingLong( LongBucket::term );
		}
	},
	TERM_DESC {
		@Override
		boolean isTermOrderDescending() {
			return true;
		}

		@Override
		<F> Comparator<Bucket<F>> toBucketComparatorInternal(Comparator<F> termComparator) {
			return (left, right) -> termComparator.compare( left.term(), right.term() );
		}

		@Override
		Comparator<LongBucket> toLongBucketComparatorInternal() {
			return Comparator.comparingLong( LongBucket::term ).reversed();
		}
	};

	public <F> Comparator<Bucket<F>> toBucketComparator(Comparator<F> termAscendingComparator) {
		return toBucketComparatorInternal(
				isTermOrderDescending() ? termAscendingComparator.reversed() : termAscendingComparator );
	}

	public <E> Comparator<LongBucket> toLongBucketComparator() {
		return toLongBucketComparatorInternal();
	}

	abstract <F> Comparator<Bucket<F>> toBucketComparatorInternal(Comparator<F> termComparator);

	abstract Comparator<LongBucket> toLongBucketComparatorInternal();

	boolean isTermOrderDescending() {
		return false;
	}
}
