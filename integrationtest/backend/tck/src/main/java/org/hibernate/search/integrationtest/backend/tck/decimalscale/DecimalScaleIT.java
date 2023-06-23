/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.decimalscale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hibernate.search.util.impl.integrationtest.common.assertion.SearchResultAssert.assertThatQuery;

import java.math.BigDecimal;
import java.math.BigInteger;

import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.types.Projectable;
import org.hibernate.search.engine.mapper.mapping.building.spi.IndexFieldTypeDefaultsProvider;
import org.hibernate.search.engine.mapper.mapping.building.spi.IndexedEntityBindingContext;
import org.hibernate.search.engine.search.query.SearchQuery;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.SimpleMappedIndex;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappedIndex;

import org.junit.Rule;
import org.junit.Test;

public class DecimalScaleIT {

	/*
	 * Longs only have 64 bits to represent the value, or approximately 18 decimal digits
	 * (actually 19, but not all values with 19 digits can be represented).
	 * We would expect the indexed value to be indexed with approximately that precision,
	 * which is limited but still higher than the precision of doubles,
	 * which have ~53 bits to represent the unscaled value, or approximately 16 decimal digits.
	 *
	 * TODO HSEARCH-3583 We do get this precision with the Lucene backend,
	 *  but unfortunately there's a bug in Elasticsearch that reduces precision
	 *  to that of a double.
	 *  We should fix that and raise this constant to 18.
	 */
	int INDEX_PRECISION = 16;

	@Rule
	public final SearchSetupHelper setupHelper = new SearchSetupHelper();

	@Test
	public void noDecimalScale_bigDecimal() {
		assertThatThrownBy( () -> setupHelper.start()
				.withIndex( StubMappedIndex.ofNonRetrievable(
						root -> root.field( "noScaled", f -> f.asBigDecimal() ).toReference()
				) )
				.setup()
		)
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll( "Invalid index field type: missing decimal scale",
						"Define the decimal scale explicitly" );
	}

	@Test
	public void noDecimalScale_bigInteger() {
		assertThatThrownBy( () -> setupHelper.start()
				.withIndex( StubMappedIndex.ofNonRetrievable(
						root -> root.field( "noScaled", f -> f.asBigInteger() ).toReference()
				) )
				.setup()
		)
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll( "Invalid index field type: missing decimal scale",
						"Define the decimal scale explicitly" );
	}

	@Test
	public void positiveDecimalScale_bigInteger() {
		assertThatThrownBy( () -> setupHelper.start()
				.withIndex( StubMappedIndex.ofNonRetrievable(
						root -> root.field( "positiveScaled", f -> f.asBigInteger().decimalScale( 3 ) ).toReference()
				) )
				.setup()
		)
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll( "Invalid index field type: decimal scale '3' is positive",
						"The decimal scale of BigInteger fields must be zero or negative" );
	}

	@Test
	public void decimalScale_bigDecimal() {
		SimpleMappedIndex<DecimalScaleIndexBinding> index = SimpleMappedIndex.of(
				root -> new DecimalScaleIndexBinding( root, 3 )
		);
		setupHelper.start().withIndex( index ).setup();

		index.index( "1", doc -> doc.addValue( index.binding().scaled, new BigDecimal( "739.11111" ) ) );

		// decimal scale is 3, affecting the search precision
		// so the provided value 739.11111 will be treated as if it were 739.111
		matchGreaterThan( index, new BigDecimal( "739.11" ) );
		doNotMatchGreaterThan( index, new BigDecimal( "739.111" ) );
	}

	@Test
	public void decimalScale_zeroScale_bigDecimal() {
		SimpleMappedIndex<DecimalScaleIndexBinding> index = SimpleMappedIndex.of(
				root -> new DecimalScaleIndexBinding( root, 0 )
		);
		setupHelper.start().withIndex( index ).setup();

		index.index( "1", doc -> doc.addValue( index.binding().scaled, new BigDecimal( "739.11111" ) ) );

		// decimal scale is 0, affecting the search precision
		// so the provided value 739.11111 will be treated as if it were 739
		matchGreaterThan( index, new BigDecimal( "738" ) );
		doNotMatchGreaterThan( index, new BigDecimal( "739" ) );
	}

	@Test
	public void decimalScale_zeroScale_bigInteger() {
		SimpleMappedIndex<IntegerScaleIndexBinding> index = SimpleMappedIndex.of(
				root -> new IntegerScaleIndexBinding( root, 0 )
		);
		setupHelper.start().withIndex( index ).setup();

		index.index( "1", doc -> doc.addValue( index.binding().scaled, new BigInteger( "739" ) ) );

		// decimal scale is 0, affecting the search precision
		// so the provided value 739 will be treated as if it were 739
		matchGreaterThan( index, new BigInteger( "738" ) );
		doNotMatchGreaterThan( index, new BigInteger( "739" ) );
	}

