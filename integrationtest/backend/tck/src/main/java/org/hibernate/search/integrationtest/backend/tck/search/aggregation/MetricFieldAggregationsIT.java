/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.backend.tck.search.aggregation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.types.Aggregable;
import org.hibernate.search.engine.search.common.ValueModel;
import org.hibernate.search.integrationtest.backend.tck.testsupport.model.singlefield.SingleFieldIndexBinding;
import org.hibernate.search.integrationtest.backend.tck.testsupport.operations.MetricAggregationsTestCase;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.FieldTypeDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.StandardFieldTypeDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.TckConfiguration;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.TypeAssertionHelper;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.extension.SearchSetupHelper;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.BulkIndexer;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.SimpleMappedIndex;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappingScope;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class MetricFieldAggregationsIT<F> {

	private static final Set<StandardFieldTypeDescriptor<?>> supportedFieldTypes = new LinkedHashSet<>();
	private static final List<MetricAggregationsTestCase<?>> testCases = new ArrayList<>();
	private static final List<Arguments> parameters = new ArrayList<>();

	static {
		for ( StandardFieldTypeDescriptor<?> typeDescriptor : FieldTypeDescriptor.getAllStandard() ) {
			if ( !typeDescriptor.supportsMetricAggregation() ) {
				continue;
			}
			MetricAggregationsTestCase<?> scenario = new MetricAggregationsTestCase<>( typeDescriptor );
			testCases.add( scenario );
			supportedFieldTypes.add( typeDescriptor );
			parameters.add( Arguments.of( scenario ) );
		}
	}

	public static List<? extends Arguments> params() {
		return parameters;
	}

	@RegisterExtension
	public static final SearchSetupHelper setupHelper = SearchSetupHelper.create();

	private static final Function<IndexSchemaElement, SingleFieldIndexBinding> bindingFactory =
			root -> SingleFieldIndexBinding.create( root, supportedFieldTypes, c -> c.aggregable( Aggregable.YES ) );
	private static final SimpleMappedIndex<SingleFieldIndexBinding> mainIndex =
			SimpleMappedIndex.of( bindingFactory ).name( "main" );

	@BeforeAll
	static void setup() {
		int expectedDocuments = 0;

		setupHelper.start().withIndexes( mainIndex ).setup();
		BulkIndexer indexer = mainIndex.bulkIndexer();
		for ( MetricAggregationsTestCase<?> scenario : testCases ) {
			expectedDocuments += scenario.contribute( indexer, mainIndex.binding() );
		}
		indexer.join();

		long createdDocuments = mainIndex.createScope().query().where( f -> f.matchAll() )
				.totalHitCountThreshold( expectedDocuments )
				.toQuery().fetch( 0 ).total().hitCountLowerBound();
		assertThat( createdDocuments ).isEqualTo( expectedDocuments );
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("params")
	void test(MetricAggregationsTestCase<F> testCase) {
		StubMappingScope scope = mainIndex.createScope();
		MetricAggregationsTestCase<?>.Result<?> result = testCase.testMetricsAggregation( scope, mainIndex.binding(),
				ValueModel.MAPPING, TypeAssertionHelper.identity( testCase.typeDescriptor() )
		);

		result.validate();
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("params")
	void testString(MetricAggregationsTestCase<F> testCase) {
		StubMappingScope scope = mainIndex.createScope();
		MetricAggregationsTestCase<?>.Result<?> result = testCase.testMetricsAggregation( scope, mainIndex.binding(),
				ValueModel.STRING, TypeAssertionHelper.string( testCase.typeDescriptor() )
		);
		result.validate();
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("params")
	void testRaw(MetricAggregationsTestCase<F> testCase) {
		StubMappingScope scope = mainIndex.createScope();
		MetricAggregationsTestCase<?>.Result<?> result = testCase.testMetricsAggregation( scope, mainIndex.binding(),
				ValueModel.RAW, TypeAssertionHelper.raw( testCase.typeDescriptor() )
		);
		result.validate();
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("params")
	void testRawDouble(MetricAggregationsTestCase<F> testCase) {
		assumeTrue(
				TckConfiguration.get().getBackendFeatures()
						.rawAggregationProduceSensibleDoubleValue( testCase.typeDescriptor() ),
				"Some date-time types with Elasticsearch backends can produce some 'garbage' double values, but for those we usually rely on the value_as_string anyway." );
		StubMappingScope scope = mainIndex.createScope();
		MetricAggregationsTestCase<?>.Result<?> result = testCase.testMetricsAggregation( scope, mainIndex.binding(),
				ValueModel.RAW, TypeAssertionHelper.rawDouble( testCase.typeDescriptor() )
		);
		result.validateDouble();
	}
}
