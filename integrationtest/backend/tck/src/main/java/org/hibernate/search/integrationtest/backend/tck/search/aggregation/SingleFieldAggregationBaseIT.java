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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;
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
import org.hibernate.search.engine.search.aggregation.dsl.AggregationFinalStep;
import org.hibernate.search.engine.search.aggregation.dsl.SearchAggregationFactory;
import org.hibernate.search.engine.search.common.ValueConvert;
import org.hibernate.search.engine.search.predicate.dsl.PredicateFinalStep;
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory;
import org.hibernate.search.integrationtest.backend.tck.testsupport.operations.AggregationDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.operations.expectations.AggregationScenario;
import org.hibernate.search.integrationtest.backend.tck.testsupport.operations.expectations.SupportedSingleFieldAggregationExpectations;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.FieldTypeDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.SimpleFieldModelsByType;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.StandardFieldMapper;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.TypeAssertionHelper;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.ValueWrapper;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.common.assertion.SearchResultAssert;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.SimpleMappedIndex;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappingScope;
import org.hibernate.search.util.impl.test.annotation.PortedFromSearch5;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import org.assertj.core.api.Assertions;

/**
 * Tests basic behavior common to all single-field aggregations (range, terms, ...)
 * on supported types.
 */
@RunWith(Parameterized.class)
public class SingleFieldAggregationBaseIT<F> {

	private static final String AGGREGATION_NAME = "aggregationName";

	private static Set<FieldTypeDescriptor<?>> supportedFieldTypes;
	private static List<DataSet<?>> dataSets;

	@Parameterized.Parameters(name = "{0}")
	public static Object[][] parameters() {
		supportedFieldTypes = new LinkedHashSet<>();
		dataSets = new ArrayList<>();
		List<Object[]> parameters = new ArrayList<>();
		for ( AggregationDescriptor aggregationDescriptor : AggregationDescriptor.getAll() ) {
			for ( FieldTypeDescriptor<?> fieldTypeDescriptor : FieldTypeDescriptor.getAll() ) {
				Optional<? extends SupportedSingleFieldAggregationExpectations<?>> expectations =
						aggregationDescriptor.getSingleFieldAggregationExpectations( fieldTypeDescriptor ).getSupported();
				if ( expectations.isPresent() ) {
					supportedFieldTypes.add( fieldTypeDescriptor );
					DataSet<?> dataSet = new DataSet<>( expectations.get() );
					dataSets.add( dataSet );
					parameters.add( new Object[] { expectations.get(), dataSet } );
				}
			}
		}
		return parameters.toArray( new Object[0][] );
	}

	@ClassRule
	public static final SearchSetupHelper setupHelper = new SearchSetupHelper();

	private static final SimpleMappedIndex<IndexBinding> mainIndex =
			SimpleMappedIndex.of( "Main", IndexBinding::new );
	private static final SimpleMappedIndex<IndexBinding> compatibleIndex =
			SimpleMappedIndex.of( "Compatible", IndexBinding::new );
	private static final SimpleMappedIndex<RawFieldCompatibleIndexBinding> rawFieldCompatibleIndex =
			SimpleMappedIndex.of( "RawFieldCompatible", RawFieldCompatibleIndexBinding::new );
	private static final SimpleMappedIndex<IncompatibleIndexBinding> incompatibleIndex =
			SimpleMappedIndex.of( "Incompatible", IncompatibleIndexBinding::new );
	private static final SimpleMappedIndex<IndexBinding> emptyIndex =
			SimpleMappedIndex.of( "Empty", IndexBinding::new );
	private static final SimpleMappedIndex<IndexBinding> nullOnlyIndex =
			SimpleMappedIndex.of( "NullOnly", IndexBinding::new );
	private static final SimpleMappedIndex<MultiValuedIndexBinding> multiValuedIndex =
			SimpleMappedIndex.of( "MultiValued", MultiValuedIndexBinding::new );

	@BeforeClass
	public static void setup() {
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

		for ( DataSet<?> dataSet : dataSets ) {
			dataSet.init();
		}
	}

	private final SupportedSingleFieldAggregationExpectations<F> expectations;
	private final FieldTypeDescriptor<F> fieldType;
	private final DataSet<F> dataSet;