	@Test
	public void decimalScale_negativeScale_bigDecimal() {
		SimpleMappedIndex<DecimalScaleIndexBinding> index = SimpleMappedIndex.of(
				root -> new DecimalScaleIndexBinding( root, -3 )
		);
		setupHelper.start().withIndex( index ).setup();

		index.index( "1", doc -> doc.addValue( index.binding().scaled, new BigDecimal( "11111.11111" ) ) );

		// decimal scale is -3, affecting the search precision
		// so the provided value 11111.11111 will be treated as if it were 11000
		matchGreaterThan( index, new BigDecimal( "10000" ) );
		doNotMatchGreaterThan( index, new BigDecimal( "11000" ) );
	}

	@Test
	public void decimalScale_negativeScale_bigInteger() {
		SimpleMappedIndex<IntegerScaleIndexBinding> index = SimpleMappedIndex.of(
				root -> new IntegerScaleIndexBinding( root, -3 )
		);
		setupHelper.start().withIndex( index ).setup();

		index.index( "1", doc -> doc.addValue( index.binding().scaled, new BigInteger( "11111" ) ) );

		// decimal scale is -3, affecting the search precision
		// so the provided value 11111 will be treated as if it were 11000
		matchGreaterThan( index, new BigInteger( "10000" ) );
		doNotMatchGreaterThan( index, new BigInteger( "11000" ) );
	}

	@Test
	public void decimalScale_largeScale_bigDecimal() {
		final int schemaDecimalScale = 275;

		SimpleMappedIndex<DecimalScaleIndexBinding> index = SimpleMappedIndex.of(
				root -> new DecimalScaleIndexBinding( root, schemaDecimalScale )
		);
		setupHelper.start().withIndex( index ).setup();

		/*
		 * Use extra digits in the original value, which will be assumed to be lost during indexing.
		 *
		 * The original value will look like this:
		 *
		 * 111111111111(...)111111.11111(...)111 * 10^-275
		 *
		 * The indexed value lower bound will look like this:
		 *
		 * 111111111111(...)111110.0 * 10^-275
		 *
		 * The indexed value upper bound will look like this:
		 *
		 * 111111111111(...)111112.0 * 10^-275
		 */
		BigDecimal originalValue = bigDecimalWithOnes( INDEX_PRECISION, 50, schemaDecimalScale );
		BigDecimal estimatedIndexedValue = bigDecimalWithOnes( INDEX_PRECISION, 0, schemaDecimalScale );
		BigDecimal indexedValueLowerBound =
				estimatedIndexedValue.subtract( new BigDecimal( BigInteger.ONE, schemaDecimalScale ) );
		BigDecimal indexedValueUpperBound = estimatedIndexedValue.add( new BigDecimal( BigInteger.ONE, schemaDecimalScale ) );
		assertThat( originalValue )
				.isBetween( indexedValueLowerBound, indexedValueUpperBound );

		index.index( "1", doc -> doc.addValue( index.binding().scaled, originalValue ) );

		matchGreaterThan( index, indexedValueLowerBound );
		doNotMatchGreaterThan( index, indexedValueUpperBound );
	}

	@Test
	public void decimalScale_negativeScale_largeScale_bigDecimal() {
		final int schemaDecimalScale = -275;

		SimpleMappedIndex<DecimalScaleIndexBinding> index = SimpleMappedIndex.of(
				root -> new DecimalScaleIndexBinding( root, schemaDecimalScale )
		);
		setupHelper.start().withIndex( index ).setup();

		/*
		 * Use extra digits in the original value, which will be assumed to be lost during indexing.
		 *
		 * The original value will look like this:
		 *
		 * 111111111111(...)111111.11111(...)111 * 10^275
		 *
		 * The indexed value lower bound will look like this:
		 *
		 * 111111111111(...)111110.0 * 10^275
		 *
		 * The indexed value upper bound will look like this:
		 *
		 * 111111111111(...)111112.0 * 10^275
		 */
		BigDecimal originalValue = bigDecimalWithOnes( INDEX_PRECISION, 5, schemaDecimalScale );
		BigDecimal estimatedIndexedValue = bigDecimalWithOnes( INDEX_PRECISION, 0, schemaDecimalScale );
		BigDecimal indexedValueLowerBound =
				estimatedIndexedValue.subtract( new BigDecimal( BigInteger.ONE, schemaDecimalScale ) );
		BigDecimal indexedValueUpperBound = estimatedIndexedValue.add( new BigDecimal( BigInteger.ONE, schemaDecimalScale ) );
		assertThat( originalValue )
				.isBetween( indexedValueLowerBound, indexedValueUpperBound );

		index.index( "1", doc -> doc.addValue( index.binding().scaled, originalValue ) );

		matchGreaterThan( index, indexedValueLowerBound );
		doNotMatchGreaterThan( index, indexedValueUpperBound );
	}

