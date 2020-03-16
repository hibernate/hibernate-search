/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.testsupport.types;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.hibernate.search.engine.backend.types.dsl.IndexFieldTypeFactory;
import org.hibernate.search.engine.backend.types.dsl.StandardIndexFieldTypeOptionsStep;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.expectations.ExistsPredicateExpectations;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.expectations.FieldProjectionExpectations;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.expectations.IndexNullAsMatchPredicateExpectactions;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.expectations.IndexingExpectations;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.expectations.MatchPredicateExpectations;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.expectations.RangePredicateExpectations;

public class BigDecimalFieldTypeDescriptor extends FieldTypeDescriptor<BigDecimal> {

	private static final int DECIMAL_SCALE = 2;

	BigDecimalFieldTypeDescriptor() {
		super( BigDecimal.class );
	}

	@Override
	public StandardIndexFieldTypeOptionsStep<?, BigDecimal> configure(IndexFieldTypeFactory fieldContext) {
		return fieldContext.asBigDecimal().decimalScale( DECIMAL_SCALE );
	}

	@Override
	public List<BigDecimal> getAscendingUniqueTermValues() {
		// Remember: scale is 2, so only two decimal digits are kept for predicates/sorts/aggregations/etc.
		return Arrays.asList(
				new BigDecimal( "-42" ),
				new BigDecimal( "-2.12" ),
				BigDecimal.ZERO,
				BigDecimal.ONE,
				BigDecimal.TEN,
				BigDecimal.valueOf( 42.42 ),
				BigDecimal.valueOf( 1548.00 ),
				BigDecimal.valueOf( 1584514514.18 )
		);
	}

	@Override
	public Optional<IndexingExpectations<BigDecimal>> getIndexingExpectations() {
		return Optional.of( new IndexingExpectations<>(
				BigDecimal.ZERO,
				BigDecimal.ONE,
				BigDecimal.TEN,
				nextUp( BigDecimal.ONE ) ,
				nextDown( BigDecimal.ONE ),
				BigDecimal.valueOf( 42.42 ),
				BigDecimal.valueOf( 1584514514.000000184 ),
				scaled( Long.MAX_VALUE ),
				scaled( Long.MIN_VALUE ),
				new BigDecimal( "-0.00000000012" ),
				BigDecimal.valueOf( 0.0000000001 )
		) );
	}

	@Override
	public Optional<MatchPredicateExpectations<BigDecimal>> getMatchPredicateExpectations() {
		return Optional.of( new MatchPredicateExpectations<>(
				BigDecimal.valueOf( 42.1 ), BigDecimal.valueOf( 67.0 )
		) );
	}

	@Override
	public Optional<RangePredicateExpectations<BigDecimal>> getRangePredicateExpectations() {
		return Optional.of( new RangePredicateExpectations<>(
				BigDecimal.valueOf( 3.0 ), BigDecimal.valueOf( 13.2 ), BigDecimal.valueOf( 25.79 ),
				BigDecimal.valueOf( 10.0 ), BigDecimal.valueOf( 19.13 )
		) );
	}

	@Override
	public ExistsPredicateExpectations<BigDecimal> getExistsPredicateExpectations() {
		return new ExistsPredicateExpectations<>(
				BigDecimal.ZERO, new BigDecimal( 42.1 )
		);
	}

	@Override
	public Optional<FieldProjectionExpectations<BigDecimal>> getFieldProjectionExpectations() {
		return Optional.of( new FieldProjectionExpectations<>(
				BigDecimal.valueOf( -1001 ), BigDecimal.valueOf( 3 ), BigDecimal.valueOf( 51 )
		) );
	}

	@Override
	public Optional<IndexNullAsMatchPredicateExpectactions<BigDecimal>> getIndexNullAsMatchPredicateExpectations() {
		return Optional.of( new IndexNullAsMatchPredicateExpectactions<>(
				BigDecimal.ZERO, BigDecimal.valueOf( 42.1 )
		) );
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder( "BigDecimalFieldTypeDescriptor{" );
		sb.append( "DECIMAL_SCALE=" ).append( DECIMAL_SCALE );
		sb.append( '}' );
		return sb.toString();
	}

	private BigDecimal scaled(long longValue) {
		BigDecimal decimal = BigDecimal.valueOf( longValue, DECIMAL_SCALE );

		// Transform to a double to make it consistent with ES behaviour
		// Lucene backend would not need that
		// TODO HSEARCH-3583 Fix the precision issue in Elasticsearch and remove this hack
		BigDecimal bigDecimal = BigDecimal.valueOf( decimal.doubleValue() );

		// for double imprecision we risk to cross the bounds
		// TODO HSEARCH-3583 Fix the precision issue in Elasticsearch and remove this hack
		return ( longValue > 0 ) ?
				bigDecimal.subtract( BigDecimal.TEN ) :
				bigDecimal.add( BigDecimal.TEN );
	}

	private BigDecimal nextUp(BigDecimal originalValue) {
		BigInteger unscaledPlusOne = originalValue.setScale( DECIMAL_SCALE ).unscaledValue().add( BigInteger.ONE );
		return new BigDecimal( unscaledPlusOne, DECIMAL_SCALE );
	}

	private BigDecimal nextDown(BigDecimal originalValue) {
		BigInteger unscaledLessOne = originalValue.setScale( DECIMAL_SCALE ).unscaledValue().subtract( BigInteger.ONE );
		return new BigDecimal( unscaledLessOne, DECIMAL_SCALE );
	}
}
