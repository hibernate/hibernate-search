/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.backend.tck.metamodel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.entry;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.hibernate.search.engine.backend.document.IndexObjectFieldReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaObjectField;
import org.hibernate.search.engine.backend.metamodel.IndexCompositeElementDescriptor;
import org.hibernate.search.engine.backend.metamodel.IndexDescriptor;
import org.hibernate.search.engine.backend.metamodel.IndexFieldDescriptor;
import org.hibernate.search.engine.backend.metamodel.IndexObjectFieldDescriptor;
import org.hibernate.search.engine.backend.metamodel.IndexObjectFieldTypeDescriptor;
import org.hibernate.search.engine.backend.metamodel.IndexValueFieldDescriptor;
import org.hibernate.search.engine.backend.metamodel.IndexValueFieldTypeDescriptor;
import org.hibernate.search.engine.backend.types.ObjectStructure;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.IndexFieldLocation;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.TestedFieldStructure;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.extension.SearchSetupHelper;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.SimpleMappedIndex;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappingSchemaManagementStrategy;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import org.assertj.core.api.Assertions;

/**
 * Tests for field descriptor features.
 * <p>
 * Tests for type-related features are located in {@link IndexValueFieldTypeDescriptorBaseIT}
 * and {@link IndexObjectFieldTypeDescriptorBaseIT}.
 */
@TestForIssue(jiraKey = "HSEARCH-3589")

class IndexFieldDescriptorIT {

	public static List<? extends Arguments> params() {
		List<Arguments> parameters = new ArrayList<>();
		for ( TestedFieldStructure fieldStructure : TestedFieldStructure.all() ) {
			parameters.add( Arguments.of( fieldStructure ) );
		}
		return parameters;
	}

	@RegisterExtension
	public static final SearchSetupHelper setupHelper = SearchSetupHelper.create();

	private static final SimpleMappedIndex<IndexBinding> index = SimpleMappedIndex.of( IndexBinding::new );

