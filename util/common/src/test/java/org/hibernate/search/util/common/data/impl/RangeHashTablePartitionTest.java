/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.common.data.impl;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.hibernate.search.util.common.data.Range;
import org.hibernate.search.util.common.data.RangeBoundInclusion;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import org.assertj.core.api.AbstractIntegerAssert;

/**
 * Tests that the partition of the integer space in {@link RangeHashTable}
 * is fair and consistent across all relevant methods.
 */
@RunWith(Parameterized.class)
public class RangeHashTablePartitionTest {

	@Parameterized.Parameters(name = "{0} buckets")
	public static List<Integer> params() {
		return IntStream.range( 1, 50 ).boxed().collect( Collectors.toList() );
	}

	@Parameterized.Parameter
	public int size;

	private RangeHashTable<Void> hashTable;

	@BeforeEach
	public void setup() {
		// The hash function is not used in this test
		RangeCompatibleHashFunction irrelevantFunction = new RangeCompatibleHashFunction() {
			@Override
			public int hash(CharSequence key) {
				throw new IllegalStateException( "This method should not be called in this test" );
			}
		};
		hashTable = new RangeHashTable<>( irrelevantFunction, size );
	}

	@Test
	public void fullSpaceCoverage() {
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

	@Test
	public void uniformRangeWidth() {
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
	@Test
	public void consistentComputeIndexAndRanges() {
		for ( int i = 0; i < size; i++ ) {
			Range<Integer> range = hashTable.rangeForBucket( i );
			int lowerBoundValue = range.lowerBoundValue().orElse( Integer.MIN_VALUE );
			int upperBoundValue = range.upperBoundValue().orElse( Integer.MAX_VALUE );
			if ( range.lowerBoundInclusion() == RangeBoundInclusion.INCLUDED ) {
				assertComputeIndexForHash( lowerBoundValue )
						.isEqualTo( i );
			}
			assertComputeIndexForHash( lowerBoundValue + 1 )
					.isEqualTo( i );
			assertComputeIndexForHash( (int) ( lowerBoundValue + rangeWidth( range ) / 2 ) )
					.isEqualTo( i );
			assertComputeIndexForHash( upperBoundValue - 1 )
					.isEqualTo( i );
			if ( range.upperBoundInclusion() == RangeBoundInclusion.INCLUDED ) {
				assertComputeIndexForHash( upperBoundValue )
						.isEqualTo( i );
			}
		}
	}

	private AbstractIntegerAssert<?> assertComputeIndexForHash(int hash) {
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
