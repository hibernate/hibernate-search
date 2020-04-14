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
import java.util.Arrays;
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
import org.hibernate.search.engine.backend.work.execution.spi.IndexIndexingPlan;
import org.hibernate.search.engine.search.aggregation.AggregationKey;
import org.hibernate.search.engine.search.aggregation.SearchAggregation;
import org.hibernate.search.engine.search.aggregation.dsl.AggregationFinalStep;
import org.hibernate.search.engine.search.aggregation.dsl.SearchAggregationFactory;
import org.hibernate.search.engine.search.predicate.dsl.PredicateFinalStep;
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory;
import org.hibernate.search.integrationtest.backend.tck.testsupport.operations.AggregationDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.operations.expectations.AggregationScenario;
import org.hibernate.search.integrationtest.backend.tck.testsupport.operations.expectations.SupportedSingleFieldAggregationExpectations;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.FieldTypeDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.IndexFieldStructure;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.IndexFieldValueCardinality;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.SimpleFieldModelsByType;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
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

/**
 * Tests basic behavior common to all single-field aggregations (range, terms, ...)
 * on supported types.
 */
@RunWith(Parameterized.class)
public class SingleFieldAggregationBaseIT<F> {

	private static final String AGGREGATION_NAME = "aggregationName";

	private static Set<FieldTypeDescriptor<?>> supportedFieldTypes;
	private static List<DataSet<?>> dataSets;

	@Parameterized.Parameters(name = "{0} - {1} - {2}")
	public static Object[][] parameters() {
		supportedFieldTypes = new LinkedHashSet<>();
		dataSets = new ArrayList<>();
		List<Object[]> parameters = new ArrayList<>();
		for ( AggregationDescriptor aggregationDescriptor : AggregationDescriptor.getAll() ) {
			for ( FieldTypeDescriptor<?> fieldTypeDescriptor : FieldTypeDescriptor.getAll() ) {
				Optional<? extends SupportedSingleFieldAggregationExpectations<?>> expectations =
						aggregationDescriptor.getSingleFieldAggregationExpectations( fieldTypeDescriptor ).getSupported();
				if ( expectations.isPresent() ) {
					for ( IndexFieldStructure fieldStructure : IndexFieldStructure.values() ) {
						if ( IndexFieldStructure.IN_NESTED_REQUIRING_FILTER.equals( fieldStructure ) ) {
							// TODO HSEARCH-3881 test filtering, too
							continue;
						}
						for ( IndexFieldValueCardinality valueCardinality : IndexFieldValueCardinality.values() ) {
							supportedFieldTypes.add( fieldTypeDescriptor );
							DataSet<?> dataSet = new DataSet<>( expectations.get(), fieldStructure, valueCardinality );
							dataSets.add( dataSet );
							parameters.add( new Object[] { expectations.get(), fieldStructure, valueCardinality, dataSet } );
						}
					}
				}
			}
		}
		return parameters.toArray( new Object[0][] );
	}

	@ClassRule
	public static final SearchSetupHelper setupHelper = new SearchSetupHelper();

	private static final SimpleMappedIndex<IndexBinding> mainIndex =
			SimpleMappedIndex.of( "Main", IndexBinding::new );
	private static final SimpleMappedIndex<IndexBinding> emptyIndex =
			SimpleMappedIndex.of( "Empty", IndexBinding::new );
	private static final SimpleMappedIndex<IndexBinding> nullOnlyIndex =
			SimpleMappedIndex.of( "NullOnly", IndexBinding::new );

	@BeforeClass
	public static void setup() {
		setupHelper.start().withIndexes( mainIndex, emptyIndex, nullOnlyIndex ).setup();

		for ( DataSet<?> dataSet : dataSets ) {
			dataSet.init();
		}
	}

	private final SupportedSingleFieldAggregationExpectations<F> expectations;
	private final FieldTypeDescriptor<F> fieldType;
	private final IndexFieldStructure fieldStructure;
	private final IndexFieldValueCardinality valueCardinality;
	private final DataSet<F> dataSet;

	public SingleFieldAggregationBaseIT(SupportedSingleFieldAggregationExpectations<F> expectations,
			IndexFieldStructure fieldStructure, IndexFieldValueCardinality valueCardinality,
			DataSet<F> dataSet) {
		this.expectations = expectations;
		this.fieldType = expectations.fieldType();
		this.fieldStructure = fieldStructure;
		this.valueCardinality = valueCardinality;
		this.dataSet = dataSet;
	}

