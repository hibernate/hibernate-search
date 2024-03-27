/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.backend.tck.search.predicate;

import static org.hibernate.search.util.impl.integrationtest.common.assertion.SearchResultAssert.assertThatQuery;

import java.util.Collection;

import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.document.IndexObjectFieldReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaObjectField;
import org.hibernate.search.engine.backend.types.ObjectStructure;
import org.hibernate.search.engine.backend.types.dsl.SearchableProjectableIndexFieldTypeOptionsStep;
import org.hibernate.search.engine.search.predicate.SearchPredicate;
import org.hibernate.search.engine.search.predicate.definition.PredicateDefinition;
import org.hibernate.search.engine.search.predicate.definition.PredicateDefinitionContext;
import org.hibernate.search.engine.search.predicate.dsl.PredicateFinalStep;
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.FieldTypeDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.SimpleFieldModelsByType;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.SimpleMappedIndex;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappingScope;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

public abstract class AbstractPredicateInObjectFieldIT {

	static final int MISSING_FIELD_INDEX_DOC_ORDINAL = 42;

	@ParameterizedTest(name = "{2}")
	@MethodSource("params")
	void flattenedX1(SimpleMappedIndex<IndexBinding> mainIndex,
			SimpleMappedIndex<MissingFieldIndexBinding> missingFieldIndex,
			AbstractPredicateDataSet dataSet) {
		assertThatQuery( mainIndex.query()
				.where( f -> predicate( f, mainIndex.binding().flattened, 0, dataSet ) )
				.routing( dataSet.routingKey ) )
				.hasDocRefHitsAnyOrder( mainIndex.typeName(), dataSet.docId( 0 ) );
	}

	@ParameterizedTest(name = "{2}")
	@MethodSource("params")
	void flattenedX2(SimpleMappedIndex<IndexBinding> mainIndex,
			SimpleMappedIndex<MissingFieldIndexBinding> missingFieldIndex,
			AbstractPredicateDataSet dataSet) {
		assertThatQuery( mainIndex.query()
				.where( f -> predicate( f, mainIndex.binding().flattened.flattened, 0, dataSet ) )
				.routing( dataSet.routingKey ) )
				.hasDocRefHitsAnyOrder( mainIndex.typeName(), dataSet.docId( 0 ) );
	}

	@ParameterizedTest(name = "{2}")
	@MethodSource("params")
	void nestedX1_explicit(SimpleMappedIndex<IndexBinding> mainIndex,
			SimpleMappedIndex<MissingFieldIndexBinding> missingFieldIndex,
			AbstractPredicateDataSet dataSet) {
		assertThatQuery( mainIndex.query()
				.where( f -> f.nested( mainIndex.binding().nested.absolutePath )
						.add( predicate( f, mainIndex.binding().nested, 0, dataSet ) ) )
				.routing( dataSet.routingKey ) )
				.hasDocRefHitsAnyOrder( mainIndex.typeName(), dataSet.docId( 0 ) );
	}

	@ParameterizedTest(name = "{2}")
	@MethodSource("params")
	void nestedX1_implicit(SimpleMappedIndex<IndexBinding> mainIndex,
			SimpleMappedIndex<MissingFieldIndexBinding> missingFieldIndex,
			AbstractPredicateDataSet dataSet) {
		assertThatQuery( mainIndex.query()
				.where( f -> predicate( f, mainIndex.binding().nested, 0, dataSet ) )
				.routing( dataSet.routingKey ) )
				.hasDocRefHitsAnyOrder( mainIndex.typeName(), dataSet.docId( 0 ) );
	}