	public SingleFieldAggregationBaseIT(SupportedSingleFieldAggregationExpectations<F> expectations,
			DataSet<F> dataSet) {
		this.expectations = expectations;
		this.fieldType = expectations.fieldType();
		this.dataSet = dataSet;
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
		doTest_simple( expectations.simple() );
	}

	private <A> void doTest_simple(AggregationScenario<A> scenario) {
		StubMappingScope scope = mainIndex.createScope();
		String fieldPath = mainIndex.binding().fieldModels.get( fieldType ).relativeFieldName;
		AggregationKey<A> aggregationKey = AggregationKey.of( AGGREGATION_NAME );

		SearchResultAssert.assertThat(
				scope.query()
						.where( f -> f.matchAll() )
						.aggregation( aggregationKey, f -> scenario.setup( f, fieldPath ) )
						.routing( dataSet.name )
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
		doTest_aggregationObject( expectations.simple() );
	}

	private <A> void doTest_aggregationObject(AggregationScenario<A> scenario) {
		StubMappingScope scope = mainIndex.createScope();
		String fieldPath = mainIndex.binding().fieldModels.get( fieldType ).relativeFieldName;
		AggregationKey<A> aggregationKey = AggregationKey.of( AGGREGATION_NAME );

		SearchAggregation<A> aggregation = scenario.setup( scope.aggregation(), fieldPath )
				.toAggregation();

		SearchResultAssert.assertThat(
				mainIndex.createScope().query()
						.where( f -> f.matchAll() )
						.aggregation( aggregationKey, aggregation )
						.routing( dataSet.name )
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
		doTest_aggregationObject_reuse_onScopeTargetingSameIndexes( expectations.simple() );
	}

	private <A> void doTest_aggregationObject_reuse_onScopeTargetingSameIndexes(AggregationScenario<A> scenario) {
		StubMappingScope scope = mainIndex.createScope();
		String fieldPath = mainIndex.binding().fieldModels.get( fieldType ).relativeFieldName;
		AggregationKey<A> aggregationKey = AggregationKey.of( AGGREGATION_NAME );

		SearchAggregation<A> aggregation = scenario.setup( scope.aggregation(), fieldPath )
				.toAggregation();

		SearchResultAssert.assertThat(
				scope.query()
						.where( f -> f.matchAll() )
						.aggregation( aggregationKey, aggregation )
						.routing( dataSet.name )
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
						.routing( dataSet.name )
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
						.routing( dataSet.name )
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
		doTest_aggregationObject_reuse_onScopeTargetingDifferentIndexes( expectations.simple() );
	}

	private <A> void doTest_aggregationObject_reuse_onScopeTargetingDifferentIndexes(AggregationScenario<A> scenario) {
		StubMappingScope scope = mainIndex.createScope();
		String fieldPath = mainIndex.binding().fieldModels.get( fieldType ).relativeFieldName;
		AggregationKey<A> aggregationKey = AggregationKey.of( AGGREGATION_NAME );

		SearchAggregation<A> aggregation = scenario.setup( scope.aggregation(), fieldPath )
				.toAggregation();

		// reuse the aggregation instance on a different scope targeting a different index
		Assertions.assertThatThrownBy( () ->
				compatibleIndex.createScope().query()
						.where( f -> f.matchAll() )
						.aggregation( aggregationKey, aggregation )
						.routing( dataSet.name )
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
						.routing( dataSet.name )
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
		String fieldPath = mainIndex.binding().fieldModels.get( fieldType ).relativeFieldName;

		AggregationScenario<?> scenario = expectations.withoutMatch();
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
		String fieldPath = mainIndex.binding().fieldModels.get( fieldType ).relativeFieldName;

		AggregationScenario<?> scenario = expectations.withoutMatch();
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
		String fieldPath = nullOnlyIndex.binding().fieldModels.get( fieldType ).relativeFieldName;

		AggregationScenario<?> scenario = expectations.withoutMatch();
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
		String fieldPath = multiValuedIndex.binding().fieldModels.get( fieldType ).relativeFieldName;

		AggregationScenario<?> scenario = expectations.onMultiValuedIndex();
		testValidAggregation(
				scenario, multiValuedIndex.createScope(), fieldPath
		);
	}

	@Test
	@PortedFromSearch5(original = "org.hibernate.search.test.query.facet.SimpleFacetingTest.testNullFieldNameThrowsException")
	public void nullFieldPath() {
		// Try to pass a "null" field type
		AggregationScenario<?> scenario = expectations.simple();

		Assertions.assertThatThrownBy( () -> scenario.setup( mainIndex.createScope().aggregation(), null ) )
				.isInstanceOf( IllegalArgumentException.class )
				.hasMessageContaining( "'absoluteFieldPath'" )
				.hasMessageContaining( "must not be null" );
	}

	@Test
	public void nullFieldType() {
		String fieldPath = mainIndex.binding().fieldModels.get( fieldType ).relativeFieldName;

		// Try to pass a "null" field type
		AggregationScenario<?> scenario = expectations.withFieldType( TypeAssertionHelper.nullType() );

		Assertions.assertThatThrownBy( () -> scenario.setup( mainIndex.createScope().aggregation(), fieldPath ) )
				.isInstanceOf( IllegalArgumentException.class )
				.hasMessageContaining( "'type'" )
				.hasMessageContaining( "must not be null" );
	}

	@Test
	public void invalidFieldType_conversionEnabled() {
		String fieldPath = mainIndex.binding().fieldModels.get( fieldType ).relativeFieldName;

		AggregationScenario<?> scenario = expectations.withFieldType( TypeAssertionHelper.wrongType( fieldType ) );

		Assertions.assertThatThrownBy( () -> scenario.setup( mainIndex.createScope().aggregation(), fieldPath ) )
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "Invalid type" )
				.hasMessageContaining( "for aggregation on field" )
				.hasMessageContaining( "'" + fieldPath + "'" );
	}

	@Test
	public void invalidFieldType_conversionDisabled() {
		String fieldPath = mainIndex.binding().fieldModels.get( fieldType ).relativeFieldName;

		AggregationScenario<?> scenario = expectations.withFieldType( TypeAssertionHelper.wrongType( fieldType ) );

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

		AggregationScenario<?> scenario = expectations.simple();

		Assertions.assertThatThrownBy( () -> scenario.setup( mainIndex.createScope().aggregation(), fieldPath ) )
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "Unknown field" )
				.hasMessageContaining( "'" + fieldPath + "'" )
				.hasMessageContaining( mainIndex.name() );
	}

	@Test
	public void objectField_nested() {
		String fieldPath = mainIndex.binding().nestedObject.relativeFieldName;

		AggregationScenario<?> scenario = expectations.withFieldType( TypeAssertionHelper.identity( fieldType ) );

		Assertions.assertThatThrownBy( () -> scenario.setup( mainIndex.createScope().aggregation(), fieldPath ) )
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "Unknown field" )
				.hasMessageContaining( "'" + fieldPath + "'" )
				.hasMessageContaining( mainIndex.name() );
	}

	@Test
	public void objectField_flattened() {
		String fieldPath = mainIndex.binding().flattenedObject.relativeFieldName;

		AggregationScenario<?> scenario = expectations.withFieldType( TypeAssertionHelper.identity( fieldType ) );

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
		String fieldPath = mainIndex.binding().fieldWithAggregationDisabledModels.get( fieldType ).relativeFieldName;

		AggregationScenario<?> scenario = expectations.withFieldType( TypeAssertionHelper.identity( fieldType ) );

		Assertions.assertThatThrownBy( () -> scenario.setup( mainIndex.createScope().aggregation(), fieldPath ) )
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "Aggregations are not enabled for field" )
				.hasMessageContaining( "'" + fieldPath + "'" );
	}