	@Test
	public void decimalScale_negativeScale_largeScale_bigInteger() {
		final int schemaDecimalScale = -275;

		SimpleMappedIndex<IntegerScaleIndexBinding> index = SimpleMappedIndex.of(
				root -> new IntegerScaleIndexBinding( root, schemaDecimalScale )
		);
		setupHelper.start().withIndex( index ).setup();

		/*
		 * Use extra digits in the original value, which will be assumed to be lost during indexing.
		 *
		 * The original value will look like this:
		 *
		 * 111111111111(...)111111.11111(...)111 * 10^275
		 *
		 * The indexed value lower bound will look like this:
		 *
		 * 111111111111(...)111110.0 * 10^275
		 *
		 * The indexed value upper bound will look like this:
		 *
		 * 111111111111(...)111112.0 * 10^275
		 */
		BigInteger originalValue = bigDecimalWithOnes( INDEX_PRECISION, 100, schemaDecimalScale ).toBigIntegerExact();
		BigInteger estimatedIndexedValue = bigDecimalWithOnes( INDEX_PRECISION, 0, schemaDecimalScale ).toBigIntegerExact();
		BigInteger indexedValueLowerBound =
				estimatedIndexedValue.subtract( new BigDecimal( BigInteger.ONE, schemaDecimalScale ).toBigIntegerExact() );
		BigInteger indexedValueUpperBound =
				estimatedIndexedValue.add( new BigDecimal( BigInteger.ONE, schemaDecimalScale ).toBigIntegerExact() );
		assertThat( originalValue )
				.isBetween( indexedValueLowerBound, indexedValueUpperBound );

		index.index( "1", doc -> doc.addValue( index.binding().scaled, originalValue ) );

		matchGreaterThan( index, indexedValueLowerBound );
		doNotMatchGreaterThan( index, indexedValueUpperBound );
	}

	@Test
	public void decimalScale_rounding_bigDecimal() {
		SimpleMappedIndex<DecimalScaleIndexBinding> index = SimpleMappedIndex.of(
				root -> new DecimalScaleIndexBinding( root, 2 )
		);
		setupHelper.start().withIndex( index ).setup();

		index.bulkIndexer()
				.add( "1", doc -> doc.addValue( index.binding().scaled, new BigDecimal( "739.114999" ) ) )
				.add( "2", doc -> doc.addValue( index.binding().scaled, new BigDecimal( "739.115" ) ) )
				.add( "3", doc -> doc.addValue( index.binding().scaled, new BigDecimal( "739.11" ) ) )
				.add( "4", doc -> doc.addValue( index.binding().scaled, new BigDecimal( "739.12" ) ) )
				.join();

		// RoundingMode.HALF_UP expected on both values:
		match( index, new BigDecimal( "739.11" ), "1", "3" );
		match( index, new BigDecimal( "739.12" ), "2", "4" );

		// and parameters:
		match( index, new BigDecimal( "739.114999" ), "1", "3" );
		match( index, new BigDecimal( "739.115" ), "2", "4" );
	}

	@Test
	public void decimalScale_rounding_bigInteger() {
		SimpleMappedIndex<IntegerScaleIndexBinding> index = SimpleMappedIndex.of(
				root -> new IntegerScaleIndexBinding( root, -4 )
		);
		setupHelper.start().withIndex( index ).setup();

		index.bulkIndexer()
				.add( "1", doc -> doc.addValue( index.binding().scaled, new BigInteger( "7394999" ) ) )
				.add( "2", doc -> doc.addValue( index.binding().scaled, new BigInteger( "7395000" ) ) )
				.add( "3", doc -> doc.addValue( index.binding().scaled, new BigInteger( "7390000" ) ) )
				.add( "4", doc -> doc.addValue( index.binding().scaled, new BigInteger( "7400000" ) ) )
				.join();

		// RoundingMode.HALF_UP expected on both values:
		match( index, new BigInteger( "7390000" ), "1", "3" );
		match( index, new BigInteger( "7400000" ), "2", "4" );

		// and parameters:
		match( index, new BigInteger( "7394999" ), "1", "3" );
		match( index, new BigInteger( "7395000" ), "2", "4" );
	}

