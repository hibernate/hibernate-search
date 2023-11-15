/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.search.aggregation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hibernate.search.util.impl.integrationtest.common.assertion.SearchResultAssert.assertThatQuery;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.hibernate.search.engine.backend.document.IndexObjectFieldReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaObjectField;
import org.hibernate.search.engine.backend.types.Aggregable;
import org.hibernate.search.engine.backend.types.ObjectStructure;
import org.hibernate.search.engine.backend.types.Projectable;
import org.hibernate.search.engine.backend.types.Searchable;
import org.hibernate.search.engine.backend.types.Sortable;
import org.hibernate.search.engine.backend.types.converter.FromDocumentValueConverter;
import org.hibernate.search.engine.backend.types.converter.ToDocumentValueConverter;
import org.hibernate.search.engine.backend.types.converter.runtime.FromDocumentValueConvertContext;
import org.hibernate.search.engine.backend.types.converter.runtime.ToDocumentValueConvertContext;
import org.hibernate.search.engine.backend.types.dsl.StandardIndexFieldTypeOptionsStep;
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
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.StandardFieldTypeDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.SimpleFieldModel;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.SimpleFieldModelsByType;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.TypeAssertionHelper;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.ValueWrapper;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.extension.SearchSetupHelper;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.BulkIndexer;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.SimpleMappedIndex;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappingScope;
import org.hibernate.search.util.impl.test.annotation.PortedFromSearch5;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Tests behavior related to type checking and type conversion of DSL arguments
 * for all single-field aggregations (range, terms, ...)
 * on supported types.
 */

class SingleFieldAggregationTypeCheckingAndConversionIT<F> {

	private static final String AGGREGATION_NAME = "aggregationName";

	private static final Set<StandardFieldTypeDescriptor<?>> supportedFieldTypes = new LinkedHashSet<>();
	private static final List<DataSet<?>> dataSets = new ArrayList<>();
	private static final List<Arguments> parameters = new ArrayList<>();

	static {
		for ( AggregationDescriptor aggregationDescriptor : AggregationDescriptor.getAll() ) {
			for ( StandardFieldTypeDescriptor<?> fieldTypeDescriptor : FieldTypeDescriptor.getAllStandard() ) {
				Optional<? extends SupportedSingleFieldAggregationExpectations<?>> expectations =
						aggregationDescriptor.getSingleFieldAggregationExpectations( fieldTypeDescriptor ).getSupported();
				if ( expectations.isPresent() ) {
					supportedFieldTypes.add( fieldTypeDescriptor );
					DataSet<?> dataSet = new DataSet<>( expectations.get() );
					dataSets.add( dataSet );
					parameters.add( Arguments.of( expectations.get(), dataSet ) );
				}
			}
		}
	}

	public static List<? extends Arguments> params() {
		return parameters;
	}

	@RegisterExtension
	public static final SearchSetupHelper setupHelper = SearchSetupHelper.create();

	private static final SimpleMappedIndex<IndexBinding> mainIndex =
			SimpleMappedIndex.of( IndexBinding::new ).name( "Main" );
	private static final SimpleMappedIndex<CompatibleIndexBinding> compatibleIndex =
			SimpleMappedIndex.of( CompatibleIndexBinding::new ).name( "Compatible" );
	private static final SimpleMappedIndex<RawFieldCompatibleIndexBinding> rawFieldCompatibleIndex =
			SimpleMappedIndex.of( RawFieldCompatibleIndexBinding::new ).name( "RawFieldCompatible" );
	private static final SimpleMappedIndex<IncompatibleIndexBinding> incompatibleIndex =
			SimpleMappedIndex.of( IncompatibleIndexBinding::new ).name( "Incompatible" );

