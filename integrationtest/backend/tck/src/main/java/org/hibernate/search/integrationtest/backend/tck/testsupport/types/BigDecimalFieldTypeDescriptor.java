/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.testsupport.types;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.hibernate.search.engine.backend.types.dsl.IndexFieldTypeFactory;
import org.hibernate.search.engine.backend.types.dsl.StandardIndexFieldTypeOptionsStep;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.expectations.IndexNullAsMatchPredicateExpectactions;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.values.AscendingUniqueTermValues;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.values.IndexableValues;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.TckConfiguration;

public class BigDecimalFieldTypeDescriptor extends StandardFieldTypeDescriptor<BigDecimal> {

	public static final BigDecimalFieldTypeDescriptor INSTANCE = new BigDecimalFieldTypeDescriptor();

	private static final int DECIMAL_SCALE = 2;

	private BigDecimalFieldTypeDescriptor() {
		super( BigDecimal.class );
	}

	@Override
	public StandardIndexFieldTypeOptionsStep<?, BigDecimal> configure(IndexFieldTypeFactory fieldContext) {
		return fieldContext.asBigDecimal().decimalScale( DECIMAL_SCALE );
	}

	@Override
	protected AscendingUniqueTermValues<BigDecimal> createAscendingUniqueTermValues() {
		return new AscendingUniqueTermValues<BigDecimal>() {
			@Override
			protected List<BigDecimal> createSingle() {
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
			protected BigDecimal delta(int multiplierForDelta) {
				return new BigDecimal( "4000.23" ).multiply( BigDecimal.valueOf( multiplierForDelta ) );
			}

			@Override
			protected BigDecimal applyDelta(BigDecimal value, int multiplierForDelta) {
				return value.add( delta( multiplierForDelta ) );
			}
		};
	}

	@Override
	protected IndexableValues<BigDecimal> createIndexableValues() {
		return new IndexableValues<BigDecimal>() {
			@Override
			protected List<BigDecimal> createSingle() {
				return Arrays.asList(
						BigDecimal.ZERO,
						BigDecimal.ONE,
						BigDecimal.TEN,
						nextUp( BigDecimal.ONE ),
						nextDown( BigDecimal.ONE ),
						BigDecimal.valueOf( 42.42 ),
						BigDecimal.valueOf( 1584514514.000000184 ),
						scaled( Long.MAX_VALUE ),
						scaled( Long.MIN_VALUE ),
						new BigDecimal( "-0.00000000012" ),
						BigDecimal.valueOf( 0.0000000001 )
				);
			}
		};
	}

	@Override
	protected List<BigDecimal> createUniquelyMatchableValues() {
		List<BigDecimal> list = new ArrayList<>( Arrays.asList(
				BigDecimal.ZERO,
				BigDecimal.ONE,
				BigDecimal.TEN,
				nextUp( BigDecimal.ONE ),
				nextDown( BigDecimal.ONE ),
				BigDecimal.valueOf( 42.42 ),
				BigDecimal.valueOf( 1584514514.000000184 )
		) );
		if ( TckConfiguration.get().getBackendFeatures().supportsExtremeScaledNumericValues() ) {
			Collections.addAll(
					list,
					scaled( Long.MAX_VALUE ),
					scaled( Long.MIN_VALUE )
			);
		}
		return list;
	}

	@Override
	protected List<BigDecimal> createNonMatchingValues() {
		return Arrays.asList(
				BigDecimal.valueOf( 123.12312312 ), BigDecimal.valueOf( 0.7939397 ), BigDecimal.valueOf( 739739.22121 ),
				BigDecimal.valueOf( 92828212.12 )
		);
	}

	@Override
	public BigDecimal valueFromInteger(int integer) {
		return new BigDecimal( BigInteger.valueOf( integer ), 2 );
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
		return ( longValue > 0 ) ? bigDecimal.subtract( BigDecimal.TEN ) : bigDecimal.add( BigDecimal.TEN );
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
