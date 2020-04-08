/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.search.aggregation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMapperUtils.referenceProvider;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.document.IndexObjectFieldReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaObjectField;
import org.hibernate.search.engine.backend.document.model.dsl.ObjectFieldStorage;
import org.hibernate.search.engine.backend.types.Aggregable;
import org.hibernate.search.engine.backend.types.Projectable;
import org.hibernate.search.engine.backend.types.converter.FromDocumentFieldValueConverter;
import org.hibernate.search.engine.backend.types.converter.runtime.FromDocumentFieldValueConvertContext;
import org.hibernate.search.engine.backend.types.dsl.IndexFieldTypeFactory;
import org.hibernate.search.engine.backend.types.dsl.StandardIndexFieldTypeOptionsStep;
import org.hibernate.search.engine.backend.work.execution.spi.IndexIndexingPlan;
import org.hibernate.search.engine.search.aggregation.AggregationKey;
import org.hibernate.search.engine.search.aggregation.SearchAggregation;
import org.hibernate.search.engine.search.common.ValueConvert;
import org.hibernate.search.engine.search.aggregation.dsl.AggregationFinalStep;
import org.hibernate.search.engine.search.aggregation.dsl.SearchAggregationFactory;
import org.hibernate.search.engine.search.predicate.dsl.PredicateFinalStep;
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory;
import org.hibernate.search.integrationtest.backend.tck.testsupport.operations.AggregationDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.operations.expectations.AggregationScenario;
import org.hibernate.search.integrationtest.backend.tck.testsupport.operations.expectations.SupportedSingleFieldAggregationExpectations;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.FieldTypeDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.SimpleFieldModel;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.StandardFieldMapper;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.TypeAssertionHelper;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.ValueWrapper;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.common.assertion.SearchResultAssert;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.SimpleMappedIndex;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappingScope;
import org.assertj.core.api.Assertions;
import org.hibernate.search.util.impl.test.annotation.PortedFromSearch5;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;
import org.hibernate.search.util.impl.test.singleinstance.BeforeAll;
import org.hibernate.search.util.impl.test.singleinstance.InstanceRule;
import org.hibernate.search.util.impl.test.singleinstance.SingleInstanceRunnerWithParameters;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

/**
 * Tests basic behavior common to all single-field aggregations (range, terms, ...)
 * on supported types.
 */
@RunWith(Parameterized.class)
@Parameterized.UseParametersRunnerFactory(SingleInstanceRunnerWithParameters.Factory.class)
public class SingleFieldAggregationBaseIT<F> {

	private static final String AGGREGATION_NAME = "aggregationName";

	@Parameterized.Parameters(name = "{0} - {1}")
	public static Object[][] aggregationTypeCombinations() {
		List<Object[]> combinations = new ArrayList<>();
		for ( AggregationDescriptor aggregationDescriptor : AggregationDescriptor.getAll() ) {
			for ( FieldTypeDescriptor<?> fieldTypeDescriptor : FieldTypeDescriptor.getAll() ) {
				Optional<? extends SupportedSingleFieldAggregationExpectations<?>> expectations =
						aggregationDescriptor.getSingleFieldAggregationExpectations( fieldTypeDescriptor ).getSupported();
				if ( expectations.isPresent() ) {
					combinations.add( new Object[] {
							aggregationDescriptor,
							fieldTypeDescriptor,
							expectations.get()
					} );
				}
			}
		}
		return combinations.toArray( new Object[0][] );
	}

	@InstanceRule
	public SearchSetupHelper setupHelper = new SearchSetupHelper();

	private final FieldTypeDescriptor<F> typeDescriptor;
	private final SupportedSingleFieldAggregationExpectations<F> expectations;

	private SimpleMappedIndex<IndexBinding> mainIndex =
			SimpleMappedIndex.of( "Main", IndexBinding::new );
	private SimpleMappedIndex<IndexBinding> compatibleIndex =
			SimpleMappedIndex.of( "Compatible", IndexBinding::new );
	private SimpleMappedIndex<RawFieldCompatibleIndexBinding> rawFieldCompatibleIndex =
			SimpleMappedIndex.of( "RawFieldCompatible", RawFieldCompatibleIndexBinding::new );
	private SimpleMappedIndex<IncompatibleIndexBinding> incompatibleIndex =
			SimpleMappedIndex.of( "Incompatible", IncompatibleIndexBinding::new );
	private SimpleMappedIndex<IndexBinding> emptyIndex =
			SimpleMappedIndex.of( "Empty", IndexBinding::new );
	private SimpleMappedIndex<IndexBinding> nullOnlyIndex =
			SimpleMappedIndex.of( "NullOnly", IndexBinding::new );
	private SimpleMappedIndex<MultiValuedIndexBinding> multiValuedIndex =
			SimpleMappedIndex.of( "MultiValued", MultiValuedIndexBinding::new );

