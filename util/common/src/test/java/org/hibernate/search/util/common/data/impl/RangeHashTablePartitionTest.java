/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.util.common.data.impl;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.hibernate.search.util.common.data.Range;
import org.hibernate.search.util.common.data.RangeBoundInclusion;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import org.assertj.core.api.AbstractIntegerAssert;

/**
 * Tests that the partition of the integer space in {@link RangeHashTable}
 * is fair and consistent across all relevant methods.
 */
class RangeHashTablePartitionTest {

	public static List<? extends Arguments> params() {
		return IntStream.range( 1, 50 )
				.mapToObj( i -> Arguments.of( i ) )
				.collect( Collectors.toList() );
	}

	// The hash function is not used in this test
	RangeCompatibleHashFunction irrelevantFunction = key -> {
		throw new IllegalStateException( "This method should not be called in this test" );
	};

	@ParameterizedTest(name = "{0} buckets")
	@MethodSource("params")
	void fullSpaceCoverage(int size) {
		RangeHashTable<Void> hashTable = new RangeHashTable<>( irrelevantFunction, size );
		Range<Integer> first = hashTable.rangeForBucket( 0 );
		assertThat( first.lowerBoundInclusion() )
				.isEqualTo( RangeBoundInclusion.INCLUDED );
		assertThat( first.lowerBoundValue() )
				.isEmpty(); // Means Integer.MIN_VALUE
		if ( size != 1 ) {
			assertThat( first.upperBoundInclusion() )
					.isEqualTo( RangeBoundInclusion.EXCLUDED );
			assertThat( first.upperBoundValue() )
					.isNotEmpty();
		}

		Range<Integer> previous = first;
		for ( int i = 1; i < ( size - 1 ); i++ ) {
			Range<Integer> current = hashTable.rangeForBucket( i );
			assertThat( current.lowerBoundInclusion() )
					.isEqualTo( RangeBoundInclusion.INCLUDED );
			assertThat( current.lowerBoundValue() )
					.contains( previous.upperBoundValue().get() );
			assertThat( current.upperBoundInclusion() )
					.isEqualTo( RangeBoundInclusion.EXCLUDED );
			assertThat( current.upperBoundValue() )
					.isNotEmpty();
			previous = current;
		}

		Range<Integer> last = hashTable.rangeForBucket( size - 1 );
		assertThat( last.lowerBoundInclusion() )
				.isEqualTo( RangeBoundInclusion.INCLUDED );
		if ( size != 1 ) {
			assertThat( last.lowerBoundValue() )
					.contains( previous.upperBoundValue().get() );
		}
		assertThat( last.upperBoundInclusion() )
				.isEqualTo( RangeBoundInclusion.INCLUDED );
		assertThat( last.upperBoundValue() )
				.isEmpty(); // Means Integer.MAX_VALUE
	}

	@ParameterizedTest(name = "{0} buckets")
	@MethodSource("params")
	void uniformRangeWidth(int size) {
		RangeHashTable<Void> hashTable = new RangeHashTable<>( irrelevantFunction, size );
		long integerSpaceWidth = ( (long) Integer.MAX_VALUE ) - Integer.MIN_VALUE + 1;
		long expectedWidth = integerSpaceWidth / size;

		for ( int i = 0; i < ( size - 1 ); i++ ) {
			Range<Integer> current = hashTable.rangeForBucket( i );
			long currentWidth = rangeWidth( current );
			assertThat( currentWidth ).as( "Width of " + current )
					.isEqualTo( expectedWidth );
		}

		Range<Integer> last = hashTable.rangeForBucket( size - 1 );
		long lastWidth = rangeWidth( last );
		assertThat( lastWidth ).as( "Width of " + last )
				// The last range might be a bit wider, depending on the size
				.isEqualTo( expectedWidth + ( integerSpaceWidth % size ) );
	}

	/**
	 * Tests that for any hash value in the range returned by
	 * {@code hashTable.rangeForBucket( i )},
	 * {@code computeIndexForHash(<hash value>)} returns {@code i}.
	 */
	@ParameterizedTest(name = "{0} buckets")
	@MethodSource("params")
	void consistentComputeIndexAndRanges(int size) {
		RangeHashTable<Void> hashTable = new RangeHashTable<>( irrelevantFunction, size );
		for ( int i = 0; i < size; i++ ) {
			Range<Integer> range = hashTable.rangeForBucket( i );
			int lowerBoundValue = range.lowerBoundValue().orElse( Integer.MIN_VALUE );
			int upperBoundValue = range.upperBoundValue().orElse( Integer.MAX_VALUE );
			if ( range.lowerBoundInclusion() == RangeBoundInclusion.INCLUDED ) {
				assertComputeIndexForHash( hashTable, lowerBoundValue )
						.isEqualTo( i );
			}
			assertComputeIndexForHash( hashTable, lowerBoundValue + 1 )
					.isEqualTo( i );
			assertComputeIndexForHash( hashTable, (int) ( lowerBoundValue + rangeWidth( range ) / 2 ) )
					.isEqualTo( i );
			assertComputeIndexForHash( hashTable, upperBoundValue - 1 )
					.isEqualTo( i );
			if ( range.upperBoundInclusion() == RangeBoundInclusion.INCLUDED ) {
				assertComputeIndexForHash( hashTable, upperBoundValue )
						.isEqualTo( i );
			}
		}
	}

	private AbstractIntegerAssert<?> assertComputeIndexForHash(RangeHashTable<Void> hashTable, int hash) {
		return assertThat( hashTable.computeIndexForHash( hash ) )
				.as( "computeIndexForHash(" + hash + ")" );
	}

	private long rangeWidth(Range<Integer> range) {
		int lowerBoundValue = range.lowerBoundValue().orElse( Integer.MIN_VALUE );
		int upperBoundValue = range.upperBoundValue().orElse( Integer.MAX_VALUE );
		assertThat( lowerBoundValue ).isLessThan( upperBoundValue );
		return ( (long) upperBoundValue - lowerBoundValue - 1 ) // values between the bounds
				+ ( range.lowerBoundInclusion() == RangeBoundInclusion.INCLUDED ? 1 : 0 ) // lower bound
				+ ( range.upperBoundInclusion() == RangeBoundInclusion.INCLUDED ? 1 : 0 ); // upper bound
	}
}
