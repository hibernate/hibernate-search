/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.util.common.data;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

import org.hibernate.search.util.common.impl.Contracts;

/**
 * A representation of a range that can be used with any type.
 * <p>
 * Because there are no restrictions on type of values that can be used with this {@link Range} class,
 * it is not able to "understand" values that are passed to its various factory methods.
 * As a result, only minimal consistency checks are performed: null-checks, mostly.
 * In particular, <strong>this class does not check that the lower bound is actually lower than the upper bound</strong>,
 * because it has no idea what ordering to use.
 * Checking the relative order of bounds is the responsibility of callers of the {@link #lowerBoundValue()}
 * and {@link #upperBoundValue()} methods.
 *
 * @param <T> The type of values in this range.
 */
public final class Range<T> {

	/**
	 * Create a canonical range, i.e. a range in the form
	 * {@code [lowerBoundValue, upperBoundValue)} (lower bound included, upper bound excluded),
	 * or {@code [lowerBoundValue, +Infinity]} (both bounds included) if the upper bound is {@code +Infinity}.
	 * <p>
	 * This is mostly useful when creating multiple, contiguous ranges,
	 * like for example in range aggregations.
	 *
	 * @param lowerBoundValue The lower bound of the range.
	 * May be {@code null} to represent {@code -Infinity} (no lower bound),
	 * @param upperBoundValue The upper bound of the range.
	 * May be {@code null} to represent {@code +Infinity} (no upper bound).
	 * @param <T> The type of range bounds.
	 * @return The range {@code [lowerBoundValue, upperBoundValue)} (lower bound included, upper bound excluded),
	 * or {@code [lowerBoundValue, +Infinity]} (both bounds included) if the upper bound is {@code +Infinity}.
	 */
	public static <T> Range<T> canonical(T lowerBoundValue, T upperBoundValue) {
		return new Range<>( lowerBoundValue, RangeBoundInclusion.INCLUDED,
				upperBoundValue, upperBoundValue == null ? RangeBoundInclusion.INCLUDED : RangeBoundInclusion.EXCLUDED );
	}

	/**
	 * @param <T> The type of range bounds.
	 * @return The range {@code [-Infinity, +Infinity]} (both bounds included).
	 */
	public static <T> Range<T> all() {
		return between( null, RangeBoundInclusion.INCLUDED, null, RangeBoundInclusion.INCLUDED );
	}

	/**
	 * @param lowerBoundValue The lower bound of the range.
	 * May be {@code null} to represent {@code -Infinity} (no lower bound),
	 * @param upperBoundValue The upper bound of the range.
	 * May be {@code null} to represent {@code +Infinity} (no upper bound).
	 * @param <T> The type of range bounds.
	 * @return The range {@code [lowerBoundValue, upperBoundValue]} (both bounds included).
	 */
	public static <T> Range<T> between(T lowerBoundValue, T upperBoundValue) {
		return between( lowerBoundValue, RangeBoundInclusion.INCLUDED, upperBoundValue, RangeBoundInclusion.INCLUDED );
	}

	/**
	 * @param lowerBoundValue The value of the lower bound of the range.
	 * May be {@code null} to represent {@code -Infinity} (no lower bound).
	 * @param lowerBoundInclusion Whether the lower bound is included in the range or excluded.
	 * @param upperBoundValue The value of the upper bound of the range.
	 * May be {@code null} to represent {@code +Infinity} (no upper bound).
	 * @param upperBoundInclusion Whether the upper bound is included in the range or excluded.
	 * @param <T> The type of range bounds.
	 * @return A {@link Range}.
	 */
	public static <T> Range<T> between(T lowerBoundValue, RangeBoundInclusion lowerBoundInclusion,
			T upperBoundValue, RangeBoundInclusion upperBoundInclusion) {
		return new Range<>( lowerBoundValue, lowerBoundInclusion, upperBoundValue, upperBoundInclusion );
	}

	/**
	 * @param lowerBoundValue The value of the lower bound of the range. Must not be {@code null}.
	 * @param <T> The type of range bounds.
	 * @return The range {@code [lowerBoundValue, +Infinity]} (both bounds included).
	 */
	public static <T> Range<T> atLeast(T lowerBoundValue) {
		Contracts.assertNotNull( lowerBoundValue, "lowerBoundValue" );
		return between( lowerBoundValue, RangeBoundInclusion.INCLUDED, null, RangeBoundInclusion.INCLUDED );
	}

	/**
	 * @param lowerBoundValue The value of the lower bound of the range. Must not be {@code null}.
	 * @param <T> The type of range bounds.
	 * @return The range {@code (lowerBoundValue, +Infinity]} (lower bound excluded, upper bound included).
	 */
	public static <T> Range<T> greaterThan(T lowerBoundValue) {
		Contracts.assertNotNull( lowerBoundValue, "lowerBoundValue" );
		return between( lowerBoundValue, RangeBoundInclusion.EXCLUDED, null, RangeBoundInclusion.INCLUDED );
	}