	@BeforeAll
	static void setup() {
		setupHelper.start()
				.withIndexes(
						mainIndex,
						compatibleIndex,
						rawFieldCompatibleIndex,
						incompatibleIndex
				)
				.setup();

		for ( DataSet<?> dataSet : dataSets ) {
			dataSet.init();
		}
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("params")
	void aggregationObject_reuse_onScopeTargetingSameIndexes(SupportedSingleFieldAggregationExpectations<F> expectations,
			DataSet<F> dataSet) {
		// Need a separate method to handle the scenario generics
		doTest_aggregationObject_reuse_onScopeTargetingSameIndexes( expectations.simple(), expectations.fieldType(), dataSet );
	}

	private <A> void doTest_aggregationObject_reuse_onScopeTargetingSameIndexes(AggregationScenario<A> scenario,
			FieldTypeDescriptor<F, ?> fieldType, DataSet<F> dataSet) {
		StubMappingScope scope = mainIndex.createScope();
		String fieldPath = mainIndex.binding().fieldModels.get( fieldType ).relativeFieldName;
		AggregationKey<A> aggregationKey = AggregationKey.of( AGGREGATION_NAME );

		SearchAggregation<A> aggregation = scenario.setup( scope.aggregation(), fieldPath )
				.toAggregation();

		assertThatQuery(
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
		assertThatQuery(
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
		assertThatQuery(
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

	@ParameterizedTest(name = "{0}")
	@MethodSource("params")
	void aggregationObject_reuse_onScopeTargetingDifferentIndexes(
			SupportedSingleFieldAggregationExpectations<F> expectations, DataSet<F> dataSet) {
		// Need a separate method to handle the scenario generics
		doTest_aggregationObject_reuse_onScopeTargetingDifferentIndexes( expectations.simple(), expectations.fieldType(),
				dataSet );
	}

	private <A> void doTest_aggregationObject_reuse_onScopeTargetingDifferentIndexes(AggregationScenario<A> scenario,
			FieldTypeDescriptor<F, ?> fieldType, DataSet<F> dataSet) {
		StubMappingScope scope = mainIndex.createScope();
		String fieldPath = mainIndex.binding().fieldModels.get( fieldType ).relativeFieldName;
		AggregationKey<A> aggregationKey = AggregationKey.of( AGGREGATION_NAME );

		SearchAggregation<A> aggregation = scenario.setup( scope.aggregation(), fieldPath )
				.toAggregation();

		// reuse the aggregation instance on a different scope targeting a different index
		assertThatThrownBy( () -> compatibleIndex.createScope().query()
				.where( f -> f.matchAll() )
				.aggregation( aggregationKey, aggregation )
				.routing( dataSet.name )
				.toQuery()
		)
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll( "Invalid search aggregation",
						"You must build the aggregation from a scope targeting indexes ", compatibleIndex.name(),
						"the given aggregation was built from a scope targeting indexes ", mainIndex.name() );

		// reuse the aggregation instance on a different scope targeting a superset of the original indexes
		assertThatThrownBy( () -> mainIndex.createScope( compatibleIndex ).query()
				.where( f -> f.matchAll() )
				.aggregation( aggregationKey, aggregation )
				.routing( dataSet.name )
				.toQuery()
		)
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll( "Invalid search aggregation",
						"You must build the aggregation from a scope targeting indexes ",
						mainIndex.name(), compatibleIndex.name(),
						"the given aggregation was built from a scope targeting indexes ", mainIndex.name() );
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("params")
	void invalidFieldType_conversionEnabled(SupportedSingleFieldAggregationExpectations<F> expectations,
			DataSet<F> dataSet) {
		String fieldPath = mainIndex.binding().fieldModels.get( expectations.fieldType() ).relativeFieldName;

		FieldTypeDescriptor<?, ?> wrongType = FieldTypeDescriptor.getIncompatible( expectations.fieldType() );

		AggregationScenario<?> scenario = expectations.withFieldType( TypeAssertionHelper.wrongType( wrongType ) );

		assertThatThrownBy( () -> scenario.setup( mainIndex.createScope().aggregation(), fieldPath ) )
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll(
						"Invalid type", "'" + wrongType.getJavaType().getName() + "'",
						"Expected '" + expectations.fieldType().getJavaType().getName() + "'",
						"field '" + fieldPath + "'"
				);
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("params")
	void invalidFieldType_conversionDisabled(SupportedSingleFieldAggregationExpectations<F> expectations,
			DataSet<F> dataSet) {
		String fieldPath = mainIndex.binding().fieldModels.get( expectations.fieldType() ).relativeFieldName;

		FieldTypeDescriptor<?, ?> wrongType = FieldTypeDescriptor.getIncompatible( expectations.fieldType() );

		AggregationScenario<?> scenario = expectations.withFieldType( TypeAssertionHelper.wrongType( wrongType ) );

		assertThatThrownBy( () -> scenario.setupWithConverterSetting(
				mainIndex.createScope().aggregation(), fieldPath, ValueConvert.NO
		) )
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll(
						"Invalid type", "'" + wrongType.getJavaType().getName() + "'",
						"Expected '" + expectations.fieldType().getJavaType().getName() + "'",
						"field '" + fieldPath + "'"
				);
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("params")
	void nullFieldType(SupportedSingleFieldAggregationExpectations<F> expectations, DataSet<F> dataSet) {
		String fieldPath = mainIndex.binding().fieldModels.get( expectations.fieldType() ).relativeFieldName;

		// Try to pass a "null" field type
		AggregationScenario<?> scenario = expectations.withFieldType( TypeAssertionHelper.nullType() );

		assertThatThrownBy( () -> scenario.setup( mainIndex.createScope().aggregation(), fieldPath ) )
				.isInstanceOf( IllegalArgumentException.class )
				.hasMessageContaining( "'type'" )
				.hasMessageContaining( "must not be null" );
	}

	/**
	 * Test that mentioning the same aggregation twice with different keys works as expected.
	 */
	@ParameterizedTest(name = "{0}")
	@MethodSource("params")
	@PortedFromSearch5(original = "org.hibernate.search.test.query.facet.SimpleFacetingTest.testMultipleFacets")
	void duplicated_differentKeys(SupportedSingleFieldAggregationExpectations<F> expectations, DataSet<F> dataSet) {
		String fieldPath = mainIndex.binding().fieldModels.get( expectations.fieldType() ).relativeFieldName;

		AggregationScenario<?> scenario = expectations.simple();

		// A separate method is needed in order to write type-safe code
		doTestDuplicatedDifferentKeys( fieldPath, scenario, dataSet );
	}

	private <A> void doTestDuplicatedDifferentKeys(String fieldPath, AggregationScenario<A> scenario, DataSet<F> dataSet) {
		AggregationKey<A> key1 = AggregationKey.of( "aggregationName1" );
		AggregationKey<A> key2 = AggregationKey.of( "aggregationName2" );

		assertThatQuery(
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
	@ParameterizedTest(name = "{0}")
	@MethodSource("params")
	void duplicated_sameKey(SupportedSingleFieldAggregationExpectations<F> expectations, DataSet<F> dataSet) {
		String fieldPath = mainIndex.binding().fieldModels.get( expectations.fieldType() ).relativeFieldName;

		AggregationScenario<?> scenario = expectations.simple();

		// A separate method is needed in order to write type-safe code
		doTestDuplicatedSameKey( fieldPath, scenario );
	}

	private <A> void doTestDuplicatedSameKey(String fieldPath, AggregationScenario<A> scenario) {
		AggregationKey<A> key1 = AggregationKey.of( "aggregationName1" );

		assertThatThrownBy( () -> mainIndex.createScope().query().where( f -> f.matchAll() )
				.aggregation( key1, f -> scenario.setup( f, fieldPath ) )
				.aggregation( key1, f -> scenario.setup( f, fieldPath ) )
		)
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "Duplicate aggregation definitions for key: 'aggregationName1'" );
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("params")
	@TestForIssue(jiraKey = "HSEARCH-1748")
	@PortedFromSearch5(
			original = "org.hibernate.search.test.query.facet.FacetUnknownFieldFailureTest.testKnownFieldNameNotConfiguredForFacetingThrowsException")
	void aggregationsDisabled(SupportedSingleFieldAggregationExpectations<F> expectations, DataSet<F> dataSet) {
		String fieldPath =
				mainIndex.binding().fieldWithAggregationDisabledModels.get( expectations.fieldType() ).relativeFieldName;

		AggregationScenario<?> scenario =
				expectations.withFieldType( TypeAssertionHelper.identity( expectations.fieldType() ) );

		assertThatThrownBy( () -> scenario.setup( mainIndex.createScope().aggregation(), fieldPath ) )
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll(
						"Cannot use 'aggregation:" + expectations.aggregationName() + "' on field '" + fieldPath + "'",
						"Make sure the field is marked as searchable/sortable/projectable/aggregable/highlightable (whichever is relevant)"
				);
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("params")
	void withConverter_conversionEnabled(SupportedSingleFieldAggregationExpectations<F> expectations,
			DataSet<F> dataSet) {
		String fieldPath = mainIndex.binding().fieldWithConverterModels.get( expectations.fieldType() ).relativeFieldName;

		AggregationScenario<?> scenario = expectations.withFieldType(
				TypeAssertionHelper.wrapper( expectations.fieldType() )
		);
		testValidAggregation(
				scenario, mainIndex.createScope(), fieldPath,
				dataSet
		);
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("params")
	void withConverter_conversionDisabled(SupportedSingleFieldAggregationExpectations<F> expectations,
			DataSet<F> dataSet) {
		String fieldPath = mainIndex.binding().fieldModels.get( expectations.fieldType() ).relativeFieldName;

		AggregationScenario<?> scenario = expectations.simple();
		testValidAggregationWithConverterSetting(
				scenario, mainIndex.createScope(), fieldPath, ValueConvert.NO,
				dataSet
		);
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("params")
	void withConverter_invalidFieldType(SupportedSingleFieldAggregationExpectations<F> expectations,
			DataSet<F> dataSet) {
		String fieldPath = mainIndex.binding().fieldWithConverterModels.get( expectations.fieldType() ).relativeFieldName;

		AggregationScenario<?> scenario = expectations.simple();

		assertThatThrownBy( () -> scenario.setup( mainIndex.createScope().aggregation(), fieldPath ) )
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll(
						"Invalid type", "'" + expectations.fieldType().getJavaType().getName() + "'",
						"Expected '" + ValueWrapper.class.getName() + "'",
						"field '" + fieldPath + "'"
				);
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("params")
	void multiIndex_withCompatibleIndex_noConverter(SupportedSingleFieldAggregationExpectations<F> expectations,
			DataSet<F> dataSet) {
		StubMappingScope scope = mainIndex.createScope( compatibleIndex );

		String fieldPath = mainIndex.binding().fieldModels.get( expectations.fieldType() ).relativeFieldName;

		AggregationScenario<?> scenario = expectations.onMainAndOtherIndex();
		testValidAggregation(
				scenario, scope, fieldPath,
				dataSet
		);
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("params")
	void multiIndex_withCompatibleIndex_conversionEnabled(SupportedSingleFieldAggregationExpectations<F> expectations,
			DataSet<F> dataSet) {
		StubMappingScope scope = mainIndex.createScope( compatibleIndex );

		String fieldPath = mainIndex.binding().fieldWithConverterModels.get( expectations.fieldType() ).relativeFieldName;

		AggregationScenario<?> scenario = expectations.withFieldTypeOnMainAndOtherIndex(
				TypeAssertionHelper.wrapper( expectations.fieldType() )
		);
		testValidAggregation(
				scenario, scope, fieldPath,
				dataSet
		);
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("params")
	void multiIndex_withRawFieldCompatibleIndex_conversionEnabled(
			SupportedSingleFieldAggregationExpectations<F> expectations, DataSet<F> dataSet) {
		StubMappingScope scope = mainIndex.createScope( rawFieldCompatibleIndex );

		String fieldPath = mainIndex.binding().fieldWithConverterModels.get( expectations.fieldType() ).relativeFieldName;

		AggregationScenario<?> scenario = expectations.withFieldTypeOnMainAndOtherIndex(
				TypeAssertionHelper.wrapper( expectations.fieldType() )
		);

		assertThatThrownBy( () -> scenario.setup( scope.aggregation(), fieldPath ) )
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll(
						"Inconsistent configuration for field '" + fieldPath + "' in a search query across multiple indexes",
						"Attribute '", "Converter' differs:", " vs. "
				);
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("params")
	void multiIndex_withRawFieldCompatibleIndex_conversionDisabled(
			SupportedSingleFieldAggregationExpectations<F> expectations, DataSet<F> dataSet) {
		StubMappingScope scope = mainIndex.createScope( rawFieldCompatibleIndex );

		String fieldPath = mainIndex.binding().fieldWithConverterModels.get( expectations.fieldType() ).relativeFieldName;

		AggregationScenario<?> scenario = expectations.onMainAndOtherIndex();
		testValidAggregationWithConverterSetting(
				scenario, scope, fieldPath, ValueConvert.NO,
				dataSet
		);
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("params")
	void multiIndex_withIncompatibleIndex_conversionEnabled(SupportedSingleFieldAggregationExpectations<F> expectations,
			DataSet<F> dataSet) {
		StubMappingScope scope = mainIndex.createScope( incompatibleIndex );

		String fieldPath = mainIndex.binding().fieldModels.get( expectations.fieldType() ).relativeFieldName;

		AggregationScenario<?> scenario = expectations.simple();

		assertThatThrownBy( () -> scenario.setup( scope.aggregation(), fieldPath ) )
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll(
						"Inconsistent configuration for field '" + fieldPath + "' in a search query across multiple indexes",
						"Inconsistent support for 'aggregation:" + expectations.aggregationName() + "'"
				);
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("params")
	void multiIndex_withIncompatibleIndex_conversionDisabled(SupportedSingleFieldAggregationExpectations<F> expectations,
			DataSet<F> dataSet) {
		StubMappingScope scope = mainIndex.createScope( incompatibleIndex );

		String fieldPath = mainIndex.binding().fieldModels.get( expectations.fieldType() ).relativeFieldName;

		AggregationScenario<?> scenario = expectations.simple();

		assertThatThrownBy( () -> scenario.setupWithConverterSetting(
				scope.aggregation(), fieldPath, ValueConvert.NO
		) )
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll(
						"Inconsistent configuration for field '" + fieldPath + "' in a search query across multiple indexes",
						"Inconsistent support for 'aggregation:" + expectations.aggregationName() + "'"
				);
	}

	private <A> void testValidAggregation(AggregationScenario<A> scenario, StubMappingScope scope,
			String fieldPath, DataSet<F> dataSet) {
		testValidAggregation(
				scenario, scope,
				f -> f.matchAll(),
				(f, e) -> e.setup( f, fieldPath ),
				dataSet
		);
	}

	private <A> void testValidAggregationWithConverterSetting(AggregationScenario<A> scenario,
			StubMappingScope scope, String fieldPath, ValueConvert convert, DataSet<F> dataSet) {
		testValidAggregation(
				scenario, scope,
				f -> f.matchAll(),
				(f, e) -> e.setupWithConverterSetting( f, fieldPath, convert ),
				dataSet
		);
	}

	private <A> void testValidAggregation(AggregationScenario<A> scenario, StubMappingScope scope,
			Function<SearchPredicateFactory, ? extends PredicateFinalStep> predicateContributor,
			BiFunction<SearchAggregationFactory, AggregationScenario<A>, AggregationFinalStep<A>> aggregationContributor,
			DataSet<F> dataSet) {
		AggregationKey<A> aggregationKey = AggregationKey.of( AGGREGATION_NAME );
		assertThatQuery(
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
		final FieldTypeDescriptor<F, ?> fieldType;
		final String name;

		private DataSet(SupportedSingleFieldAggregationExpectations<F> expectations) {
			this.expectations = expectations;
			this.fieldType = expectations.fieldType();
			this.name = expectations.aggregationName() + "_" + expectations.fieldType().getUniqueName();
		}

		private void init() {
			FieldTypeDescriptor<F, ?> fieldType = expectations.fieldType();

			List<F> mainIndexDocumentFieldValues = expectations.getMainIndexDocumentFieldValues();
			List<F> otherIndexDocumentFieldValues = expectations.getOtherIndexDocumentFieldValues();

			BulkIndexer mainIndexer = mainIndex.bulkIndexer();
			for ( int i = 0; i < mainIndexDocumentFieldValues.size(); i++ ) {
				F value = mainIndexDocumentFieldValues.get( i );
				mainIndexer.add( name + "_document_" + i, name, document -> {
					document.addValue( mainIndex.binding().fieldModels.get( fieldType ).reference, value );
					document.addValue( mainIndex.binding().fieldWithConverterModels.get( fieldType ).reference, value );
				} );
			}
			mainIndexer.add( name + "_document_empty", name, document -> {} );
			BulkIndexer compatibleIndexer = compatibleIndex.bulkIndexer();
			for ( int i = 0; i < otherIndexDocumentFieldValues.size(); i++ ) {
				F value = otherIndexDocumentFieldValues.get( i );
				compatibleIndexer.add( name + "_compatibleindex_document_" + i, name, document -> {
					document.addValue( compatibleIndex.binding().fieldModels.get( fieldType ).reference, value );
					document.addValue( compatibleIndex.binding().fieldWithConverterModels.get( fieldType ).reference, value );
				} );
			}
			BulkIndexer rawFieldCompatibleIndexer = rawFieldCompatibleIndex.bulkIndexer();
			for ( int i = 0; i < otherIndexDocumentFieldValues.size(); i++ ) {
				F value = otherIndexDocumentFieldValues.get( i );
				rawFieldCompatibleIndexer.add( name + "_rawcompatibleindex_document_" + i, name, document -> {
					document.addValue( rawFieldCompatibleIndex.binding().fieldWithConverterModels.get( fieldType ).reference,
							value );
				} );
			}
			mainIndexer.join( compatibleIndexer, rawFieldCompatibleIndexer );
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
							.dslConverter( ValueWrapper.class, ValueWrapper.toDocumentValueConverter() )
							.projectionConverter( ValueWrapper.class, ValueWrapper.fromDocumentValueConverter() ) );
			fieldWithAggregationDisabledModels = SimpleFieldModelsByType.mapAll( supportedFieldTypes, root,
					"nonAggregable_", c -> c.aggregable( Aggregable.NO ) );

			flattenedObject = new ObjectBinding( root, "flattenedObject", ObjectStructure.FLATTENED );
			nestedObject = new ObjectBinding( root, "nestedObject", ObjectStructure.NESTED );
		}
	}

	private static class ObjectBinding {
		final String relativeFieldName;
		final IndexObjectFieldReference self;

		ObjectBinding(IndexSchemaElement parent, String relativeFieldName, ObjectStructure structure) {
			this.relativeFieldName = relativeFieldName;
			IndexSchemaObjectField objectField = parent.objectField( relativeFieldName, structure );
			self = objectField.toReference();
		}
	}

	private static class CompatibleIndexBinding {
		final SimpleFieldModelsByType fieldModels;
		final SimpleFieldModelsByType fieldWithConverterModels;

		CompatibleIndexBinding(IndexSchemaElement root) {
			fieldModels = SimpleFieldModelsByType.mapAll( supportedFieldTypes, root,
					"", (fieldType, c) -> {
						c.aggregable( Aggregable.YES );
						addIrrelevantOptions( fieldType, c );
					} );
			fieldWithConverterModels = SimpleFieldModelsByType.mapAll( supportedFieldTypes, root,
					"converted_", (fieldType, c) -> {
						c.aggregable( Aggregable.YES )
								.dslConverter( ValueWrapper.class, ValueWrapper.toDocumentValueConverter() )
								.projectionConverter( ValueWrapper.class, ValueWrapper.fromDocumentValueConverter() );
						addIrrelevantOptions( fieldType, c );
					} );
		}

		// See HSEARCH-3307: this checks that irrelevant options are ignored when checking cross-index field compatibility
		protected void addIrrelevantOptions(FieldTypeDescriptor<?, ?> fieldType, StandardIndexFieldTypeOptionsStep<?, ?> c) {
			c.searchable( Searchable.NO );
			c.projectable( Projectable.YES );
			if ( fieldType.isFieldSortSupported() ) {
				c.sortable( Sortable.YES );
			}
		}
	}

	private static class RawFieldCompatibleIndexBinding {
		final SimpleFieldModelsByType fieldWithConverterModels;

		RawFieldCompatibleIndexBinding(IndexSchemaElement root) {
			/*
			 * Add a field with the same name as the fieldWithConverterModel from IndexBinding,
			 * but with an incompatible projection converter.
			 */
			fieldWithConverterModels = SimpleFieldModelsByType.mapAll( supportedFieldTypes, root,
					"converted_", c -> c.aggregable( Aggregable.YES )
							.dslConverter( ValueWrapper.class, new IncompatibleDslConverter<>() )
							.projectionConverter( ValueWrapper.class, new IncompatibleProjectionConverter() ) );
		}

		@SuppressWarnings("rawtypes")
		private static class IncompatibleDslConverter<F>
				implements ToDocumentValueConverter<ValueWrapper, F> {
			@Override
			public F toDocumentValue(ValueWrapper value, ToDocumentValueConvertContext context) {
				return null;
			}
		}

		@SuppressWarnings("rawtypes")
		private static class IncompatibleProjectionConverter
				implements FromDocumentValueConverter<Object, ValueWrapper> {
			@Override
			public ValueWrapper fromDocumentValue(Object value, FromDocumentValueConvertContext context) {
				return null;
			}
		}
	}

	private static class IncompatibleIndexBinding {
		IncompatibleIndexBinding(IndexSchemaElement root) {
			/*
			 * Add fields with the same name as the fieldsModels from IndexBinding,
			 * but with an incompatible type.
			 */
			mapFieldsWithIncompatibleType( root );
		}

		private static void mapFieldsWithIncompatibleType(IndexSchemaElement parent) {
			supportedFieldTypes.forEach(
					typeDescriptor -> SimpleFieldModel.mapper( FieldTypeDescriptor.getIncompatible( typeDescriptor ),
							o -> o.aggregable( Aggregable.YES ) )
							.map( parent, "" + typeDescriptor.getUniqueName() )
			);
		}
	}
}
