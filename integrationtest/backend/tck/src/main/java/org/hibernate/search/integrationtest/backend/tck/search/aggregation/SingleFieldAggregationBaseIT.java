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
	private static final SimpleMappedIndex<IndexBinding> emptyIndex =
			SimpleMappedIndex.of( "Empty", IndexBinding::new );
	private static final SimpleMappedIndex<IndexBinding> nullOnlyIndex =
			SimpleMappedIndex.of( "NullOnly", IndexBinding::new );
	private static final SimpleMappedIndex<MultiValuedIndexBinding> multiValuedIndex =
			SimpleMappedIndex.of( "MultiValued", MultiValuedIndexBinding::new );

	@BeforeClass
	public static void setup() {
		setupHelper.start().withIndexes( mainIndex, emptyIndex, nullOnlyIndex, multiValuedIndex ).setup();

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
	public void inFlattenedObject() {
		String fieldPath = mainIndex.binding().fieldModels.get( fieldType ).relativeFieldName;

		AggregationScenario<?> scenario = expectations.simple();
		testValidAggregation(
				scenario, mainIndex.createScope(), fieldPath
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
			List<List<F>> multiValuedIndexDocumentFieldValues = expectations.getMultiValuedIndexDocumentFieldValues();

			IndexIndexingPlan<?> plan = mainIndex.createIndexingPlan();
			for ( int i = 0; i < mainIndexDocumentFieldValues.size(); i++ ) {
				F value = mainIndexDocumentFieldValues.get( i );
				plan.add( referenceProvider( name + "_document_" + i, name ), document -> {
					document.addValue( mainIndex.binding().fieldModels.get( fieldType ).reference, value );

					// Note: this object must be single-valued for these tests
					DocumentElement nestedObject = document.addObject( mainIndex.binding().nestedObject.self );
					nestedObject.addValue( mainIndex.binding().nestedObject.fieldModels.get( fieldType ).reference, value );
				} );
			}
			plan.add( referenceProvider( name + "_document_empty", name ), document -> { } );
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
			SearchResultAssert.assertThat( multiValuedIndex.createScope().query()
					.where( f -> f.matchAll() )
					.routing( name )
					.toQuery() )
					.hasTotalHitCount( multiValuedIndexDocumentFieldValues.size() + 1 /* +1 for the empty document */ );
		}
	}

	private static class IndexBinding {
		final SimpleFieldModelsByType fieldModels;

		final ObjectBinding nestedObject;

		IndexBinding(IndexSchemaElement root) {
			fieldModels = SimpleFieldModelsByType.mapAll( supportedFieldTypes, root,
					"", c -> c.aggregable( Aggregable.YES ) );

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

	private static class MultiValuedIndexBinding {
		final SimpleFieldModelsByType fieldModels;

		MultiValuedIndexBinding(IndexSchemaElement root) {
			fieldModels = SimpleFieldModelsByType.mapAllMultiValued( supportedFieldTypes, root,
					"", c -> c.aggregable( Aggregable.YES ) );
		}
	}

}