	@ParameterizedTest(name = "{2}")
	@MethodSource("params")
	void nestedX2_explicit(SimpleMappedIndex<IndexBinding> mainIndex,
			SimpleMappedIndex<MissingFieldIndexBinding> missingFieldIndex,
			AbstractPredicateDataSet dataSet) {
		assertThatQuery( mainIndex.query()
				.where( f -> f.nested( mainIndex.binding().nested.absolutePath )
						.add( f.nested( mainIndex.binding().nested.nested.absolutePath )
								.add( predicate( f, mainIndex.binding().nested.nested, 0, dataSet ) ) ) )
				.routing( dataSet.routingKey ) )
				.hasDocRefHitsAnyOrder( mainIndex.typeName(), dataSet.docId( 0 ) );
	}

	@ParameterizedTest(name = "{2}")
	@MethodSource("params")
	void nestedX2_implicit(SimpleMappedIndex<IndexBinding> mainIndex,
			SimpleMappedIndex<MissingFieldIndexBinding> missingFieldIndex,
			AbstractPredicateDataSet dataSet) {
		assertThatQuery( mainIndex.query()
				.where( f -> predicate( f, mainIndex.binding().nested.nested, 0, dataSet ) )
				.routing( dataSet.routingKey ) )
				.hasDocRefHitsAnyOrder( mainIndex.typeName(), dataSet.docId( 0 ) );
	}

	@ParameterizedTest(name = "{2}")
	@MethodSource("params")
	void nestedX2_explicit_implicit(SimpleMappedIndex<IndexBinding> mainIndex,
			SimpleMappedIndex<MissingFieldIndexBinding> missingFieldIndex,
			AbstractPredicateDataSet dataSet) {
		assertThatQuery( mainIndex.query()
				.where( f -> f.nested( mainIndex.binding().nested.absolutePath )
						.add( predicate( f, mainIndex.binding().nested.nested, 0, dataSet ) ) )
				.routing( dataSet.routingKey ) )
				.hasDocRefHitsAnyOrder( mainIndex.typeName(), dataSet.docId( 0 ) );
	}

	@ParameterizedTest(name = "{2}")
	@MethodSource("params")
	void nestedX3_explicitX2_implicit(SimpleMappedIndex<IndexBinding> mainIndex,
			SimpleMappedIndex<MissingFieldIndexBinding> missingFieldIndex,
			AbstractPredicateDataSet dataSet) {
		assertThatQuery( mainIndex.query()
				.where( f -> f.nested( mainIndex.binding().nested.absolutePath )
						.add( f.nested( mainIndex.binding().nested.nested.absolutePath )
								.add( predicate( f, mainIndex.binding().nested.nested.nested, 0, dataSet ) ) ) )
				.routing( dataSet.routingKey ) )
				.hasDocRefHitsAnyOrder( mainIndex.typeName(), dataSet.docId( 0 ) );
	}

	@ParameterizedTest(name = "{2}")
	@MethodSource("params")
	void nestedX3_explicit_implicitX2(SimpleMappedIndex<IndexBinding> mainIndex,
			SimpleMappedIndex<MissingFieldIndexBinding> missingFieldIndex,
			AbstractPredicateDataSet dataSet) {
		assertThatQuery( mainIndex.query()
				.where( f -> f.nested( mainIndex.binding().nested.absolutePath )
						.add( predicate( f, mainIndex.binding().nested.nested.nested, 0, dataSet ) ) )
				.routing( dataSet.routingKey ) )
				.hasDocRefHitsAnyOrder( mainIndex.typeName(), dataSet.docId( 0 ) );
	}

	@ParameterizedTest(name = "{2}")
	@MethodSource("params")
	void nestedFlattenedNested_implicit(SimpleMappedIndex<IndexBinding> mainIndex,
			SimpleMappedIndex<MissingFieldIndexBinding> missingFieldIndex,
			AbstractPredicateDataSet dataSet) {
		assertThatQuery( mainIndex.query()
				.where( f -> predicate( f, mainIndex.binding().nested.flattened.nested, 0, dataSet ) )
				.routing( dataSet.routingKey ) )
				.hasDocRefHitsAnyOrder( mainIndex.typeName(), dataSet.docId( 0 ) );
	}