	@BeforeAll
	static void setup() {
		setupHelper.start().withIndex( index )
				.withSchemaManagement( StubMappingSchemaManagementStrategy.NONE )
				.setup();
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("params")
	void valueField(TestedFieldStructure fieldStructure) {
		IndexDescriptor indexDescriptor = index.toApi().descriptor();
		IndexFieldDescriptor baseFieldDescriptor = indexDescriptor.field( getAbsoluteFieldPath( fieldStructure ) ).get();

		// Basic getters
		assertThat( baseFieldDescriptor )
				.returns( false, IndexFieldDescriptor::isObjectField )
				.returns( true, IndexFieldDescriptor::isValueField );

		IndexValueFieldDescriptor fieldDescriptor = baseFieldDescriptor.toValueField();
		assertThat( fieldDescriptor ).isSameAs( baseFieldDescriptor );
		assertThatThrownBy( fieldDescriptor::toObjectField )
				.isInstanceOf( SearchException.class )
				.hasMessageContaining(
						"Invalid type: field '" + getAbsoluteFieldPath( fieldStructure ) + "' is not an object field"
				);

		assertThat( fieldDescriptor )
				.returns( getRelativeFieldName( fieldStructure ), IndexFieldDescriptor::relativeName )
				.returns( getAbsoluteFieldPath( fieldStructure ), IndexFieldDescriptor::absolutePath )
				.returns( fieldStructure.isMultiValued(), IndexFieldDescriptor::multiValued )
				// In this specific test, we decided that nested fields are multi-valued, while flattened fields are not.
				// That's just a detail though: the two characteristics are not usually related.
				.returns( fieldStructure.isMultiValued() || fieldStructure.isInNested(),
						IndexFieldDescriptor::multiValuedInRoot );

		// Type
		// More advanced tests in IndexValueFieldTypeDescriptorIT
		IndexValueFieldTypeDescriptor type = fieldDescriptor.type();
		assertThat( type ).isNotNull();
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("params")
	void parent(TestedFieldStructure fieldStructure) {
		IndexDescriptor indexDescriptor = index.toApi().descriptor();
		IndexFieldDescriptor childFieldDescriptor = indexDescriptor.field( getAbsoluteFieldPath( fieldStructure ) ).get();

		IndexCompositeElementDescriptor elementDescriptor = childFieldDescriptor.parent();
		assertThat( elementDescriptor ).isNotNull();
		if ( IndexFieldLocation.ROOT.equals( fieldStructure.location ) ) {
			// The parent is not an object field, just the root.
			assertThat( elementDescriptor.isRoot() ).isTrue();
			assertThat( elementDescriptor.isObjectField() ).isFalse();
			assertThatThrownBy( elementDescriptor::toObjectField )
					.isInstanceOf( SearchException.class )
					.hasMessageContaining(
							"Invalid type: index schema root is not an object field"
					);
			assertThat( elementDescriptor ).isSameAs( indexDescriptor.root() );
			assertThat( elementDescriptor.staticChildren() )
					.extracting( IndexFieldDescriptor::absolutePath )
					.containsExactly( "flattenedObject", "myField", "myMultiValuedField", "nestedObject" );
			assertThat( elementDescriptor.staticChildrenByName() )
					.extractingFromEntries( e -> entry( e.getKey(), e.getValue().absolutePath() ) )
					.containsExactly(
							entry( "flattenedObject", "flattenedObject" ),
							entry( "myField", "myField" ),
							entry( "myMultiValuedField", "myMultiValuedField" ),
							entry( "nestedObject", "nestedObject" )
					);
			return;
		}

		assertThat( elementDescriptor.isRoot() ).isFalse();
		assertThat( elementDescriptor.isObjectField() ).isTrue();

		IndexObjectFieldDescriptor fieldDescriptor = elementDescriptor.toObjectField();
		assertThat( fieldDescriptor ).isSameAs( elementDescriptor );
		assertThatThrownBy( fieldDescriptor::toValueField )
				.isInstanceOf( SearchException.class )
				.hasMessageContaining(
						"Invalid type: field '" + getParentAbsoluteFieldPath( fieldStructure ) + "' is not a value field"
				);

		// Basic getters
		assertThat( fieldDescriptor )
				.returns( true, IndexFieldDescriptor::isObjectField )
				.returns( false, IndexFieldDescriptor::isValueField );
		assertThat( fieldDescriptor )
				.returns( getParentRelativeFieldName( fieldStructure ), IndexFieldDescriptor::relativeName )
				.returns( getParentAbsoluteFieldPath( fieldStructure ), IndexFieldDescriptor::absolutePath )
				// In this specific test, we decided that nested fields are multi-valued, while flattened fields are not.
				// That's just a detail though: the two characteristics are not usually related.
				.returns( fieldStructure.isInNested(), IndexFieldDescriptor::multiValued )
				.returns( fieldStructure.isInNested(), IndexFieldDescriptor::multiValuedInRoot );

		// Type
		// More advanced tests in IndexObjectFieldTypeDescriptorIT
		IndexObjectFieldTypeDescriptor type = fieldDescriptor.type();
		assertThat( type ).isNotNull();

		// Static children
		Collection<? extends IndexFieldDescriptor> children = fieldDescriptor.staticChildren();
		Map<String, ? extends IndexFieldDescriptor> childrenByName = fieldDescriptor.staticChildrenByName();
		Assertions.<IndexFieldDescriptor>assertThat( children ).contains( childFieldDescriptor );
		@SuppressWarnings("unchecked") // Workaround for assertThat(Map) not taking wildcard type into account like assertThat(Collection) does
		Map<String, IndexFieldDescriptor> castChildrenByName = (Map<String, IndexFieldDescriptor>) childrenByName;
		assertThat( castChildrenByName )
				.contains( entry( getRelativeFieldName( fieldStructure ), childFieldDescriptor ) );

		switch ( fieldStructure.location ) {
			case IN_FLATTENED:
				assertThat( children )
						.extracting( IndexFieldDescriptor::absolutePath )
						.containsExactly(
								"flattenedObject.myField",
								"flattenedObject.myMultiValuedField"
						);
				assertThat( childrenByName )
						.extractingFromEntries( e -> entry( e.getKey(), e.getValue().absolutePath() ) )
						.containsExactly(
								entry( "myField", "flattenedObject.myField" ),
								entry( "myMultiValuedField", "flattenedObject.myMultiValuedField" )
						);
				break;
			case IN_NESTED:
				assertThat( children )
						.extracting( IndexFieldDescriptor::absolutePath )
						.containsExactly(
								"nestedObject.myField",
								"nestedObject.myMultiValuedField",
								"nestedObject.nestedObject"
						);
				assertThat( childrenByName )
						.extractingFromEntries( e -> entry( e.getKey(), e.getValue().absolutePath() ) )
						.containsExactly(
								entry( "myField", "nestedObject.myField" ),
								entry( "myMultiValuedField", "nestedObject.myMultiValuedField" ),
								entry( "nestedObject", "nestedObject.nestedObject" )
						);
				break;
			case IN_NESTED_TWICE:
				assertThat( children )
						.extracting( IndexFieldDescriptor::absolutePath )
						.containsExactly(
								"nestedObject.nestedObject.myField",
								"nestedObject.nestedObject.myMultiValuedField"
						);
				assertThat( childrenByName )
						.extractingFromEntries( e -> entry( e.getKey(), e.getValue().absolutePath() ) )
						.containsExactly(
								entry( "myField", "nestedObject.nestedObject.myField" ),
								entry( "myMultiValuedField", "nestedObject.nestedObject.myMultiValuedField" )
						);
				break;
		}

		IndexCompositeElementDescriptor parentParentDescriptor = fieldDescriptor.parent();
		if ( IndexFieldLocation.IN_NESTED_TWICE.equals( fieldStructure.location ) ) {
			assertThat( parentParentDescriptor )
					.returns( false, IndexCompositeElementDescriptor::isRoot )
					.returns( true, IndexCompositeElementDescriptor::isObjectField );
			IndexObjectFieldDescriptor parentParentObjectFieldDescriptor = parentParentDescriptor.toObjectField();
			assertThat( parentParentObjectFieldDescriptor.parent() )
					.isSameAs( indexDescriptor.root() )
					.returns( true, IndexCompositeElementDescriptor::isRoot )
					.returns( false, IndexCompositeElementDescriptor::isObjectField );
		}
		else {
			assertThat( parentParentDescriptor )
					.isSameAs( indexDescriptor.root() )
					.returns( true, IndexCompositeElementDescriptor::isRoot )
					.returns( false, IndexCompositeElementDescriptor::isObjectField );
		}
	}

	private String getRelativeFieldName(TestedFieldStructure fieldStructure) {
		if ( fieldStructure.isSingleValued() ) {
			return "myField";
		}
		else {
			return "myMultiValuedField";
		}
	}

	private String getAbsoluteFieldPath(TestedFieldStructure fieldStructure) {
		String parentFieldPath = getParentAbsoluteFieldPath( fieldStructure );
		if ( parentFieldPath.isEmpty() ) {
			return getRelativeFieldName( fieldStructure );
		}
		else {
			return parentFieldPath + "." + getRelativeFieldName( fieldStructure );
		}
	}

	private String getParentAbsoluteFieldPath(TestedFieldStructure fieldStructure) {
		IndexBinding binding = index.binding();
		switch ( fieldStructure.location ) {
			case ROOT:
				return "";
			case IN_FLATTENED:
				return binding.flattenedObject.relativeFieldName;
			case IN_NESTED:
				return binding.nestedObject.relativeFieldName;
			case IN_NESTED_TWICE:
				return binding.nestedObject.relativeFieldName
						+ "." + binding.nestedObject.nestedObject.relativeFieldName;
			default:
				throw new IllegalStateException( "Unexpected value: " + fieldStructure.location );
		}
	}

	private String getParentRelativeFieldName(TestedFieldStructure fieldStructure) {
		IndexBinding binding = index.binding();
		switch ( fieldStructure.location ) {
			case ROOT:
				return "";
			case IN_FLATTENED:
				return binding.flattenedObject.relativeFieldName;
			case IN_NESTED:
				return binding.nestedObject.relativeFieldName;
			case IN_NESTED_TWICE:
				return binding.nestedObject.nestedObject.relativeFieldName;
			default:
				throw new IllegalStateException( "Unexpected value: " + fieldStructure.location );
		}
	}

	private abstract static class AbstractObjectBinding {
		protected AbstractObjectBinding(IndexSchemaElement root) {
			root.field( "myField", f -> f.asString() ).toReference();
			root.field( "myMultiValuedField", f -> f.asString() )
					.multiValued()
					.toReference();
		}
	}

	private static class IndexBinding extends AbstractObjectBinding {
		final FlattenedObjectBinding flattenedObject;
		final NestedObjectBinding nestedObject;

		IndexBinding(IndexSchemaElement root) {
			super( root );
			flattenedObject = FlattenedObjectBinding.create( root, "flattenedObject" );
			nestedObject = NestedObjectBinding.create( root, "nestedObject" );
		}
	}

	private static class FlattenedObjectBinding extends AbstractObjectBinding {
		public static FlattenedObjectBinding create(IndexSchemaElement parent, String relativeFieldName) {
			IndexSchemaObjectField objectField = parent.objectField( relativeFieldName, ObjectStructure.NESTED );
			return new FlattenedObjectBinding( objectField, relativeFieldName );
		}

		final String relativeFieldName;
		final IndexObjectFieldReference self;

		private FlattenedObjectBinding(IndexSchemaObjectField objectField, String relativeFieldName) {
			super( objectField );
			this.relativeFieldName = relativeFieldName;
			self = objectField.toReference();
		}
	}

	private static class NestedObjectBinding extends AbstractObjectBinding {
		public static NestedObjectBinding create(IndexSchemaElement parent, String relativeFieldName) {
			IndexSchemaObjectField objectField = parent.objectField( relativeFieldName, ObjectStructure.NESTED );
			objectField.multiValued();
			return new NestedObjectBinding( objectField, relativeFieldName );
		}

		final String relativeFieldName;
		final IndexObjectFieldReference self;

		final SecondLevelNestedObjectBinding nestedObject;

		private NestedObjectBinding(IndexSchemaObjectField objectField, String relativeFieldName) {
			super( objectField );
			this.relativeFieldName = relativeFieldName;
			self = objectField.toReference();
			nestedObject = SecondLevelNestedObjectBinding.create( objectField, "nestedObject" );
		}
	}

	private static class SecondLevelNestedObjectBinding extends AbstractObjectBinding {
		public static SecondLevelNestedObjectBinding create(IndexSchemaElement parent, String relativeFieldName) {
			IndexSchemaObjectField objectField = parent.objectField( relativeFieldName, ObjectStructure.NESTED );
			objectField.multiValued();
			return new SecondLevelNestedObjectBinding( objectField, relativeFieldName );
		}

		final String relativeFieldName;
		final IndexObjectFieldReference self;

		private SecondLevelNestedObjectBinding(IndexSchemaObjectField objectField, String relativeFieldName) {
			super( objectField );
			this.relativeFieldName = relativeFieldName;
			self = objectField.toReference();
		}
	}
}