	@Test
	@TestForIssue(jiraKey = {
			"HSEARCH-726", "HSEARCH-900", "HSEARCH-809",
			"HSEARCH-2376", "HSEARCH-2472", "HSEARCH-2954", "HSEARCH-2535",
			"HSEARCH-1927", "HSEARCH-1929"
	})
	@PortedFromSearch5(original = {
			"org.hibernate.search.test.query.facet.NumberFacetingTest",
			"org.hibernate.search.test.query.facet.RangeFacetingTest.testRangeQueryForInteger",
			"org.hibernate.search.test.query.facet.RangeFacetingTest.testDateRangeFaceting",
			"org.hibernate.search.test.query.facet.SimpleFacetingTest.testSimpleDiscretFaceting",
			"org.hibernate.search.test.query.facet.StringFacetingTest",
			"org.hibernate.search.test.query.facet.EmbeddedCollectionFacetingTest",
			"org.hibernate.search.test.query.facet.ManyToOneFacetingTest",
			"org.hibernate.search.test.query.facet.MultiValuedFacetingTest"
	})
	public void simple() {
		// Need a separate method to handle the scenario generics
		doTest_simple( getSimpleScenario() );
	}

	private <A> void doTest_simple(AggregationScenario<A> scenario) {
		StubMappingScope scope = mainIndex.createScope();
		String fieldPath = getFieldPath( mainIndex.binding() );
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
		doTest_aggregationObject( getSimpleScenario() );
	}