	/**
	 * Test that no failure occurs when an implicit nested predicate targets a nested field
	 * that only exists in one of the targeted indexes.
	 */
	@ParameterizedTest(name = "{2}")
	@MethodSource("params")
	@TestForIssue(jiraKey = "HSEARCH-4173")
	void multiIndex_missingNestedField_implicit(SimpleMappedIndex<IndexBinding> mainIndex,
			SimpleMappedIndex<MissingFieldIndexBinding> missingFieldIndex,
			AbstractPredicateDataSet dataSet) {
		StubMappingScope scope = mainIndex.createScope( missingFieldIndex );

		// The "nested" predicate should not match anything in missingFieldIndex
		assertThatQuery( scope.query()
				.where( f -> predicate( f, mainIndex.binding().nested.nested, 0, dataSet ) )
				.routing( dataSet.routingKey ) )
				.hasDocRefHitsAnyOrder( mainIndex.typeName(), dataSet.docId( 0 ) );

		// ... but it should not prevent the query from executing either:
		// if the "nested" predicate is optional, it should be ignored for missingFieldIndex.
		assertThatQuery( scope.query()
				.where( f -> f.or(
						predicate( f, mainIndex.binding().nested.nested, 0, dataSet ),
						f.id().matching( dataSet.docId( MISSING_FIELD_INDEX_DOC_ORDINAL ) ) ) )
				.routing( dataSet.routingKey ) )
				.hasDocRefHitsAnyOrder( c -> c
						.doc( mainIndex.typeName(), dataSet.docId( 0 ) )
						.doc( missingFieldIndex.typeName(), dataSet.docId( MISSING_FIELD_INDEX_DOC_ORDINAL ) ) )
				.hasTotalHitCount( 2 );
	}

	protected abstract PredicateFinalStep predicate(SearchPredicateFactory f, ObjectFieldBinding objectFieldBinding,
			int matchingDocOrdinal, AbstractPredicateDataSet dataSet);

	abstract static class AbstractObjectBinding {
		final String absolutePath;
		final SimpleFieldModelsByType field;

		AbstractObjectBinding(IndexSchemaElement self, String absolutePath,
				Collection<? extends FieldTypeDescriptor<?,
						? extends SearchableProjectableIndexFieldTypeOptionsStep<?, ?>>> fieldTypes) {
			this.absolutePath = absolutePath;
			this.field = SimpleFieldModelsByType.mapAll( fieldTypes, self, "" );
		}

		String absoluteFieldPath(FieldTypeDescriptor<?, ?> fieldType) {
			String prefix = absolutePath == null ? "" : absolutePath + ".";
			return prefix + relativeFieldPath( fieldType );
		}

		String relativeFieldPath(FieldTypeDescriptor<?, ?> fieldType) {
			return field.get( fieldType ).relativeFieldName;
		}
	}

	static class IndexBinding extends AbstractObjectBinding {

		final ObjectFieldBinding nested;
		final ObjectFieldBinding flattened;

		IndexBinding(IndexSchemaElement root, Collection<
				? extends FieldTypeDescriptor<?, ? extends SearchableProjectableIndexFieldTypeOptionsStep<?, ?>>> fieldTypes) {
			super( root, null, fieldTypes );
			nested = ObjectFieldBinding.create( root, absolutePath, "nested", ObjectStructure.NESTED,
					fieldTypes, 1 );
			flattened = ObjectFieldBinding.create( root, absolutePath, "flattened", ObjectStructure.FLATTENED,
					fieldTypes, 1 );
		}