	@Test
	public void withConverter_conversionEnabled() {
		String fieldPath = mainIndex.binding().fieldWithConverterModels.get( fieldType ).relativeFieldName;

		AggregationScenario<?> scenario = expectations.withFieldType(
				TypeAssertionHelper.wrapper( fieldType )
		);
		testValidAggregation(
				scenario, mainIndex.createScope(), fieldPath
		);
	}

	@Test
	public void withConverter_conversionDisabled() {
		String fieldPath = mainIndex.binding().fieldModels.get( fieldType ).relativeFieldName;

		AggregationScenario<?> scenario = expectations.simple();
		testValidAggregationWithConverterSetting(
				scenario, mainIndex.createScope(), fieldPath, ValueConvert.NO
		);
	}

	@Test
	public void withConverter_invalidFieldType() {
		String fieldPath = mainIndex.binding().fieldWithConverterModels.get( fieldType ).relativeFieldName;

		AggregationScenario<?> scenario = expectations.simple();

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
		String fieldPath = mainIndex.binding().fieldModels.get( fieldType ).relativeFieldName;

		AggregationScenario<?> scenario = expectations.simple();

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
						.routing( dataSet.name )
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
		String fieldPath = mainIndex.binding().fieldModels.get( fieldType ).relativeFieldName;

		AggregationScenario<?> scenario = expectations.simple();

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
		String fieldPath = mainIndex.binding().fieldModels.get( fieldType ).relativeFieldName;

		AggregationScenario<?> scenario = expectations.simple();
		testValidAggregation(
				scenario, mainIndex.createScope(), fieldPath
		);
	}

