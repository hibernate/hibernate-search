/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.backend.tck.search.predicate;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hibernate.search.util.impl.integrationtest.common.assertion.SearchResultAssert.assertThatQuery;

import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.engine.backend.document.IndexObjectFieldReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaObjectField;
import org.hibernate.search.engine.backend.types.ObjectStructure;
import org.hibernate.search.engine.reporting.spi.EventContexts;
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.extension.SearchSetupHelper;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.common.reporting.FailureReportUtils;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.SimpleMappedIndex;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappedIndex;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappingScope;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class ExistsPredicateObjectsSpecificsIT {

	// this document is empty
	private static final String DOCUMENT_0 = "0";

	// this document has only first level fields: string and numeric
	private static final String DOCUMENT_1 = "1";

	// this document has also an empty nested and an empty flattened objects
	private static final String DOCUMENT_2 = "2";

	// this document has also second level fields:
	// string and numeric within both nested and flattened objects
	private static final String DOCUMENT_3 = "3";

	// this document has only a field within an object field
	// that is, in turn, within an object field
	private static final String DOCUMENT_4 = "4";

	// this document has only an object field with no child,
	// it will never be matched by an exists predicate
	private static final String DOCUMENT_5 = "5";

	// Document not from the main index. All fields are filled out
	private static final String DOCUMENT_6 = "6";

	// Document not from the main index. Only object fields are populated
	private static final String DOCUMENT_7 = "7";

	public static final String ANY_STRING = "Any String";
	public static final int ANY_INTEGER = 173173;

	@RegisterExtension
	public static final SearchSetupHelper setupHelper = SearchSetupHelper.create();

	private static final SimpleMappedIndex<IndexBinding> mainIndex =
			SimpleMappedIndex.of( IndexBinding::new ).name( "main" );
	private static final SimpleMappedIndex<IndexBinding> compatibleIndex =
			SimpleMappedIndex.of( IndexBinding::new ).name( "compatible" );
	private static final StubMappedIndex emptyIndex =
			SimpleMappedIndex.withoutFields().name( "empty" );
	private static final SimpleMappedIndex<IncompatibleIndexBinding> incompatibleIndex =
			SimpleMappedIndex.of( IncompatibleIndexBinding::new ).name( "incompatible" );
	private static final SimpleMappedIndex<InvertedIndexBinding> invertedIndex =
			SimpleMappedIndex.of( InvertedIndexBinding::new ).name( "inverted" );
	private static final SimpleMappedIndex<DifferentFieldsIndexBinding> differentFieldsIndex =
			SimpleMappedIndex.of( DifferentFieldsIndexBinding::new ).name( "differentFields" );
	private static final SimpleMappedIndex<DifferentFieldsNoInnerNestedIndexBinding> noInnerNestedField =
			SimpleMappedIndex.of( DifferentFieldsNoInnerNestedIndexBinding::new ).name( "noInnerNestedField" );
	private static final SimpleMappedIndex<DifferentFieldsDifferentInnerNestedFieldsIndexBinding> differentInnerNestedField =
			SimpleMappedIndex.of( DifferentFieldsDifferentInnerNestedFieldsIndexBinding::new )
					.name( "differentInnerNestedField" );


	@BeforeAll
	static void setup() {
		setupHelper.start()
				.withIndexes(
						mainIndex, compatibleIndex, emptyIndex,
						incompatibleIndex, invertedIndex, differentFieldsIndex,
						noInnerNestedField, differentInnerNestedField
				)
				.setup();

		initData();
	}

	@Test
	void nested() {
		assertThatQuery( mainIndex.query()
				.where( f -> f.exists().field( "nested" ) ) )
				// DOCUMENT_2 won't be matched either, since it hasn't got any not-null field
				// DOCUMENT_4 won't be matched either, since we use a nested structure for the inner object field
				.hasDocRefHitsAnyOrder( mainIndex.typeName(), DOCUMENT_3 );
	}

	@Test
	void nested_noChild() {
		assertThatQuery( mainIndex.query()
				.where( f -> f.exists().field( "nestedNoChild" ) ) )
				.hasNoHits();
	}

	@Test
	void nested_multiIndexes_compatibleIndexBinding() {
		StubMappingScope scope = mainIndex.createScope( compatibleIndex );

		assertThatQuery( scope.query()
				.where( f -> f.exists().field( "nested" ) ) )
				// DOCUMENT_2 won't be matched either, since it hasn't got any not-null field
				// DOCUMENT_4 won't be matched either, since we use a nested structure for the inner object field
				.hasDocRefHitsAnyOrder( mainIndex.typeName(), DOCUMENT_3 );
	}

	@Test
	void nested_multiIndexes_incompatibleIndexBinding() {
		SearchPredicateFactory f = mainIndex.createScope( incompatibleIndex ).predicate();
		String fieldPath = "nested";

		assertThatThrownBy( () -> f.exists().field( fieldPath ) )
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll(
						"Inconsistent configuration for field '" + fieldPath + "' in a search query across multiple indexes",
						"This field is a value field in some indexes, but an object field in other indexes" )
				.satisfies( FailureReportUtils.hasContext(
						EventContexts.fromIndexNames( mainIndex.name(), incompatibleIndex.name() )
				) );
	}

	@Test
	void nested_multiIndexes_emptyIndexBinding() {
		StubMappingScope scope = mainIndex.createScope( emptyIndex );

		assertThatQuery( scope.query()
				.where( f -> f.exists().field( "nested" ) ) )
				// DOCUMENT_2 won't be matched either, since it hasn't got any not-null field
				// DOCUMENT_4 won't be matched either, since we use a nested structure for the inner object field
				.hasDocRefHitsAnyOrder( mainIndex.typeName(), DOCUMENT_3 );
	}

	@Test
	void nested_multiIndexes_wrongStructure() {
		SearchPredicateFactory f = mainIndex.createScope( invertedIndex ).predicate();

		String fieldPath = "nested";

		assertThatThrownBy( () -> f.exists().field( fieldPath ) )
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll(
						"Cannot use 'predicate:exists' on field '" + fieldPath + "'",
						"Inconsistent configuration for field '" + fieldPath + "' in a search query across multiple indexes",
						"Attribute 'nestedPathHierarchy' differs:", " vs. " )
				.satisfies( FailureReportUtils.hasContext(
						EventContexts.fromIndexNames( mainIndex.name(), invertedIndex.name() ),
						EventContexts.fromIndexFieldAbsolutePath( fieldPath )
				) );
	}

	@Test
	void nested_multiIndexes_differentFields() {
		String fieldPath = "nested";

		assertThatQuery( mainIndex.createScope( differentFieldsIndex ).query()
				.where( f -> f.exists().field( fieldPath ) ) )
				.hasDocRefHitsAnyOrder( mainIndex.typeName(), DOCUMENT_3 );

		assertThatQuery( mainIndex.createScope( noInnerNestedField ).query()
				.where( f -> f.exists().field( fieldPath ) ) )
				.hasDocRefHitsAnyOrder( c -> {
					c.doc( mainIndex.typeName(), DOCUMENT_3 );
					c.doc( noInnerNestedField.typeName(), DOCUMENT_6 );
				} );

		assertThatQuery( mainIndex.createScope( differentInnerNestedField ).query()
				.where( f -> f.exists().field( fieldPath ) ) )
				.hasDocRefHitsAnyOrder( mainIndex.typeName(), DOCUMENT_3 );

		assertThatQuery( mainIndex.createScope( differentInnerNestedField ).query()
				.where( f -> f.exists().field( "nested.nestedX2" ) ) )
				.hasDocRefHitsAnyOrder( c -> {
					c.doc( mainIndex.typeName(), DOCUMENT_4 );
					c.doc( differentInnerNestedField.typeName(), DOCUMENT_6 );
				} );
	}

	@Test
	void flattened() {
		assertThatQuery( mainIndex.query()
				.where( p -> p.exists().field( "flattened" ) ) )
				// DOCUMENT_2 won't be matched either, since it hasn't got any not-null field
				// DOCUMENT_4 will match, even if the matching field is not a direct child of the targeted path
				.hasDocRefHitsAnyOrder( mainIndex.typeName(), DOCUMENT_3, DOCUMENT_4 );
	}

	@Test
	void flattened_noChild() {
		assertThatQuery( mainIndex.query()
				.where( p -> p.exists().field( "flattenedNoChild" ) ) )
				.hasNoHits();
	}

	@Test
	void flattened_multiIndexes_compatibleIndexBinding() {
		assertThatQuery( mainIndex.query()
				.where( p -> p.exists().field( "flattened" ) ) )
				// DOCUMENT_2 won't be matched either, since it hasn't got any not-null field
				// DOCUMENT_4 will match, even if the matching field is not a direct child of the targeted path
				.hasDocRefHitsAnyOrder( mainIndex.typeName(), DOCUMENT_3, DOCUMENT_4 );
	}

	@Test
	void flattened_multiIndexes_incompatibleIndexBinding() {
		SearchPredicateFactory f = incompatibleIndex.createScope( mainIndex ).predicate();
		String fieldPath = "flattened";

		assertThatThrownBy( () -> f.exists().field( fieldPath ) )
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll(
						"Inconsistent configuration for field '" + fieldPath + "' in a search query across multiple indexes",
						"This field is a value field in some indexes, but an object field in other indexes" )
				.satisfies( FailureReportUtils.hasContext(
						EventContexts.fromIndexNames( incompatibleIndex.name(), mainIndex.name() )
				) );
	}

	@Test
	void flattened_multiIndexes_emptyIndexBinding() {
		StubMappingScope scope = mainIndex.createScope( emptyIndex );

		assertThatQuery( scope.query()
				.where( p -> p.exists().field( "flattened" ) ) )
				// DOCUMENT_2 won't be matched either, since it hasn't got any not-null field
				// DOCUMENT_4 will match, even if the matching field is not a direct child of the targeted path
				.hasDocRefHitsAnyOrder( mainIndex.typeName(), DOCUMENT_3, DOCUMENT_4 );
	}

	@Test
	void flattened_multiIndexes_wrongStructure() {
		SearchPredicateFactory f = invertedIndex.createScope( mainIndex ).predicate();

		String fieldPath = "flattened";

		assertThatThrownBy( () -> f.exists().field( fieldPath ) )
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll(
						"Cannot use 'predicate:exists' on field '" + fieldPath + "'",
						"Inconsistent configuration for field '" + fieldPath + "' in a search query across multiple indexes",
						"Attribute 'nestedPathHierarchy' differs:", " vs. " )
				.satisfies( FailureReportUtils.hasContext(
						EventContexts.fromIndexNames( invertedIndex.name(), mainIndex.name() ),
						EventContexts.fromIndexFieldAbsolutePath( fieldPath )
				) );
	}

	@Test
	void flattened_multiIndexes_differentFields() {
		StubMappingScope scope = differentFieldsIndex.createScope( mainIndex );
		String fieldPath = "flattened";

		assertThatQuery( scope.query()
				.where( f -> f.exists().field( fieldPath ) ) )
				.hasDocRefHitsAnyOrder( mainIndex.typeName(), DOCUMENT_3, DOCUMENT_4 );
	}

	private static void initData() {
		mainIndex.bulkIndexer()
				.add( DOCUMENT_0, document -> {} )
				.add( DOCUMENT_1, document -> {
					document.addValue( mainIndex.binding().string, ANY_STRING );
					document.addValue( mainIndex.binding().numeric, ANY_INTEGER );
				} )
				.add( DOCUMENT_2, document -> {
					document.addValue( mainIndex.binding().string, ANY_STRING );
					document.addValue( mainIndex.binding().numeric, ANY_INTEGER );

					document.addObject( mainIndex.binding().nested );
					document.addObject( mainIndex.binding().flattened );
				} )
				.add( DOCUMENT_3, document -> {
					document.addValue( mainIndex.binding().string, ANY_STRING );
					document.addValue( mainIndex.binding().numeric, ANY_INTEGER );

					DocumentElement nestedDocument = document.addObject( mainIndex.binding().nested );
					nestedDocument.addValue( mainIndex.binding().nestedString, ANY_STRING );
					nestedDocument.addValue( mainIndex.binding().nestedNumeric, ANY_INTEGER );

					DocumentElement flattedDocument = document.addObject( mainIndex.binding().flattened );
					flattedDocument.addValue( mainIndex.binding().flattenedString, ANY_STRING );
					flattedDocument.addValue( mainIndex.binding().flattenedNumeric, ANY_INTEGER );
				} )
				.add( DOCUMENT_4, document -> {
					DocumentElement nestedDocument = document.addObject( mainIndex.binding().nested );
					DocumentElement nestedX2Document = nestedDocument.addObject( mainIndex.binding().nestedX2 );
					nestedX2Document.addValue( mainIndex.binding().nestedX2String, ANY_STRING );

					DocumentElement flattedDocument = document.addObject( mainIndex.binding().flattened );
					DocumentElement flattedX2Document = flattedDocument.addObject( mainIndex.binding().flattenedX2 );
					flattedX2Document.addValue( mainIndex.binding().flattenedX2String, ANY_STRING );
				} )
				.add( DOCUMENT_5, document -> {
					document.addObject( mainIndex.binding().nestedNoChild );
					document.addObject( mainIndex.binding().flattenedNoChild );
				} )
				.join();

		noInnerNestedField.bulkIndexer()
				.add( DOCUMENT_6, document -> {
					DocumentElement nestedDocument = document.addObject( noInnerNestedField.binding().nested );
					nestedDocument.addValue( noInnerNestedField.binding().nestedString, ANY_STRING );
					nestedDocument.addValue( noInnerNestedField.binding().nestedNumeric, ANY_INTEGER );

				} )
				.add( DOCUMENT_7, document -> {
					document.addObject( noInnerNestedField.binding().nested );
				} )
				.join();

		differentInnerNestedField.bulkIndexer()
				.add( DOCUMENT_6, document -> {
					DocumentElement nestedDocument = document.addObject( differentInnerNestedField.binding().nested );
					DocumentElement nestedX2Document = nestedDocument.addObject( differentInnerNestedField.binding().nestedX2 );
					nestedX2Document.addValue( differentInnerNestedField.binding().nestedX2String, ANY_STRING );
				} )
				.add( DOCUMENT_7, document -> {
					DocumentElement nestedDocument = document.addObject( differentInnerNestedField.binding().nested );
					nestedDocument.addObject( differentInnerNestedField.binding().nestedX2 );
				} )
				.join();
	}

	private static class IndexBinding {
		final IndexFieldReference<String> string;
		final IndexFieldReference<Integer> numeric;

		final IndexObjectFieldReference nested;
		final IndexFieldReference<String> nestedString;
		final IndexFieldReference<Integer> nestedNumeric;

		final IndexObjectFieldReference nestedX2;
		final IndexFieldReference<String> nestedX2String;

		final IndexObjectFieldReference nestedNoChild;

		final IndexObjectFieldReference flattened;
		final IndexFieldReference<String> flattenedString;
		final IndexFieldReference<Integer> flattenedNumeric;

		final IndexObjectFieldReference flattenedX2;
		final IndexFieldReference<String> flattenedX2String;

		final IndexObjectFieldReference flattenedNoChild;

		IndexBinding(IndexSchemaElement root) {
			this.string = root.field( "string", f -> f.asString() ).toReference();
			this.numeric = root.field( "numeric", f -> f.asInteger() ).toReference();

			IndexSchemaObjectField nestedObject = root.objectField( "nested", ObjectStructure.NESTED );
			this.nested = nestedObject.toReference();
			this.nestedString = nestedObject.field( "string", f -> f.asString() ).toReference();
			this.nestedNumeric = nestedObject.field( "numeric", f -> f.asInteger() ).toReference();

			IndexSchemaObjectField nestedX2Object = nestedObject.objectField( "nestedX2", ObjectStructure.NESTED );
			this.nestedX2 = nestedX2Object.toReference();
			this.nestedX2String = nestedX2Object.field( "string", f -> f.asString() ).toReference();

			this.nestedNoChild = root.objectField( "nestedNoChild", ObjectStructure.NESTED ).toReference();

			IndexSchemaObjectField flattenedObject = root.objectField( "flattened", ObjectStructure.FLATTENED );
			this.flattened = flattenedObject.toReference();
			this.flattenedString = flattenedObject.field( "string", f -> f.asString() ).toReference();
			this.flattenedNumeric = flattenedObject.field( "numeric", f -> f.asInteger() ).toReference();

			IndexSchemaObjectField flattenedX2Object = flattenedObject.objectField( "flattenedX2", ObjectStructure.FLATTENED );
			this.flattenedX2 = flattenedX2Object.toReference();
			this.flattenedX2String = flattenedX2Object.field( "string", f -> f.asString() ).toReference();

			this.flattenedNoChild = root.objectField( "flattenedNoChild", ObjectStructure.FLATTENED ).toReference();
		}
	}

	private static class IncompatibleIndexBinding {
		IncompatibleIndexBinding(IndexSchemaElement root) {
			// Define a field instead of an object for the path "nested"
			root.field( "nested", f -> f.asString() ).toReference();

			// Define a field instead of an object for the path "flattened"
			root.field( "flattened", f -> f.asString() ).toReference();
		}
	}

	private static class InvertedIndexBinding {
		InvertedIndexBinding(IndexSchemaElement root) {
			// Use FLATTENED for nested
			root.objectField( "nested", ObjectStructure.FLATTENED ).toReference();

			// Use NESTED for flattened
			root.objectField( "flattened", ObjectStructure.NESTED ).toReference();
		}
	}

	private static class DifferentFieldsIndexBinding {
		DifferentFieldsIndexBinding(IndexSchemaElement root) {
			IndexSchemaObjectField nestedObject = root.objectField( "nested", ObjectStructure.NESTED );
			nestedObject.toReference();

			// change field string into stringDifferentName
			nestedObject.field( "stringDifferentName", f -> f.asString() ).toReference();
			// change field numeric into numericDifferentName
			nestedObject.field( "numericDifferentName", f -> f.asInteger() ).toReference();

			nestedObject.objectField( "nestedX2", ObjectStructure.NESTED ).toReference();

			IndexSchemaObjectField flattenedObject = root.objectField( "flattened", ObjectStructure.FLATTENED );
			flattenedObject.toReference();

			// change field string into stringDifferentName
			flattenedObject.field( "stringDifferentName", f -> f.asString() ).toReference();
			// change field numeric into numericDifferentName
			flattenedObject.field( "numericDifferentName", f -> f.asInteger() ).toReference();

			nestedObject.objectField( "flattenedX2", ObjectStructure.FLATTENED ).toReference();
		}
	}

	private static class DifferentFieldsNoInnerNestedIndexBinding {
		final IndexObjectFieldReference nested;
		final IndexFieldReference<String> nestedString;
		final IndexFieldReference<Integer> nestedNumeric;

		DifferentFieldsNoInnerNestedIndexBinding(IndexSchemaElement root) {
			IndexSchemaObjectField nestedObject = root.objectField( "nested", ObjectStructure.NESTED );
			nested = nestedObject.toReference();

			nestedString = nestedObject.field( "string", f -> f.asString() ).toReference();
			nestedNumeric = nestedObject.field( "numeric", f -> f.asInteger() ).toReference();
		}
	}

	private static class DifferentFieldsDifferentInnerNestedFieldsIndexBinding {
		final IndexObjectFieldReference nested;
		final IndexObjectFieldReference nestedX2;
		final IndexFieldReference<String> nestedX2String;

		DifferentFieldsDifferentInnerNestedFieldsIndexBinding(IndexSchemaElement root) {
			IndexSchemaObjectField nestedObject = root.objectField( "nested", ObjectStructure.NESTED );
			nested = nestedObject.toReference();

			IndexSchemaObjectField nestedX2Object = nestedObject.objectField( "nestedX2", ObjectStructure.NESTED );
			nestedX2 = nestedX2Object.toReference();
			nestedX2String = nestedX2Object.field( "stringDifferentName", f -> f.asString() ).toReference();
		}
	}
}