	private <A> void doTest_aggregationObject(AggregationScenario<A> scenario) {
		StubMappingScope scope = mainIndex.createScope();
		String fieldPath = getFieldPath( mainIndex.binding() );
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
	@TestForIssue(jiraKey = { "HSEARCH-1968" })
	@PortedFromSearch5(original = "org.hibernate.search.test.query.facet.NoQueryResultsFacetingTest")
	public void noMatch() {
		String fieldPath = getFieldPath( mainIndex.binding() );

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
		String fieldPath = getFieldPath( emptyIndex.binding() );

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
		String fieldPath = getFieldPath( nullOnlyIndex.binding() );

		AggregationScenario<?> scenario = expectations.withoutMatch();
		testValidAggregation(
				scenario, nullOnlyIndex.createScope(), fieldPath
		);
	}

	private <A> void testValidAggregation(AggregationScenario<A> scenario, StubMappingScope scope,
			String fieldPath) {
		testValidAggregation(
				scenario, scope,
				f -> f.matchAll(),
				(f, e) -> e.setup( f, fieldPath )
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

	private AggregationScenario<?> getSimpleScenario() {
		AggregationScenario<?> scenario;
		if ( IndexFieldValueCardinality.SINGLE_VALUED.equals( valueCardinality ) ) {
			scenario = expectations.simple();
		}
		else {
			scenario = expectations.onMultiValuedIndex();
		}
		return scenario;
	}

	private String getFieldPath(IndexBinding indexBinding) {
		switch ( fieldStructure ) {
			case ROOT:
				return getRelativeFieldName( indexBinding );
			case IN_FLATTENED:
				return indexBinding.flattenedObject.relativeFieldName
						+ "." + getRelativeFieldName( indexBinding.flattenedObject );
			case IN_NESTED:
				return indexBinding.nestedObject.relativeFieldName
						+ "." + getRelativeFieldName( indexBinding.nestedObject );
			case IN_NESTED_TWICE:
				return indexBinding.nestedObject.relativeFieldName
						+ "." + indexBinding.nestedObject.nestedObject.relativeFieldName
						+ "." + getRelativeFieldName( indexBinding.nestedObject.nestedObject );
			case IN_NESTED_REQUIRING_FILTER:
				// TODO HSEARCH-3881 test filtering, too
			default:
				throw new IllegalStateException( "Unexpected value: " + fieldStructure );
		}
	}

	private String getRelativeFieldName(AbstractObjectBinding binding) {
		return getFieldModelsByType( binding ).get( fieldType ).relativeFieldName;
	}

	private SimpleFieldModelsByType getFieldModelsByType(AbstractObjectBinding binding) {
		switch ( valueCardinality ) {
			case SINGLE_VALUED:
				return binding.fieldWithSingleValueModels;
			case MULTI_VALUED:
				return binding.fieldWithMultipleValuesModels;
			default:
				throw new IllegalStateException( "Unexpected field value cardinality: " + valueCardinality );
		}
	}

	private static class DataSet<F> {
		final SupportedSingleFieldAggregationExpectations<F> expectations;
		final FieldTypeDescriptor<F> fieldType;
		final String name;
		private final IndexFieldStructure fieldStructure;
		private final IndexFieldValueCardinality valueCardinality;

		private DataSet(SupportedSingleFieldAggregationExpectations<F> expectations,
				IndexFieldStructure fieldStructure, IndexFieldValueCardinality valueCardinality) {
			this.expectations = expectations;
			this.fieldType = expectations.fieldType();
			this.name = expectations.aggregationName() + "_" + expectations.fieldType().getUniqueName()
					+ "_" + fieldStructure.name() + "_" + valueCardinality.name();
			this.fieldStructure = fieldStructure;
			this.valueCardinality = valueCardinality;
		}

		private void init() {
			IndexIndexingPlan<?> plan = mainIndex.createIndexingPlan();
			int mainIndexDocumentCount;
			if ( IndexFieldValueCardinality.SINGLE_VALUED.equals( valueCardinality ) ) {
				List<F> values = expectations.getMainIndexDocumentFieldValues();
				mainIndexDocumentCount = values.size();
				for ( int i = 0; i < values.size(); i++ ) {
					F valueForDocument = values.get( i );
					plan.add( referenceProvider( name + "_document_" + i, name ), document -> {
						initSingleValued( mainIndex.binding(), document, valueForDocument );
					} );
				}
			}
			else {
				List<List<F>> values = expectations.getMultiValuedIndexDocumentFieldValues();
				mainIndexDocumentCount = values.size();
				for ( int i = 0; i < values.size(); i++ ) {
					List<F> valuesForDocument = values.get( i );
					plan.add( referenceProvider( name + "_document_" + i, name ), document -> {
						initMultiValued( mainIndex.binding(), document, valuesForDocument );
					} );
				}
			}
			plan.add( referenceProvider( name + "_document_empty", name ), document -> { } );
			plan.execute().join();

			plan = nullOnlyIndex.createIndexingPlan();
			plan.add( referenceProvider( name + "_nullOnlyIndex_document_0", name ), document -> {
				if ( IndexFieldValueCardinality.SINGLE_VALUED.equals( valueCardinality ) ) {
					initSingleValued( nullOnlyIndex.binding(), document, null );
				}
				else {
					initMultiValued( nullOnlyIndex.binding(), document, Arrays.asList( null, null ) );
				}
			} );
			plan.execute().join();

			// Check that all documents are searchable
			SearchResultAssert.assertThat( mainIndex.createScope().query()
					.where( f -> f.matchAll() )
					.routing( name )
					.toQuery() )
					.hasTotalHitCount( mainIndexDocumentCount + 1 /* +1 for the empty document */ );
			SearchResultAssert.assertThat( nullOnlyIndex.createScope().query()
					.where( f -> f.matchAll() )
					.routing( name )
					.toQuery() )
					.hasTotalHitCount( 1 );
		}

		private void initSingleValued(IndexBinding binding, DocumentElement document, F value) {
			switch ( fieldStructure ) {
				case ROOT:
					document.addValue( binding.fieldWithSingleValueModels.get( fieldType ).reference, value );
					break;
				case IN_FLATTENED:
					DocumentElement flattenedObject = document.addObject( binding.flattenedObject.self );
					flattenedObject.addValue( binding.flattenedObject.fieldWithSingleValueModels.get( fieldType ).reference,
							value
					);
					break;
				case IN_NESTED:
					DocumentElement nestedObject = document.addObject( binding.nestedObject.self );
					nestedObject.addValue( binding.nestedObject.fieldWithSingleValueModels.get( fieldType ).reference,
							value
					);
					break;
				case IN_NESTED_TWICE:
					DocumentElement nestedObjectFirstLevel = document.addObject( binding.nestedObject.self );
					DocumentElement nestedObjectSecondLevel =
							nestedObjectFirstLevel.addObject( binding.nestedObject.nestedObject.self );
					nestedObjectSecondLevel.addValue(
							binding.nestedObject.nestedObject.fieldWithSingleValueModels.get( fieldType ).reference,
							value
					);
					break;
				case IN_NESTED_REQUIRING_FILTER:
					// TODO HSEARCH-3881 test filtering, too
					throw new UnsupportedOperationException( "Not tested yet" );
			}
		}

		private void initMultiValued(IndexBinding binding, DocumentElement document, List<F> values) {
			switch ( fieldStructure ) {
				case ROOT:
					for ( F value : values ) {
						document.addValue( binding.fieldWithMultipleValuesModels.get( fieldType ).reference, value );
					}
					break;
				case IN_FLATTENED:
					DocumentElement flattenedObject = document.addObject( binding.flattenedObject.self );
					for ( F value : values ) {
						flattenedObject.addValue(
								binding.flattenedObject.fieldWithMultipleValuesModels.get( fieldType ).reference,
								value
						);
					}
					break;
				case IN_NESTED:
					DocumentElement nestedObject = document.addObject( binding.nestedObject.self );
					for ( F value : values ) {
						nestedObject.addValue(
								binding.nestedObject.fieldWithMultipleValuesModels.get( fieldType ).reference,
								value
						);
					}
					break;
				case IN_NESTED_TWICE:
					DocumentElement nestedObjectFirstLevel = document.addObject( binding.nestedObject.self );
					DocumentElement nestedObjectSecondLevel =
							nestedObjectFirstLevel.addObject( binding.nestedObject.nestedObject.self );
					for ( F value : values ) {
						nestedObjectSecondLevel.addValue(
								binding.nestedObject.nestedObject.fieldWithMultipleValuesModels.get( fieldType ).reference,
								value
						);
					}
					break;
				case IN_NESTED_REQUIRING_FILTER:
					// TODO HSEARCH-3881 test filtering, too
					throw new UnsupportedOperationException( "Not tested yet" );
			}
		}
	}

	private static class AbstractObjectBinding {
		final SimpleFieldModelsByType fieldWithSingleValueModels;
		final SimpleFieldModelsByType fieldWithMultipleValuesModels;

		AbstractObjectBinding(IndexSchemaElement self) {
			fieldWithSingleValueModels = SimpleFieldModelsByType.mapAll( supportedFieldTypes, self,
					"", c -> c.aggregable( Aggregable.YES ) );
			fieldWithMultipleValuesModels = SimpleFieldModelsByType.mapAllMultiValued( supportedFieldTypes, self,
					"multiValued_", c -> c.aggregable( Aggregable.YES ) );
		}
	}

	private static class IndexBinding extends AbstractObjectBinding {
		final FirstLevelObjectBinding flattenedObject;
		final FirstLevelObjectBinding nestedObject;

		IndexBinding(IndexSchemaElement root) {
			super( root );
			flattenedObject = FirstLevelObjectBinding.create( root, "flattenedObject", ObjectFieldStorage.FLATTENED );
			nestedObject = FirstLevelObjectBinding.create( root, "nestedObject", ObjectFieldStorage.NESTED );
		}
	}

	private static class FirstLevelObjectBinding extends AbstractObjectBinding {
		final String relativeFieldName;
		final IndexObjectFieldReference self;

		final SecondLevelObjectBinding nestedObject;

		public static FirstLevelObjectBinding create(IndexSchemaElement parent, String relativeFieldName,
				ObjectFieldStorage storage) {
			return create( parent, relativeFieldName, storage, false );
		}

		public static FirstLevelObjectBinding create(IndexSchemaElement parent, String relativeFieldName,
				ObjectFieldStorage storage,
				boolean multiValued) {
			IndexSchemaObjectField objectField = parent.objectField( relativeFieldName, storage );
			if ( multiValued ) {
				objectField.multiValued();
			}
			return new FirstLevelObjectBinding( relativeFieldName, objectField );
		}

		FirstLevelObjectBinding(String relativeFieldName, IndexSchemaObjectField objectField) {
			super( objectField );
			this.relativeFieldName = relativeFieldName;
			self = objectField.toReference();
			nestedObject = SecondLevelObjectBinding.create(
					objectField, "nestedObject", ObjectFieldStorage.NESTED
			);
		}
	}

	private static class SecondLevelObjectBinding extends AbstractObjectBinding {
		final String relativeFieldName;
		final IndexObjectFieldReference self;

		public static SecondLevelObjectBinding create(IndexSchemaElement parent, String relativeFieldName,
				ObjectFieldStorage storage) {
			IndexSchemaObjectField objectField = parent.objectField( relativeFieldName, storage );
			return new SecondLevelObjectBinding( relativeFieldName, objectField );
		}

		SecondLevelObjectBinding(String relativeFieldName, IndexSchemaObjectField objectField) {
			super( objectField );
			this.relativeFieldName = relativeFieldName;
			self = objectField.toReference();
		}
	}

}