	@Test
	public void multiIndex_withCompatibleIndex_noConverter() {
		StubMappingScope scope = mainIndex.createScope( compatibleIndex );

		String fieldPath = mainIndex.binding().fieldModels.get( fieldType ).relativeFieldName;

		AggregationScenario<?> scenario = expectations.onMainAndOtherIndex();
		testValidAggregation(
				scenario, scope, fieldPath
		);
	}

	@Test
	public void multiIndex_withCompatibleIndex_conversionEnabled() {
		StubMappingScope scope = mainIndex.createScope( compatibleIndex );

		String fieldPath = mainIndex.binding().fieldWithConverterModels.get( fieldType ).relativeFieldName;

		AggregationScenario<?> scenario = expectations.withFieldTypeOnMainAndOtherIndex(
				TypeAssertionHelper.wrapper( fieldType )
		);
		testValidAggregation(
				scenario, scope, fieldPath
		);
	}

	@Test
	public void multiIndex_withRawFieldCompatibleIndex_conversionEnabled() {
		StubMappingScope scope = mainIndex.createScope( rawFieldCompatibleIndex );

		String fieldPath = mainIndex.binding().fieldWithConverterModels.get( fieldType ).relativeFieldName;

		AggregationScenario<?> scenario = expectations.withFieldTypeOnMainAndOtherIndex(
				TypeAssertionHelper.wrapper( fieldType )
		);

		Assertions.assertThatThrownBy( () -> scenario.setup( scope.aggregation(), fieldPath ) )
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "Multiple conflicting types to build an aggregation" )
				.hasMessageContaining( "'" + fieldPath + "'" );
	}

	@Test
	public void multiIndex_withRawFieldCompatibleIndex_conversionDisabled() {
		StubMappingScope scope = mainIndex.createScope( rawFieldCompatibleIndex );

		String fieldPath = mainIndex.binding().fieldWithConverterModels.get( fieldType ).relativeFieldName;

		AggregationScenario<?> scenario = expectations.onMainAndOtherIndex();
		testValidAggregationWithConverterSetting(
				scenario, scope, fieldPath, ValueConvert.NO
		);
	}

	@Test
	public void multiIndex_withIncompatibleIndex_conversionEnabled() {
		StubMappingScope scope = mainIndex.createScope( incompatibleIndex );

		String fieldPath = mainIndex.binding().fieldModels.get( fieldType ).relativeFieldName;

		AggregationScenario<?> scenario = expectations.simple();

		Assertions.assertThatThrownBy( () -> scenario.setup( scope.aggregation(), fieldPath ) )
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "Multiple conflicting types to build an aggregation" )
				.hasMessageContaining( "'" + fieldPath + "'" );
	}

	@Test
	public void multiIndex_withIncompatibleIndex_conversionDisabled() {
		StubMappingScope scope = mainIndex.createScope( incompatibleIndex );

		String fieldPath = mainIndex.binding().fieldModels.get( fieldType ).relativeFieldName;

		AggregationScenario<?> scenario = expectations.simple();

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
						.routing( dataSet.name )
						.toQuery()
		)
				.aggregation(
						aggregationKey,
						a -> assertThat( a ).isNotNull().satisfies( scenario::check )
				);
	}

	private static class DataSet<F> {
		final SupportedSingleFieldAggregationExpectations<F> expectations;
		final FieldTypeDescriptor<F> fieldType;
		final String name;

		private DataSet(SupportedSingleFieldAggregationExpectations<F> expectations) {
			this.expectations = expectations;
			this.fieldType = expectations.fieldType();
			this.name = expectations.aggregationName() + "_" + expectations.fieldType().getUniqueName();
		}

		private void init() {
			FieldTypeDescriptor<F> fieldType = expectations.fieldType();

			List<F> mainIndexDocumentFieldValues = expectations.getMainIndexDocumentFieldValues();
			List<F> otherIndexDocumentFieldValues = expectations.getOtherIndexDocumentFieldValues();
			List<List<F>> multiValuedIndexDocumentFieldValues = expectations.getMultiValuedIndexDocumentFieldValues();

			IndexIndexingPlan<?> plan = mainIndex.createIndexingPlan();
			for ( int i = 0; i < mainIndexDocumentFieldValues.size(); i++ ) {
				F value = mainIndexDocumentFieldValues.get( i );
				plan.add( referenceProvider( name + "_document_" + i, name ), document -> {
					document.addValue( mainIndex.binding().fieldModels.get( fieldType ).reference, value );
					document.addValue( mainIndex.binding().fieldWithConverterModels.get( fieldType ).reference, value );

					// Note: this object must be single-valued for these tests
					DocumentElement flattenedObject = document.addObject( mainIndex.binding().flattenedObject.self );
					flattenedObject.addValue( mainIndex.binding().flattenedObject.fieldModels.get( fieldType ).reference, value );

					// Note: this object must be single-valued for these tests
					DocumentElement nestedObject = document.addObject( mainIndex.binding().nestedObject.self );
					nestedObject.addValue( mainIndex.binding().nestedObject.fieldModels.get( fieldType ).reference, value );
				} );
			}
			plan.add( referenceProvider( name + "_document_empty", name ), document -> { } );
			plan.execute().join();

			plan = compatibleIndex.createIndexingPlan();
			for ( int i = 0; i < otherIndexDocumentFieldValues.size(); i++ ) {
				F value = otherIndexDocumentFieldValues.get( i );
				plan.add( referenceProvider( name + "_compatibleindex_document_" + i, name ), document -> {
					document.addValue( compatibleIndex.binding().fieldModels.get( fieldType ).reference, value );
					document.addValue( compatibleIndex.binding().fieldWithConverterModels.get( fieldType ).reference, value );
				} );
			}
			plan.execute().join();

			plan = rawFieldCompatibleIndex.createIndexingPlan();
			for ( int i = 0; i < otherIndexDocumentFieldValues.size(); i++ ) {
				F value = otherIndexDocumentFieldValues.get( i );
				plan.add( referenceProvider( name + "_rawcompatibleindex_document_" + i, name ), document -> {
					document.addValue( rawFieldCompatibleIndex.binding().fieldWithConverterModels.get( fieldType ).reference, value );
				} );
			}
			plan.execute().join();

			plan = nullOnlyIndex.createIndexingPlan();
			plan.add( referenceProvider( name + "_nullOnlyIndex_document_0", name ), document -> {
				document.addValue( nullOnlyIndex.binding().fieldModels.get( fieldType ).reference, null );
			} );
			plan.execute().join();

			plan = multiValuedIndex.createIndexingPlan();
			for ( int i = 0; i < multiValuedIndexDocumentFieldValues.size(); i++ ) {
				List<F> values = multiValuedIndexDocumentFieldValues.get( i );
				plan.add( referenceProvider( name + "_document_" + i, name ), document -> {
					for ( F value : values ) {
						document.addValue( multiValuedIndex.binding().fieldModels.get( fieldType ).reference, value );
					}
				} );
			}
			plan.add( referenceProvider( name + "_document_empty", name ), document -> { } );
			plan.execute().join();

			// Check that all documents are searchable
			SearchResultAssert.assertThat( mainIndex.createScope().query()
					.where( f -> f.matchAll() )
					.routing( name )
					.toQuery() )
					.hasTotalHitCount( mainIndexDocumentFieldValues.size() + 1 /* +1 for the empty document */ );
			SearchResultAssert.assertThat( compatibleIndex.createScope().query()
					.where( f -> f.matchAll() )
					.routing( name )
					.toQuery() )
					.hasTotalHitCount( otherIndexDocumentFieldValues.size() );
			SearchResultAssert.assertThat( rawFieldCompatibleIndex.createScope().query()
					.where( f -> f.matchAll() )
					.routing( name )
					.toQuery() )
					.hasTotalHitCount( otherIndexDocumentFieldValues.size() );
			SearchResultAssert.assertThat( nullOnlyIndex.createScope().query()
					.where( f -> f.matchAll() )
					.routing( name )
					.toQuery() )
					.hasTotalHitCount( 1 );
			SearchResultAssert.assertThat( multiValuedIndex.createScope().query()
					.where( f -> f.matchAll() )
					.routing( name )
					.toQuery() )
					.hasTotalHitCount( multiValuedIndexDocumentFieldValues.size() + 1 /* +1 for the empty document */ );
		}
	}

	private static class IndexBinding {
		final SimpleFieldModelsByType fieldModels;
		final SimpleFieldModelsByType fieldWithConverterModels;
		final SimpleFieldModelsByType fieldWithAggregationDisabledModels;

		final ObjectBinding flattenedObject;
		final ObjectBinding nestedObject;

		IndexBinding(IndexSchemaElement root) {
			fieldModels = SimpleFieldModelsByType.mapAll( supportedFieldTypes, root,
					"", c -> c.aggregable( Aggregable.YES ) );
			fieldWithConverterModels = SimpleFieldModelsByType.mapAll( supportedFieldTypes, root,
					"converted_", c -> c.aggregable( Aggregable.YES )
							.dslConverter( ValueWrapper.class, ValueWrapper.toIndexFieldConverter() )
							.projectionConverter( ValueWrapper.class, ValueWrapper.fromIndexFieldConverter() ) );
			fieldWithAggregationDisabledModels = SimpleFieldModelsByType.mapAll( supportedFieldTypes, root,
					"nonAggregable_", c -> c.aggregable( Aggregable.NO ) );

			flattenedObject = new ObjectBinding( root, "flattenedObject", ObjectFieldStorage.FLATTENED );
			nestedObject = new ObjectBinding( root, "nestedObject", ObjectFieldStorage.NESTED );
		}
	}

	private static class ObjectBinding {
		final String relativeFieldName;
		final IndexObjectFieldReference self;
		final SimpleFieldModelsByType fieldModels;

		ObjectBinding(IndexSchemaElement parent, String relativeFieldName, ObjectFieldStorage storage) {
			this.relativeFieldName = relativeFieldName;
			IndexSchemaObjectField objectField = parent.objectField( relativeFieldName, storage );
			self = objectField.toReference();
			fieldModels = SimpleFieldModelsByType.mapAll( supportedFieldTypes, objectField, "" );
		}
	}

	private static class RawFieldCompatibleIndexBinding {
		final SimpleFieldModelsByType fieldWithConverterModels;

		RawFieldCompatibleIndexBinding(IndexSchemaElement root) {
			/*
			 * Add a field with the same name as the fieldWithConverterModel from IndexMapping,
			 * but with an incompatible projection converter.
			 */
			fieldWithConverterModels = SimpleFieldModelsByType.mapAll( supportedFieldTypes, root,
					"converted_", c -> c.aggregable( Aggregable.YES )
							.dslConverter( ValueWrapper.class, ValueWrapper.toIndexFieldConverter() )
							.projectionConverter( ValueWrapper.class, new IncompatibleProjectionConverter() ) );
		}

		@SuppressWarnings("rawtypes")
		private static class IncompatibleProjectionConverter
				implements FromDocumentFieldValueConverter<Object, ValueWrapper> {
			@Override
			public ValueWrapper convert(Object value, FromDocumentFieldValueConvertContext context) {
				return null;
			}
		}
	}

	private static class IncompatibleIndexBinding {
		IncompatibleIndexBinding(IndexSchemaElement root) {
			/*
			 * Add fields with the same name as the fieldsModels from IndexMapping,
			 * but with an incompatible type.
			 */
			mapFieldsWithIncompatibleType( root );
		}

		private static void mapFieldsWithIncompatibleType(IndexSchemaElement parent) {
			supportedFieldTypes.forEach( typeDescriptor ->
					IncompatibleFieldModel.mapper( FieldTypeDescriptor.getIncompatible( typeDescriptor )::configure )
							.map( parent, "" + typeDescriptor.getUniqueName() )
			);
		}
	}

	private static class MultiValuedIndexBinding {
		final SimpleFieldModelsByType fieldModels;

		MultiValuedIndexBinding(IndexSchemaElement root) {
			fieldModels = SimpleFieldModelsByType.mapAllMultiValued( supportedFieldTypes, root,
					"", c -> c.aggregable( Aggregable.YES ) );
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
