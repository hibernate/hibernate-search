/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.backend.tck.testsupport.types.values;

import static java.util.Arrays.asList;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.hibernate.search.engine.search.common.SortMode;

public abstract class AscendingUniqueTermValues<F> {

	private final List<F> single = Collections.unmodifiableList( createSingle() );
	private final List<List<F>> multiResultingInSingleAfterMin =
			deepUnmodifiableList( createMultiResultingInSingleAfterMin() );
	private final List<List<F>> multiResultingInSingleAfterMax =
			deepUnmodifiableList( createMultiResultingInSingleAfterMax() );
	private final List<List<F>> multiResultingInSingleAfterSum =
			deepUnmodifiableList( createMultiResultingInSingleAfterSum() );
	private final List<List<F>> multiResultingInSingleAfterAvg =
			deepUnmodifiableList( createMultiResultingInSingleAfterAvg() );
	private final List<List<F>> multiResultingInSingleAfterMedian =
			deepUnmodifiableList( createMultiResultingInSingleAfterMedian() );

	public List<F> getSingle() {
		return single;
	}

	/**
	 * @return A list of of lists of "terms" (single-token) values,
	 * where lists are guaranteed to produce the same terms as {@link #createSingle()} in the same order
	 * when aggregated according to the given sort mode
	 * (after analysis/normalization).
	 * @throws UnsupportedOperationException If value lookup is not supported for this field type
	 * (hence this method should never be called).
	 */
	public final List<List<F>> getMultiResultingInSingle(SortMode sortMode) {
		switch ( sortMode ) {
			case SUM:
				return multiResultingInSingleAfterSum;
			case MIN:
				return multiResultingInSingleAfterMin;
			case MAX:
				return multiResultingInSingleAfterMax;
			case AVG:
				return multiResultingInSingleAfterAvg;
			case MEDIAN:
				return multiResultingInSingleAfterMedian;
			default:
				throw new IllegalStateException( "Unexpected sort mode: " + sortMode );
		}
	}

	protected abstract List<F> createSingle();

	protected List<List<F>> createMultiResultingInSingleAfterMin() {
		if ( single.size() < 8 ) {
			return valuesThatWontBeUsed();
		}
		// Return multiple values per ordinal, but the min of the list of values at ordinal N is always single.get( N )
		return asList(
				asList( single.get( 0 ), single.get( 1 ), single.get( 5 ) ),
				asList( single.get( 2 ), single.get( 1 ), single.get( 5 ) ),
				asList( single.get( 2 ), single.get( 7 ), single.get( 5 ) ),
				asList( single.get( 4 ), single.get( 7 ), single.get( 3 ) ),
				asList( single.get( 4 ), single.get( 7 ) ),
				asList( single.get( 5 ) ),
				asList( single.get( 7 ), single.get( 6 ), single.get( 6 ) ),
				asList( single.get( 7 ), single.get( 7 ), single.get( 7 ) )
		);
	}

	protected List<List<F>> createMultiResultingInSingleAfterMax() {
		if ( single.size() < 8 ) {
			return valuesThatWontBeUsed();
		}
		// Return multiple values per ordinal, but the max of the list of values at ordinal N is always single.get( N )
		return asList(
				asList( single.get( 0 ), single.get( 0 ), single.get( 0 ) ),
				asList( single.get( 0 ), single.get( 1 ), single.get( 0 ) ),
				asList( single.get( 2 ) ),
				asList( single.get( 2 ), single.get( 1 ), single.get( 3 ) ),
				asList( single.get( 4 ), single.get( 0 ) ),
				asList( single.get( 5 ), single.get( 0 ), single.get( 2 ) ),
				asList( single.get( 6 ), single.get( 1 ), single.get( 5 ) ),
				asList( single.get( 7 ), single.get( 1 ), single.get( 5 ) )
		);
	}

