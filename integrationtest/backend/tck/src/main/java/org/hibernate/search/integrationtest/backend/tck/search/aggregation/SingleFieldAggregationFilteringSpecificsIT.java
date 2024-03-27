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
import java.util.function.Function;

import org.hibernate.search.engine.backend.document.IndexObjectFieldReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaObjectField;
import org.hibernate.search.engine.backend.types.Aggregable;
import org.hibernate.search.engine.backend.types.ObjectStructure;
import org.hibernate.search.integrationtest.backend.tck.testsupport.operations.AggregationDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.operations.expectations.AggregationScenario;
import org.hibernate.search.integrationtest.backend.tck.testsupport.operations.expectations.SupportedSingleFieldAggregationExpectations;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.FieldTypeDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.StandardFieldTypeDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.SimpleFieldModelsByType;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.extension.SearchSetupHelper;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.SimpleMappedIndex;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappingScope;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Tests behavior related to
 * {@link org.hibernate.search.engine.search.aggregation.dsl.AggregationFilterStep#filter(Function) filtering}
 * that is not tested in {@link SingleFieldAggregationBaseIT}.
 */

class SingleFieldAggregationFilteringSpecificsIT<F> {

	private static final Set<StandardFieldTypeDescriptor<?>> supportedFieldTypes = new LinkedHashSet<>();
	private static final List<Arguments> parameters = new ArrayList<>();

	static {
		for ( AggregationDescriptor aggregationDescriptor : AggregationDescriptor.getAll() ) {
			for ( StandardFieldTypeDescriptor<?> fieldType : FieldTypeDescriptor.getAllStandard() ) {
				Optional<? extends SupportedSingleFieldAggregationExpectations<?>> expectations =
						aggregationDescriptor.getSingleFieldAggregationExpectations( fieldType ).getSupported();
				if ( expectations.isPresent() ) {
					supportedFieldTypes.add( fieldType );
					parameters.add( Arguments.of( expectations.get() ) );
				}
			}
		}
	}

	public static List<? extends Arguments> params() {
		return parameters;
	}

	@RegisterExtension
	public static final SearchSetupHelper setupHelper = SearchSetupHelper.create();

	private static final SimpleMappedIndex<IndexBinding> mainIndex = SimpleMappedIndex.of( IndexBinding::new );

	@BeforeAll
	static void setup() {
		setupHelper.start().withIndexes( mainIndex ).setup();
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("params")
	@TestForIssue(jiraKey = "HSEARCH-3881")
	void nonNested(SupportedSingleFieldAggregationExpectations<F> expectations) {
		StubMappingScope scope = mainIndex.createScope();
		AggregationScenario<?> scenario = expectations.simple();
		String fieldPath = mainIndex.binding().flattenedObject.relativeFieldName + "."
				+ mainIndex.binding().flattenedObject.fieldModels.get( expectations.fieldType() ).relativeFieldName;

		assertThatThrownBy(
				() -> scenario.setup( scope.aggregation(), fieldPath, pf -> pf.exists().field( fieldPath ) )
		)
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll(
						"Invalid aggregation filter: field '" + fieldPath + "' is not contained in a nested object.",
						"Aggregation filters are only available if the field to aggregate on is contained in a nested object."
				);
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("params")
	@TestForIssue(jiraKey = "HSEARCH-3881")
	void invalidNestedPath_parent(SupportedSingleFieldAggregationExpectations<F> expectations) {
		StubMappingScope scope = mainIndex.createScope();
		AggregationScenario<?> scenario = expectations.simple();
		String fieldPath = mainIndex.binding().nestedObject1.relativeFieldName + "."
				+ mainIndex.binding().nestedObject1.fieldModels.get( expectations.fieldType() ).relativeFieldName;
		String fieldInParentPath = mainIndex.binding().fieldModels.get( expectations.fieldType() ).relativeFieldName;

		assertThatThrownBy(
				() -> scenario.setup( scope.aggregation(), fieldPath, pf -> pf.exists().field( fieldInParentPath ) )
		)
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll( "Invalid search predicate",
						"This predicate targets fields [" + fieldInParentPath + "]",
						"only fields that are contained in the nested object with path '"
								+ mainIndex.binding().nestedObject1.relativeFieldName + "'"
								+ " are allowed here." );
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("params")
	@TestForIssue(jiraKey = "HSEARCH-3881")
	void invalidNestedPath_sibling(SupportedSingleFieldAggregationExpectations<F> expectations) {
		StubMappingScope scope = mainIndex.createScope();
		AggregationScenario<?> scenario = expectations.simple();
		String fieldPath = mainIndex.binding().nestedObject1.relativeFieldName + "."
				+ mainIndex.binding().nestedObject1.fieldModels.get( expectations.fieldType() ).relativeFieldName;
		String fieldInSiblingPath = mainIndex.binding().nestedObject2.relativeFieldName + "."
				+ mainIndex.binding().nestedObject2.fieldModels.get( expectations.fieldType() ).relativeFieldName;

		assertThatThrownBy(
				() -> scenario.setup( scope.aggregation(), fieldPath, pf -> pf.exists().field( fieldInSiblingPath ) )
		)
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll( "Invalid search predicate",
						"This predicate targets fields [" + fieldInSiblingPath + "]",
						"only fields that are contained in the nested object with path '"
								+ mainIndex.binding().nestedObject1.relativeFieldName + "'"
								+ " are allowed here." );
	}

	private static class AbstractObjectBinding {
		final SimpleFieldModelsByType fieldModels;

		AbstractObjectBinding(IndexSchemaElement self) {
			fieldModels = SimpleFieldModelsByType.mapAll( supportedFieldTypes, self,
					"", c -> c.aggregable( Aggregable.YES ) );
		}
	}

	private static class IndexBinding extends AbstractObjectBinding {
		final FirstLevelObjectBinding flattenedObject;
		final FirstLevelObjectBinding nestedObject1;
		final FirstLevelObjectBinding nestedObject2;

		IndexBinding(IndexSchemaElement root) {
			super( root );
			flattenedObject = FirstLevelObjectBinding.create( root, "flattenedObject", ObjectStructure.FLATTENED );
			nestedObject1 = FirstLevelObjectBinding.create( root, "nestedObject1",
					ObjectStructure.NESTED );
			nestedObject2 = FirstLevelObjectBinding.create( root, "nestedObject2",
					ObjectStructure.NESTED );
		}
	}

	private static class FirstLevelObjectBinding extends AbstractObjectBinding {
		final String relativeFieldName;
		final IndexObjectFieldReference self;

		public static FirstLevelObjectBinding create(IndexSchemaElement parent, String relativeFieldName,
				ObjectStructure structure) {
			IndexSchemaObjectField objectField = parent.objectField( relativeFieldName, structure );
			return new FirstLevelObjectBinding( relativeFieldName, objectField );
		}

		FirstLevelObjectBinding(String relativeFieldName, IndexSchemaObjectField objectField) {
			super( objectField );
			this.relativeFieldName = relativeFieldName;
			self = objectField.toReference();
		}
	}
}
