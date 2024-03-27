/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.backend.tck.search.aggregation;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.types.dsl.SearchableProjectableIndexFieldTypeOptionsStep;
import org.hibernate.search.engine.reporting.spi.EventContexts;
import org.hibernate.search.integrationtest.backend.tck.testsupport.operations.AggregationDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.operations.expectations.UnsupportedSingleFieldAggregationExpectations;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.FieldTypeDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.SimpleFieldModel;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.SimpleFieldModelsByType;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.extension.SearchSetupHelper;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.common.reporting.FailureReportUtils;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.SimpleMappedIndex;
import org.hibernate.search.util.impl.test.annotation.PortedFromSearch5;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Tests behavior common to all single-field aggregations (range, terms, ...)
 * on unsupported types.
 */

class SingleFieldAggregationUnsupportedTypesIT<F> {

	private static final Set<
			FieldTypeDescriptor<?, ? extends SearchableProjectableIndexFieldTypeOptionsStep<?, ?>>> unsupportedFieldTypes =
					new LinkedHashSet<>();
	private static final List<Arguments> parameters = new ArrayList<>();

	static {
		for ( AggregationDescriptor aggregationDescriptor : AggregationDescriptor.getAll() ) {
			for ( FieldTypeDescriptor<?,
					? extends SearchableProjectableIndexFieldTypeOptionsStep<?, ?>> fieldTypeDescriptor : FieldTypeDescriptor
							.getAll() ) {
				Optional<? extends UnsupportedSingleFieldAggregationExpectations> expectations =
						aggregationDescriptor.getSingleFieldAggregationExpectations( fieldTypeDescriptor ).getUnsupported();
				if ( expectations.isPresent() ) {
					unsupportedFieldTypes.add( fieldTypeDescriptor );
					parameters.add( Arguments.of( fieldTypeDescriptor, expectations.get() ) );
				}
			}
		}
	}

	public static List<? extends Arguments> params() {
		return parameters;
	}

	@RegisterExtension
	public static final SearchSetupHelper setupHelper = SearchSetupHelper.create();

	private static final SimpleMappedIndex<IndexBinding> index = SimpleMappedIndex.of( IndexBinding::new );

	@BeforeAll
	static void setup() {
		setupHelper.start().withIndex( index ).setup();
	}

	@ParameterizedTest(name = "{1}")
	@MethodSource("params")
	@TestForIssue(jiraKey = "HSEARCH-1748")
	@PortedFromSearch5(original = "org.hibernate.search.test.query.facet.RangeFacetingTest.testRangeQueryWithUnsupportedType")
	void simple(FieldTypeDescriptor<F, ?> fieldType, UnsupportedSingleFieldAggregationExpectations expectations) {
		SimpleFieldModel<F> model = index.binding().fieldModels.get( fieldType );
		String fieldPath = model.relativeFieldName;

		assertThatThrownBy(
				() -> expectations.trySetup( index.createScope().aggregation(), fieldPath )
		)
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll(
						"Cannot use 'aggregation:" + expectations.aggregationName() + "' on field '" + fieldPath + "'",
						"'aggregation:" + expectations.aggregationName()
								+ "' is not available for fields of this type"
				)
				.satisfies( FailureReportUtils.hasContext(
						EventContexts.fromIndexFieldAbsolutePath( fieldPath )
				) );
	}

	private static class IndexBinding {
		final SimpleFieldModelsByType fieldModels;

		IndexBinding(IndexSchemaElement root) {
			fieldModels = SimpleFieldModelsByType.mapAll( unsupportedFieldTypes, root, "" );
		}
	}
}
