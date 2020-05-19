/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.metamodel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.hibernate.search.engine.backend.document.IndexObjectFieldReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaObjectField;
import org.hibernate.search.engine.backend.document.model.dsl.ObjectFieldStorage;
import org.hibernate.search.engine.backend.metamodel.IndexCompositeElementDescriptor;
import org.hibernate.search.engine.backend.metamodel.IndexDescriptor;
import org.hibernate.search.engine.backend.metamodel.IndexFieldDescriptor;
import org.hibernate.search.engine.backend.metamodel.IndexObjectFieldDescriptor;
import org.hibernate.search.engine.backend.metamodel.IndexObjectFieldTypeDescriptor;
import org.hibernate.search.engine.backend.metamodel.IndexValueFieldDescriptor;
import org.hibernate.search.engine.backend.metamodel.IndexValueFieldTypeDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.IndexFieldLocation;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.TestedFieldStructure;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.SimpleMappedIndex;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappingSchemaManagementStrategy;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import org.assertj.core.api.Assertions;

/**
 * Tests for field descriptor features.
 * <p>
 * Tests for type-related features are located in {@link IndexValueFieldTypeDescriptorBaseIT}
 * and {@link IndexObjectFieldTypeDescriptorBaseIT}.
 */
@RunWith(Parameterized.class)
@TestForIssue(jiraKey = "HSEARCH-3589")
public class IndexFieldDescriptorIT {

	@Parameterized.Parameters(name = "{0}")
	public static Object[][] parameters() {
		List<Object[]> parameters = new ArrayList<>();
		for ( TestedFieldStructure fieldStructure : TestedFieldStructure.all() ) {
			parameters.add( new Object[] { fieldStructure } );
		}
		return parameters.toArray( new Object[0][] );
	}

	@ClassRule
	public static final SearchSetupHelper setupHelper = new SearchSetupHelper();

	private static final SimpleMappedIndex<IndexBinding> index = SimpleMappedIndex.of( IndexBinding::new );

	@BeforeClass
	public static void setup() {
		setupHelper.start().withIndex( index )
				.withSchemaManagement( StubMappingSchemaManagementStrategy.NONE )
				.setup();
	}

	private final TestedFieldStructure fieldStructure;

	public IndexFieldDescriptorIT(TestedFieldStructure fieldStructure) {
		this.fieldStructure = fieldStructure;
	}

	@Test
	public void valueField() {
		IndexDescriptor indexDescriptor = index.toApi().descriptor();
		IndexFieldDescriptor baseFieldDescriptor = indexDescriptor.field( getAbsoluteFieldPath() ).get();

		// Basic getters
		assertThat( baseFieldDescriptor )
				.returns( false, IndexFieldDescriptor::isObjectField )
				.returns( true, IndexFieldDescriptor::isValueField );

		IndexValueFieldDescriptor fieldDescriptor = baseFieldDescriptor.toValueField();
		assertThat( fieldDescriptor ).isSameAs( baseFieldDescriptor );
		assertThatThrownBy( fieldDescriptor::toObjectField )
				.isInstanceOf( SearchException.class )
				.hasMessageContaining(
						"Invalid type: '" + getAbsoluteFieldPath() + "' is a value field, not an object field"
				);

		assertThat( fieldDescriptor )
				.returns( getRelativeFieldName(), IndexFieldDescriptor::relativeName )
				.returns( getAbsoluteFieldPath(), IndexFieldDescriptor::absolutePath )
				.returns( fieldStructure.isMultiValued(), IndexFieldDescriptor::isMultiValued );

		// Type
		// More advanced tests in IndexValueFieldTypeDescriptorIT
		IndexValueFieldTypeDescriptor type = fieldDescriptor.type();
		assertThat( type ).isNotNull();
	}

