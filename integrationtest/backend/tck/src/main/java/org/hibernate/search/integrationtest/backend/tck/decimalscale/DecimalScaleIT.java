/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.decimalscale;

import static org.hibernate.search.util.impl.integrationtest.common.assertion.SearchResultAssert.assertThat;
import static org.hibernate.search.util.impl.integrationtest.common.stub.mapper.StubMapperUtils.referenceProvider;

import java.math.BigDecimal;
import java.math.BigInteger;

import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.work.execution.spi.IndexWorkPlan;
import org.hibernate.search.engine.backend.types.Projectable;
import org.hibernate.search.engine.mapper.mapping.building.spi.IndexFieldTypeDefaultsProvider;
import org.hibernate.search.engine.mapper.mapping.building.spi.IndexedEntityBindingContext;
import org.hibernate.search.engine.search.DocumentReference;
import org.hibernate.search.engine.search.query.SearchQuery;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.common.stub.mapper.StubMappingIndexManager;
import org.hibernate.search.util.impl.test.SubTest;

import org.junit.Rule;
import org.junit.Test;

import org.assertj.core.api.Assertions;

public class DecimalScaleIT {

	private static final String INDEX_NAME = "indexname";

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
	public SearchSetupHelper setupHelper = new SearchSetupHelper();

	private DecimalScaleIndexMapping decimalScaleIndexMapping;
	private DefaultDecimalScaleIndexMapping defaultDecimalScaleIndexMapping;
	private BothDecimalScaleIndexMapping bothDecimalScaleIndexMapping;

	private IntegerScaleIndexMapping integerScaleIndexMapping;
	private DefaultIntegerScaleIndexMapping defaultIntegerScaleIndexMapping;
	private BothIntegerScaleIndexMapping bothIntegerScaleIndexMapping;

	private StubMappingIndexManager indexManager;

