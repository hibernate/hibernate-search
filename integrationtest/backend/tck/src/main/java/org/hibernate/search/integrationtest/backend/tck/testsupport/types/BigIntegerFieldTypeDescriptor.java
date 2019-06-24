/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.testsupport.types;

import java.math.BigInteger;
import java.util.Optional;

import org.hibernate.search.engine.backend.types.dsl.IndexFieldTypeFactory;
import org.hibernate.search.engine.backend.types.dsl.StandardIndexFieldTypeOptionsStep;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.expectations.ExistsPredicateExpectations;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.expectations.FieldProjectionExpectations;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.expectations.FieldSortExpectations;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.expectations.IndexNullAsMatchPredicateExpectactions;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.expectations.IndexingExpectations;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.expectations.MatchPredicateExpectations;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.expectations.RangePredicateExpectations;

public class BigIntegerFieldTypeDescriptor extends FieldTypeDescriptor<BigInteger> {

	private static final int DECIMAL_SCALE = -2;

	BigIntegerFieldTypeDescriptor() {
		super( BigInteger.class );
	}

	@Override
	public StandardIndexFieldTypeOptionsStep<?, BigInteger> configure(IndexFieldTypeFactory fieldContext) {
		return fieldContext.asBigInteger().decimalScale( DECIMAL_SCALE );
	}

	@Override
	public Optional<IndexingExpectations<BigInteger>> getIndexingExpectations() {
		return Optional.of( new IndexingExpectations<>(
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
		) );
	}

	@Override
	public Optional<MatchPredicateExpectations<BigInteger>> getMatchPredicateExpectations() {
		return Optional.of( new MatchPredicateExpectations<>(
				BigInteger.valueOf( 4200 ), BigInteger.valueOf( 6700 )
		) );
	}

	@Override
	public Optional<RangePredicateExpectations<BigInteger>> getRangePredicateExpectations() {
		return Optional.of( new RangePredicateExpectations<>(
				BigInteger.valueOf( 300 ), BigInteger.valueOf( 1300 ), BigInteger.valueOf( 2500 ),
				BigInteger.valueOf( 1000 ), BigInteger.valueOf( 1900 )
		) );
	}

	@Override
	public ExistsPredicateExpectations<BigInteger> getExistsPredicateExpectations() {
		return new ExistsPredicateExpectations<>(
				BigInteger.ZERO, BigInteger.valueOf( 6700L )
		);
	}

	@Override
	public Optional<FieldSortExpectations<BigInteger>> getFieldSortExpectations() {
		return Optional.of( new FieldSortExpectations<>(
				BigInteger.ONE, BigInteger.valueOf( 300 ), BigInteger.valueOf( 500L ), // keeping *L* on the last on purpose. It should be the same
				BigInteger.valueOf( Long.MIN_VALUE ), BigInteger.valueOf( 200 ), BigInteger.valueOf( 400 ), BigInteger.valueOf( Long.MAX_VALUE )
		) );
	}

	@Override
	public Optional<FieldProjectionExpectations<BigInteger>> getFieldProjectionExpectations() {
		return Optional.of( new FieldProjectionExpectations<>(
				BigInteger.ONE, BigInteger.valueOf( 300 ), BigInteger.valueOf( 500 )
		) );
	}

	@Override
	public Optional<IndexNullAsMatchPredicateExpectactions<BigInteger>> getIndexNullAsMatchPredicateExpectations() {
		return Optional.of( new IndexNullAsMatchPredicateExpectactions<>(
				BigInteger.TEN, BigInteger.valueOf( 6700 )
		) );
	}
}