	@Test
	public void decimalScale_largeDecimal_bigDecimal() {
		SimpleMappedIndex<DecimalScaleIndexBinding> index = SimpleMappedIndex.of(
				root -> new DecimalScaleIndexBinding( root, 0 )
		);
		setupHelper.start().withIndex( index ).setup();

		// That seems a limit for ES. Even if new BigDecimal( "2" ).pow( 54 ) << Long.MAX_VALUE
		// If the exponent were 54, the test would fail for Elasticsearch, whereas it would work for Lucene backend.
		BigDecimal largeDecimal = new BigDecimal( "2" ).pow( 53 );

		index.index(
				"1", doc -> doc.addValue( index.binding().scaled, largeDecimal )
		);

		// the precision is supposed to be preserved
		matchGreaterThan( index, largeDecimal.subtract( BigDecimal.ONE ) );
		doNotMatchGreaterThan( index, largeDecimal );
	}

	@Test
	public void decimalScale_largeDecimal_bigInteger() {
		SimpleMappedIndex<IntegerScaleIndexBinding> index = SimpleMappedIndex.of(
				root -> new IntegerScaleIndexBinding( root, 0 )
		);
		setupHelper.start().withIndex( index ).setup();

		// That seems a limit for ES. Even if new BigDecimal( "2" ).pow( 54 ) << Long.MAX_VALUE
		// If the exponent were 54, the test would fail for Elasticsearch, whereas it would work for Lucene backend.
		BigInteger largeInteger = new BigInteger( "2" ).pow( 53 );

		index.index(
				"1", doc -> doc.addValue( index.binding().scaled, largeInteger )
		);

		// the precision is supposed to be preserved
		matchGreaterThan( index, largeInteger.subtract( BigInteger.ONE ) );
		doNotMatchGreaterThan( index, largeInteger );
	}

	@Test
	public void decimalScale_tooLargeDecimal_scale0_bigDecimal() {
		SimpleMappedIndex<DecimalScaleIndexBinding> index = SimpleMappedIndex.of(
				root -> new DecimalScaleIndexBinding( root, 0 )
		);
		setupHelper.start().withIndex( index ).setup();

		// Provide a value that cannot be represented as a long
		BigDecimal tooLargeDecimal = BigDecimal.valueOf( Long.MAX_VALUE ).multiply( BigDecimal.TEN );

		assertThatThrownBy( () -> index.index(
				"1", doc -> doc.addValue( index.binding().scaled, tooLargeDecimal )
		) )
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll( "Unable to encode value '" + tooLargeDecimal.toString() + "'",
						"this field type only supports values ranging from ",
						"If you want to encode values that are outside this range,"
								+ " change the decimal scale for this field" );
	}

	@Test
	public void decimalScale_tooLargeDecimal_scale0_bigDecimal_queryPredicateBuildTime() {
		SimpleMappedIndex<DecimalScaleIndexBinding> index = SimpleMappedIndex.of(
				root -> new DecimalScaleIndexBinding( root, 0 )
		);
		setupHelper.start().withIndex( index ).setup();

		BigDecimal veryLargeDecimal = BigDecimal.valueOf( Long.MAX_VALUE );
		// Provide a value that cannot be represented as a long
		BigDecimal tooLargeDecimal = veryLargeDecimal.multiply( BigDecimal.TEN );

		index.index(
				"1", doc -> doc.addValue( index.binding().scaled, veryLargeDecimal )
		);

		assertThatThrownBy( () -> index.createScope()
				.query().selectEntityReference()
				.where( p -> p.range().field( "scaled" ).atMost( tooLargeDecimal ) )
		)
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll( "Unable to encode value '" + tooLargeDecimal.toString() + "'",
						"this field type only supports values ranging from ",
						"If you want to encode values that are outside this range,"
								+ " change the decimal scale for this field" );
	}

	@Test
	public void decimalScale_tooLargeDecimal_scale0_bigDecimal_lowerBound() {
		SimpleMappedIndex<DecimalScaleIndexBinding> index = SimpleMappedIndex.of(
				root -> new DecimalScaleIndexBinding( root, 0 )
		);
		setupHelper.start().withIndex( index ).setup();

		// Provide a value that cannot be represented as a long
		BigDecimal tooLargeDecimal = BigDecimal.valueOf( Long.MIN_VALUE ).multiply( BigDecimal.TEN );

		assertThatThrownBy( () -> index.index(
				"1", doc -> doc.addValue( index.binding().scaled, tooLargeDecimal )
		) )
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll( "Unable to encode value '" + tooLargeDecimal.toString() + "'",
						"this field type only supports values ranging from ",
						"If you want to encode values that are outside this range,"
								+ " change the decimal scale for this field" );
	}

