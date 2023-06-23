/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.testsupport.types;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.hibernate.search.engine.backend.types.dsl.IndexFieldTypeFactory;
import org.hibernate.search.engine.backend.types.dsl.StandardIndexFieldTypeOptionsStep;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.expectations.IndexNullAsMatchPredicateExpectactions;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.values.AscendingUniqueTermValues;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.values.IndexableValues;

public class BigIntegerFieldTypeDescriptor extends FieldTypeDescriptor<BigInteger> {

	public static final BigIntegerFieldTypeDescriptor INSTANCE = new BigIntegerFieldTypeDescriptor();

	private static final int DECIMAL_SCALE = -2;

	private BigIntegerFieldTypeDescriptor() {
		super( BigInteger.class );
	}

	@Override
	public StandardIndexFieldTypeOptionsStep<?, BigInteger> configure(IndexFieldTypeFactory fieldContext) {
		return fieldContext.asBigInteger().decimalScale( DECIMAL_SCALE );
	}

	@Override
	protected AscendingUniqueTermValues<BigInteger> createAscendingUniqueTermValues() {
		return new AscendingUniqueTermValues<BigInteger>() {
			@Override
			public List<BigInteger> createSingle() {
				// Remember: scale is -2, so the last two digits are dropped for predicates/sorts/aggregations/etc.
				return Arrays.asList(
						new BigInteger( "-4200" ),
						new BigInteger( "-200" ),
						BigInteger.ZERO,
						BigInteger.valueOf( 4200 ),
						BigInteger.valueOf( 7800 ),
						BigInteger.valueOf( 154800 ),
						BigInteger.valueOf( 151_484_200L ),
						BigInteger.valueOf( 151_484_254_000L )
				);
			}

			@Override
			protected BigInteger delta(int multiplierForDelta) {
				return new BigInteger( "4242" ).multiply( BigInteger.valueOf( multiplierForDelta ) );
			}

			@Override
			protected BigInteger applyDelta(BigInteger value, int multiplierForDelta) {
				return value.add( delta( multiplierForDelta ) );
			}
		};
	}

	@Override
	protected IndexableValues<BigInteger> createIndexableValues() {
		return new IndexableValues<BigInteger>() {
			@Override
			protected List<BigInteger> createSingle() {
				return Arrays.asList(
						BigInteger.valueOf( Long.MIN_VALUE ).multiply( BigInteger.valueOf( 100 ) ),
						BigInteger.valueOf( Long.MAX_VALUE ).multiply( BigInteger.valueOf( 100 ) ),
						BigInteger.valueOf( Long.MIN_VALUE ),
						BigInteger.valueOf( Long.MAX_VALUE ),
						BigInteger.valueOf( Integer.MIN_VALUE ),
						BigInteger.valueOf( Integer.MAX_VALUE ),
						BigInteger.valueOf( -251_484_254 ),
						BigInteger.valueOf( -42 ),
						BigInteger.valueOf( -1 ),
						BigInteger.ZERO,
						BigInteger.ONE,
						BigInteger.TEN,
						new BigInteger( "42" ),
						BigInteger.valueOf( 151_484_254L )
				);
			}
		};
	}

	@Override
	protected List<BigInteger> createUniquelyMatchableValues() {
		return Arrays.asList(
				BigInteger.valueOf( Long.MIN_VALUE ).multiply( BigInteger.valueOf( 100 ) ),
				BigInteger.valueOf( Long.MAX_VALUE ).multiply( BigInteger.valueOf( 100 ) ),
				BigInteger.valueOf( Long.MIN_VALUE ),
				BigInteger.valueOf( Long.MAX_VALUE ),
				BigInteger.valueOf( Integer.MIN_VALUE ),
				BigInteger.valueOf( Integer.MAX_VALUE ),
				BigInteger.valueOf( -251_484_254 ),
				BigInteger.valueOf( 42 ),
				BigInteger.valueOf( 151_484_254L )
		);
	}

	@Override
	protected List<BigInteger> createNonMatchingValues() {
		return Arrays.asList(
				BigInteger.valueOf( 12_312_312_312L ), BigInteger.valueOf( 7_939_397 ),
				BigInteger.valueOf( 73_973_922_121L ), BigInteger.valueOf( 9_282_821_212L )
		);
	}

	@Override
	public BigInteger valueFromInteger(int integer) {
		return BigInteger.valueOf( integer );
	}

	@Override
	public Optional<IndexNullAsMatchPredicateExpectactions<BigInteger>> getIndexNullAsMatchPredicateExpectations() {
		return Optional.of( new IndexNullAsMatchPredicateExpectactions<>(
				BigInteger.TEN, BigInteger.valueOf( 6700 )
		) );
	}
}
