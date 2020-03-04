/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.search.aggregation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMapperUtils.referenceProvider;
import static org.junit.Assume.assumeTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.document.IndexFieldReference;
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
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.StandardFieldMapper;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.TckConfiguration;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.TypeAssertionHelper;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.ValueWrapper;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.common.assertion.SearchResultAssert;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappingIndexManager;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappingScope;
import org.hibernate.search.util.impl.test.SubTest;
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

	private static final String INDEX_NAME = "IndexName";
	private static final String COMPATIBLE_INDEX_NAME = "IndexWithCompatibleFields";
	private static final String RAW_FIELD_COMPATIBLE_INDEX_NAME = "IndexWithCompatibleRawFields";
	private static final String INCOMPATIBLE_INDEX_NAME = "IndexWithIncompatibleFields";
	private static final String EMPTY_INDEX_NAME = "EmptyIndexName";
	private static final String NULL_ONLY_INDEX_NAME = "NullOnlyIndexName";
	private static final String MULTI_VALUED_INDEX_NAME = "MultiValuedIndexName";

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

	private IndexMapping indexMapping;
	private StubMappingIndexManager indexManager;

	private IndexMapping compatibleIndexMapping;
	private StubMappingIndexManager compatibleIndexManager;

	private RawFieldCompatibleIndexMapping rawFieldCompatibleIndexMapping;
	private StubMappingIndexManager rawFieldCompatibleIndexManager;

	private StubMappingIndexManager incompatibleIndexManager;

	private StubMappingIndexManager emptyIndexManager;

	private IndexMapping nullOnlyIndexMapping;
	private StubMappingIndexManager nullOnlyIndexManager;

	private MultiValuedIndexMapping multiValuedIndexMapping;
	private StubMappingIndexManager multiValuedIndexManager;

	public SingleFieldAggregationBaseIT(AggregationDescriptor thisIsJustForTestName,
			FieldTypeDescriptor<F> typeDescriptor,
			SupportedSingleFieldAggregationExpectations<F> expectations) {
		this.typeDescriptor = typeDescriptor;
		this.expectations = expectations;
	}

	@BeforeAll
	public void setup() {
		SearchSetupHelper.SetupContext setupContext = setupHelper.start()
				.withIndex(
						INDEX_NAME,
						ctx -> this.indexMapping = new IndexMapping( ctx.getSchemaElement() ),
						indexManager -> this.indexManager = indexManager
				)
				.withIndex(
						COMPATIBLE_INDEX_NAME,
						ctx -> this.compatibleIndexMapping = new IndexMapping( ctx.getSchemaElement() ),
						indexManager -> this.compatibleIndexManager = indexManager
				)
				.withIndex(
						RAW_FIELD_COMPATIBLE_INDEX_NAME,
						ctx -> this.rawFieldCompatibleIndexMapping = new RawFieldCompatibleIndexMapping( ctx.getSchemaElement() ),
						indexManager -> this.rawFieldCompatibleIndexManager = indexManager
				)
				.withIndex(
						INCOMPATIBLE_INDEX_NAME,
						ctx -> new IncompatibleIndexMapping( ctx.getSchemaElement() ),
						indexManager -> this.incompatibleIndexManager = indexManager
				)
				.withIndex(
						EMPTY_INDEX_NAME,
						ctx -> new IndexMapping( ctx.getSchemaElement() ),
						indexManager -> this.emptyIndexManager = indexManager
				)
				.withIndex(
						NULL_ONLY_INDEX_NAME,
						ctx -> this.nullOnlyIndexMapping = new IndexMapping( ctx.getSchemaElement() ),
						indexManager -> this.nullOnlyIndexManager = indexManager
				);

		if ( TckConfiguration.get().getBackendFeatures().aggregationsOnMultiValuedFields( typeDescriptor.getJavaType() ) ) {
			setupContext
					.withIndex(
							MULTI_VALUED_INDEX_NAME,
							ctx -> this.multiValuedIndexMapping = new MultiValuedIndexMapping( ctx.getSchemaElement() ),
							indexManager -> this.multiValuedIndexManager = indexManager
					);
		}

		setupContext.setup();

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
		StubMappingScope scope = indexManager.createScope();
		String fieldPath = indexMapping.fieldModel.relativeFieldName;
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
		StubMappingScope scope = indexManager.createScope();
		String fieldPath = indexMapping.fieldModel.relativeFieldName;
		AggregationKey<A> aggregationKey = AggregationKey.of( AGGREGATION_NAME );

		SearchAggregation<A> aggregation = scenario.setup( scope.aggregation(), fieldPath )
				.toAggregation();

		SearchResultAssert.assertThat(
				indexManager.createScope().query()
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
		StubMappingScope scope = indexManager.createScope();
		String fieldPath = indexMapping.fieldModel.relativeFieldName;
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
		scope = indexManager.createScope();
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
		StubMappingScope scope = indexManager.createScope();
		String fieldPath = indexMapping.fieldModel.relativeFieldName;
		AggregationKey<A> aggregationKey = AggregationKey.of( AGGREGATION_NAME );

		SearchAggregation<A> aggregation = scenario.setup( scope.aggregation(), fieldPath )
				.toAggregation();

		// reuse the aggregation instance on a different scope targeting a different index
		SubTest.expectException( () ->
				compatibleIndexManager.createScope().query()
						.where( f -> f.matchAll() )
						.aggregation( aggregationKey, aggregation )
						.toQuery()
		)
				.assertThrown()
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "scope targeting different indexes" )
				.hasMessageContaining( INDEX_NAME )
				.hasMessageContaining( COMPATIBLE_INDEX_NAME );

		// reuse the aggregation instance on a different scope targeting a superset of the original indexes
		SubTest.expectException( () ->
				indexManager.createScope( compatibleIndexManager ).query()
						.where( f -> f.matchAll() )
						.aggregation( aggregationKey, aggregation )
						.toQuery()
		)
				.assertThrown()
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "scope targeting different indexes" )
				.hasMessageContaining( INDEX_NAME )
				.hasMessageContaining( COMPATIBLE_INDEX_NAME );
	}

	@Test
	@TestForIssue(jiraKey = { "HSEARCH-1968" })
	@PortedFromSearch5(original = "org.hibernate.search.test.query.facet.NoQueryResultsFacetingTest")
	public void noMatch() {
		String fieldPath = indexMapping.fieldModel.relativeFieldName;

		AggregationScenario<?> scenario = expectations.withoutMatch( typeDescriptor );
		testValidAggregation(
				scenario, indexManager.createScope(),
				f -> f.id().matching( "none" ), // Don't match any document
				(f, e) -> e.setup( f, fieldPath )
		);
	}

	/**
	 * Test behavior when aggregating on an index with no value for the targeted field
	 * because there is no document in the index.
	 */
	@Test
	@TestForIssue(jiraKey = { "HSEARCH-2955", "HSEARCH-745" })
	@PortedFromSearch5(original = {
			"org.hibernate.search.test.facet.NoIndexedValueFacetingTest",
			"org.hibernate.search.test.query.facet.EdgeCaseFacetTest"
	})
	public void emptyIndex() {
		String fieldPath = indexMapping.fieldModel.relativeFieldName;

		AggregationScenario<?> scenario = expectations.withoutMatch( typeDescriptor );
		testValidAggregation(
				scenario, emptyIndexManager.createScope(), fieldPath
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
		String fieldPath = nullOnlyIndexMapping.fieldModel.relativeFieldName;

		AggregationScenario<?> scenario = expectations.withoutMatch( typeDescriptor );
		testValidAggregation(
				scenario, nullOnlyIndexManager.createScope(), fieldPath
		);
	}

	/**
	 * Test behavior when aggregating on an multi-valued field.
	 */
	@Test
	@TestForIssue(jiraKey = {"HSEARCH-726", "HSEARCH-900", "HSEARCH-2535"})
	@PortedFromSearch5(original = {
			"org.hibernate.search.test.query.facet.EmbeddedCollectionFacetingTest",
			"org.hibernate.search.test.query.facet.ManyToOneFacetingTest",
			"org.hibernate.search.test.query.facet.MultiValuedFacetingTest"
	})
	public void multiValued() {
		assumeTrue(
				"Aggregations on multi-valued fields are not supported with this backend",
				TckConfiguration.get().getBackendFeatures().aggregationsOnMultiValuedFields( typeDescriptor.getJavaType() )
		);

		String fieldPath = multiValuedIndexMapping.fieldModel.relativeFieldName;

		AggregationScenario<?> scenario = expectations.onMultiValuedIndex( typeDescriptor );
		testValidAggregation(
				scenario, multiValuedIndexManager.createScope(), fieldPath
		);
	}

	@Test
	@PortedFromSearch5(original = "org.hibernate.search.test.query.facet.SimpleFacetingTest.testNullFieldNameThrowsException")
	public void nullFieldPath() {
		// Try to pass a "null" field type
		AggregationScenario<?> scenario = expectations.simple( typeDescriptor );

		SubTest.expectException( () -> scenario.setup( indexManager.createScope().aggregation(), null ) )
				.assertThrown()
				.isInstanceOf( IllegalArgumentException.class )
				.hasMessageContaining( "'absoluteFieldPath'" )
				.hasMessageContaining( "must not be null" );
	}

	@Test
	public void nullFieldType() {
		String fieldPath = indexMapping.fieldModel.relativeFieldName;

		// Try to pass a "null" field type
		AggregationScenario<?> scenario = expectations.withFieldType( TypeAssertionHelper.nullType() );

		SubTest.expectException( () -> scenario.setup( indexManager.createScope().aggregation(), fieldPath ) )
				.assertThrown()
				.isInstanceOf( IllegalArgumentException.class )
				.hasMessageContaining( "'type'" )
				.hasMessageContaining( "must not be null" );
	}

	@Test
	public void invalidFieldType_conversionEnabled() {
		String fieldPath = indexMapping.fieldModel.relativeFieldName;

		AggregationScenario<?> scenario = expectations.withFieldType( TypeAssertionHelper.wrongType( typeDescriptor ) );

		SubTest.expectException( () -> scenario.setup( indexManager.createScope().aggregation(), fieldPath ) )
				.assertThrown()
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "Invalid type" )
				.hasMessageContaining( "for aggregation on field" )
				.hasMessageContaining( "'" + fieldPath + "'" );
	}

	@Test
	public void invalidFieldType_conversionDisabled() {
		String fieldPath = indexMapping.fieldModel.relativeFieldName;

		AggregationScenario<?> scenario = expectations.withFieldType( TypeAssertionHelper.wrongType( typeDescriptor ) );

		SubTest.expectException( () -> scenario.setupWithConverterSetting(
				indexManager.createScope().aggregation(), fieldPath, ValueConvert.NO
		) )
				.assertThrown()
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

		SubTest.expectException( () -> scenario.setup( indexManager.createScope().aggregation(), fieldPath ) )
				.assertThrown()
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "Unknown field" )
				.hasMessageContaining( "'" + fieldPath + "'" )
				.hasMessageContaining( INDEX_NAME );
	}

	@Test
	public void objectField_nested() {
		String fieldPath = indexMapping.nestedObject.relativeFieldName;

		AggregationScenario<?> scenario = expectations.withFieldType( TypeAssertionHelper.identity( typeDescriptor ) );

		SubTest.expectException( () -> scenario.setup( indexManager.createScope().aggregation(), fieldPath ) )
				.assertThrown()
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "Unknown field" )
				.hasMessageContaining( "'" + fieldPath + "'" )
				.hasMessageContaining( INDEX_NAME );
	}

	@Test
	public void objectField_flattened() {
		String fieldPath = indexMapping.flattenedObject.relativeFieldName;

		AggregationScenario<?> scenario = expectations.withFieldType( TypeAssertionHelper.identity( typeDescriptor ) );

		SubTest.expectException( () -> scenario.setup( indexManager.createScope().aggregation(), fieldPath ) )
				.assertThrown()
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "Unknown field" )
				.hasMessageContaining( "'" + fieldPath + "'" )
				.hasMessageContaining( INDEX_NAME );
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-1748")
	@PortedFromSearch5(original = "org.hibernate.search.test.query.facet.FacetUnknownFieldFailureTest.testKnownFieldNameNotConfiguredForFacetingThrowsException")
	public void aggregationsDisabled() {
		String fieldPath = indexMapping.fieldWithAggregationDisabledModel.relativeFieldName;

		AggregationScenario<?> scenario = expectations.withFieldType( TypeAssertionHelper.identity( typeDescriptor ) );

		SubTest.expectException( () -> scenario.setup( indexManager.createScope().aggregation(), fieldPath ) )
				.assertThrown()
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "Aggregations are not enabled for field" )
				.hasMessageContaining( "'" + fieldPath + "'" );
	}

	@Test
	public void withConverter_conversionEnabled() {
		String fieldPath = indexMapping.fieldWithConverterModel.relativeFieldName;

		AggregationScenario<?> scenario = expectations.withFieldType(
				TypeAssertionHelper.wrapper( typeDescriptor )
		);
		testValidAggregation(
				scenario, indexManager.createScope(), fieldPath
		);
	}

	@Test
	public void withConverter_conversionDisabled() {
		String fieldPath = indexMapping.fieldModel.relativeFieldName;

		AggregationScenario<?> scenario = expectations.simple( typeDescriptor );
		testValidAggregationWithConverterSetting(
				scenario, indexManager.createScope(), fieldPath, ValueConvert.NO
		);
	}

	@Test
	public void withConverter_invalidFieldType() {
		String fieldPath = indexMapping.fieldWithConverterModel.relativeFieldName;

		AggregationScenario<?> scenario = expectations.simple( typeDescriptor );

		SubTest.expectException( () -> scenario.setup( indexManager.createScope().aggregation(), fieldPath ) )
				.assertThrown()
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
		String fieldPath = indexMapping.fieldModel.relativeFieldName;

		AggregationScenario<?> scenario = expectations.simple( typeDescriptor );

		// A separate method is needed in order to write type-safe code
		doTestDuplicatedDifferentKeys( fieldPath, scenario );
	}

	private <A> void doTestDuplicatedDifferentKeys(String fieldPath, AggregationScenario<A> scenario) {
		AggregationKey<A> key1 = AggregationKey.of( "aggregationName1" );
		AggregationKey<A> key2 = AggregationKey.of( "aggregationName2" );

		SearchResultAssert.assertThat(
				indexManager.createScope().query().where( f -> f.matchAll() )
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
		String fieldPath = indexMapping.fieldModel.relativeFieldName;

		AggregationScenario<?> scenario = expectations.simple( typeDescriptor );

		// A separate method is needed in order to write type-safe code
		doTestDuplicatedSameKey( fieldPath, scenario );
	}

	private <A> void doTestDuplicatedSameKey(String fieldPath, AggregationScenario<A> scenario) {
		AggregationKey<A> key1 = AggregationKey.of( "aggregationName1" );

		SubTest.expectException( () ->
				indexManager.createScope().query().where( f -> f.matchAll() )
						.aggregation( key1, f -> scenario.setup( f, fieldPath ) )
						.aggregation( key1, f -> scenario.setup( f, fieldPath ) )
		)
				.assertThrown()
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "Multiple aggregations with the same key: " )
				.hasMessageContaining( "'aggregationName1'" );
	}

	@Test
	public void inFlattenedObject() {
		String fieldPath = indexMapping.fieldModel.relativeFieldName;

		AggregationScenario<?> scenario = expectations.simple( typeDescriptor );
		testValidAggregation(
				scenario, indexManager.createScope(), fieldPath
		);
	}

	@Test
	public void multiIndex_withCompatibleIndexManager_noConverter() {
		StubMappingScope scope = indexManager.createScope( compatibleIndexManager );

		String fieldPath = indexMapping.fieldModel.relativeFieldName;

		AggregationScenario<?> scenario = expectations.onMainAndOtherIndex( typeDescriptor );
		testValidAggregation(
				scenario, scope, fieldPath
		);
	}

	@Test
	public void multiIndex_withCompatibleIndexManager_conversionEnabled() {
		StubMappingScope scope = indexManager.createScope( compatibleIndexManager );

		String fieldPath = indexMapping.fieldWithConverterModel.relativeFieldName;

		AggregationScenario<?> scenario = expectations.withFieldTypeOnMainAndOtherIndex(
				TypeAssertionHelper.wrapper( typeDescriptor )
		);
		testValidAggregation(
				scenario, scope, fieldPath
		);
	}

	@Test
	public void multiIndex_withRawFieldCompatibleIndexManager_conversionEnabled() {
		StubMappingScope scope = indexManager.createScope( rawFieldCompatibleIndexManager );

		String fieldPath = indexMapping.fieldWithConverterModel.relativeFieldName;

		AggregationScenario<?> scenario = expectations.withFieldTypeOnMainAndOtherIndex(
				TypeAssertionHelper.wrapper( typeDescriptor )
		);

		SubTest.expectException( () -> scenario.setup( scope.aggregation(), fieldPath ) )
				.assertThrown()
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "Multiple conflicting types to build an aggregation" )
				.hasMessageContaining( "'" + fieldPath + "'" );
	}

	@Test
	public void multiIndex_withRawFieldCompatibleIndexManager_conversionDisabled() {
		StubMappingScope scope = indexManager.createScope( rawFieldCompatibleIndexManager );

		String fieldPath = indexMapping.fieldWithConverterModel.relativeFieldName;

		AggregationScenario<?> scenario = expectations.onMainAndOtherIndex( typeDescriptor );
		testValidAggregationWithConverterSetting(
				scenario, scope, fieldPath, ValueConvert.NO
		);
	}

	@Test
	public void multiIndex_withIncompatibleIndexManager_conversionEnabled() {
		StubMappingScope scope = indexManager.createScope( incompatibleIndexManager );

		String fieldPath = indexMapping.fieldModel.relativeFieldName;

		AggregationScenario<?> scenario = expectations.simple( typeDescriptor );

		SubTest.expectException( () -> scenario.setup( scope.aggregation(), fieldPath ) )
				.assertThrown()
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "Multiple conflicting types to build an aggregation" )
				.hasMessageContaining( "'" + fieldPath + "'" );
	}

	@Test
	public void multiIndex_withIncompatibleIndexManager_conversionDisabled() {
		StubMappingScope scope = indexManager.createScope( incompatibleIndexManager );

		String fieldPath = indexMapping.fieldModel.relativeFieldName;

		AggregationScenario<?> scenario = expectations.simple( typeDescriptor );

		SubTest.expectException( () -> scenario.setupWithConverterSetting(
				scope.aggregation(), fieldPath, ValueConvert.NO
		) )
				.assertThrown()
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

		IndexIndexingPlan plan = indexManager.createIndexingPlan();
		for ( int i = 0; i < mainIndexDocumentFieldValues.size(); i++ ) {
			F value = mainIndexDocumentFieldValues.get( i );
			plan.add( referenceProvider( "document_" + i ), document -> {
				document.addValue( indexMapping.fieldModel.reference, value );
				document.addValue( indexMapping.fieldWithConverterModel.reference, value );

				// Note: this object must be single-valued for these tests
				DocumentElement flattenedObject = document.addObject( indexMapping.flattenedObject.self );
				flattenedObject.addValue( indexMapping.flattenedObject.fieldModel.reference, value );

				// Note: this object must be single-valued for these tests
				DocumentElement nestedObject = document.addObject( indexMapping.nestedObject.self );
				nestedObject.addValue( indexMapping.nestedObject.fieldModel.reference, value );
			} );
		}
		plan.add( referenceProvider( "document_empty" ), document -> { } );
		plan.execute().join();

		plan = compatibleIndexManager.createIndexingPlan();
		for ( int i = 0; i < otherIndexDocumentFieldValues.size(); i++ ) {
			F value = otherIndexDocumentFieldValues.get( i );
			plan.add( referenceProvider( "compatibleindex_document_" + i ), document -> {
				document.addValue( compatibleIndexMapping.fieldModel.reference, value );
				document.addValue( compatibleIndexMapping.fieldWithConverterModel.reference, value );
			} );
		}
		plan.execute().join();

		plan = rawFieldCompatibleIndexManager.createIndexingPlan();
		for ( int i = 0; i < otherIndexDocumentFieldValues.size(); i++ ) {
			F value = otherIndexDocumentFieldValues.get( i );
			plan.add( referenceProvider( "rawcompatibleindex_document_" + i ), document -> {
				document.addValue( rawFieldCompatibleIndexMapping.fieldWithConverterModel.reference, value );
			} );
		}
		plan.execute().join();

		plan = nullOnlyIndexManager.createIndexingPlan();
		plan.add( referenceProvider( "nullOnlyIndexManager_document_0" ), document -> {
			document.addValue( nullOnlyIndexMapping.fieldModel.reference, null );
		} );
		plan.execute().join();

		if ( TckConfiguration.get().getBackendFeatures().aggregationsOnMultiValuedFields( typeDescriptor.getJavaType() ) ) {
			plan = multiValuedIndexManager.createIndexingPlan();
			for ( int i = 0; i < multiValuedIndexDocumentFieldValues.size(); i++ ) {
				List<F> values = multiValuedIndexDocumentFieldValues.get( i );
				plan.add( referenceProvider( "document_" + i ), document -> {
					for ( F value : values ) {
						document.addValue( multiValuedIndexMapping.fieldModel.reference, value );
					}
				} );
			}
			plan.add( referenceProvider( "document_empty" ), document -> { } );
			plan.execute().join();
		}

		// Check that all documents are searchable
		SearchResultAssert.assertThat( indexManager.createScope().query()
				.where( f -> f.matchAll() )
				.toQuery() )
				.hasTotalHitCount( mainIndexDocumentFieldValues.size() + 1 /* +1 for the empty document */ );
		SearchResultAssert.assertThat( compatibleIndexManager.createScope().query()
				.where( f -> f.matchAll() )
				.toQuery() )
				.hasTotalHitCount( otherIndexDocumentFieldValues.size() );
		SearchResultAssert.assertThat( rawFieldCompatibleIndexManager.createScope().query()
				.where( f -> f.matchAll() )
				.toQuery() )
				.hasTotalHitCount( otherIndexDocumentFieldValues.size() );
		SearchResultAssert.assertThat( nullOnlyIndexManager.createScope().query()
				.where( f -> f.matchAll() )
				.toQuery() )
				.hasTotalHitCount( 1 );
		if ( TckConfiguration.get().getBackendFeatures().aggregationsOnMultiValuedFields( typeDescriptor.getJavaType() ) ) {
			SearchResultAssert.assertThat( multiValuedIndexManager.createScope().query()
					.where( f -> f.matchAll() )
					.toQuery() )
					.hasTotalHitCount( multiValuedIndexDocumentFieldValues.size() + 1 /* +1 for the empty document */ );
		}
	}

	private FieldModel<F> mapField(IndexSchemaElement parent, String prefix,
			Consumer<StandardIndexFieldTypeOptionsStep<?, F>> additionalConfiguration) {
		return FieldModel.mapper( typeDescriptor )
				.map( parent, prefix + typeDescriptor.getUniqueName(), additionalConfiguration );
	}

	private FieldModel<F> mapMultiValuedField(IndexSchemaElement parent, String prefix,
			Consumer<StandardIndexFieldTypeOptionsStep<?, F>> additionalConfiguration) {
		return FieldModel.mapper( typeDescriptor )
				.mapMultiValued( parent, prefix + typeDescriptor.getUniqueName(), additionalConfiguration );
	}

	private class IndexMapping {
		final FieldModel<F> fieldModel;
		final FieldModel<F> fieldWithConverterModel;
		final FieldModel<F> fieldWithAggregationDisabledModel;

		final ObjectMapping flattenedObject;
		final ObjectMapping nestedObject;

		IndexMapping(IndexSchemaElement root) {
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

			flattenedObject = new ObjectMapping( root, "flattenedObject", ObjectFieldStorage.FLATTENED );
			nestedObject = new ObjectMapping( root, "nestedObject", ObjectFieldStorage.NESTED );
		}
	}

	private class ObjectMapping {
		final String relativeFieldName;
		final IndexObjectFieldReference self;
		final FieldModel<F> fieldModel;

		ObjectMapping(IndexSchemaElement parent, String relativeFieldName, ObjectFieldStorage storage) {
			this.relativeFieldName = relativeFieldName;
			IndexSchemaObjectField objectField = parent.objectField( relativeFieldName, storage );
			self = objectField.toReference();
			fieldModel = mapField(
					objectField, "", ignored -> { }
			);
		}
	}

	private class RawFieldCompatibleIndexMapping {
		final FieldModel<F> fieldWithConverterModel;

		RawFieldCompatibleIndexMapping(IndexSchemaElement root) {
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

	private class IncompatibleIndexMapping {
		IncompatibleIndexMapping(IndexSchemaElement root) {
			/*
			 * Add a field with the same name as the fieldModel from IndexMapping,
			 * but with an incompatible type.
			 */
			IncompatibleFieldModel.mapper( FieldTypeDescriptor.getIncompatible( typeDescriptor )::configure )
					.map( root, typeDescriptor.getUniqueName() );
		}
	}

	private class MultiValuedIndexMapping {
		final FieldModel<F> fieldModel;

		MultiValuedIndexMapping(IndexSchemaElement root) {
			fieldModel = mapMultiValuedField(
					root, "",
					c -> c.aggregable( Aggregable.YES )
			);
		}
	}

	private static class FieldModel<F> {
		static <F> StandardFieldMapper<F, FieldModel<F>> mapper(FieldTypeDescriptor<F> typeDescriptor) {
			return StandardFieldMapper.of(
					typeDescriptor::configure,
					FieldModel::new
			);
		}

		final IndexFieldReference<F> reference;
		final String relativeFieldName;

		private FieldModel(IndexFieldReference<F> reference, String relativeFieldName) {
			this.reference = reference;
			this.relativeFieldName = relativeFieldName;
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