	@Test
	public void decimalScale_tooLargeDecimal_scale0_bigInteger() {
		SimpleMappedIndex<IntegerScaleIndexBinding> index = SimpleMappedIndex.of(
				root -> new IntegerScaleIndexBinding( root, 0 )
		);
		setupHelper.start().withIndex( index ).setup();

		// Provide a value that cannot be represented as a long
		BigInteger tooLargeInteger = BigInteger.valueOf( Long.MAX_VALUE ).multiply( BigInteger.TEN );

		assertThatThrownBy( () -> index.index(
				"1", doc -> doc.addValue( index.binding().scaled, tooLargeInteger )
		) )
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll( "Unable to encode value '" + tooLargeInteger.toString() + "'",
						"this field type only supports values ranging from ",
						"If you want to encode values that are outside this range,"
								+ " change the decimal scale for this field" );
	}

	@Test
	public void decimalScale_tooLargeDecimal_scale0_bigInteger_lowerBound() {
		SimpleMappedIndex<IntegerScaleIndexBinding> index = SimpleMappedIndex.of(
				root -> new IntegerScaleIndexBinding( root, 0 )
		);
		setupHelper.start().withIndex( index ).setup();

		// Provide a value that cannot be represented as a long
		BigInteger tooLargeInteger = BigInteger.valueOf( Long.MIN_VALUE ).multiply( BigInteger.TEN );

		assertThatThrownBy( () -> index.index(
				"1", doc -> doc.addValue( index.binding().scaled, tooLargeInteger )
		) )
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll( "Unable to encode value '" + tooLargeInteger.toString() + "'",
						"this field type only supports values ranging from ",
						"If you want to encode values that are outside this range,"
								+ " change the decimal scale for this field" );
	}

	@Test
	public void decimalScale_tooLargeDecimal_scale0_bigInteger_lowerBound_queryPredicateBuildTime() {
		SimpleMappedIndex<IntegerScaleIndexBinding> index = SimpleMappedIndex.of(
				root -> new IntegerScaleIndexBinding( root, 0 )
		);
		setupHelper.start().withIndex( index ).setup();

		BigInteger veryLargeNegativeInteger = BigInteger.valueOf( Long.MIN_VALUE );
		// Provide a value that cannot be represented as a long
		BigInteger tooLargeInteger = veryLargeNegativeInteger.multiply( BigInteger.TEN );

		index.index(
				"1", doc -> doc.addValue( index.binding().scaled, veryLargeNegativeInteger )
		);

		assertThatThrownBy( () -> index.createScope()
				.query().selectEntityReference()
				.where( p -> p.range().field( "scaled" ).atLeast( tooLargeInteger ) )
		)
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll( "Unable to encode value '" + tooLargeInteger.toString() + "'",
						"this field type only supports values ranging from ",
						"If you want to encode values that are outside this range,"
								+ " change the decimal scale for this field" );
	}

	@Test
	public void decimalScale_tooLargeDecimal_scale2_bigDecimal() {
		SimpleMappedIndex<DecimalScaleIndexBinding> index = SimpleMappedIndex.of(
				root -> new DecimalScaleIndexBinding( root, 2 )
		);
		setupHelper.start().withIndex( index ).setup();

		// Provide a value that if it were divided by 10, could not be represented as a long, because the scale of 2
		BigDecimal tooLargeDecimal = BigDecimal.valueOf( Long.MAX_VALUE ).divide( BigDecimal.TEN );

		assertThatThrownBy( () -> index.index(
				"1", doc -> doc.addValue( index.binding().scaled, tooLargeDecimal )
		) )
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll( "Unable to encode value '" + tooLargeDecimal.toString() + "'",
						"this field type only supports values ranging from ",
						"If you want to encode values that are outside this range,"
								+ " change the decimal scale for this field" );
	}

	@Test
	public void decimalScale_tooLargeDecimal_scale2_bigDecimal_lowerBound() {
		SimpleMappedIndex<DecimalScaleIndexBinding> index = SimpleMappedIndex.of(
				root -> new DecimalScaleIndexBinding( root, 2 )
		);
		setupHelper.start().withIndex( index ).setup();

		// Provide a value that if it were divided by 10, could not be represented as a long, because the scale of 2
		BigDecimal tooLargeDecimal = BigDecimal.valueOf( Long.MIN_VALUE ).divide( BigDecimal.TEN );

		assertThatThrownBy( () -> index.index(
				"1", doc -> doc.addValue( index.binding().scaled, tooLargeDecimal )
		) )
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll( "Unable to encode value '" + tooLargeDecimal.toString() + "'",
						"this field type only supports values ranging from ",
						"If you want to encode values that are outside this range,"
								+ " change the decimal scale for this field" );
	}