	public SingleFieldAggregationBaseIT(AggregationDescriptor thisIsJustForTestName,
			FieldTypeDescriptor<F> typeDescriptor,
			SupportedSingleFieldAggregationExpectations<F> expectations) {
		this.typeDescriptor = typeDescriptor;
		this.expectations = expectations;
	}

	@BeforeAll
	public void setup() {
		setupHelper.start()
				.withIndexes(
						mainIndex,
						compatibleIndex,
						rawFieldCompatibleIndex,
						incompatibleIndex,
						emptyIndex,
						nullOnlyIndex,
						multiValuedIndex
				)
				.setup();

		initData();
	}

	@Test
	@TestForIssue(jiraKey = { "HSEARCH-809", "HSEARCH-2376", "HSEARCH-2472", "HSEARCH-2954" })
	@PortedFromSearch5(original = {
			"org.hibernate.search.test.query.facet.NumberFacetingTest",
			"org.hibernate.search.test.query.facet.RangeFacetingTest.testRangeQueryForInteger",
			"org.hibernate.search.test.query.facet.RangeFacetingTest.testDateRangeFaceting",
			"org.hibernate.search.test.query.facet.SimpleFacetingTest.testSimpleDiscretFaceting",
			"org.hibernate.search.test.query.facet.StringFacetingTest"
	})
	public void simple() {
		// Need a separate method to handle the scenario generics
		doTest_simple( expectations.simple( typeDescriptor ) );
	}

	private <A> void doTest_simple(AggregationScenario<A> scenario) {
		StubMappingScope scope = mainIndex.createScope();
		String fieldPath = mainIndex.binding().fieldModel.relativeFieldName;
		AggregationKey<A> aggregationKey = AggregationKey.of( AGGREGATION_NAME );

		SearchResultAssert.assertThat(
				scope.query()
						.where( f -> f.matchAll() )
						.aggregation( aggregationKey, f -> scenario.setup( f, fieldPath ) )
						.toQuery()
		)
				.aggregation(
						aggregationKey,
						a -> assertThat( a ).isNotNull().satisfies( scenario::check )
				);
	}

	@Test
	public void aggregationObject() {
		// Need a separate method to handle the scenario generics
		doTest_aggregationObject( expectations.simple( typeDescriptor ) );
	}

	private <A> void doTest_aggregationObject(AggregationScenario<A> scenario) {
		StubMappingScope scope = mainIndex.createScope();
		String fieldPath = mainIndex.binding().fieldModel.relativeFieldName;
		AggregationKey<A> aggregationKey = AggregationKey.of( AGGREGATION_NAME );

		SearchAggregation<A> aggregation = scenario.setup( scope.aggregation(), fieldPath )
				.toAggregation();

		SearchResultAssert.assertThat(
				mainIndex.createScope().query()
						.where( f -> f.matchAll() )
						.aggregation( aggregationKey, aggregation )
						.toQuery()
		)
				.aggregation(
						aggregationKey,
						a -> assertThat( a ).isNotNull().satisfies( scenario::check )
				);
	}

	@Test
	public void aggregationObject_reuse_onScopeTargetingSameIndexes() {
		// Need a separate method to handle the scenario generics
		doTest_aggregationObject_reuse_onScopeTargetingSameIndexes( expectations.simple( typeDescriptor ) );
	}

	private <A> void doTest_aggregationObject_reuse_onScopeTargetingSameIndexes(AggregationScenario<A> scenario) {
		StubMappingScope scope = mainIndex.createScope();
		String fieldPath = mainIndex.binding().fieldModel.relativeFieldName;
		AggregationKey<A> aggregationKey = AggregationKey.of( AGGREGATION_NAME );

		SearchAggregation<A> aggregation = scenario.setup( scope.aggregation(), fieldPath )
				.toAggregation();

		SearchResultAssert.assertThat(
				scope.query()
						.where( f -> f.matchAll() )
						.aggregation( aggregationKey, aggregation )
						.toQuery()
		)
				.aggregation(
						aggregationKey,
						a -> assertThat( a ).isNotNull().satisfies( scenario::check )
				);

		// reuse the aggregation instance on the same scope
		SearchResultAssert.assertThat(
				scope.query()
						.where( f -> f.matchAll() )
						.aggregation( aggregationKey, aggregation )
						.toQuery()
		)
				.aggregation(
						aggregationKey,
						a -> assertThat( a ).isNotNull().satisfies( scenario::check )
				);

		// reuse the aggregation instance on a different scope targeting the same index
		scope = mainIndex.createScope();
		SearchResultAssert.assertThat(
				scope.query()
						.where( f -> f.matchAll() )
						.aggregation( aggregationKey, aggregation )
						.toQuery()
		)
				.aggregation(
						aggregationKey,
						a -> assertThat( a ).isNotNull().satisfies( scenario::check )
				);
	}

