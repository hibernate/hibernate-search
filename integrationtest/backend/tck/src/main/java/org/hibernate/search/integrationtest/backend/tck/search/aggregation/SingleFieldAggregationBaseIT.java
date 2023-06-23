/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.search.aggregation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.search.util.impl.integrationtest.common.assertion.SearchResultAssert.assertThatQuery;
import static org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMapperUtils.documentProvider;
import static org.junit.Assume.assumeTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.types.Aggregable;
import org.hibernate.search.engine.search.aggregation.AggregationKey;
import org.hibernate.search.engine.search.aggregation.SearchAggregation;
import org.hibernate.search.engine.search.aggregation.dsl.AggregationFinalStep;
import org.hibernate.search.engine.search.aggregation.dsl.SearchAggregationFactory;
import org.hibernate.search.engine.search.predicate.dsl.PredicateFinalStep;
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory;
import org.hibernate.search.integrationtest.backend.tck.testsupport.model.singlefield.AbstractObjectBinding;
import org.hibernate.search.integrationtest.backend.tck.testsupport.model.singlefield.SingleFieldIndexBinding;
import org.hibernate.search.integrationtest.backend.tck.testsupport.operations.AggregationDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.operations.expectations.AggregationScenario;
import org.hibernate.search.integrationtest.backend.tck.testsupport.operations.expectations.SupportedSingleFieldAggregationExpectations;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.FieldTypeDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.TestedFieldStructure;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.BulkIndexer;
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

	@Parameterized.Parameters(name = "{0} - {1}")
	public static Object[][] parameters() {
		supportedFieldTypes = new LinkedHashSet<>();
		dataSets = new ArrayList<>();
		List<Object[]> parameters = new ArrayList<>();
		for ( AggregationDescriptor aggregationDescriptor : AggregationDescriptor.getAll() ) {
			for ( FieldTypeDescriptor<?> fieldTypeDescriptor : FieldTypeDescriptor.getAll() ) {
				Optional<? extends SupportedSingleFieldAggregationExpectations<?>> expectations =
						aggregationDescriptor.getSingleFieldAggregationExpectations( fieldTypeDescriptor ).getSupported();
				if ( expectations.isPresent() ) {
					for ( TestedFieldStructure fieldStructure : TestedFieldStructure.all() ) {
						supportedFieldTypes.add( fieldTypeDescriptor );
						DataSet<?> dataSet = new DataSet<>( expectations.get(), fieldStructure );
						dataSets.add( dataSet );
						parameters.add( new Object[] { expectations.get(), fieldStructure, dataSet } );
					}
				}
			}
		}
		return parameters.toArray( new Object[0][] );
	}

	@ClassRule
	public static final SearchSetupHelper setupHelper = new SearchSetupHelper();

	private static final Function<IndexSchemaElement, SingleFieldIndexBinding> bindingFactory =
			root -> SingleFieldIndexBinding.create( root, supportedFieldTypes, c -> c.aggregable( Aggregable.YES ) );

	private static final SimpleMappedIndex<SingleFieldIndexBinding> mainIndex =
			SimpleMappedIndex.of( bindingFactory ).name( "Main" );

	private static final SimpleMappedIndex<SingleFieldIndexBinding> emptyIndex =
			SimpleMappedIndex.of( bindingFactory ).name( "Empty" );
	private static final SimpleMappedIndex<SingleFieldIndexBinding> nullOnlyIndex =
			SimpleMappedIndex.of( bindingFactory ).name( "NullOnly" );

	@BeforeClass
	public static void setup() {
		setupHelper.start().withIndexes( mainIndex, emptyIndex, nullOnlyIndex ).setup();

		BulkIndexer mainIndexer = mainIndex.bulkIndexer();
		BulkIndexer nullOnlyIndexer = nullOnlyIndex.bulkIndexer();
		for ( DataSet<?> dataSet : dataSets ) {
			dataSet.contribute( mainIndexer, nullOnlyIndexer );
		}

		mainIndexer.join( nullOnlyIndexer );
	}

	private final SupportedSingleFieldAggregationExpectations<F> expectations;
	private final FieldTypeDescriptor<F> fieldType;
	private final TestedFieldStructure fieldStructure;
	private final DataSet<F> dataSet;

	public SingleFieldAggregationBaseIT(SupportedSingleFieldAggregationExpectations<F> expectations,
			TestedFieldStructure fieldStructure, DataSet<F> dataSet) {
		this.expectations = expectations;
		this.fieldType = expectations.fieldType();
		this.fieldStructure = fieldStructure;
		this.dataSet = dataSet;
	}

	@Test
	@TestForIssue(jiraKey = {
			"HSEARCH-726",
			"HSEARCH-900",
			"HSEARCH-809",
			"HSEARCH-2376",
			"HSEARCH-2472",
			"HSEARCH-2954",
			"HSEARCH-2535",
			"HSEARCH-1927",
			"HSEARCH-1929",
			"HSEARCH-3881"
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
		testValidAggregation( getSimpleScenario(), mainIndex.createScope(),
				getFieldPath( mainIndex.binding() ), getFilterOrNull( mainIndex.binding() ) );
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

		SearchAggregation<A> aggregation =
				scenario.setup( scope.aggregation(), fieldPath, getFilterOrNull( mainIndex.binding() ) )
						.toAggregation();

		assertThatQuery(
				mainIndex.createScope().query()
						.where( f -> f.matchAll() )
						.aggregation( aggregationKey, aggregation )
						.routing( dataSet.routingKey )
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
				(f, e) -> e.setup( f, fieldPath, getFilterOrNull( mainIndex.binding() ) )
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
				scenario, emptyIndex.createScope(), fieldPath, null
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
				scenario, nullOnlyIndex.createScope(), fieldPath, null
		);
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-4162")
	public void factoryWithRoot() {
		AbstractObjectBinding parentObjectBinding = mainIndex.binding().getParentObject( fieldStructure );

		assumeTrue( "This test is only relevant when the field is located on an object field",
				parentObjectBinding.absolutePath != null );

		testValidAggregation(
				getSimpleScenario(), mainIndex.createScope(),
				f -> f.matchAll(),
				(f, e) -> e.setup(
						f.withRoot( parentObjectBinding.absolutePath ),
						parentObjectBinding.getRelativeFieldName( fieldStructure, fieldType ),
						getFilterOrNull( AbstractObjectBinding.DISCRIMINATOR_FIELD_NAME )
				)
		);
	}

	private <A> void testValidAggregation(AggregationScenario<A> scenario, StubMappingScope scope,
			String fieldPath, Function<? super SearchPredicateFactory, ? extends PredicateFinalStep> filterOrNull) {
		testValidAggregation(
				scenario, scope,
				f -> f.matchAll(),
				(f, e) -> e.setup( f, fieldPath, filterOrNull )
		);
	}

	private <A> void testValidAggregation(AggregationScenario<A> scenario, StubMappingScope scope,
			Function<SearchPredicateFactory, ? extends PredicateFinalStep> predicateContributor,
			BiFunction<SearchAggregationFactory, AggregationScenario<A>, AggregationFinalStep<A>> aggregationContributor) {
		AggregationKey<A> aggregationKey = AggregationKey.of( AGGREGATION_NAME );
		assertThatQuery(
				scope.query()
						.where( predicateContributor )
						.aggregation( aggregationKey, f -> aggregationContributor.apply( f, scenario ) )
						.routing( dataSet.routingKey )
						.toQuery()
		)
				.aggregation(
						aggregationKey,
						a -> assertThat( a ).isNotNull().satisfies( scenario::check )
				);
	}

	private AggregationScenario<?> getSimpleScenario() {
		AggregationScenario<?> scenario;
		if ( fieldStructure.isSingleValued() ) {
			scenario = expectations.simple();
		}
		else {
			scenario = expectations.onMultiValuedIndex();
		}
		return scenario;
	}

	private String getFieldPath(SingleFieldIndexBinding indexBinding) {
		return indexBinding.getFieldPath( fieldStructure, fieldType );
	}

	private Function<? super SearchPredicateFactory, ? extends PredicateFinalStep> getFilterOrNull(
			SingleFieldIndexBinding binding) {
		return getFilterOrNull( binding.getDiscriminatorFieldPath( fieldStructure ) );
	}

	private Function<? super SearchPredicateFactory, ? extends PredicateFinalStep> getFilterOrNull(
			String discriminatorPath) {
		if ( fieldStructure.isInNested() ) {
			return pf -> pf.match()
					.field( discriminatorPath )
					.matching( SingleFieldIndexBinding.DISCRIMINATOR_VALUE_INCLUDED );
		}
		else {
			return null;
		}
	}

	private static class DataSet<F> {
		final SupportedSingleFieldAggregationExpectations<F> expectations;
		final FieldTypeDescriptor<F> fieldType;
		final String routingKey;
		private final TestedFieldStructure fieldStructure;

		private DataSet(SupportedSingleFieldAggregationExpectations<F> expectations,
				TestedFieldStructure fieldStructure) {
			this.expectations = expectations;
			this.fieldType = expectations.fieldType();
			this.routingKey = expectations.aggregationName() + "_" + expectations.fieldType().getUniqueName()
					+ "_" + fieldStructure.getUniqueName();
			this.fieldStructure = fieldStructure;
		}

		private void contribute(BulkIndexer mainIndexer, BulkIndexer nullOnlyIndexer) {
			if ( fieldStructure.isSingleValued() ) {
				List<F> values = expectations.getMainIndexDocumentFieldValues();
				for ( int i = 0; i < values.size(); i++ ) {
					F valueForDocument = values.get( i );
					F garbageValueForDocument = values.get( i == 0 ? 1 : i - 1 );
					mainIndexer.add(
							documentProvider( routingKey + "_document_" + i, routingKey, document -> {
								mainIndex.binding().initSingleValued( fieldType, fieldStructure.location,
										document, valueForDocument, garbageValueForDocument );
							} )
					);
				}
			}
			else {
				List<List<F>> values = expectations.getMultiValuedIndexDocumentFieldValues();
				for ( int i = 0; i < values.size(); i++ ) {
					List<F> valuesForDocument = values.get( i );
					List<F> garbageValuesForDocument = values.get( i == 0 ? 1 : i - 1 );
					mainIndexer.add(
							documentProvider( routingKey + "_document_" + i, routingKey, document -> {
								mainIndex.binding().initMultiValued( fieldType, fieldStructure.location,
										document, valuesForDocument, garbageValuesForDocument );
							} )
					);
				}
			}
			mainIndexer.add(
					documentProvider( routingKey + "_document_empty", routingKey, document -> {} )
			);

			nullOnlyIndexer.add(
					documentProvider( routingKey + "_nullOnlyIndex_document_0", routingKey, document -> {
						if ( fieldStructure.isSingleValued() ) {
							nullOnlyIndex.binding().initSingleValued( fieldType, fieldStructure.location,
									document, null, null );
						}
						else {
							nullOnlyIndex.binding().initMultiValued( fieldType, fieldStructure.location,
									document, Arrays.asList( null, null ), Arrays.asList( null, null ) );
						}
					} )
			);
		}
	}

}