	@Test
	public void decimalScale_tooLargeDecimal_scale2_bigDecimal_lowerBound_queryPredicateBuildTime() {
		SimpleMappedIndex<DecimalScaleIndexBinding> index = SimpleMappedIndex.of(
				root -> new DecimalScaleIndexBinding( root, 2 )
		);
		setupHelper.start().withIndex( index ).setup();

		// Provide a value that if it were divided by 10, could not be represented as a long, because the scale of 2
		BigDecimal tooLargeDecimal = BigDecimal.valueOf( Long.MIN_VALUE ).divide( BigDecimal.TEN );
		BigDecimal veryLargeDecimal = tooLargeDecimal.divide( BigDecimal.TEN );

		index.index(
				"1", doc -> doc.addValue( index.binding().scaled, veryLargeDecimal )
		);

		assertThatThrownBy( () -> index.createScope()
				.query().selectEntityReference()
				.where( p -> p.range().field( "scaled" ).atLeast( tooLargeDecimal ) )
		)
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll( "Unable to encode value '" + tooLargeDecimal.toString() + "'",
						"this field type only supports values ranging from ",
						"If you want to encode values that are outside this range,"
								+ " change the decimal scale for this field" );
	}

	@Test
	public void decimalScale_tooLargeDecimal_scaleMinus2_bigInteger() {
		SimpleMappedIndex<IntegerScaleIndexBinding> index = SimpleMappedIndex.of(
				root -> new IntegerScaleIndexBinding( root, -2 )
		);
		setupHelper.start().withIndex( index ).setup();

		// Provide a value that if it were multiplied by 100, could not be represented as a long, because the scale of -2
		BigInteger tooLargeInteger = BigInteger.valueOf( Long.MAX_VALUE ).multiply( new BigInteger( "1000" ) );

		assertThatThrownBy( () -> index.index(
				"1", doc -> doc.addValue( index.binding().scaled, tooLargeInteger )
		) )
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll( "Unable to encode value '" + tooLargeInteger.toString() + "'",
						"this field type only supports values ranging from ",
						"If you want to encode values that are outside this range,"
								+ " change the decimal scale for this field" );
	}

	@Test
	public void decimalScale_tooLargeDecimal_scaleMinus2_bigInteger_queryPredicateBuildTime() {
		SimpleMappedIndex<IntegerScaleIndexBinding> index = SimpleMappedIndex.of(
				root -> new IntegerScaleIndexBinding( root, -2 )
		);
		setupHelper.start().withIndex( index ).setup();

		BigInteger veryLargeInteger = BigInteger.valueOf( Long.MAX_VALUE );
		// Provide a value that if it were multiplied by 100, could not be represented as a long, because the scale of -2
		BigInteger tooLargeInteger = veryLargeInteger.multiply( new BigInteger( "1000" ) );

		index.index(
				"1", doc -> doc.addValue( index.binding().scaled, veryLargeInteger )
		);

		assertThatThrownBy( () -> index.createScope()
				.query().selectEntityReference()
				.where( p -> p.range().field( "scaled" ).atMost( tooLargeInteger ) )
		)
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll( "Unable to encode value '" + tooLargeInteger.toString() + "'",
						"this field type only supports values ranging from ",
						"If you want to encode values that are outside this range,"
								+ " change the decimal scale for this field" );
	}

	@Test
	public void decimalScale_tooLargeDecimal_scaleMinus2_bigInteger_lowerBound() {
		SimpleMappedIndex<IntegerScaleIndexBinding> index = SimpleMappedIndex.of(
				root -> new IntegerScaleIndexBinding( root, -2 )
		);
		setupHelper.start().withIndex( index ).setup();

		// Provide a value that if it were multiplied by 100, could not be represented as a long, because the scale of -2
		BigInteger tooLargeInteger = BigInteger.valueOf( Long.MIN_VALUE ).multiply( new BigInteger( "1000" ) );

		assertThatThrownBy( () -> index.index(
				"1", doc -> doc.addValue( index.binding().scaled, tooLargeInteger )
		) )
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll( "Unable to encode value '" + tooLargeInteger.toString() + "'",
						"this field type only supports values ranging from ",
						"If you want to encode values that are outside this range,"
								+ " change the decimal scale for this field" );
	}