	protected List<List<F>> createMultiResultingInSingleAfterSum() {
		if ( single.size() < 8 ) {
			return valuesThatWontBeUsed();
		}
		// Return multiple values per ordinal, but the sum of the list of values at ordinal N is always single.get( N )
		return asList(
				asList( single.get( 0 ), delta( 1 ), delta( -1 ) ),
				asList( delta( 2 ), single.get( 1 ), delta( -1 ), delta( -1 ) ),
				asList( delta( -3 ), delta( 3 ), single.get( 2 ) ),
				asList( delta( -10 ), delta( 3 ), single.get( 3 ), delta( 7 ) ),
				asList( single.get( 4 ), delta( 13 ), delta( -13 ) ),
				asList( single.get( 5 ), delta( 100 ), delta( -100 ) ),
				asList( delta( 11 ), delta( 10 ), single.get( 6 ),
						delta( -10 ), delta( -11 ) ),
				asList( delta( 11 ), delta( 10 ), single.get( 7 ),
						delta( -10 ), delta( -11 ) )
		);
	}

	protected List<List<F>> createMultiResultingInSingleAfterAvg() {
		if ( single.size() < 8 ) {
			return valuesThatWontBeUsed();
		}
		// Return multiple values per ordinal, but the avg of the list of values at ordinal N is always single.get( N )
		return asList(
				asList( single.get( 0 ),
						applyDelta( single.get( 0 ), 1 ),
						applyDelta( single.get( 0 ), -1 ) ),
				asList( applyDelta( single.get( 1 ), 2 ),
						single.get( 1 ),
						applyDelta( single.get( 1 ), -1 ),
						applyDelta( single.get( 1 ), -1 ) ),
				asList( applyDelta( single.get( 2 ), -3 ),
						applyDelta( single.get( 2 ), 3 ),
						single.get( 2 ) ),
				asList( applyDelta( single.get( 3 ), -10 ),
						applyDelta( single.get( 3 ), 3 ),
						single.get( 3 ),
						applyDelta( single.get( 3 ), 7 ) ),
				asList( single.get( 4 ),
						applyDelta( single.get( 4 ), 13 ),
						applyDelta( single.get( 4 ), -13 ) ),
				asList( single.get( 5 ),
						applyDelta( single.get( 5 ), 50 ),
						applyDelta( single.get( 5 ), -50 ) ),
				asList( applyDelta( single.get( 6 ), 20 ),
						applyDelta( single.get( 6 ), 10 ),
						single.get( 6 ),
						applyDelta( single.get( 6 ), -5 ),
						applyDelta( single.get( 6 ), -15 ),
						applyDelta( single.get( 6 ), -15 ) ),
				asList( applyDelta( single.get( 7 ), 1 ),
						applyDelta( single.get( 7 ), 2 ),
						single.get( 7 ),
						applyDelta( single.get( 7 ), -1 ),
						applyDelta( single.get( 7 ), -2 ) )
		);
	}

	protected List<List<F>> createMultiResultingInSingleAfterMedian() {
		if ( single.size() < 8 ) {
			return valuesThatWontBeUsed();
		}
		// Return multiple values per ordinal, but the median of the list of values at ordinal N is always single.get( N )
		return asList(
				asList( single.get( 0 ), single.get( 0 ) ),
				asList( single.get( 0 ), single.get( 4 ), single.get( 1 ) ),
				asList( single.get( 0 ), single.get( 1 ), single.get( 2 ), single.get( 2 ), single.get( 2 ) ),
				asList( single.get( 3 ), single.get( 2 ), single.get( 4 ), single.get( 3 ) ),
				asList( single.get( 7 ), single.get( 3 ), single.get( 4 ), single.get( 7 ), single.get( 2 ) ),
				asList( single.get( 2 ), single.get( 5 ), single.get( 6 ) ),
				asList( single.get( 6 ), single.get( 3 ), single.get( 6 ) ),
				asList( single.get( 7 ), single.get( 7 ), single.get( 0 ), single.get( 7 ), single.get( 2 ) )
		);
	}

	protected F applyDelta(F value, int multiplierForDelta) {
		// Callers should be overridden if this is not implemented
		throw new UnsupportedOperationException();
	}

	protected F delta(int multiplierForDelta) {
		// Callers should be overridden if this is not implemented
		throw new UnsupportedOperationException();
	}

	// When this is called, we expect the values to be indexed, but not actually tested
	// Used for String types in particular
	protected static <F> List<List<F>> valuesThatWontBeUsed() {
		return Stream.generate( Collections::<F>emptyList ).limit( 10 ).collect( Collectors.toList() );
	}

	private static <F> List<List<F>> deepUnmodifiableList(List<List<F>> list) {
		return Collections.unmodifiableList(
				list.stream().map( Collections::unmodifiableList ).collect( Collectors.toList() )
		);
	}
}
