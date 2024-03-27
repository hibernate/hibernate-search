/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.backend.types.dsl;

/**
 * The initial and final step in a "scaled number" index field type definition, where optional parameters can be set.
 *
 * @param <S> The "self" type (the actual exposed type of this step).
 * @param <F> The type of field values.
 */
public interface ScaledNumberIndexFieldTypeOptionsStep<S extends ScaledNumberIndexFieldTypeOptionsStep<?, F>, F>
		extends StandardIndexFieldTypeOptionsStep<S, F> {

	/**
	 * @param decimalScale How the scale of values should be adjusted before indexing as a fixed-precision integer.
	 * A positive {@code decimalScale} will shift the decimal point to the right before rounding to the nearest integer and indexing,
	 * effectively retaining that many after the decimal place in the index,
	 * Since numbers are indexed with a fixed number of bits,
	 * this increase in precision also means that the maximum value that can be indexed will be smaller.
	 * A negative {@code decimalScale} will shift the decimal point to the left before rounding to the nearest integer and indexing,
	 * effectively setting that many of the smaller digits to zero in the index.
	 * Since numbers are indexed with a fixed number of bits,
	 * this decrease in precision also means that the maximum value that can be indexed will be larger.
	 * @return {@code this}, for method chaining.
	 */
	S decimalScale(int decimalScale);

}