	@Test
	public void defaultDecimalScale_bigDecimal() {
		SimpleMappedIndex<DefaultDecimalScaleIndexBinding> index = SimpleMappedIndex.ofAdvanced(
				DefaultDecimalScaleIndexBinding::new
		);
		setupHelper.start().withIndex( index ).setup();

		index.index(
				"1", doc -> doc.addValue( index.binding().scaled, new BigDecimal( "739.11111" ) )
		);

		// default decimal scale is 2, affecting the search precision
		// so the provided value 739.11111 will be treated as if it were 739.11
		matchGreaterThan( index, new BigDecimal( "739.1" ) );
		doNotMatchGreaterThan( index, new BigDecimal( "739.11" ) );
	}

	@Test
	public void defaultDecimalScale_bigInteger() {
		SimpleMappedIndex<DefaultIntegerScaleIndexBinding> index = SimpleMappedIndex.ofAdvanced(
				DefaultIntegerScaleIndexBinding::new
		);
		setupHelper.start().withIndex( index ).setup();

		index.index(
				"1", doc -> doc.addValue( index.binding().scaled, new BigInteger( "7391111" ) )
		);

		// default decimal scale is -2, affecting the search precision
		// so the provided value 7391111 will be treated as if it were 7391100
		matchGreaterThan( index, new BigInteger( "7391000" ) );
		doNotMatchGreaterThan( index, new BigInteger( "7391100" ) );
	}

	@Test
	public void decimalScale_andDefaultDecimalScale_bigDecimal() {
		SimpleMappedIndex<BothDecimalScaleIndexBinding> index = SimpleMappedIndex.ofAdvanced(
				BothDecimalScaleIndexBinding::new
		);
		setupHelper.start().withIndex( index ).setup();

		index.index(
				"1", doc -> doc.addValue( index.binding().scaled, new BigDecimal( "739.11111" ) )
		);

		// default decimal scale is 2
		// decimal scale has been set to 3, overriding the default and affecting the search precision
		// so the provided value 739.11111 will be treated as if it were 739.111
		matchGreaterThan( index, new BigDecimal( "739.11" ) );
		doNotMatchGreaterThan( index, new BigDecimal( "739.111" ) );
	}

	@Test
	public void decimalScale_andDefaultDecimalScale_bigInteger() {
		SimpleMappedIndex<BothIntegerScaleIndexBinding> index = SimpleMappedIndex.ofAdvanced(
				BothIntegerScaleIndexBinding::new
		);
		setupHelper.start().withIndex( index ).setup();

		index.index(
				"1", doc -> doc.addValue( index.binding().scaled, new BigInteger( "7391111" ) )
		);

		// default decimal scale is -2,
		// decimal scale has been set to -3, overriding the default and affecting the search precision
		// so the provided value 7391111 will be treated as if it were 7391000
		matchGreaterThan( index, new BigInteger( "7390000" ) );
		doNotMatchGreaterThan( index, new BigInteger( "7391000" ) );
	}

	@Test
	public void decimalScale_doesNotAffectProjections_bigDecimal() {
		SimpleMappedIndex<DecimalScaleIndexBinding> index = SimpleMappedIndex.of(
				root -> new DecimalScaleIndexBinding( root, 3 )
		);
		setupHelper.start().withIndex( index ).setup();

		index.index(
				"1", doc -> doc.addValue( index.binding().scaled, new BigDecimal( "739.11111" ) )
		);

		// even though decimal scale is 3, projected values wont be affected to
		projection( index, new BigDecimal( "739.11111" ) );
	}

	@Test
	public void decimalScale_doesNotAffectProjections_bigInteger() {
		SimpleMappedIndex<IntegerScaleIndexBinding> index = SimpleMappedIndex.of(
				root -> new IntegerScaleIndexBinding( root, -7 )
		);
		setupHelper.start().withIndex( index ).setup();

		index.index(
				"1", doc -> doc.addValue( index.binding().scaled, new BigInteger( "73911111" ) )
		);

		// even though decimal scale is -7, projected values wont be affected to
		projection( index, new BigInteger( "73911111" ) );
	}

	private void matchGreaterThan(StubMappedIndex index, Object value) {
		assertThatQuery( index.query()
				.where( p -> p.range().field( "scaled" ).greaterThan( value ) ) )
				.hasDocRefHitsAnyOrder( index.typeName(), "1" );
	}

	public void doNotMatchGreaterThan(StubMappedIndex index, Object value) {
		assertThatQuery( index.query()
				.where( p -> p.range().field( "scaled" ).greaterThan( value ) ) )
				.hasNoHits();
	}

	public void projection(StubMappedIndex index, BigDecimal value) {
		SearchQuery<Object> query = index.createScope().query()
				.select( p -> p.field( "scaled" ) )
				.where( p -> p.matchAll() )
				.toQuery();
		assertThatQuery( query ).hasHitsExactOrder( value );
	}

