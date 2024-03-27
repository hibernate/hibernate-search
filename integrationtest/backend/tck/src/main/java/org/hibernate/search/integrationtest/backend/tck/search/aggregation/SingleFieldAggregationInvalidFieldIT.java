/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.backend.tck.search.aggregation;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.hibernate.search.engine.backend.document.IndexObjectFieldReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaObjectField;
import org.hibernate.search.engine.backend.types.ObjectStructure;
import org.hibernate.search.integrationtest.backend.tck.testsupport.operations.AggregationDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.operations.expectations.AggregationScenario;
import org.hibernate.search.integrationtest.backend.tck.testsupport.operations.expectations.SupportedSingleFieldAggregationExpectations;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.IntegerFieldTypeDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.TypeAssertionHelper;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.extension.SearchSetupHelper;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.SimpleMappedIndex;
import org.hibernate.search.util.impl.test.annotation.PortedFromSearch5;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Tests behavior of all single-field aggregations (range, terms, ...)
 * when invalid fields are targeted.
 */

class SingleFieldAggregationInvalidFieldIT<F> {

	private static final IntegerFieldTypeDescriptor FIELD_TYPE = IntegerFieldTypeDescriptor.INSTANCE;

	public static List<? extends Arguments> params() {
		List<Arguments> parameters = new ArrayList<>();
		for ( AggregationDescriptor aggregationDescriptor : AggregationDescriptor.getAll() ) {
			Optional<? extends SupportedSingleFieldAggregationExpectations<?>> expectations =
					aggregationDescriptor.getSingleFieldAggregationExpectations( FIELD_TYPE ).getSupported();
			parameters.add( Arguments.of( expectations.get() ) );
		}
		return parameters;
	}

	@RegisterExtension
	public static final SearchSetupHelper setupHelper = SearchSetupHelper.create();

	private static final SimpleMappedIndex<IndexBinding> index = SimpleMappedIndex.of( IndexBinding::new );

	@BeforeAll
	static void setup() {
		setupHelper.start().withIndex( index ).setup();
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("params")
	@PortedFromSearch5(
			original = "org.hibernate.search.test.query.facet.FacetUnknownFieldFailureTest.testUnknownFieldNameThrowsException")
	void unknownField(SupportedSingleFieldAggregationExpectations<F> expectations) {
		String fieldPath = "unknownField";

		AggregationScenario<?> scenario = expectations.simple();

		assertThatThrownBy( () -> scenario.setup( index.createScope().aggregation(), fieldPath ) )
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "Unknown field" )
				.hasMessageContaining( "'" + fieldPath + "'" )
				.hasMessageContaining( index.name() );
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("params")
	@PortedFromSearch5(original = "org.hibernate.search.test.query.facet.SimpleFacetingTest.testNullFieldNameThrowsException")
	void nullFieldPath(SupportedSingleFieldAggregationExpectations<F> expectations) {
		// Try to pass a "null" field type
		AggregationScenario<?> scenario = expectations.simple();

		assertThatThrownBy( () -> scenario.setup( index.createScope().aggregation(), null ) )
				.isInstanceOf( IllegalArgumentException.class )
				.hasMessageContaining( "'fieldPath'" )
				.hasMessageContaining( "must not be null" );
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("params")
	void objectField_nested(SupportedSingleFieldAggregationExpectations<F> expectations) {
		String fieldPath = index.binding().nestedObject.relativeFieldName;

		AggregationScenario<?> scenario =
				expectations.withFieldType( TypeAssertionHelper.identity( expectations.fieldType() ) );

		assertThatThrownBy( () -> scenario.setup( index.createScope().aggregation(), fieldPath ) )
				.isInstanceOf( SearchException.class )
				.hasMessageContaining(
						"Cannot use 'aggregation:" + expectations.aggregationName() + "' on field '" + fieldPath + "'" );
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("params")
	void objectField_flattened(SupportedSingleFieldAggregationExpectations<F> expectations) {
		String fieldPath = index.binding().flattenedObject.relativeFieldName;

		AggregationScenario<?> scenario =
				expectations.withFieldType( TypeAssertionHelper.identity( expectations.fieldType() ) );

		assertThatThrownBy( () -> scenario.setup( index.createScope().aggregation(), fieldPath ) )
				.isInstanceOf( SearchException.class )
				.hasMessageContaining(
						"Cannot use 'aggregation:" + expectations.aggregationName() + "' on field '" + fieldPath + "'" );
	}

	private static class IndexBinding {
		final ObjectMapping flattenedObject;
		final ObjectMapping nestedObject;

		IndexBinding(IndexSchemaElement root) {
			flattenedObject = new ObjectMapping( root, "flattenedObject", ObjectStructure.FLATTENED );
			nestedObject = new ObjectMapping( root, "nestedObject", ObjectStructure.NESTED );
		}
	}

	private static class ObjectMapping {
		final String relativeFieldName;
		final IndexObjectFieldReference self;

		ObjectMapping(IndexSchemaElement parent, String relativeFieldName, ObjectStructure structure) {
			this.relativeFieldName = relativeFieldName;
			IndexSchemaObjectField objectField = parent.objectField( relativeFieldName, structure );
			self = objectField.toReference();
		}
	}
}