	@Test
	public void noDecimalScale_bigDecimal() {
		SubTest.expectException( () ->
				setupHelper.withDefaultConfiguration()
						.withIndex( INDEX_NAME, ctx -> ctx.getSchemaElement().field( "noScaled", f -> f.asBigDecimal() ).toReference() )
						.setup()
		).assertThrown()
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "Impossible to detect a decimal scale to use for this field." );
	}

	@Test
	public void noDecimalScale_bigInteger() {
		SubTest.expectException( () ->
				setupHelper.withDefaultConfiguration()
						.withIndex( INDEX_NAME, ctx -> ctx.getSchemaElement().field( "noScaled", f -> f.asBigInteger() ).toReference() )
						.setup()
		).assertThrown()
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "Impossible to detect a decimal scale to use for this field." );
	}

	@Test
	public void positiveDecimalScale_bigInteger() {
		SubTest.expectException( () ->
				setupHelper.withDefaultConfiguration()
						.withIndex( INDEX_NAME, ctx -> ctx.getSchemaElement().field( "positiveScaled", f -> f.asBigInteger().decimalScale( 3 ) ).toReference() )
						.setup()
		).assertThrown()
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "Positive decimal scale ['3'] is not allowed for BigInteger fields" );
	}

	@Test
	public void decimalScale_bigDecimal() {
		setupHelper.withDefaultConfiguration()
				.withIndex( INDEX_NAME,
						ctx -> this.decimalScaleIndexMapping = new DecimalScaleIndexMapping( ctx.getSchemaElement(), 3 ),
						indexManager -> this.indexManager = indexManager )
				.setup();

		IndexWorkPlan<? extends DocumentElement> workPlan = indexManager.createWorkPlan();
		workPlan.add( referenceProvider( "1" ), doc -> doc.addValue( decimalScaleIndexMapping.scaled, new BigDecimal( "739.11111" ) ) );
		workPlan.execute().join();

		// decimal scale is 3, affecting the search precision
		// so the provided value 739.11111 will be treated as if it were 739.111
		matchAbove( new BigDecimal( "739.11" ) );
		doNotMatchAbove( new BigDecimal( "739.111" ) );
	}

	@Test
	public void decimalScale_zeroScale_bigDecimal() {
		setupHelper.withDefaultConfiguration()
				.withIndex( INDEX_NAME,
						ctx -> this.decimalScaleIndexMapping = new DecimalScaleIndexMapping( ctx.getSchemaElement(), 0 ),
						indexManager -> this.indexManager = indexManager )
				.setup();

		IndexWorkPlan<? extends DocumentElement> workPlan = indexManager.createWorkPlan();
		workPlan.add( referenceProvider( "1" ), doc -> doc.addValue( decimalScaleIndexMapping.scaled, new BigDecimal( "739.11111" ) ) );
		workPlan.execute().join();

		// decimal scale is 0, affecting the search precision
		// so the provided value 739.11111 will be treated as if it were 739
		matchAbove( new BigDecimal( "738" ) );
		doNotMatchAbove( new BigDecimal( "739" ) );
	}

	@Test
	public void decimalScale_zeroScale_bigInteger() {
		setupHelper.withDefaultConfiguration()
				.withIndex( INDEX_NAME,
						ctx -> this.integerScaleIndexMapping = new IntegerScaleIndexMapping( ctx.getSchemaElement(), 0 ),
						indexManager -> this.indexManager = indexManager )
				.setup();

		IndexWorkPlan<? extends DocumentElement> workPlan = indexManager.createWorkPlan();
		workPlan.add( referenceProvider( "1" ), doc -> doc.addValue( integerScaleIndexMapping.scaled, new BigInteger( "739" ) ) );
		workPlan.execute().join();

		// decimal scale is 0, affecting the search precision
		// so the provided value 739 will be treated as if it were 739
		matchAbove( new BigInteger( "738" ) );
		doNotMatchAbove( new BigInteger( "739" ) );
	}

	@Test
	public void decimalScale_negativeScale_bigDecimal() {
		setupHelper.withDefaultConfiguration()
				.withIndex( INDEX_NAME,
						ctx -> this.decimalScaleIndexMapping = new DecimalScaleIndexMapping( ctx.getSchemaElement(), -3 ),
						indexManager -> this.indexManager = indexManager )
				.setup();

		IndexWorkPlan<? extends DocumentElement> workPlan = indexManager.createWorkPlan();
		workPlan.add( referenceProvider( "1" ), doc -> doc.addValue( decimalScaleIndexMapping.scaled, new BigDecimal( "11111.11111" ) ) );
		workPlan.execute().join();

		// decimal scale is -3, affecting the search precision
		// so the provided value 11111.11111 will be treated as if it were 11000
		matchAbove( new BigDecimal( "10000" ) );
		doNotMatchAbove( new BigDecimal( "11000" ) );
	}

	@Test
	public void decimalScale_negativeScale_bigInteger() {
		setupHelper.withDefaultConfiguration()
				.withIndex( INDEX_NAME,
						ctx -> this.integerScaleIndexMapping = new IntegerScaleIndexMapping( ctx.getSchemaElement(), -3 ),
						indexManager -> this.indexManager = indexManager )
				.setup();

		IndexWorkPlan<? extends DocumentElement> workPlan = indexManager.createWorkPlan();
		workPlan.add( referenceProvider( "1" ), doc -> doc.addValue( integerScaleIndexMapping.scaled, new BigInteger( "11111" ) ) );
		workPlan.execute().join();

		// decimal scale is -3, affecting the search precision
		// so the provided value 11111 will be treated as if it were 11000
		matchAbove( new BigInteger( "10000" ) );
		doNotMatchAbove( new BigInteger( "11000" ) );
	}

	@Test
	public void decimalScale_largeScale_bigDecimal() {
		final int schemaDecimalScale = 275;

		setupHelper.withDefaultConfiguration()
				.withIndex( INDEX_NAME,
						ctx -> this.decimalScaleIndexMapping = new DecimalScaleIndexMapping( ctx.getSchemaElement(), schemaDecimalScale ),
						indexManager -> this.indexManager = indexManager )
				.setup();

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
		BigDecimal indexedValueLowerBound = estimatedIndexedValue.subtract( new BigDecimal( BigInteger.ONE, schemaDecimalScale ) );
		BigDecimal indexedValueUpperBound = estimatedIndexedValue.add( new BigDecimal( BigInteger.ONE, schemaDecimalScale ) );
		Assertions.assertThat( originalValue )
				.isBetween( indexedValueLowerBound, indexedValueUpperBound );

		IndexWorkPlan<? extends DocumentElement> workPlan = indexManager.createWorkPlan();
		workPlan.add( referenceProvider( "1" ), doc -> doc.addValue( decimalScaleIndexMapping.scaled, originalValue ) );
		workPlan.execute().join();

		matchAbove( indexedValueLowerBound );
		doNotMatchAbove( indexedValueUpperBound );
	}

	@Test
	public void decimalScale_negativeScale_largeScale_bigDecimal() {
		final int schemaDecimalScale = -275;

		setupHelper.withDefaultConfiguration()
				.withIndex( INDEX_NAME,
						ctx -> this.decimalScaleIndexMapping = new DecimalScaleIndexMapping( ctx.getSchemaElement(), schemaDecimalScale ),
						indexManager -> this.indexManager = indexManager )
				.setup();

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
		BigDecimal indexedValueLowerBound = estimatedIndexedValue.subtract( new BigDecimal( BigInteger.ONE, schemaDecimalScale ) );
		BigDecimal indexedValueUpperBound = estimatedIndexedValue.add( new BigDecimal( BigInteger.ONE, schemaDecimalScale ) );
		Assertions.assertThat( originalValue )
				.isBetween( indexedValueLowerBound, indexedValueUpperBound );

		IndexWorkPlan<? extends DocumentElement> workPlan = indexManager.createWorkPlan();
		workPlan.add( referenceProvider( "1" ), doc -> doc.addValue( decimalScaleIndexMapping.scaled, originalValue ) );
		workPlan.execute().join();

		matchAbove( indexedValueLowerBound );
		doNotMatchAbove( indexedValueUpperBound );
	}

	@Test
	public void decimalScale_negativeScale_largeScale_bigInteger() {
		final int schemaDecimalScale = -275;

		setupHelper.withDefaultConfiguration()
				.withIndex( INDEX_NAME,
						ctx -> this.integerScaleIndexMapping = new IntegerScaleIndexMapping( ctx.getSchemaElement(), schemaDecimalScale ),
						indexManager -> this.indexManager = indexManager )
				.setup();

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
		BigInteger indexedValueLowerBound = estimatedIndexedValue.subtract( new BigDecimal( BigInteger.ONE, schemaDecimalScale ).toBigIntegerExact() );
		BigInteger indexedValueUpperBound = estimatedIndexedValue.add( new BigDecimal( BigInteger.ONE, schemaDecimalScale ).toBigIntegerExact() );
		Assertions.assertThat( originalValue )
				.isBetween( indexedValueLowerBound, indexedValueUpperBound );

		IndexWorkPlan<? extends DocumentElement> workPlan = indexManager.createWorkPlan();
		workPlan.add( referenceProvider( "1" ), doc -> doc.addValue( integerScaleIndexMapping.scaled, originalValue ) );
		workPlan.execute().join();

		matchAbove( indexedValueLowerBound );
		doNotMatchAbove( indexedValueUpperBound );
	}

	@Test
	public void decimalScale_rounding_bigDecimal() {
		setupHelper.withDefaultConfiguration()
				.withIndex( INDEX_NAME,
						ctx -> this.decimalScaleIndexMapping = new DecimalScaleIndexMapping( ctx.getSchemaElement(), 2 ),
						indexManager -> this.indexManager = indexManager )
				.setup();

		IndexWorkPlan<? extends DocumentElement> workPlan = indexManager.createWorkPlan();
		workPlan.add( referenceProvider( "1" ), doc -> doc.addValue( decimalScaleIndexMapping.scaled, new BigDecimal( "739.114999" ) ) );
		workPlan.add( referenceProvider( "2" ), doc -> doc.addValue( decimalScaleIndexMapping.scaled, new BigDecimal( "739.115" ) ) );
		workPlan.add( referenceProvider( "3" ), doc -> doc.addValue( decimalScaleIndexMapping.scaled, new BigDecimal( "739.11" ) ) );
		workPlan.add( referenceProvider( "4" ), doc -> doc.addValue( decimalScaleIndexMapping.scaled, new BigDecimal( "739.12" ) ) );
		workPlan.execute().join();

		// RoundingMode.HALF_UP expected on both values:
		match( new BigDecimal( "739.11" ), "1", "3" );
		match( new BigDecimal( "739.12" ), "2", "4" );

		// and parameters:
		match( new BigDecimal( "739.114999" ), "1", "3" );
		match( new BigDecimal( "739.115" ), "2", "4" );
	}

	@Test
	public void decimalScale_rounding_bigInteger() {
		setupHelper.withDefaultConfiguration()
				.withIndex( INDEX_NAME,
						ctx -> this.integerScaleIndexMapping = new IntegerScaleIndexMapping( ctx.getSchemaElement(), -4 ),
						indexManager -> this.indexManager = indexManager )
				.setup();

		IndexWorkPlan<? extends DocumentElement> workPlan = indexManager.createWorkPlan();
		workPlan.add( referenceProvider( "1" ), doc -> doc.addValue( integerScaleIndexMapping.scaled, new BigInteger( "7394999" ) ) );
		workPlan.add( referenceProvider( "2" ), doc -> doc.addValue( integerScaleIndexMapping.scaled, new BigInteger( "7395000" ) ) );
		workPlan.add( referenceProvider( "3" ), doc -> doc.addValue( integerScaleIndexMapping.scaled, new BigInteger( "7390000" ) ) );
		workPlan.add( referenceProvider( "4" ), doc -> doc.addValue( integerScaleIndexMapping.scaled, new BigInteger( "7400000" ) ) );
		workPlan.execute().join();

		// RoundingMode.HALF_UP expected on both values:
		match( new BigInteger( "7390000" ), "1", "3" );
		match( new BigInteger( "7400000" ), "2", "4" );

		// and parameters:
		match( new BigInteger( "7394999" ), "1", "3" );
		match( new BigInteger( "7395000" ), "2", "4" );
	}

	@Test
	public void decimalScale_largeDecimal_bigDecimal() {
		setupHelper.withDefaultConfiguration()
				.withIndex( INDEX_NAME,
						ctx -> this.decimalScaleIndexMapping = new DecimalScaleIndexMapping( ctx.getSchemaElement(), 0 ),
						indexManager -> this.indexManager = indexManager )
				.setup();

		// That seems a limit for ES. Even if new BigDecimal( "2" ).pow( 54 ) << Long.MAX_VALUE
		// If the exponent were 54, the test would fail for Elasticsearch, whereas it would work for Lucene backend.
		BigDecimal largeDecimal = new BigDecimal( "2" ).pow( 53 );

		IndexWorkPlan<? extends DocumentElement> workPlan = indexManager.createWorkPlan();
		workPlan.add( referenceProvider( "1" ), doc -> doc.addValue( decimalScaleIndexMapping.scaled, largeDecimal ) );
		workPlan.execute().join();

		// the precision is supposed to be preserved
		matchAbove( largeDecimal.subtract( BigDecimal.ONE ) );
		doNotMatchAbove( largeDecimal );
	}

	@Test
	public void decimalScale_largeDecimal_bigInteger() {
		setupHelper.withDefaultConfiguration()
				.withIndex( INDEX_NAME,
						ctx -> this.integerScaleIndexMapping = new IntegerScaleIndexMapping( ctx.getSchemaElement(), 0 ),
						indexManager -> this.indexManager = indexManager )
				.setup();

		// That seems a limit for ES. Even if new BigDecimal( "2" ).pow( 54 ) << Long.MAX_VALUE
		// If the exponent were 54, the test would fail for Elasticsearch, whereas it would work for Lucene backend.
		BigInteger largeInteger = new BigInteger( "2" ).pow( 53 );

		IndexWorkPlan<? extends DocumentElement> workPlan = indexManager.createWorkPlan();
		workPlan.add( referenceProvider( "1" ), doc -> doc.addValue( integerScaleIndexMapping.scaled, largeInteger ) );
		workPlan.execute().join();

		// the precision is supposed to be preserved
		matchAbove( largeInteger.subtract( BigInteger.ONE ) );
		doNotMatchAbove( largeInteger );
	}

	@Test
	public void decimalScale_tooLargeDecimal_scale0_bigDecimal() {
		setupHelper.withDefaultConfiguration()
				.withIndex( INDEX_NAME,
						ctx -> this.decimalScaleIndexMapping = new DecimalScaleIndexMapping( ctx.getSchemaElement(), 0 ),
						indexManager -> this.indexManager = indexManager )
				.setup();

		// Provide a value that cannot be represented as a long
		BigDecimal tooLargeDecimal = BigDecimal.valueOf( Long.MAX_VALUE ).multiply( BigDecimal.TEN );

		SubTest.expectException( () -> {
			IndexWorkPlan<? extends DocumentElement> workPlan = indexManager.createWorkPlan();
			workPlan.add( referenceProvider( "1" ), doc -> doc.addValue( decimalScaleIndexMapping.scaled, tooLargeDecimal ) );
			workPlan.execute().join();
		} )
				.assertThrown()
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "cannot be indexed because its absolute value is too large." );
	}

	@Test
	public void decimalScale_tooLargeDecimal_scale0_bigDecimal_queryPredicateBuildTime() {
		setupHelper.withDefaultConfiguration()
				.withIndex( INDEX_NAME,
						ctx -> this.decimalScaleIndexMapping = new DecimalScaleIndexMapping( ctx.getSchemaElement(), 0 ),
						indexManager -> this.indexManager = indexManager )
				.setup();

		BigDecimal veryLargeDecimal = BigDecimal.valueOf( Long.MAX_VALUE );
		// Provide a value that cannot be represented as a long
		BigDecimal tooLargeDecimal = veryLargeDecimal.multiply( BigDecimal.TEN );

		IndexWorkPlan<? extends DocumentElement> workPlan = indexManager.createWorkPlan();
		workPlan.add( referenceProvider( "1" ), doc -> doc.addValue( decimalScaleIndexMapping.scaled, veryLargeDecimal ) );
		workPlan.execute().join();

		SubTest.expectException( () -> {
			indexManager.createScope()
					.query().asEntityReference()
					.predicate( p -> p.range().onField( "scaled" ).below( tooLargeDecimal ) );
		} )
				.assertThrown()
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "cannot be indexed because its absolute value is too large." );
	}

	@Test
	public void decimalScale_tooLargeDecimal_scale0_bigDecimal_lowerBound() {
		setupHelper.withDefaultConfiguration()
				.withIndex( INDEX_NAME,
						ctx -> this.decimalScaleIndexMapping = new DecimalScaleIndexMapping( ctx.getSchemaElement(), 0 ),
						indexManager -> this.indexManager = indexManager )
				.setup();

		// Provide a value that cannot be represented as a long
		BigDecimal tooLargeDecimal = BigDecimal.valueOf( Long.MIN_VALUE ).multiply( BigDecimal.TEN );

		SubTest.expectException( () -> {
			IndexWorkPlan<? extends DocumentElement> workPlan = indexManager.createWorkPlan();
			workPlan.add( referenceProvider( "1" ), doc -> doc.addValue( decimalScaleIndexMapping.scaled, tooLargeDecimal ) );
			workPlan.execute().join();
		} )
				.assertThrown()
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "cannot be indexed because its absolute value is too large." );
	}

	@Test
	public void decimalScale_tooLargeDecimal_scale0_bigInteger() {
		setupHelper.withDefaultConfiguration()
				.withIndex( INDEX_NAME,
						ctx -> this.integerScaleIndexMapping = new IntegerScaleIndexMapping( ctx.getSchemaElement(), 0 ),
						indexManager -> this.indexManager = indexManager )
				.setup();

		// Provide a value that cannot be represented as a long
		BigInteger tooLargeInteger = BigInteger.valueOf( Long.MAX_VALUE ).multiply( BigInteger.TEN );

		SubTest.expectException( () -> {
			IndexWorkPlan<? extends DocumentElement> workPlan = indexManager.createWorkPlan();
			workPlan.add( referenceProvider( "1" ), doc -> doc.addValue( integerScaleIndexMapping.scaled, tooLargeInteger ) );
			workPlan.execute().join();
		} )
				.assertThrown()
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "cannot be indexed because its absolute value is too large." );
	}

	@Test
	public void decimalScale_tooLargeDecimal_scale0_bigInteger_lowerBound() {
		setupHelper.withDefaultConfiguration()
				.withIndex( INDEX_NAME,
						ctx -> this.integerScaleIndexMapping = new IntegerScaleIndexMapping( ctx.getSchemaElement(), 0 ),
						indexManager -> this.indexManager = indexManager )
				.setup();

		// Provide a value that cannot be represented as a long
		BigInteger tooLargeInteger = BigInteger.valueOf( Long.MIN_VALUE ).multiply( BigInteger.TEN );

		SubTest.expectException( () -> {
			IndexWorkPlan<? extends DocumentElement> workPlan = indexManager.createWorkPlan();
			workPlan.add( referenceProvider( "1" ), doc -> doc.addValue( integerScaleIndexMapping.scaled, tooLargeInteger ) );
			workPlan.execute().join();
		} )
				.assertThrown()
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "cannot be indexed because its absolute value is too large." );
	}

	@Test
	public void decimalScale_tooLargeDecimal_scale0_bigInteger_lowerBound_queryPredicateBuildTime() {
		setupHelper.withDefaultConfiguration()
				.withIndex( INDEX_NAME,
						ctx -> this.integerScaleIndexMapping = new IntegerScaleIndexMapping( ctx.getSchemaElement(), 0 ),
						indexManager -> this.indexManager = indexManager )
				.setup();

		BigInteger veryLargeNegativeInteger = BigInteger.valueOf( Long.MIN_VALUE );
		// Provide a value that cannot be represented as a long
		BigInteger tooLargeInteger = veryLargeNegativeInteger.multiply( BigInteger.TEN );

		IndexWorkPlan<? extends DocumentElement> workPlan = indexManager.createWorkPlan();
		workPlan.add( referenceProvider( "1" ), doc -> doc.addValue( integerScaleIndexMapping.scaled, veryLargeNegativeInteger ) );
		workPlan.execute().join();

		SubTest.expectException( () -> {
			indexManager.createScope()
					.query().asEntityReference()
					.predicate( p -> p.range().onField( "scaled" ).above( tooLargeInteger ) );
		} )
				.assertThrown()
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "cannot be indexed because its absolute value is too large." );
	}

	@Test
	public void decimalScale_tooLargeDecimal_scale2_bigDecimal() {
		setupHelper.withDefaultConfiguration()
				.withIndex( INDEX_NAME,
						ctx -> this.decimalScaleIndexMapping = new DecimalScaleIndexMapping( ctx.getSchemaElement(), 2 ),
						indexManager -> this.indexManager = indexManager )
				.setup();

		// Provide a value that if it were divided by 10, could not be represented as a long, because the scale of 2
		BigDecimal tooLargeDecimal = BigDecimal.valueOf( Long.MAX_VALUE ).divide( BigDecimal.TEN );

		SubTest.expectException( () -> {
			IndexWorkPlan<? extends DocumentElement> workPlan = indexManager.createWorkPlan();
			workPlan.add( referenceProvider( "1" ), doc -> doc.addValue( decimalScaleIndexMapping.scaled, tooLargeDecimal ) );
			workPlan.execute().join();
		} )
				.assertThrown()
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "cannot be indexed because its absolute value is too large." );
	}

	@Test
	public void decimalScale_tooLargeDecimal_scale2_bigDecimal_lowerBound() {
		setupHelper.withDefaultConfiguration()
				.withIndex( INDEX_NAME,
						ctx -> this.decimalScaleIndexMapping = new DecimalScaleIndexMapping( ctx.getSchemaElement(), 2 ),
						indexManager -> this.indexManager = indexManager )
				.setup();

		// Provide a value that if it were divided by 10, could not be represented as a long, because the scale of 2
		BigDecimal tooLargeDecimal = BigDecimal.valueOf( Long.MIN_VALUE ).divide( BigDecimal.TEN );

		SubTest.expectException( () -> {
			IndexWorkPlan<? extends DocumentElement> workPlan = indexManager.createWorkPlan();
			workPlan.add( referenceProvider( "1" ), doc -> doc.addValue( decimalScaleIndexMapping.scaled, tooLargeDecimal ) );
			workPlan.execute().join();
		} )
				.assertThrown()
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "cannot be indexed because its absolute value is too large." );
	}

	@Test
	public void decimalScale_tooLargeDecimal_scale2_bigDecimal_lowerBound_queryPredicateBuildTime() {
		setupHelper.withDefaultConfiguration()
				.withIndex( INDEX_NAME,
						ctx -> this.decimalScaleIndexMapping = new DecimalScaleIndexMapping( ctx.getSchemaElement(), 2 ),
						indexManager -> this.indexManager = indexManager )
				.setup();

		// Provide a value that if it were divided by 10, could not be represented as a long, because the scale of 2
		BigDecimal tooLargeDecimal = BigDecimal.valueOf( Long.MIN_VALUE ).divide( BigDecimal.TEN );
		BigDecimal veryLargeDecimal = tooLargeDecimal.divide( BigDecimal.TEN );

		IndexWorkPlan<? extends DocumentElement> workPlan = indexManager.createWorkPlan();
		workPlan.add( referenceProvider( "1" ), doc -> doc.addValue( decimalScaleIndexMapping.scaled, veryLargeDecimal ) );
		workPlan.execute().join();

		SubTest.expectException( () -> {
			indexManager.createScope()
					.query().asEntityReference()
					.predicate( p -> p.range().onField( "scaled" ).above( tooLargeDecimal ) );
		} )
				.assertThrown()
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "cannot be indexed because its absolute value is too large." );
	}

	@Test
	public void decimalScale_tooLargeDecimal_scaleMinus2_bigInteger() {
		setupHelper.withDefaultConfiguration()
				.withIndex( INDEX_NAME,
						ctx -> this.integerScaleIndexMapping = new IntegerScaleIndexMapping( ctx.getSchemaElement(), -2 ),
						indexManager -> this.indexManager = indexManager )
				.setup();

		// Provide a value that if it were multiplied by 100, could not be represented as a long, because the scale of -2
		BigInteger tooLargeInteger = BigInteger.valueOf( Long.MAX_VALUE ).multiply( new BigInteger( "1000" ) );

		SubTest.expectException( () -> {
			IndexWorkPlan<? extends DocumentElement> workPlan = indexManager.createWorkPlan();
			workPlan.add( referenceProvider( "1" ), doc -> doc.addValue( integerScaleIndexMapping.scaled, tooLargeInteger ) );
			workPlan.execute().join();
		} )
				.assertThrown()
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "cannot be indexed because its absolute value is too large." );
	}

	@Test
	public void decimalScale_tooLargeDecimal_scaleMinus2_bigInteger_queryPredicateBuildTime() {
		setupHelper.withDefaultConfiguration()
				.withIndex( INDEX_NAME,
						ctx -> this.integerScaleIndexMapping = new IntegerScaleIndexMapping( ctx.getSchemaElement(), -2 ),
						indexManager -> this.indexManager = indexManager )
				.setup();

		BigInteger veryLargeInteger = BigInteger.valueOf( Long.MAX_VALUE );
		// Provide a value that if it were multiplied by 100, could not be represented as a long, because the scale of -2
		BigInteger tooLargeInteger = veryLargeInteger.multiply( new BigInteger( "1000" ) );

		IndexWorkPlan<? extends DocumentElement> workPlan = indexManager.createWorkPlan();
		workPlan.add( referenceProvider( "1" ), doc -> doc.addValue( integerScaleIndexMapping.scaled, veryLargeInteger ) );
		workPlan.execute().join();

		SubTest.expectException( () -> {
			indexManager.createScope()
					.query().asEntityReference()
					.predicate( p -> p.range().onField( "scaled" ).below( tooLargeInteger ) );
		} )
				.assertThrown()
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "cannot be indexed because its absolute value is too large." );
	}

	@Test
	public void decimalScale_tooLargeDecimal_scaleMinus2_bigInteger_lowerBound() {
		setupHelper.withDefaultConfiguration()
				.withIndex( INDEX_NAME,
						ctx -> this.integerScaleIndexMapping = new IntegerScaleIndexMapping( ctx.getSchemaElement(), -2 ),
						indexManager -> this.indexManager = indexManager )
				.setup();

		// Provide a value that if it were multiplied by 100, could not be represented as a long, because the scale of -2
		BigInteger tooLargeInteger = BigInteger.valueOf( Long.MIN_VALUE ).multiply( new BigInteger( "1000" ) );

		SubTest.expectException( () -> {
			IndexWorkPlan<? extends DocumentElement> workPlan = indexManager.createWorkPlan();
			workPlan.add( referenceProvider( "1" ), doc -> doc.addValue( integerScaleIndexMapping.scaled, tooLargeInteger ) );
			workPlan.execute().join();
		} )
				.assertThrown()
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "cannot be indexed because its absolute value is too large." );
	}

	@Test
	public void defaultDecimalScale_bigDecimal() {
		setupHelper.withDefaultConfiguration()
				.withIndex( INDEX_NAME,
						ctx -> this.defaultDecimalScaleIndexMapping = new DefaultDecimalScaleIndexMapping( ctx ),
						indexManager -> this.indexManager = indexManager )
				.setup();

		IndexWorkPlan<? extends DocumentElement> workPlan = indexManager.createWorkPlan();
		workPlan.add( referenceProvider( "1" ), doc -> doc.addValue( defaultDecimalScaleIndexMapping.scaled, new BigDecimal( "739.11111" ) ) );
		workPlan.execute().join();

		// default decimal scale is 2, affecting the search precision
		// so the provided value 739.11111 will be treated as if it were 739.11
		matchAbove( new BigDecimal( "739.1" ) );
		doNotMatchAbove( new BigDecimal( "739.11" ) );
	}

	@Test
	public void defaultDecimalScale_bigInteger() {
		setupHelper.withDefaultConfiguration()
				.withIndex( INDEX_NAME,
						ctx -> this.defaultIntegerScaleIndexMapping = new DefaultIntegerScaleIndexMapping( ctx ),
						indexManager -> this.indexManager = indexManager )
				.setup();

		IndexWorkPlan<? extends DocumentElement> workPlan = indexManager.createWorkPlan();
		workPlan.add( referenceProvider( "1" ), doc -> doc.addValue( defaultIntegerScaleIndexMapping.scaled, new BigInteger( "7391111" ) ) );
		workPlan.execute().join();

		// default decimal scale is -2, affecting the search precision
		// so the provided value 7391111 will be treated as if it were 7391100
		matchAbove( new BigInteger( "7391000" ) );
		doNotMatchAbove( new BigInteger( "7391100" ) );
	}

	@Test
	public void decimalScale_andDefaultDecimalScale_bigDecimal() {
		setupHelper.withDefaultConfiguration()
				.withIndex( INDEX_NAME,
						ctx -> this.bothDecimalScaleIndexMapping = new BothDecimalScaleIndexMapping( ctx ),
						indexManager -> this.indexManager = indexManager )
				.setup();

		IndexWorkPlan<? extends DocumentElement> workPlan = indexManager.createWorkPlan();
		workPlan.add( referenceProvider( "1" ), doc -> doc.addValue( bothDecimalScaleIndexMapping.scaled, new BigDecimal( "739.11111" ) ) );
		workPlan.execute().join();

		// default decimal scale is 2
		// decimal scale has been set to 3, overriding the default and affecting the search precision
		// so the provided value 739.11111 will be treated as if it were 739.111
		matchAbove( new BigDecimal( "739.11" ) );
		doNotMatchAbove( new BigDecimal( "739.111" ) );
	}

	@Test
	public void decimalScale_andDefaultDecimalScale_bigInteger() {
		setupHelper.withDefaultConfiguration()
				.withIndex( INDEX_NAME,
						ctx -> this.bothIntegerScaleIndexMapping = new BothIntegerScaleIndexMapping( ctx ),
						indexManager -> this.indexManager = indexManager )
				.setup();

		IndexWorkPlan<? extends DocumentElement> workPlan = indexManager.createWorkPlan();
		workPlan.add( referenceProvider( "1" ), doc -> doc.addValue( bothIntegerScaleIndexMapping.scaled, new BigInteger( "7391111" ) ) );
		workPlan.execute().join();

		// default decimal scale is -2,
		// decimal scale has been set to -3, overriding the default and affecting the search precision
		// so the provided value 7391111 will be treated as if it were 7391000
		matchAbove( new BigInteger( "7390000" ) );
		doNotMatchAbove( new BigInteger( "7391000" ) );
	}

	@Test
	public void decimalScale_doesNotAffectProjections_bigDecimal() {
		setupHelper.withDefaultConfiguration()
				.withIndex( INDEX_NAME,
						ctx -> this.decimalScaleIndexMapping = new DecimalScaleIndexMapping( ctx.getSchemaElement(), 3 ),
						indexManager -> this.indexManager = indexManager )
				.setup();

		IndexWorkPlan<? extends DocumentElement> workPlan = indexManager.createWorkPlan();
		workPlan.add( referenceProvider( "1" ), doc -> doc.addValue( decimalScaleIndexMapping.scaled, new BigDecimal( "739.11111" ) ) );
		workPlan.execute().join();

		// even though decimal scale is 3, projected values wont be affected to
		projection( new BigDecimal( "739.11111" ) );
	}

	@Test
	public void decimalScale_doesNotAffectProjections_bigInteger() {
		setupHelper.withDefaultConfiguration()
				.withIndex( INDEX_NAME,
						ctx -> this.integerScaleIndexMapping = new IntegerScaleIndexMapping( ctx.getSchemaElement(), -7 ),
						indexManager -> this.indexManager = indexManager )
				.setup();

		IndexWorkPlan<? extends DocumentElement> workPlan = indexManager.createWorkPlan();
		workPlan.add( referenceProvider( "1" ), doc -> doc.addValue( integerScaleIndexMapping.scaled, new BigInteger( "73911111" ) ) );
		workPlan.execute().join();

		// even though decimal scale is -7, projected values wont be affected to
		projection( new BigInteger( "73911111" ) );
	}

	private void matchAbove(BigDecimal value) {
		SearchQuery<DocumentReference> query = indexManager.createScope()
				.query().asEntityReference()
				.predicate( p -> p.range().onField( "scaled" ).above( value ).excludeLimit() )
				.toQuery();
		assertThat( query ).hasDocRefHitsAnyOrder( INDEX_NAME, "1" );
	}

	private void matchAbove(BigInteger value) {
		SearchQuery<DocumentReference> query = indexManager.createScope()
				.query().asEntityReference()
				.predicate( p -> p.range().onField( "scaled" ).above( value ).excludeLimit() )
				.toQuery();
		assertThat( query ).hasDocRefHitsAnyOrder( INDEX_NAME, "1" );
	}

	public void doNotMatchAbove(BigDecimal value) {
		SearchQuery<DocumentReference> query = indexManager.createScope()
				.query().asEntityReference()
				.predicate( p -> p.range().onField( "scaled" ).above( value ).excludeLimit() )
				.toQuery();
		assertThat( query ).hasNoHits();
	}

	public void doNotMatchAbove(BigInteger value) {
		SearchQuery<DocumentReference> query = indexManager.createScope()
				.query().asEntityReference()
				.predicate( p -> p.range().onField( "scaled" ).above( value ).excludeLimit() )
				.toQuery();
		assertThat( query ).hasNoHits();
	}

	public void projection(BigDecimal value) {
		SearchQuery<Object> query = indexManager.createScope().query()
				.asProjection( p -> p.field( "scaled" ) )
				.predicate( p -> p.matchAll() )
				.toQuery();
		assertThat( query ).hasHitsExactOrder( value );
	}

	public void projection(BigInteger value) {
		SearchQuery<Object> query = indexManager.createScope().query()
				.asProjection( p -> p.field( "scaled" ) )
				.predicate( p -> p.matchAll() )
				.toQuery();
		assertThat( query ).hasHitsExactOrder( value );
	}

	private void match(BigDecimal matching, String match1, String match2 ) {
		SearchQuery<DocumentReference> query = indexManager.createScope()
				.query().asEntityReference()
				.predicate( p -> p.match().onField( "scaled" ).matching( matching ) )
				.toQuery();
		assertThat( query ).hasDocRefHitsAnyOrder( INDEX_NAME, match1, match2 );
	}

	private void match(BigInteger matching, String match1, String match2 ) {
		SearchQuery<DocumentReference> query = indexManager.createScope()
				.query().asEntityReference()
				.predicate( p -> p.match().onField( "scaled" ).matching( matching ) )
				.toQuery();
		assertThat( query ).hasDocRefHitsAnyOrder( INDEX_NAME, match1, match2 );
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

	private static class DecimalScaleIndexMapping {
		IndexFieldReference<BigDecimal> scaled;

		DecimalScaleIndexMapping(IndexSchemaElement root, int decimalScale) {
			scaled = root.field( "scaled", f -> f.asBigDecimal().projectable( Projectable.YES ).decimalScale( decimalScale ) ).toReference();
		}
	}

	private static class DefaultDecimalScaleIndexMapping {
		IndexFieldReference<BigDecimal> scaled;

		DefaultDecimalScaleIndexMapping(IndexedEntityBindingContext ctx) {
			scaled = ctx.getSchemaElement()
					.field( "scaled", ctx.createTypeFactory( new IndexFieldTypeDefaultsProvider( 2 ) ).asBigDecimal() ).toReference();
		}
	}

	private static class BothDecimalScaleIndexMapping {
		IndexFieldReference<BigDecimal> scaled;

		BothDecimalScaleIndexMapping(IndexedEntityBindingContext ctx) {
			scaled = ctx.getSchemaElement()
					// setting both default decimal scale
					.field( "scaled", ctx.createTypeFactory( new IndexFieldTypeDefaultsProvider( 2 ) )
							// and the not-default decimal scale
							.asBigDecimal().decimalScale( 3 ) ).toReference();
		}
	}

	private static class IntegerScaleIndexMapping {
		IndexFieldReference<BigInteger> scaled;

		IntegerScaleIndexMapping(IndexSchemaElement root, int decimalScale) {
			scaled = root.field( "scaled", f -> f.asBigInteger().projectable( Projectable.YES ).decimalScale( decimalScale ) ).toReference();
		}
	}

	private static class DefaultIntegerScaleIndexMapping {
		IndexFieldReference<BigInteger> scaled;

		DefaultIntegerScaleIndexMapping(IndexedEntityBindingContext ctx) {
			scaled = ctx.getSchemaElement()
					.field( "scaled", ctx.createTypeFactory( new IndexFieldTypeDefaultsProvider( -2 ) ).asBigInteger() ).toReference();
		}
	}

	private static class BothIntegerScaleIndexMapping {
		IndexFieldReference<BigInteger> scaled;

		BothIntegerScaleIndexMapping(IndexedEntityBindingContext ctx) {
			scaled = ctx.getSchemaElement()
					// setting both default decimal scale
					.field( "scaled", ctx.createTypeFactory( new IndexFieldTypeDefaultsProvider( -2 ) )
							// and the not-default decimal scale
							.asBigInteger().decimalScale( -3 ) ).toReference();
		}
	}
}