	@Test
	public void parent() {
		IndexDescriptor indexDescriptor = index.toApi().descriptor();
		IndexFieldDescriptor childFieldDescriptor = indexDescriptor.field( getAbsoluteFieldPath() ).get();

		IndexCompositeElementDescriptor elementDescriptor = childFieldDescriptor.parent();
		assertThat( elementDescriptor ).isNotNull();
		if ( IndexFieldLocation.ROOT.equals( fieldStructure.location ) ) {
			// The parent is not an object field, just the root.
			assertThat( elementDescriptor.isRoot() ).isTrue();
			assertThat( elementDescriptor.isObjectField() ).isFalse();
			assertThatThrownBy( elementDescriptor::toObjectField )
					.isInstanceOf( SearchException.class )
					.hasMessageContaining(
							"Invalid type: the index root is not an object field"
					);
			assertThat( elementDescriptor ).isSameAs( indexDescriptor.root() );
			assertThat( elementDescriptor.staticChildren() )
					.extracting( IndexFieldDescriptor::absolutePath )
					.containsExactlyInAnyOrder( "myField", "myMultiValuedField", "nestedObject", "flattenedObject" );
			return;
		}

		assertThat( elementDescriptor.isRoot() ).isFalse();
		assertThat( elementDescriptor.isObjectField() ).isTrue();

		IndexObjectFieldDescriptor fieldDescriptor = elementDescriptor.toObjectField();
		assertThat( fieldDescriptor ).isSameAs( elementDescriptor );
		assertThatThrownBy( fieldDescriptor::toValueField )
				.isInstanceOf( SearchException.class )
				.hasMessageContaining(
						"Invalid type: '" + getParentAbsoluteFieldPath() + "' is an object field, not a value field"
				);

		// Basic getters
		assertThat( fieldDescriptor )
				.returns( true, IndexFieldDescriptor::isObjectField )
				.returns( false, IndexFieldDescriptor::isValueField );
		assertThat( fieldDescriptor )
				.returns( getParentRelativeFieldName(), IndexFieldDescriptor::relativeName )
				.returns( getParentAbsoluteFieldPath(), IndexFieldDescriptor::absolutePath )
				// In this specific test, we decided that nested fields are multi-valued, while flattened fields are not.
				// That's just a detail though: the two characteristics are not usually related.
				.returns( fieldStructure.isInNested(), IndexFieldDescriptor::isMultiValued );

		// Type
		// More advanced tests in IndexObjectFieldTypeDescriptorIT
		IndexObjectFieldTypeDescriptor type = fieldDescriptor.type();
		assertThat( type ).isNotNull();

		// Static children
		Collection<? extends IndexFieldDescriptor> children = fieldDescriptor.staticChildren();
		Assertions.<IndexFieldDescriptor>assertThat( children ).contains( childFieldDescriptor );

		switch ( fieldStructure.location ) {
			case IN_FLATTENED:
				assertThat( children )
						.extracting( IndexFieldDescriptor::absolutePath )
						.containsExactlyInAnyOrder(
								"flattenedObject.myField",
								"flattenedObject.myMultiValuedField"
						);
				break;
			case IN_NESTED:
				assertThat( children )
						.extracting( IndexFieldDescriptor::absolutePath )
						.containsExactlyInAnyOrder(
								"nestedObject.myField",
								"nestedObject.myMultiValuedField",
								"nestedObject.nestedObject"
						);
				break;
			case IN_NESTED_TWICE:
				assertThat( children )
						.extracting( IndexFieldDescriptor::absolutePath )
						.containsExactlyInAnyOrder(
								"nestedObject.nestedObject.myField",
								"nestedObject.nestedObject.myMultiValuedField"
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

	private String getRelativeFieldName() {
		if ( fieldStructure.isSingleValued() ) {
			return "myField";
		}
		else {
			return "myMultiValuedField";
		}
	}

	private String getAbsoluteFieldPath() {
		String parentFieldPath = getParentAbsoluteFieldPath();
		if ( parentFieldPath.isEmpty() ) {
			return getRelativeFieldName();
		}
		else {
			return parentFieldPath + "." + getRelativeFieldName();
		}
	}

	private String getParentAbsoluteFieldPath() {
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

	private String getParentRelativeFieldName() {
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
			IndexSchemaObjectField objectField = parent.objectField( relativeFieldName, ObjectFieldStorage.NESTED );
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
			IndexSchemaObjectField objectField = parent.objectField( relativeFieldName, ObjectFieldStorage.NESTED );
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
			IndexSchemaObjectField objectField = parent.objectField( relativeFieldName, ObjectFieldStorage.NESTED );
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