		protected <F> void initDocument(DocumentElement document, FieldTypeDescriptor<F, ?> fieldType, F fieldValue) {
			DocumentElement flattenedDocument = document.addObject( flattened.reference );
			addValue( flattenedDocument, flattened, fieldType, fieldValue );

			DocumentElement flattenedX2Document = flattenedDocument.addObject( flattened.flattened.reference );
			addValue( flattenedX2Document, flattened.flattened, fieldType, fieldValue );

			DocumentElement nestedDocument = document.addObject( nested.reference );
			addValue( nestedDocument, nested, fieldType, fieldValue );

			DocumentElement nestedX2Document = nestedDocument.addObject( nested.nested.reference );
			addValue( nestedX2Document, nested.nested, fieldType, fieldValue );

			DocumentElement nestedX3Document = nestedX2Document.addObject( nested.nested.nested.reference );
			addValue( nestedX3Document, nested.nested.nested, fieldType, fieldValue );

			DocumentElement nestedFlattenedDocument = nestedDocument.addObject( nested.flattened.reference );

			DocumentElement nestedFlattenedNestedDocument = nestedFlattenedDocument
					.addObject( nested.flattened.nested.reference );
			addValue( nestedFlattenedNestedDocument, nested.flattened.nested, fieldType, fieldValue );

			// Also add some "leaf" objects inside the documents, for tests of the exists() predicate on object fields
			DocumentElement flattenedNestedDocument = flattenedDocument.addObject( flattened.nested.reference );
			addValue( flattenedNestedDocument, flattened.nested, fieldType, fieldValue );

			DocumentElement flattenedX2NestedDocument = flattenedX2Document.addObject( flattened.flattened.nested.reference );
			addValue( flattenedX2NestedDocument, flattened.flattened.nested, fieldType, fieldValue );

			DocumentElement flattenedX3Document = flattenedX2Document.addObject( flattened.flattened.flattened.reference );
			addValue( flattenedX3Document, flattened.flattened.flattened, fieldType, fieldValue );

			addValue( nestedFlattenedDocument, nested.flattened, fieldType, fieldValue );

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
				FieldTypeDescriptor<F, ?> fieldType, F fieldValue) {
			object.addValue( binding.field.get( fieldType ).reference, fieldValue );
		}
	}

	static class MissingFieldIndexBinding {

		MissingFieldIndexBinding(IndexSchemaElement root, Collection<? extends FieldTypeDescriptor<?, ?>> fieldTypes) {
		}

		protected void initDocument() {
		}
	}

	static class ObjectFieldBinding extends AbstractObjectBinding {
		private static final int MAX_DEPTH = 4;

		final String relativeName;
		final IndexObjectFieldReference reference;

		final ObjectFieldBinding nested;
		final ObjectFieldBinding flattened;

		static ObjectFieldBinding create(IndexSchemaElement parent, String parentAbsolutePath, String relativeFieldName,
				ObjectStructure structure, Collection<? extends FieldTypeDescriptor<?, ?>> fieldTypes,
				int depth) {
			IndexSchemaObjectField objectField = parent.objectField( relativeFieldName, structure );
			return new ObjectFieldBinding( objectField, parentAbsolutePath, relativeFieldName, fieldTypes, depth );
		}

		ObjectFieldBinding(IndexSchemaObjectField objectField, String parentAbsolutePath, String relativeFieldName,
				Collection<? extends FieldTypeDescriptor<?, ?>> fieldTypes, int depth) {
			super( objectField, parentAbsolutePath == null ? relativeFieldName : parentAbsolutePath + "." + relativeFieldName,
					fieldTypes );
			relativeName = relativeFieldName;
			reference = objectField.toReference();
			objectField.namedPredicate( StubPredicateDefinition.NAME, new StubPredicateDefinition() );
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

	public static class StubPredicateDefinition implements PredicateDefinition {
		public static final String NAME = "stub-predicate";
		public static final String IMPL_PARAM_NAME = "impl";

		@Override
		public SearchPredicate create(PredicateDefinitionContext context) {
			PredicateDefinition impl = context.param( IMPL_PARAM_NAME, PredicateDefinition.class );
			return impl.create( context );
		}
	}
}