	/**
	 * @param upperBoundValue The value of the upper bound of the range. Must not be {@code null}.
	 * @param <T> The type of range bounds.
	 * @return The range {@code [-Infinity, upperBoundValue]} (both bounds included).
	 */
	public static <T> Range<T> atMost(T upperBoundValue) {
		Contracts.assertNotNull( upperBoundValue, "upperBoundValue" );
		return between( null, RangeBoundInclusion.INCLUDED, upperBoundValue, RangeBoundInclusion.INCLUDED );
	}

	/**
	 * @param upperBoundValue The value of the upper bound of the range. Must not be {@code null}.
	 * @param <T> The type of range bounds.
	 * @return The range {@code [-Infinity, upperBoundValue)} (lower bound included, upper bound excluded).
	 */
	public static <T> Range<T> lessThan(T upperBoundValue) {
		Contracts.assertNotNull( upperBoundValue, "upperBoundValue" );
		return between( null, RangeBoundInclusion.INCLUDED, upperBoundValue, RangeBoundInclusion.EXCLUDED );
	}

	private final Optional<T> lowerBoundValue;
	private final RangeBoundInclusion lowerBoundInclusion;
	private final Optional<T> upperBoundValue;
	private final RangeBoundInclusion upperBoundInclusion;

	private Range(T lowerBoundValue, RangeBoundInclusion lowerBoundInclusion,
			T upperBoundValue, RangeBoundInclusion upperBoundInclusion) {
		Contracts.assertNotNull( lowerBoundInclusion, "lowerBoundInclusion" );
		Contracts.assertNotNull( upperBoundInclusion, "upperBoundInclusion" );
		this.lowerBoundValue = Optional.ofNullable( lowerBoundValue );
		this.lowerBoundInclusion = lowerBoundInclusion;
		this.upperBoundValue = Optional.ofNullable( upperBoundValue );
		this.upperBoundInclusion = upperBoundInclusion;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		switch ( lowerBoundInclusion ) {
			case INCLUDED:
				builder.append( '[' );
				break;
			case EXCLUDED:
				builder.append( '(' );
				break;
		}
		if ( lowerBoundValue.isPresent() ) {
			builder.append( lowerBoundValue.get() );
		}
		else {
			builder.append( "-Infinity" );
		}
		builder.append( "," );
		if ( upperBoundValue.isPresent() ) {
			builder.append( upperBoundValue.get() );
		}
		else {
			builder.append( "+Infinity" );
		}
		switch ( upperBoundInclusion ) {
			case INCLUDED:
				builder.append( ']' );
				break;
			case EXCLUDED:
				builder.append( ')' );
				break;
		}
		return builder.toString();
	}

	@Override
	public boolean equals(Object obj) {
		if ( this == obj ) {
			return true;
		}
		if ( obj == null || getClass() != obj.getClass() ) {
			return false;
		}
		Range<?> other = (Range<?>) obj;
		return lowerBoundValue.equals( other.lowerBoundValue )
				&& lowerBoundInclusion == other.lowerBoundInclusion
				&& upperBoundValue.equals( other.upperBoundValue )
				&& upperBoundInclusion == other.upperBoundInclusion;
	}

	@Override
	public int hashCode() {
		return Objects.hash( lowerBoundValue, lowerBoundInclusion, upperBoundValue, upperBoundInclusion );
	}

	/**
	 * @return The value of the lower bound, or an empty optional to represent {-Infinity} (no lower bound).
	 */
	public Optional<T> lowerBoundValue() {
		return lowerBoundValue;
	}

	/**
	 * @return Whether the lower bound is included in the range or excluded.
	 * Always {@link RangeBoundInclusion#EXCLUDED} if there is no lower bound.
	 */
	public RangeBoundInclusion lowerBoundInclusion() {
		return lowerBoundInclusion;
	}

	/**
	 * @return The value of the lower bound, or an empty optional to represent {+Infinity} (no upper bound).
	 */
	public Optional<T> upperBoundValue() {
		return upperBoundValue;
	}

	/**
	 * @return Whether the upper bound is included in the range or excluded.
	 * Always {@link RangeBoundInclusion#EXCLUDED} if there is no upper bound.
	 */
	public RangeBoundInclusion upperBoundInclusion() {
		return upperBoundInclusion;
	}

	public <R> Range<R> map(Function<? super T, ? extends R> function) {
		return Range.between(
				lowerBoundValue.map( function ).orElse( null ),
				lowerBoundInclusion,
				upperBoundValue.map( function ).orElse( null ),
				upperBoundInclusion
		);
	}

}
