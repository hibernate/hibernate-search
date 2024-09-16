/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.backend.tck.testsupport.types.values;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public abstract class MetricAggregationsValues<F> {

	private static final int[] DRIVER_VALUES = new int[] { 9, 18, 3, 18, 7, -10, 3, 0, 7, 0 };

	private final List<F> values = Collections.unmodifiableList( createValues() );

	public final List<F> values() {
		return values;
	}

	public F sum() {
		return valueOf( 55 );
	}

	public F min() {
		return valueOf( -10 );
	}

	public F max() {
		return valueOf( 18 );
	}

	public long count() {
		return 10;
	}

	public long countDistinct() {
		return 6;
	}

	public F avg() {
		return valueOf( 5 );
	}

	public double avgRaw() {
		return doubleValueOf( 5.5 );
	}

	public double minRaw() {
		return doubleValueOf( -10 );
	}

	public double maxRaw() {
		return doubleValueOf( 18 );
	}

	public double sumRaw() {
		return doubleValueOf( 55 );
	}

	protected double doubleValueOf(double value) {
		return value;
	}

	protected abstract F valueOf(int value);

	private List<F> createValues() {
		return Arrays.stream( driverValues() ).mapToObj( this::valueOf ).toList();
	}

	private int[] driverValues() {
		return DRIVER_VALUES;
	}
}
