/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.common.data.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.Percentage.withPercentage;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.search.util.impl.test.logging.Log;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import org.apache.commons.math3.stat.StatUtils;
import org.apache.commons.math3.stat.inference.TestUtils;
import org.apache.commons.math3.util.FastMath;

/**
 * Tests that hash tables map various datasets to their buckets uniformly,
 * i.e. with approximately the same amount of elements in each bucket.
 */
@RunWith(Parameterized.class)
public class HashTableUniformityTest {

	@Parameterized.Parameters(name = "{0} - {1}")
	public static List<Object[]> params() {
		// Try many datasets because some datasets contain lots of similar data (same string size, small character set, ...)
		// and thus may not lead to uniform hashes with the imperfect hash functions we're using.
		List<StringGenerator> generators = Arrays.asList(
				// Sometimes entity IDs are integers
				StringGenerator.integerSequenceAsStrings( 0 ),
				StringGenerator.integerSequenceAsStrings( Integer.MAX_VALUE / 2 ),
				// Sometimes entity IDs are fixed-length codes
				RandomStringGenerator.randomCodeStrings( 4 ),
				RandomStringGenerator.randomCodeStrings( 5 ),
				RandomStringGenerator.randomCodeStrings( 6 ),
				RandomStringGenerator.randomCodeStrings( 7 ),
				RandomStringGenerator.randomCodeStrings( 8 ),
				// Sometimes entity IDs are just variable-length text
				RandomStringGenerator.randomSentenceStrings( 1, 10 ),
				RandomStringGenerator.randomSentenceStrings( 4, 6 ),
				RandomStringGenerator.randomSentenceStrings( 5, 10 ),
				RandomStringGenerator.randomAsciiStrings( 1, 10 ),
				RandomStringGenerator.randomAsciiStrings( 4, 6 ),
				RandomStringGenerator.randomAsciiStrings( 5, 10 ),
				RandomStringGenerator.randomUtf16Strings( 1, 10 ),
				RandomStringGenerator.randomUtf16Strings( 4, 6 ),
				RandomStringGenerator.randomUtf16Strings( 5, 10 )
		);
		List<Object[]> params = new ArrayList<>();
		for ( HashTableProvider hashTableProvider : HashTableProvider.values() ) {
			for ( StringGenerator generator : generators ) {
				params.add( new Object[] { hashTableProvider, generator } );
			}
		}
		return params;
	}

	@Parameterized.Parameter(0)
	public HashTableProvider hashTableProvider;

	@Parameterized.Parameter(1)
	public StringGenerator generator;

	@Test
	public void with2buckets() {
		testUniformity( 2, 10_000 );
	}

	@Test
	public void with5buckets() {
		testUniformity( 5, 10_000 );
	}

	@Test
	public void with10buckets() {
		testUniformity( 10, 100_000 );
	}

	@Test
	public void with20buckets() {
		testUniformity( 20, 1_000_000 );
	}

	private void testUniformity(int bucketCount, long maxKeyCount) {
		HashTable<Long> hashTable = hashTableProvider.create( bucketCount );

		for ( int i = 0; i < hashTable.size(); i++ ) {
			hashTable.set( i, 0L );
		}

		// Put keys in buckets
		generator.stream().limit( maxKeyCount )
				.forEach( key -> {
					int i = hashTable.computeIndex( key );
					if ( Log.INSTANCE.isTraceEnabled() ) {
						Log.INSTANCE.tracef( "Index \t%d for generated key '%s'", i, key );
					}
					Long previous = hashTable.get( i );
					hashTable.set( i, previous + 1 );
				} );

		// Structure data as necessary
		long[] actualCounts = new long[hashTable.size()];
		double[] actualCountsAsDoubles = new double[hashTable.size()];
		Map<Integer, Double> actualCountsAsMap = new HashMap<>();
		for ( int i = 0; i < hashTable.size(); i++ ) {
			actualCounts[i] = hashTable.get( i );
			actualCountsAsDoubles[i] = (double) actualCounts[i];
			actualCountsAsMap.put( i, actualCountsAsDoubles[i] );
		}

		// Stats
		double mean = StatUtils.mean( actualCountsAsDoubles );
		double[] expectedCounts = new double[hashTable.size()];
		Arrays.fill( expectedCounts, mean );
		double chiSquareTestPValue = TestUtils.chiSquareTest( expectedCounts, actualCounts );
		double standardDeviation = FastMath.sqrt( StatUtils.variance( actualCountsAsDoubles ) );
		double relativeStandardDeviation = Math.abs( standardDeviation / mean );

		// Debug
		Log.INSTANCE.debugf( "Chi-square test p-value: %.2f", chiSquareTestPValue );
		Log.INSTANCE.debugf( "Relative standard deviation: %.2f%%", relativeStandardDeviation * 100 );
		for ( int i = 0; i < hashTable.size(); i++ ) {
			Log.INSTANCE.debugf( "Bucket %d: %d elements (expected close to %.2f)",
					(Object) i, hashTable.get( i ), mean );
		}

		// Actual assertions

		// Supposedly the chi-square test p-value is exactly what we need
		// (https://en.wikipedia.org/wiki/Hash_function#Testing_and_measurement),
		// but my attempts at using it failed miserably.
		// It's supposed to be in the range [0.95,1.05] to indicate a uniform distribution,
		// and most of the time is is, but some times it's very low for seemingly no reason.
		// For example the distribution [0=1947, 1=2047, 2=2032, 3=2042, 4=1932] has a p-value of only 0.18,
		// but this distribution is fine for our purposes.
		// So I'll just ignore the chi-square test for now.

		// Check that buckets have a similar count overall:
		// relative standard deviation of 15% at most.
		assertThat( relativeStandardDeviation ).isLessThanOrEqualTo( 0.15 );

		// Check that no bucket deviates too much from the expectations:
		// no more than 25% deviation from the mean in a given bucket.
		assertThat( actualCountsAsMap )
				.allSatisfy( (key, value) -> assertThat( value ).as( "Bucket #" + key )
						.isCloseTo( mean, withPercentage( 25 ) ) );
	}

	enum HashTableProvider {
		SIMPLE_MODULO {
			@Override
			public <T> HashTable<T> create(int size) {
				return new ModuloHashTable<>( SimpleHashFunction.INSTANCE, size );
			}
		},

		// Not testing new RangeHashTable<>( SimpleHashFunction.INSTANCE, size ),
		// because the resulting distribution of the hash (before applying the modulo operation)
		// is not uniform at all,
		// especially with fixed-length strings and small character sets.
		// That's why I added a tagging interface to prevent SimpleHashFunction
		// from being used with RangeHashTable.

		MURMUR3_MODULO {
			@Override
			public <T> HashTable<T> create(int size) {
				return new ModuloHashTable<>( Murmur3HashFunction.INSTANCE, size );
			}
		},
		MURMUR3_RANGE {
			@Override
			public <T> HashTable<T> create(int size) {
				return new RangeHashTable<>( Murmur3HashFunction.INSTANCE, size );
			}
		};

		public abstract <T> HashTable<T> create(int size);
	}
}
