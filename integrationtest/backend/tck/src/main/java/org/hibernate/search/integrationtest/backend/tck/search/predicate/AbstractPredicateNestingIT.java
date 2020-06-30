/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.search.predicate;

import static org.hibernate.search.util.impl.integrationtest.common.assertion.SearchResultAssert.assertThatQuery;

import java.util.Collection;

import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.document.IndexObjectFieldReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaObjectField;
import org.hibernate.search.engine.backend.types.ObjectStructure;
import org.hibernate.search.engine.search.predicate.dsl.PredicateFinalStep;
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.FieldTypeDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.SimpleFieldModelsByType;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.SimpleMappedIndex;

import org.junit.Test;

public abstract class AbstractPredicateNestingIT {

	private final SimpleMappedIndex<IndexBinding> index;
	private final IndexBinding binding;
	private final AbstractPredicateDataSet dataSet;

	public AbstractPredicateNestingIT(SimpleMappedIndex<IndexBinding> index, AbstractPredicateDataSet dataSet) {
		this.index = index;
		this.binding = index.binding();
		this.dataSet = dataSet;
	}

	@Test
	public void nestedX2_explicit() {
		assertThatQuery( index.query()
				.where( f -> f.nested().objectField( binding.nested.absolutePath )
						.nest( f.nested().objectField( binding.nested.nested.absolutePath )
								.nest( predicate( f, binding.nested.nested, 0 ) )
						) )
				.routing( dataSet.routingKey ) )
				.hasDocRefHitsAnyOrder( index.typeName(), dataSet.docId( 0 ) );
	}

	@Test
	public void nestedX2_implicit() {
		assertThatQuery( index.query()
				.where( f -> predicate( f, binding.nested.nested, 0 ) )
				.routing( dataSet.routingKey ) )
				.hasDocRefHitsAnyOrder( index.typeName(), dataSet.docId( 0 ) );
	}

	@Test
	public void nestedX2_explicit_implicit() {
		assertThatQuery( index.query()
				.where( f -> f.nested().objectField( binding.nested.absolutePath )
						.nest( predicate( f, binding.nested.nested, 0 ) ) )
				.routing( dataSet.routingKey ) )
				.hasDocRefHitsAnyOrder( index.typeName(), dataSet.docId( 0 ) );
	}

	@Test
	public void nestedX3_explicitX2_implicit() {
		assertThatQuery( index.query()
				.where( f -> f.nested().objectField( binding.nested.absolutePath )
						.nest( f.nested().objectField( binding.nested.nested.absolutePath )
								.nest( predicate( f, binding.nested.nested.nested, 0 ) ) ) )
				.routing( dataSet.routingKey ) )
				.hasDocRefHitsAnyOrder( index.typeName(), dataSet.docId( 0 ) );
	}

	@Test
	public void nestedX3_explicit_implicitX2() {
		assertThatQuery( index.query()
				.where( f -> f.nested().objectField( binding.nested.absolutePath )
						.nest( predicate( f, binding.nested.nested.nested, 0 ) ) )
				.routing( dataSet.routingKey ) )
				.hasDocRefHitsAnyOrder( index.typeName(), dataSet.docId( 0 ) );
	}

	@Test
	public void nestedFlattenedNested_implicit() {
		assertThatQuery( index.query()
				.where( f -> predicate( f, index.binding().nested.flattened.nested, 0 ) )
				.routing( dataSet.routingKey ) )
				.hasDocRefHitsAnyOrder( index.typeName(), dataSet.docId( 0 ) );
	}

	protected abstract PredicateFinalStep predicate(SearchPredicateFactory f, ObjectFieldBinding objectFieldBinding,
			int matchingDocOrdinal);

	abstract static class AbstractObjectBinding {
		final String absolutePath;
		final SimpleFieldModelsByType field;

		AbstractObjectBinding(IndexSchemaElement self, String absolutePath,
				Collection<? extends FieldTypeDescriptor<?>> fieldTypes) {
			this.absolutePath = absolutePath;
			this.field = SimpleFieldModelsByType.mapAll( fieldTypes, self, "" );
		}

		String fieldPath(FieldTypeDescriptor<?> fieldType) {
			String prefix = absolutePath == null ? "" : absolutePath + ".";
			return prefix + field.get( fieldType ).relativeFieldName;
		}
	}

	static class IndexBinding extends AbstractObjectBinding {