	public void projection(StubMappedIndex index, BigInteger value) {
		SearchQuery<Object> query = index.createScope().query()
				.select( p -> p.field( "scaled" ) )
				.where( p -> p.matchAll() )
				.toQuery();
		assertThatQuery( query ).hasHitsExactOrder( value );
	}

	private void match(StubMappedIndex index, Object matching, String match1, String match2) {
		assertThatQuery( index.query()
				.where( p -> p.match().field( "scaled" ).matching( matching ) ) )
				.hasDocRefHitsAnyOrder( index.typeName(), match1, match2 );
	}

	/**
	 * @param digitsBeforeDot The number of times the digit {@code 1} appears before the decimal dot.
	 * @param digitsAfterDot The number of times the digit {@code 1} appears after the decimal dot.
	 * @param scale The scale of the resulting number.
	 * @return A BigDecimal equal to this number:
	 * {@code <the digit '1' as many times as digitsBeforeDot>.<the digit '1' as many times as digitsAfterDot> * 10^<scale>}
	 */
	private static BigDecimal bigDecimalWithOnes(int digitsBeforeDot, int digitsAfterDot, int scale) {
		BigInteger unscaled = bigIntegerWithOnes( digitsBeforeDot + digitsAfterDot );
		return new BigDecimal( unscaled, scale + digitsAfterDot );
	}

	/**
	 * @param oneDigits The number of times the digit {@code 1} appears in most significant digits.
	 * @return A BigInteger equal to this number:
	 * {@code <the digit '1' as many times as oneDigits><the digit '0' as many times as zeroDigits>}
	 */
	private static BigInteger bigIntegerWithOnes(int oneDigits) {
		if ( oneDigits < 1 ) {
			throw new IllegalArgumentException();
		}
		BigInteger number = BigInteger.ONE;
		for ( int i = 1 /* we start with one digit */; i < oneDigits; i++ ) {
			number = number.multiply( BigInteger.TEN ).add( BigInteger.ONE );
		}
		return number;
	}

	private static class DecimalScaleIndexBinding {
		IndexFieldReference<BigDecimal> scaled;

		DecimalScaleIndexBinding(IndexSchemaElement root, int decimalScale) {
			scaled = root.field( "scaled", f -> f.asBigDecimal().projectable( Projectable.YES ).decimalScale( decimalScale ) )
					.toReference();
		}
	}

	private static class DefaultDecimalScaleIndexBinding {
		IndexFieldReference<BigDecimal> scaled;

		DefaultDecimalScaleIndexBinding(IndexedEntityBindingContext ctx) {
			scaled = ctx.schemaElement()
					.field( "scaled", ctx.createTypeFactory( new IndexFieldTypeDefaultsProvider( 2 ) ).asBigDecimal() )
					.toReference();
		}
	}

	private static class BothDecimalScaleIndexBinding {
		IndexFieldReference<BigDecimal> scaled;

		BothDecimalScaleIndexBinding(IndexedEntityBindingContext ctx) {
			scaled = ctx.schemaElement()
					// setting both default decimal scale
					.field( "scaled", ctx.createTypeFactory( new IndexFieldTypeDefaultsProvider( 2 ) )
							// and the not-default decimal scale
							.asBigDecimal().decimalScale( 3 ) )
					.toReference();
		}
	}

	private static class IntegerScaleIndexBinding {
		IndexFieldReference<BigInteger> scaled;

		IntegerScaleIndexBinding(IndexSchemaElement root, int decimalScale) {
			scaled = root.field( "scaled", f -> f.asBigInteger().projectable( Projectable.YES ).decimalScale( decimalScale ) )
					.toReference();
		}
	}

	private static class DefaultIntegerScaleIndexBinding {
		IndexFieldReference<BigInteger> scaled;

		DefaultIntegerScaleIndexBinding(IndexedEntityBindingContext ctx) {
			scaled = ctx.schemaElement()
					.field( "scaled", ctx.createTypeFactory( new IndexFieldTypeDefaultsProvider( -2 ) ).asBigInteger() )
					.toReference();
		}
	}

	private static class BothIntegerScaleIndexBinding {
		IndexFieldReference<BigInteger> scaled;

		BothIntegerScaleIndexBinding(IndexedEntityBindingContext ctx) {
			scaled = ctx.schemaElement()
					// setting both default decimal scale
					.field( "scaled", ctx.createTypeFactory( new IndexFieldTypeDefaultsProvider( -2 ) )
							// and the not-default decimal scale
							.asBigInteger().decimalScale( -3 ) )
					.toReference();
		}
	}
}
