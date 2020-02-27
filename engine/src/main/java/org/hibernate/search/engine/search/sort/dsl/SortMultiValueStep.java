/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.sort.dsl;


/**
 * The step in a sort definition where the behavior on missing values can be set.
 *
 * @param <S> The type of the next step (returned by {@link FieldSortMissingValueBehaviorStep#first()}, for example).
 *
 */
public interface SortMultiValueStep<S> {

	/**
	 * Multivalue the sum of all the values.
	 *
	 * @return {@code this}, for method chaining.
	 */
	default S sum() {
		return func( SortMultiFunc.SUM );
	}

	/**
	 * Multivalue the lowest value.
	 *
	 * @return {@code this}, for method chaining.
	 */
	default S min() {
		return func( SortMultiFunc.MIN );
	}

	/**
	 * Multivalue the highest value.
	 *
	 * @return {@code this}, for method chaining.
	 */
	default S max() {
		return func( SortMultiFunc.MAX );
	}

	/**
	 * Multivalue the average of all the values.
	 *
	 * @return {@code this}, for method chaining.
	 */
	default S avg() {
		return func( SortMultiFunc.AVG );
	}

	/**
	 * Multivalue the highest value.
	 *
	 * @return {@code this}, for method chaining.
	 */
	default S median() {
		return func( SortMultiFunc.MEDIAN );
	}

	/**
	 * Multivalue the median of the values.
	 *
	 * @param multi The func.
	 * @return {@code this}, for method chaining.
	 */
	S func(SortMultiFunc multi);
}