	@Test
	public void aggregationObject_reuse_onScopeTargetingDifferentIndexes() {
		// Need a separate method to handle the scenario generics
		doTest_aggregationObject_reuse_onScopeTargetingDifferentIndexes( expectations.simple( typeDescriptor ) );
	}

	private <A> void doTest_aggregationObject_reuse_onScopeTargetingDifferentIndexes(AggregationScenario<A> scenario) {
		StubMappingScope scope = mainIndex.createScope();
		String fieldPath = mainIndex.binding().fieldModel.relativeFieldName;
		AggregationKey<A> aggregationKey = AggregationKey.of( AGGREGATION_NAME );

		SearchAggregation<A> aggregation = scenario.setup( scope.aggregation(), fieldPath )
				.toAggregation();

		// reuse the aggregation instance on a different scope targeting a different index
		Assertions.assertThatThrownBy( () ->
				compatibleIndex.createScope().query()
						.where( f -> f.matchAll() )
						.aggregation( aggregationKey, aggregation )
						.toQuery()
		)
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "scope targeting different indexes" )
				.hasMessageContaining( mainIndex.name() )
				.hasMessageContaining( compatibleIndex.name() );

		// reuse the aggregation instance on a different scope targeting a superset of the original indexes
		Assertions.assertThatThrownBy( () ->
				mainIndex.createScope( compatibleIndex ).query()
						.where( f -> f.matchAll() )
						.aggregation( aggregationKey, aggregation )
						.toQuery()
		)
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "scope targeting different indexes" )
				.hasMessageContaining( mainIndex.name() )
				.hasMessageContaining( compatibleIndex.name() );
	}

	@Test
	@TestForIssue(jiraKey = { "HSEARCH-1968" })
	@PortedFromSearch5(original = "org.hibernate.search.test.query.facet.NoQueryResultsFacetingTest")
	public void noMatch() {
		String fieldPath = mainIndex.binding().fieldModel.relativeFieldName;

		AggregationScenario<?> scenario = expectations.withoutMatch( typeDescriptor );
		testValidAggregation(
				scenario, mainIndex.createScope(),
				f -> f.id().matching( "none" ), // Don't match any document
				(f, e) -> e.setup( f, fieldPath )
		);
	}

	/**
	 * Test behavior when aggregating on an index with no value for the targeted field
	 * because there is no document in the mainIndex.
	 */
	@Test
	@TestForIssue(jiraKey = { "HSEARCH-2955", "HSEARCH-745" })
	@PortedFromSearch5(original = {
			"org.hibernate.search.test.facet.NoIndexedValueFacetingTest",
			"org.hibernate.search.test.query.facet.EdgeCaseFacetTest"
	})
	public void emptyIndex() {
		String fieldPath = mainIndex.binding().fieldModel.relativeFieldName;

		AggregationScenario<?> scenario = expectations.withoutMatch( typeDescriptor );
		testValidAggregation(
				scenario, emptyIndex.createScope(), fieldPath
		);
	}

	/**
	 * Test behavior when aggregating on an index with no value for the targeted field
	 * because no document has a non-null value for this field.
	 */
	@Test
	@TestForIssue(jiraKey = "HSEARCH-2955")
	@PortedFromSearch5(original = "org.hibernate.search.test.facet.NoIndexedValueFacetingTest")
	public void nullOnlyIndex() {
		String fieldPath = nullOnlyIndex.binding().fieldModel.relativeFieldName;

		AggregationScenario<?> scenario = expectations.withoutMatch( typeDescriptor );
		testValidAggregation(
				scenario, nullOnlyIndex.createScope(), fieldPath
		);
	}

	/**
	 * Test behavior when aggregating on an multi-valued field.
	 */
	@Test
	@TestForIssue(jiraKey = {"HSEARCH-726", "HSEARCH-900", "HSEARCH-2535", "HSEARCH-1927", "HSEARCH-1929"})
	@PortedFromSearch5(original = {
			"org.hibernate.search.test.query.facet.EmbeddedCollectionFacetingTest",
			"org.hibernate.search.test.query.facet.ManyToOneFacetingTest",
			"org.hibernate.search.test.query.facet.MultiValuedFacetingTest"
	})
	public void multiValued() {
		String fieldPath = multiValuedIndex.binding().fieldModel.relativeFieldName;

		AggregationScenario<?> scenario = expectations.onMultiValuedIndex( typeDescriptor );
		testValidAggregation(
				scenario, multiValuedIndex.createScope(), fieldPath
		);
	}

	@Test
	@PortedFromSearch5(original = "org.hibernate.search.test.query.facet.SimpleFacetingTest.testNullFieldNameThrowsException")
	public void nullFieldPath() {
		// Try to pass a "null" field type
		AggregationScenario<?> scenario = expectations.simple( typeDescriptor );

		Assertions.assertThatThrownBy( () -> scenario.setup( mainIndex.createScope().aggregation(), null ) )
				.isInstanceOf( IllegalArgumentException.class )
				.hasMessageContaining( "'absoluteFieldPath'" )
				.hasMessageContaining( "must not be null" );
	}

	@Test
	public void nullFieldType() {
		String fieldPath = mainIndex.binding().fieldModel.relativeFieldName;

		// Try to pass a "null" field type
		AggregationScenario<?> scenario = expectations.withFieldType( TypeAssertionHelper.nullType() );

		Assertions.assertThatThrownBy( () -> scenario.setup( mainIndex.createScope().aggregation(), fieldPath ) )
				.isInstanceOf( IllegalArgumentException.class )
				.hasMessageContaining( "'type'" )
				.hasMessageContaining( "must not be null" );
	}

	@Test
	public void invalidFieldType_conversionEnabled() {
		String fieldPath = mainIndex.binding().fieldModel.relativeFieldName;

		AggregationScenario<?> scenario = expectations.withFieldType( TypeAssertionHelper.wrongType( typeDescriptor ) );

		Assertions.assertThatThrownBy( () -> scenario.setup( mainIndex.createScope().aggregation(), fieldPath ) )
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "Invalid type" )
				.hasMessageContaining( "for aggregation on field" )
				.hasMessageContaining( "'" + fieldPath + "'" );
	}

	@Test
	public void invalidFieldType_conversionDisabled() {
		String fieldPath = mainIndex.binding().fieldModel.relativeFieldName;

		AggregationScenario<?> scenario = expectations.withFieldType( TypeAssertionHelper.wrongType( typeDescriptor ) );

		Assertions.assertThatThrownBy( () -> scenario.setupWithConverterSetting(
				mainIndex.createScope().aggregation(), fieldPath, ValueConvert.NO
		) )
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "Invalid type" )
				.hasMessageContaining( "for aggregation on field" )
				.hasMessageContaining( "'" + fieldPath + "'" );
	}

	@Test
	@PortedFromSearch5(original = "org.hibernate.search.test.query.facet.FacetUnknownFieldFailureTest.testUnknownFieldNameThrowsException")
	public void unknownField() {
		String fieldPath = "unknownField";

		AggregationScenario<?> scenario = expectations.simple( typeDescriptor );

		Assertions.assertThatThrownBy( () -> scenario.setup( mainIndex.createScope().aggregation(), fieldPath ) )
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "Unknown field" )
				.hasMessageContaining( "'" + fieldPath + "'" )
				.hasMessageContaining( mainIndex.name() );
	}

	@Test
	public void objectField_nested() {
		String fieldPath = mainIndex.binding().nestedObject.relativeFieldName;

		AggregationScenario<?> scenario = expectations.withFieldType( TypeAssertionHelper.identity( typeDescriptor ) );

		Assertions.assertThatThrownBy( () -> scenario.setup( mainIndex.createScope().aggregation(), fieldPath ) )
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "Unknown field" )
				.hasMessageContaining( "'" + fieldPath + "'" )
				.hasMessageContaining( mainIndex.name() );
	}

	@Test
	public void objectField_flattened() {
		String fieldPath = mainIndex.binding().flattenedObject.relativeFieldName;

		AggregationScenario<?> scenario = expectations.withFieldType( TypeAssertionHelper.identity( typeDescriptor ) );

		Assertions.assertThatThrownBy( () -> scenario.setup( mainIndex.createScope().aggregation(), fieldPath ) )
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "Unknown field" )
				.hasMessageContaining( "'" + fieldPath + "'" )
				.hasMessageContaining( mainIndex.name() );
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-1748")
	@PortedFromSearch5(original = "org.hibernate.search.test.query.facet.FacetUnknownFieldFailureTest.testKnownFieldNameNotConfiguredForFacetingThrowsException")
	public void aggregationsDisabled() {
		String fieldPath = mainIndex.binding().fieldWithAggregationDisabledModel.relativeFieldName;

		AggregationScenario<?> scenario = expectations.withFieldType( TypeAssertionHelper.identity( typeDescriptor ) );

		Assertions.assertThatThrownBy( () -> scenario.setup( mainIndex.createScope().aggregation(), fieldPath ) )
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "Aggregations are not enabled for field" )
				.hasMessageContaining( "'" + fieldPath + "'" );
	}

	@Test
	public void withConverter_conversionEnabled() {
		String fieldPath = mainIndex.binding().fieldWithConverterModel.relativeFieldName;

		AggregationScenario<?> scenario = expectations.withFieldType(
				TypeAssertionHelper.wrapper( typeDescriptor )
		);
		testValidAggregation(
				scenario, mainIndex.createScope(), fieldPath
		);
	}

	@Test
	public void withConverter_conversionDisabled() {
		String fieldPath = mainIndex.binding().fieldModel.relativeFieldName;

		AggregationScenario<?> scenario = expectations.simple( typeDescriptor );
		testValidAggregationWithConverterSetting(
				scenario, mainIndex.createScope(), fieldPath, ValueConvert.NO
		);
	}

	@Test
	public void withConverter_invalidFieldType() {
		String fieldPath = mainIndex.binding().fieldWithConverterModel.relativeFieldName;

		AggregationScenario<?> scenario = expectations.simple( typeDescriptor );

		Assertions.assertThatThrownBy( () -> scenario.setup( mainIndex.createScope().aggregation(), fieldPath ) )
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "Invalid type" )
				.hasMessageContaining( "for aggregation on field" )
				.hasMessageContaining( "'" + fieldPath + "'" );
	}

	/**
	 * Test that mentioning the same aggregation twice with different keys works as expected.
	 */
	@Test
	@PortedFromSearch5(original = "org.hibernate.search.test.query.facet.SimpleFacetingTest.testMultipleFacets")
	public void duplicated_differentKeys() {
		String fieldPath = mainIndex.binding().fieldModel.relativeFieldName;

		AggregationScenario<?> scenario = expectations.simple( typeDescriptor );

		// A separate method is needed in order to write type-safe code
		doTestDuplicatedDifferentKeys( fieldPath, scenario );
	}

	private <A> void doTestDuplicatedDifferentKeys(String fieldPath, AggregationScenario<A> scenario) {
		AggregationKey<A> key1 = AggregationKey.of( "aggregationName1" );
		AggregationKey<A> key2 = AggregationKey.of( "aggregationName2" );

		SearchResultAssert.assertThat(
				mainIndex.createScope().query().where( f -> f.matchAll() )
						.aggregation( key1, f -> scenario.setup( f, fieldPath ) )
						.aggregation( key2, f -> scenario.setup( f, fieldPath ) )
						.toQuery()
		)
				.aggregation(
						key1,
						a -> assertThat( a ).isNotNull().satisfies( scenario::check )
				)
				.aggregation(
						key2,
						a -> assertThat( a ).isNotNull().satisfies( scenario::check )
				);
	}

	/**
	 * Test that mentioning the same aggregation twice with the same key throws an exception as expected.
	 */
	@Test
	public void duplicated_sameKey() {
		String fieldPath = mainIndex.binding().fieldModel.relativeFieldName;

		AggregationScenario<?> scenario = expectations.simple( typeDescriptor );

		// A separate method is needed in order to write type-safe code
		doTestDuplicatedSameKey( fieldPath, scenario );
	}

	private <A> void doTestDuplicatedSameKey(String fieldPath, AggregationScenario<A> scenario) {
		AggregationKey<A> key1 = AggregationKey.of( "aggregationName1" );

		Assertions.assertThatThrownBy( () ->
				mainIndex.createScope().query().where( f -> f.matchAll() )
						.aggregation( key1, f -> scenario.setup( f, fieldPath ) )
						.aggregation( key1, f -> scenario.setup( f, fieldPath ) )
		)
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "Multiple aggregations with the same key: " )
				.hasMessageContaining( "'aggregationName1'" );
	}

	@Test
	public void inFlattenedObject() {
		String fieldPath = mainIndex.binding().fieldModel.relativeFieldName;

		AggregationScenario<?> scenario = expectations.simple( typeDescriptor );
		testValidAggregation(
				scenario, mainIndex.createScope(), fieldPath
		);
	}

	@Test
	public void multiIndex_withCompatibleIndex_noConverter() {
		StubMappingScope scope = mainIndex.createScope( compatibleIndex );

		String fieldPath = mainIndex.binding().fieldModel.relativeFieldName;

		AggregationScenario<?> scenario = expectations.onMainAndOtherIndex( typeDescriptor );
		testValidAggregation(
				scenario, scope, fieldPath
		);
	}

	@Test
	public void multiIndex_withCompatibleIndex_conversionEnabled() {
		StubMappingScope scope = mainIndex.createScope( compatibleIndex );

		String fieldPath = mainIndex.binding().fieldWithConverterModel.relativeFieldName;

		AggregationScenario<?> scenario = expectations.withFieldTypeOnMainAndOtherIndex(
				TypeAssertionHelper.wrapper( typeDescriptor )
		);
		testValidAggregation(
				scenario, scope, fieldPath
		);
	}

	@Test
	public void multiIndex_withRawFieldCompatibleIndex_conversionEnabled() {
		StubMappingScope scope = mainIndex.createScope( rawFieldCompatibleIndex );

		String fieldPath = mainIndex.binding().fieldWithConverterModel.relativeFieldName;

		AggregationScenario<?> scenario = expectations.withFieldTypeOnMainAndOtherIndex(
				TypeAssertionHelper.wrapper( typeDescriptor )
		);

		Assertions.assertThatThrownBy( () -> scenario.setup( scope.aggregation(), fieldPath ) )
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "Multiple conflicting types to build an aggregation" )
				.hasMessageContaining( "'" + fieldPath + "'" );
	}

	@Test
	public void multiIndex_withRawFieldCompatibleIndex_conversionDisabled() {
		StubMappingScope scope = mainIndex.createScope( rawFieldCompatibleIndex );

		String fieldPath = mainIndex.binding().fieldWithConverterModel.relativeFieldName;

		AggregationScenario<?> scenario = expectations.onMainAndOtherIndex( typeDescriptor );
		testValidAggregationWithConverterSetting(
				scenario, scope, fieldPath, ValueConvert.NO
		);
	}

	@Test
	public void multiIndex_withIncompatibleIndex_conversionEnabled() {
		StubMappingScope scope = mainIndex.createScope( incompatibleIndex );

		String fieldPath = mainIndex.binding().fieldModel.relativeFieldName;

		AggregationScenario<?> scenario = expectations.simple( typeDescriptor );

		Assertions.assertThatThrownBy( () -> scenario.setup( scope.aggregation(), fieldPath ) )
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "Multiple conflicting types to build an aggregation" )
				.hasMessageContaining( "'" + fieldPath + "'" );
	}

	@Test
	public void multiIndex_withIncompatibleIndex_conversionDisabled() {
		StubMappingScope scope = mainIndex.createScope( incompatibleIndex );

		String fieldPath = mainIndex.binding().fieldModel.relativeFieldName;

		AggregationScenario<?> scenario = expectations.simple( typeDescriptor );

		Assertions.assertThatThrownBy( () -> scenario.setupWithConverterSetting(
				scope.aggregation(), fieldPath, ValueConvert.NO
		) )
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "Multiple conflicting types to build an aggregation" )
				.hasMessageContaining( "'" + fieldPath + "'" );
	}

	private <A> void testValidAggregation(AggregationScenario<A> scenario, StubMappingScope scope,
			String fieldPath) {
		testValidAggregation(
				scenario, scope,
				f -> f.matchAll(),
				(f, e) -> e.setup( f, fieldPath )
		);
	}

	private <A> void testValidAggregationWithConverterSetting(AggregationScenario<A> scenario,
			StubMappingScope scope, String fieldPath, ValueConvert convert) {
		testValidAggregation(
				scenario, scope,
				f -> f.matchAll(),
				(f, e) -> e.setupWithConverterSetting( f, fieldPath, convert )
		);
	}

	private <A> void testValidAggregation(AggregationScenario<A> scenario, StubMappingScope scope,
			Function<SearchPredicateFactory, ? extends PredicateFinalStep> predicateContributor,
			BiFunction<SearchAggregationFactory, AggregationScenario<A>, AggregationFinalStep<A>> aggregationContributor) {
		AggregationKey<A> aggregationKey = AggregationKey.of( AGGREGATION_NAME );
		SearchResultAssert.assertThat(
				scope.query()
						.where( predicateContributor )
						.aggregation( aggregationKey, f -> aggregationContributor.apply( f, scenario ) )
						.toQuery()
		)
				.aggregation(
						aggregationKey,
						a -> assertThat( a ).isNotNull().satisfies( scenario::check )
				);
	}

	private void initData() {
		List<F> mainIndexDocumentFieldValues = expectations.getMainIndexDocumentFieldValues();
		List<F> otherIndexDocumentFieldValues = expectations.getOtherIndexDocumentFieldValues();
		List<List<F>> multiValuedIndexDocumentFieldValues = expectations.getMultiValuedIndexDocumentFieldValues();

		IndexIndexingPlan<?> plan = mainIndex.createIndexingPlan();
		for ( int i = 0; i < mainIndexDocumentFieldValues.size(); i++ ) {
			F value = mainIndexDocumentFieldValues.get( i );
			plan.add( referenceProvider( "document_" + i ), document -> {
				document.addValue( mainIndex.binding().fieldModel.reference, value );
				document.addValue( mainIndex.binding().fieldWithConverterModel.reference, value );

				// Note: this object must be single-valued for these tests
				DocumentElement flattenedObject = document.addObject( mainIndex.binding().flattenedObject.self );
				flattenedObject.addValue( mainIndex.binding().flattenedObject.fieldModel.reference, value );

				// Note: this object must be single-valued for these tests
				DocumentElement nestedObject = document.addObject( mainIndex.binding().nestedObject.self );
				nestedObject.addValue( mainIndex.binding().nestedObject.fieldModel.reference, value );
			} );
		}
		plan.add( referenceProvider( "document_empty" ), document -> { } );
		plan.execute().join();

		plan = compatibleIndex.createIndexingPlan();
		for ( int i = 0; i < otherIndexDocumentFieldValues.size(); i++ ) {
			F value = otherIndexDocumentFieldValues.get( i );
			plan.add( referenceProvider( "compatibleindex_document_" + i ), document -> {
				document.addValue( compatibleIndex.binding().fieldModel.reference, value );
				document.addValue( compatibleIndex.binding().fieldWithConverterModel.reference, value );
			} );
		}
		plan.execute().join();

		plan = rawFieldCompatibleIndex.createIndexingPlan();
		for ( int i = 0; i < otherIndexDocumentFieldValues.size(); i++ ) {
			F value = otherIndexDocumentFieldValues.get( i );
			plan.add( referenceProvider( "rawcompatibleindex_document_" + i ), document -> {
				document.addValue( rawFieldCompatibleIndex.binding().fieldWithConverterModel.reference, value );
			} );
		}
		plan.execute().join();

		plan = nullOnlyIndex.createIndexingPlan();
		plan.add( referenceProvider( "nullOnlyIndex_document_0" ), document -> {
			document.addValue( nullOnlyIndex.binding().fieldModel.reference, null );
		} );
		plan.execute().join();

		plan = multiValuedIndex.createIndexingPlan();
		for ( int i = 0; i < multiValuedIndexDocumentFieldValues.size(); i++ ) {
			List<F> values = multiValuedIndexDocumentFieldValues.get( i );
			plan.add( referenceProvider( "document_" + i ), document -> {
				for ( F value : values ) {
					document.addValue( multiValuedIndex.binding().fieldModel.reference, value );
				}
			} );
		}
		plan.add( referenceProvider( "document_empty" ), document -> { } );
		plan.execute().join();

		// Check that all documents are searchable
		SearchResultAssert.assertThat( mainIndex.createScope().query()
				.where( f -> f.matchAll() )
				.toQuery() )
				.hasTotalHitCount( mainIndexDocumentFieldValues.size() + 1 /* +1 for the empty document */ );
		SearchResultAssert.assertThat( compatibleIndex.createScope().query()
				.where( f -> f.matchAll() )
				.toQuery() )
				.hasTotalHitCount( otherIndexDocumentFieldValues.size() );
		SearchResultAssert.assertThat( rawFieldCompatibleIndex.createScope().query()
				.where( f -> f.matchAll() )
				.toQuery() )
				.hasTotalHitCount( otherIndexDocumentFieldValues.size() );
		SearchResultAssert.assertThat( nullOnlyIndex.createScope().query()
				.where( f -> f.matchAll() )
				.toQuery() )
				.hasTotalHitCount( 1 );
		SearchResultAssert.assertThat( multiValuedIndex.createScope().query()
				.where( f -> f.matchAll() )
				.toQuery() )
				.hasTotalHitCount( multiValuedIndexDocumentFieldValues.size() + 1 /* +1 for the empty document */ );
	}

	private SimpleFieldModel<F> mapField(IndexSchemaElement parent, String prefix,
			Consumer<StandardIndexFieldTypeOptionsStep<?, F>> additionalConfiguration) {
		return SimpleFieldModel.mapper( typeDescriptor, additionalConfiguration )
				.map( parent, prefix + typeDescriptor.getUniqueName() );
	}

	private SimpleFieldModel<F> mapMultiValuedField(IndexSchemaElement parent, String prefix,
			Consumer<StandardIndexFieldTypeOptionsStep<?, F>> additionalConfiguration) {
		return SimpleFieldModel.mapper( typeDescriptor, additionalConfiguration )
				.mapMultiValued( parent, prefix + typeDescriptor.getUniqueName() );
	}

	private class IndexBinding {
		final SimpleFieldModel<F> fieldModel;
		final SimpleFieldModel<F> fieldWithConverterModel;
		final SimpleFieldModel<F> fieldWithAggregationDisabledModel;

		final ObjectBinding flattenedObject;
		final ObjectBinding nestedObject;

		IndexBinding(IndexSchemaElement root) {
			fieldModel = mapField(
					root, "",
					c -> c.aggregable( Aggregable.YES )
			);
			fieldWithConverterModel = mapField(
					root, "converted_",
					c -> c.aggregable( Aggregable.YES )
							.dslConverter( ValueWrapper.class, ValueWrapper.toIndexFieldConverter() )
							.projectionConverter( ValueWrapper.class, ValueWrapper.fromIndexFieldConverter() )
			);
			fieldWithAggregationDisabledModel = mapField(
					root, "nonAggregable_",
					c -> c.aggregable( Aggregable.NO )
			);

			flattenedObject = new ObjectBinding( root, "flattenedObject", ObjectFieldStorage.FLATTENED );
			nestedObject = new ObjectBinding( root, "nestedObject", ObjectFieldStorage.NESTED );
		}
	}

	private class ObjectBinding {
		final String relativeFieldName;
		final IndexObjectFieldReference self;
		final SimpleFieldModel<F> fieldModel;

		ObjectBinding(IndexSchemaElement parent, String relativeFieldName, ObjectFieldStorage storage) {
			this.relativeFieldName = relativeFieldName;
			IndexSchemaObjectField objectField = parent.objectField( relativeFieldName, storage );
			self = objectField.toReference();
			fieldModel = mapField(
					objectField, "", ignored -> { }
			);
		}
	}

	private class RawFieldCompatibleIndexBinding {
		final SimpleFieldModel<F> fieldWithConverterModel;

		RawFieldCompatibleIndexBinding(IndexSchemaElement root) {
			/*
			 * Add a field with the same name as the fieldWithConverterModel from IndexMapping,
			 * but with an incompatible projection converter.
			 */
			fieldWithConverterModel = mapField(
					root, "converted_",
					c -> c.aggregable( Aggregable.YES )
							.dslConverter( ValueWrapper.class, ValueWrapper.toIndexFieldConverter() )
							.projectionConverter( ValueWrapper.class, new IncompatibleProjectionConverter() )
			);
		}

		private class IncompatibleProjectionConverter
				implements FromDocumentFieldValueConverter<F, ValueWrapper> {
			@Override
			public ValueWrapper<F> convert(F value, FromDocumentFieldValueConvertContext context) {
				return null;
			}
		}
	}

	private class IncompatibleIndexBinding {
		IncompatibleIndexBinding(IndexSchemaElement root) {
			/*
			 * Add a field with the same name as the fieldModel from IndexMapping,
			 * but with an incompatible type.
			 */
			IncompatibleFieldModel.mapper( FieldTypeDescriptor.getIncompatible( typeDescriptor )::configure )
					.map( root, typeDescriptor.getUniqueName() );
		}
	}

	private class MultiValuedIndexBinding {
		final SimpleFieldModel<F> fieldModel;

		MultiValuedIndexBinding(IndexSchemaElement root) {
			fieldModel = mapMultiValuedField(
					root, "",
					c -> c.aggregable( Aggregable.YES )
			);
		}
	}

	private static class IncompatibleFieldModel {
		static <F> StandardFieldMapper<F, IncompatibleFieldModel> mapper(
				Function<IndexFieldTypeFactory, StandardIndexFieldTypeOptionsStep<?, F>> configuration) {
			return StandardFieldMapper.of(
					configuration,
					c -> c.projectable( Projectable.YES ),
					(reference, name) -> new IncompatibleFieldModel( name )
			);
		}

		final String relativeFieldName;

		private IncompatibleFieldModel(String relativeFieldName) {
			this.relativeFieldName = relativeFieldName;
		}
	}
}
