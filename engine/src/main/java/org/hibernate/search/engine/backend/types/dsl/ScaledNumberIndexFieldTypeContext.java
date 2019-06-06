/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.backend.types.dsl;

/**
 * A context for specifying a scaled number type.
 *
 * @param <S> The concrete type of this context.
 * @param <F> The type of field values.
 */
public interface ScaledNumberIndexFieldTypeContext<S extends ScaledNumberIndexFieldTypeContext<? extends S, F>, F>
		extends StandardIndexFieldTypeContext<S, F> {

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
