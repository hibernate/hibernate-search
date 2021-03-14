/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.intercepting;

import java.util.Objects;

/**
 * Represents a predicate (boolean-valued function) of active predicate.
 *
 * <p>
 * This is a <a href="package-summary.html">functional interface</a>
 * whose functional method is {@link #test()}.
 *
 */
public interface LoadingProcessActivePredicate {

	/**
	 * Evaluates this predicate on the intercepting process.
	 *
	 * @return {@code true} if the intercepting process is active,
	 * otherwise {@code false}
	 */
	boolean test();

	/**
	 * Returns a composed predicate that represents a short-circuiting logical
	 * AND of this predicate and another. When evaluating the composed
	 * predicate, if this predicate is {@code false}, then the {@code other}
	 * predicate is not evaluated.
	 *
	 * @param other a predicate that will be logically-ANDed with this
	 * predicate
	 * @return a composed predicate that represents the short-circuiting logical
	 * AND of this predicate and the {@code other} predicate
	 * @throws NullPointerException if other is null
	 */
	default LoadingProcessActivePredicate and(LoadingProcessActivePredicate other) {
		Objects.requireNonNull( other );
		return () -> test() && other.test();
	}

	/**
	 * Returns a predicate that represents the logical negation of this
	 * predicate.
	 *
	 * @return a predicate that represents the logical negation of this
	 * predicate
	 */
	default LoadingProcessActivePredicate negate() {
		return () -> !test();
	}

	/**
	 * Returns a composed predicate that represents a short-circuiting logical
	 * OR of this predicate and another. When evaluating the composed
	 * predicate, if this predicate is {@code true}, then the {@code other}
	 * predicate is not evaluated.
	 *
	 * @param other a predicate that will be logically-ORed with this
	 * predicate
	 * @return a composed predicate that represents the short-circuiting logical
	 * OR of this predicate and the {@code other} predicate
	 * @throws NullPointerException if other is null
	 */
	default LoadingProcessActivePredicate or(LoadingProcessActivePredicate other) {
		Objects.requireNonNull( other );
		return () -> test() || other.test();
	}

	/**
	 * Returns a predicate that is the negation of the supplied predicate.
	 * This is accomplished by returning result of the calling
	 * {@code target.negate()}.
	 *
	 * @param target predicate to negate
	 *
	 * @return a predicate that negates the results of the supplied
	 * predicate
	 */
	static LoadingProcessActivePredicate not(LoadingProcessActivePredicate target) {
		Objects.requireNonNull( target );
		return target.negate();
	}

}