		final ObjectFieldBinding nested;
		final ObjectFieldBinding flattened;

		IndexBinding(IndexSchemaElement root, Collection<? extends FieldTypeDescriptor<?>> fieldTypes) {
			super( root, null, fieldTypes );
			nested = ObjectFieldBinding.create( root, absolutePath, "nested", ObjectStructure.NESTED,
					fieldTypes, 1 );
			flattened = ObjectFieldBinding.create( root, absolutePath, "flattened", ObjectStructure.FLATTENED,
					fieldTypes, 1 );
		}

		protected <F> void initDocument(DocumentElement document, FieldTypeDescriptor<F> fieldType, F fieldValue) {
			DocumentElement nestedDocument = document.addObject( nested.reference );
			addValue( nestedDocument, nested, fieldType, fieldValue );

			DocumentElement nestedX2Document = nestedDocument.addObject( nested.nested.reference );
			addValue( nestedX2Document, nested.nested, fieldType, fieldValue );

			DocumentElement nestedX3Document = nestedX2Document.addObject( nested.nested.nested.reference );
			addValue( nestedX3Document, nested.nested.nested, fieldType, fieldValue );

			DocumentElement nestedFlattenedNestedDocument = nestedDocument.addObject( nested.flattened.reference )
					.addObject( nested.flattened.nested.reference );
			addValue( nestedFlattenedNestedDocument, nested.flattened.nested, fieldType, fieldValue );

			// Also add some "leaf" objects inside the documents, for tests of the exists() predicate on object fields

			DocumentElement nestedX2FlattenedDocument = nestedX2Document.addObject( nested.nested.flattened.reference );
			addValue( nestedX2FlattenedDocument, nested.nested.flattened, fieldType, fieldValue );

			DocumentElement nestedX4Document = nestedX3Document.addObject( nested.nested.nested.nested.reference );
			addValue( nestedX4Document, nested.nested.nested.nested, fieldType, fieldValue );

			DocumentElement nestedX3FlattenedDocument = nestedX3Document.addObject( nested.nested.nested.flattened.reference );
			addValue( nestedX3FlattenedDocument, nested.nested.nested.flattened, fieldType, fieldValue );

			DocumentElement nestedFlattenedNestedNestedDocument = nestedFlattenedNestedDocument
					.addObject( nested.flattened.nested.nested.reference );
			addValue( nestedFlattenedNestedNestedDocument, nested.flattened.nested.nested, fieldType, fieldValue );

			DocumentElement nestedFlattenedNestedFlattenedDocument = nestedFlattenedNestedDocument
					.addObject( nested.flattened.nested.flattened.reference );
			addValue( nestedFlattenedNestedFlattenedDocument, nested.flattened.nested.flattened, fieldType, fieldValue );
		}

		protected <F> void addValue(DocumentElement object, AbstractObjectBinding binding,
				FieldTypeDescriptor<F> fieldType, F fieldValue) {
			object.addValue( binding.field.get( fieldType ).reference, fieldValue );
		}
	}

	static class ObjectFieldBinding extends AbstractObjectBinding {
		private static final int MAX_DEPTH = 4;

		final IndexObjectFieldReference reference;

		final ObjectFieldBinding nested;
		final ObjectFieldBinding flattened;

		static ObjectFieldBinding create(IndexSchemaElement parent, String parentAbsolutePath, String relativeFieldName,
				ObjectStructure structure, Collection<? extends FieldTypeDescriptor<?>> fieldTypes,
				int depth) {
			IndexSchemaObjectField objectField = parent.objectField( relativeFieldName, structure );
			String absolutePath = parentAbsolutePath == null ? relativeFieldName : parentAbsolutePath + "." + relativeFieldName;
			return new ObjectFieldBinding( objectField, absolutePath, fieldTypes, depth );
		}

		ObjectFieldBinding(IndexSchemaObjectField objectField, String absolutePath,
				Collection<? extends FieldTypeDescriptor<?>> fieldTypes, int depth) {
			super( objectField, absolutePath, fieldTypes );
			reference = objectField.toReference();
			if ( depth < MAX_DEPTH ) {
				nested = create( objectField, absolutePath, "nested", ObjectStructure.NESTED,
						fieldTypes, depth + 1 );
				flattened = create( objectField, absolutePath, "flattened", ObjectStructure.FLATTENED,
						fieldTypes, depth + 1 );
			}
			else {
				nested = null;
				flattened = null;
			}
		}
	}
}
