/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.lowlevel.aggregation.collector.impl;

/**
 * <p>
 * Copied with some changes from {@code org.opensearch.search.aggregations.metrics.CompensatedSum}
 * of <a href="https://opensearch.org/">OpenSearch</a>.
 */
public class KahanSummation {

	private static final double NO_CORRECTION = 0.0;

	private double value;
	private double delta;

	/**
	 * Used to calculate sums using the Kahan summation algorithm.
	 *
	 * @param value the sum
	 * @param delta correction term
	 */
	public KahanSummation(double value, double delta) {
		this.value = value;
		this.delta = delta;
	}

	/**
	 * The value of the sum.
	 */
	public double value() {
		return value;
	}

	/**
	 * The correction term.
	 */
	public double delta() {
		return delta;
	}

	/**
	 * Increments the Kahan sum by adding a value without a correction term.
	 */
	public KahanSummation add(double value) {
		return add( value, NO_CORRECTION );
	}

	/**
	 * Resets the internal state to use the new value and compensation delta
	 */
	public void reset(double value, double delta) {
		this.value = value;
		this.delta = delta;
	}

	/**
	 * Increments the Kahan sum by adding two sums, and updating the correction term for reducing numeric errors.
	 */
	public KahanSummation add(double value, double delta) {
		// If the value is Inf or NaN, just add it to the running tally to "convert" to
		// Inf/NaN. This keeps the behavior bwc from before kahan summing
		if ( Double.isFinite( value ) ) {
			this.value = value + this.value;
		}
		else {
			double correctedSum = value + ( this.delta + delta );
			double updatedValue = this.value + correctedSum;
			this.delta = correctedSum - ( updatedValue - this.value );
			this.value = updatedValue;
		}

		return this;
	}

}
