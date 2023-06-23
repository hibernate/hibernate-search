/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.aggregation.impl;

import java.util.Comparator;

enum BucketOrder {
	COUNT_ASC {
		@Override
		<F> Comparator<Bucket<F>> toBucketComparatorInternal(Comparator<F> termComparator) {
			return (left, right) -> {
				int order = Long.compare( left.count, right.count );
				if ( order != 0 ) {
					return order;
				}
				order = termComparator.compare( left.term, right.term );
				return order;
			};
		}
	},
	COUNT_DESC {
		@Override
		<F> Comparator<Bucket<F>> toBucketComparatorInternal(Comparator<F> termComparator) {
			return (left, right) -> {
				int order = Long.compare( right.count, left.count ); // reversed, because desc
				if ( order != 0 ) {
					return order;
				}
				order = termComparator.compare( left.term, right.term );
				return order;
			};
		}
	},
	TERM_ASC {
		@Override
		<F> Comparator<Bucket<F>> toBucketComparatorInternal(Comparator<F> termComparator) {
			return (left, right) -> termComparator.compare( left.term, right.term );
		}
	},
	TERM_DESC {
		@Override
		boolean isTermOrderDescending() {
			return true;
		}

		@Override
		<F> Comparator<Bucket<F>> toBucketComparatorInternal(Comparator<F> termComparator) {
			return (left, right) -> termComparator.compare( left.term, right.term );
		}
	};

	<F> Comparator<Bucket<F>> toBucketComparator(Comparator<F> termAscendingComparator) {
		return toBucketComparatorInternal(
				isTermOrderDescending() ? termAscendingComparator.reversed() : termAscendingComparator );
	}

	abstract <F> Comparator<Bucket<F>> toBucketComparatorInternal(Comparator<F> termComparator);

	boolean isTermOrderDescending() {
		return false;
	}
}
