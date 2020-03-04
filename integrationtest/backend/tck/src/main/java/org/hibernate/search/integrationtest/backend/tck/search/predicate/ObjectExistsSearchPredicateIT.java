/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.search.predicate;

import static org.hibernate.search.util.impl.integrationtest.common.assertion.SearchHitsAssert.assertThat;
import static org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMapperUtils.referenceProvider;
import static org.junit.Assume.assumeFalse;

import java.util.List;

import org.hibernate.search.engine.backend.common.DocumentReference;
import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.engine.backend.document.IndexObjectFieldReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaObjectField;
import org.hibernate.search.engine.backend.document.model.dsl.ObjectFieldStorage;
import org.hibernate.search.engine.backend.types.Sortable;
import org.hibernate.search.engine.backend.work.execution.spi.IndexIndexingPlan;
import org.hibernate.search.engine.reporting.spi.EventContexts;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.TckConfiguration;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.common.FailureReportUtils;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappingIndexManager;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappingScope;
import org.hibernate.search.util.impl.test.SubTest;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class ObjectExistsSearchPredicateIT {

	private static final String INDEX_NAME = "IndexName";
	private static final String COMPATIBLE_INDEX_NAME = "CompatibleIndexName";
	private static final String INCOMPATIBLE_INDEX_NAME = "IncompatibleIndexName";
	private static final String EMPTY_INDEX_NAME = "EmptyIndexName";
	private static final String INVERTED_INDEX_NAME = "InvertedIndexName";
	private static final String DIFFERENT_FIELDS_INDEX_NAME = "DifferentFieldsIndexName";
	private static final String INCOMPATIBLE_FIELDS_INDEX_NAME = "IncompatibleFieldsIndexName";

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

	public static final String ANY_STRING = "Any String";
	public static final int ANY_INTEGER = 173173;

	@Rule
	public SearchSetupHelper setupHelper = new SearchSetupHelper();

	private IndexMapping indexMapping;

	private StubMappingIndexManager indexManager;
	private StubMappingIndexManager compatibleIndexManager;
	private StubMappingIndexManager incompatibleIndexManager;
	private StubMappingIndexManager emptyIndexManager;
	private StubMappingIndexManager invertedIndexManager;
	private StubMappingIndexManager differentFieldsIndexManager;
	private StubMappingIndexManager incompatibleFieldsIndexManager;

	@Before
	public void setup() {
		setupHelper.start()
				.withIndex(
						INDEX_NAME,
						ctx -> this.indexMapping = new IndexMapping( ctx.getSchemaElement() ),
						indexManager -> this.indexManager = indexManager
				)
				.withIndex(
						COMPATIBLE_INDEX_NAME,
						ctx -> new IndexMapping( ctx.getSchemaElement() ),
						indexManager -> this.compatibleIndexManager = indexManager
				)
				.withIndex(
						INCOMPATIBLE_INDEX_NAME,
						ctx -> new IncompatibleIndexMapping( ctx.getSchemaElement() ),
						indexManager -> this.incompatibleIndexManager = indexManager
				)
				.withIndex(
						EMPTY_INDEX_NAME,
						ctx -> { /* do not define any mapping here */ },
						indexManager -> this.emptyIndexManager = indexManager
				)
				.withIndex(
						INVERTED_INDEX_NAME,
						ctx -> new InvertedIndexMapping( ctx.getSchemaElement() ),
						indexManager -> this.invertedIndexManager = indexManager
				)
				.withIndex(
						DIFFERENT_FIELDS_INDEX_NAME,
						ctx -> new DifferentFieldsMapping( ctx.getSchemaElement() ),
						indexManager -> this.differentFieldsIndexManager = indexManager
				)
				.withIndex(
						INCOMPATIBLE_FIELDS_INDEX_NAME,
						ctx -> new IncompatibleFieldsMapping( ctx.getSchemaElement() ),
						indexManager -> this.incompatibleFieldsIndexManager = indexManager
				)
				.setup();

		initData();
	}

	@Test
	public void nested() {
		StubMappingScope scope = indexManager.createScope();

		List<DocumentReference> docs = scope.query().selectEntityReference()
				.where( p -> p.nested().objectField( "nested" ).nest( f -> f.exists().field( "nested" ) ) )
				.fetchAllHits();

		// DOCUMENT_2 won't be matched either, since it hasn't any not-null field
		// TODO HSEARCH-3762 DOCUMENT_4 won't be matched either, since we use a nested storage for the inner object field
		assertThat( docs ).hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_3 );
	}

	@Test
	public void nested_noChild() {
		StubMappingScope scope = indexManager.createScope();

		List<DocumentReference> docs = scope.query().selectEntityReference()
				.where( p -> p.nested().objectField( "nestedNoChild" ).nest( f -> f.exists().field( "nestedNoChild" ) ) )
				.fetchAllHits();

		assertThat( docs ).isEmpty();
	}

	@Test
	public void nested_multiIndexes_compatibleIndexMapping() {
		StubMappingScope scope = indexManager.createScope( compatibleIndexManager );

		List<DocumentReference> docs = scope.query().selectEntityReference()
				.where( p -> p.nested().objectField( "nested" ).nest( f -> f.exists().field( "nested" ) ) )
				.fetchAllHits();

		// DOCUMENT_2 won't be matched either, since it hasn't any not-null field
		// TODO HSEARCH-3762 DOCUMENT_4 won't be matched either, since we use a nested storage for the inner object field
		assertThat( docs ).hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_3 );
	}

	@Test
	public void nested_multiIndexes_incompatibleIndexMapping() {
		assumeFullMultiIndexCompatibilityCheck();
		StubMappingScope scope = indexManager.createScope( incompatibleIndexManager );

		SubTest.expectException(
				() -> scope.predicate().nested().objectField( "nested" ).nest( f -> f.exists().field( "nested" ) )
		)
				.assertThrown()
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "Multiple conflicting models for field" )
				.hasMessageContaining( "'nested'" )
				.satisfies( FailureReportUtils.hasContext(
						EventContexts.fromIndexNames( INDEX_NAME, INCOMPATIBLE_INDEX_NAME )
				) );
	}

	@Test
	public void nested_multiIndexes_emptyIndexMapping() {
		StubMappingScope scope = indexManager.createScope( emptyIndexManager );

		List<DocumentReference> docs = scope.query().selectEntityReference()
				.where( p -> p.nested().objectField( "nested" ).nest( f -> f.exists().field( "nested" ) ) )
				.fetchAllHits();

		// DOCUMENT_2 won't be matched either, since it hasn't any not-null field
		// TODO HSEARCH-3762 DOCUMENT_4 won't be matched either, since we use a nested storage for the inner object field
		assertThat( docs ).hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_3 );
	}

	@Test
	public void nested_multiIndexes_wrongStorageType() {
		assumeFullMultiIndexCompatibilityCheck();
		StubMappingScope scope = indexManager.createScope( invertedIndexManager );

		SubTest.expectException(
				() -> scope.predicate().exists().field( "nested" )
		)
				.assertThrown()
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "Multiple conflicting models for object field" )
				.hasMessageContaining( "'nested'" )
				.satisfies( FailureReportUtils.hasContext(
						EventContexts.fromIndexNames( INDEX_NAME, INVERTED_INDEX_NAME )
				) );
	}

	@Test
	public void nested_multiIndexes_differentFields() {
		assumeFullMultiIndexCompatibilityCheck();
		StubMappingScope scope = indexManager.createScope( differentFieldsIndexManager );

		SubTest.expectException(
				() -> scope.predicate().nested().objectField( "nested" ).nest( f -> f.exists().field( "nested" ) )
		)
				.assertThrown()
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "Multiple conflicting models for object field" )
				.hasMessageContaining( "'nested'" )
				.satisfies( FailureReportUtils.hasContext(
						EventContexts.fromIndexNames( INDEX_NAME, DIFFERENT_FIELDS_INDEX_NAME )
				) );
	}

	@Test
	public void nested_multiIndexes_incompatibleFields() {
		assumeFullMultiIndexCompatibilityCheck();
		StubMappingScope scope = indexManager.createScope( incompatibleFieldsIndexManager );

		SubTest.expectException(
				() -> scope.predicate().nested().objectField( "nested" ).nest( f -> f.exists().field( "nested" ) )
		)
				.assertThrown()
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "Multiple conflicting models for object field" )
				.hasMessageContaining( "'nested'" )
				.satisfies( FailureReportUtils.hasContext(
						EventContexts.fromIndexNames( INDEX_NAME, INCOMPATIBLE_FIELDS_INDEX_NAME )
				) );
	}

	@Test
	public void flattened() {
		StubMappingScope scope = indexManager.createScope();

		List<DocumentReference> docs = scope.query().selectEntityReference()
				.where( p -> p.exists().field( "flattened" ) )
				.fetchAllHits();

		// DOCUMENT_2 won't be matched either, since it hasn't any not-null field
		// DOCUMENT_4 will match, even if the matching field is not a direct child of the targeted path
		assertThat( docs ).hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_3, DOCUMENT_4 );
	}

	@Test
	public void flattened_noChild() {
		StubMappingScope scope = indexManager.createScope();

		List<DocumentReference> docs = scope.query().selectEntityReference()
				.where( p -> p.exists().field( "flattenedNoChild" ) )
				.fetchAllHits();

		assertThat( docs ).isEmpty();
	}

	@Test
	public void flattened_multiIndexes_compatibleIndexMapping() {
		StubMappingScope scope = compatibleIndexManager.createScope( indexManager );

		List<DocumentReference> docs = scope.query().selectEntityReference()
				.where( p -> p.exists().field( "flattened" ) )
				.fetchAllHits();

		// DOCUMENT_2 won't be matched either, since it hasn't any not-null field
		// DOCUMENT_4 will match, even if the matching field is not a direct child of the targeted path
		assertThat( docs ).hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_3, DOCUMENT_4 );
	}

	@Test
	public void flattened_multiIndexes_incompatibleIndexMapping() {
		assumeFullMultiIndexCompatibilityCheck();
		StubMappingScope scope = incompatibleIndexManager.createScope( indexManager );

		SubTest.expectException(
				() -> scope.predicate().exists().field( "flattened" )
		)
				.assertThrown()
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "Multiple conflicting models for field" )
				.hasMessageContaining( "'flattened'" )
				.satisfies( FailureReportUtils.hasContext(
						EventContexts.fromIndexNames( INCOMPATIBLE_INDEX_NAME, INDEX_NAME )
				) );
	}

	@Test
	public void flattened_multiIndexes_emptyIndexMapping() {
		StubMappingScope scope = indexManager.createScope( emptyIndexManager );

		List<DocumentReference> docs = scope.query().selectEntityReference()
				.where( p -> p.exists().field( "flattened" ) )
				.fetchAllHits();

		// DOCUMENT_2 won't be matched either, since it hasn't any not-null field
		// DOCUMENT_4 will match, even if the matching field is not a direct child of the targeted path
		assertThat( docs ).hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_3, DOCUMENT_4 );
	}

	@Test
	public void flattened_multiIndexes_wrongStorageType() {
		assumeFullMultiIndexCompatibilityCheck();
		StubMappingScope scope = invertedIndexManager.createScope( indexManager );

		SubTest.expectException(
				() -> scope.predicate().exists().field( "flattened" )
		)
				.assertThrown()
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "Multiple conflicting models for object field" )
				.hasMessageContaining( "'flattened'" )
				.satisfies( FailureReportUtils.hasContext(
						EventContexts.fromIndexNames( INVERTED_INDEX_NAME, INDEX_NAME )
				) );
	}

	@Test
	public void flattened_multiIndexes_differentFields() {
		assumeFullMultiIndexCompatibilityCheck();
		StubMappingScope scope = differentFieldsIndexManager.createScope( indexManager );

		SubTest.expectException(
				() -> scope.predicate().exists().field( "flattened" )
		)
				.assertThrown()
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "Multiple conflicting models for object field" )
				.hasMessageContaining( "'flattened'" )
				.satisfies( FailureReportUtils.hasContext(
						EventContexts.fromIndexNames( DIFFERENT_FIELDS_INDEX_NAME, INDEX_NAME )
				) );
	}

	@Test
	public void flattened_multiIndexes_incompatibleFields() {
		assumeFullMultiIndexCompatibilityCheck();
		StubMappingScope scope = incompatibleFieldsIndexManager.createScope( indexManager );

		SubTest.expectException(
				() -> scope.predicate().exists().field( "flattened" )
		)
				.assertThrown()
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "Multiple conflicting models for object field" )
				.hasMessageContaining( "'flattened'" )
				.satisfies( FailureReportUtils.hasContext(
						EventContexts.fromIndexNames( INCOMPATIBLE_FIELDS_INDEX_NAME, INDEX_NAME )
				) );
	}

	private void initData() {
		IndexIndexingPlan<?> plan = indexManager.createIndexingPlan();
		plan.add( referenceProvider( DOCUMENT_0 ), document -> { } );

		plan.add( referenceProvider( DOCUMENT_1 ), document -> {
			document.addValue( indexMapping.string, ANY_STRING );
			document.addValue( indexMapping.numeric, ANY_INTEGER );
		} );
		plan.add( referenceProvider( DOCUMENT_2 ), document -> {
			document.addValue( indexMapping.string, ANY_STRING );
			document.addValue( indexMapping.numeric, ANY_INTEGER );

			document.addObject( indexMapping.nested );
			document.addObject( indexMapping.flattened );
		} );
		plan.add( referenceProvider( DOCUMENT_3 ), document -> {
			document.addValue( indexMapping.string, ANY_STRING );
			document.addValue( indexMapping.numeric, ANY_INTEGER );

			DocumentElement nestedDocument = document.addObject( indexMapping.nested );
			nestedDocument.addValue( indexMapping.nestedString, ANY_STRING );
			nestedDocument.addValue( indexMapping.nestedNumeric, ANY_INTEGER );

			DocumentElement flattedDocument = document.addObject( indexMapping.flattened );
			flattedDocument.addValue( indexMapping.flattenedString, ANY_STRING );
			flattedDocument.addValue( indexMapping.flattenedNumeric, ANY_INTEGER );
		} );
		plan.add( referenceProvider( DOCUMENT_4 ), document -> {
			DocumentElement nestedDocument = document.addObject( indexMapping.nested );
			DocumentElement nestedX2Document = nestedDocument.addObject( indexMapping.nestedX2 );
			nestedX2Document.addValue( indexMapping.nestedX2String, ANY_STRING );

			DocumentElement flattedDocument = document.addObject( indexMapping.flattened );
			DocumentElement flattedX2Document = flattedDocument.addObject( indexMapping.flattenedX2 );
			flattedX2Document.addValue( indexMapping.flattenedX2String, ANY_STRING );
		} );
		plan.add( referenceProvider( DOCUMENT_5 ), document -> {
			document.addObject( indexMapping.nestedNoChild );
			document.addObject( indexMapping.flattenedNoChild );
		} );

		plan.execute().join();
		checkDocumentsCreation();
	}

	private void checkDocumentsCreation() {
		List<DocumentReference> docs = indexManager.createScope().query().selectEntityReference()
				.where( p -> p.matchAll() )
				.fetchAllHits();

		assertThat( docs ).hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_0, DOCUMENT_1, DOCUMENT_2, DOCUMENT_3, DOCUMENT_4, DOCUMENT_5 );
	}

	private void assumeFullMultiIndexCompatibilityCheck() {
		assumeFalse(
				"We do not test some Multi-indexing compatibility checks if the backend allows these",
				TckConfiguration.get().getBackendFeatures().lenientOnMultiIndexesCompatibilityChecks()
		);
	}

	private static class IndexMapping {
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

		IndexMapping(IndexSchemaElement root) {
			this.string = root.field( "string", f -> f.asString() ).toReference();
			this.numeric = root.field( "numeric", f -> f.asInteger() ).toReference();

			IndexSchemaObjectField nestedObject = root.objectField( "nested", ObjectFieldStorage.NESTED );
			this.nested = nestedObject.toReference();
			this.nestedString = nestedObject.field( "string", f -> f.asString() ).toReference();
			this.nestedNumeric = nestedObject.field( "numeric", f -> f.asInteger() ).toReference();

			IndexSchemaObjectField nestedX2Object = nestedObject.objectField( "nestedX2", ObjectFieldStorage.NESTED );
			this.nestedX2 = nestedX2Object.toReference();
			this.nestedX2String = nestedX2Object.field( "string", f -> f.asString() ).toReference();

			this.nestedNoChild = root.objectField( "nestedNoChild", ObjectFieldStorage.NESTED ).toReference();

			IndexSchemaObjectField flattenedObject = root.objectField( "flattened", ObjectFieldStorage.FLATTENED );
			this.flattened = flattenedObject.toReference();
			this.flattenedString = flattenedObject.field( "string", f -> f.asString() ).toReference();
			this.flattenedNumeric = flattenedObject.field( "numeric", f -> f.asInteger() ).toReference();

			IndexSchemaObjectField flattenedX2Object = flattenedObject.objectField( "flattenedX2", ObjectFieldStorage.FLATTENED );
			this.flattenedX2 = flattenedX2Object.toReference();
			this.flattenedX2String = flattenedX2Object.field( "string", f -> f.asString() ).toReference();

			this.flattenedNoChild = root.objectField( "flattenedNoChild", ObjectFieldStorage.FLATTENED ).toReference();
		}
	}

	private static class IncompatibleIndexMapping {
		IncompatibleIndexMapping(IndexSchemaElement root) {
			// Define a field instead of an object for the path "nested"
			root.field( "nested", f -> f.asString() ).toReference();

			// Define a field instead of an object for the path "flattened"
			root.field( "flattened", f -> f.asString() ).toReference();
		}
	}

	private static class InvertedIndexMapping {
		InvertedIndexMapping(IndexSchemaElement root) {
			// Use FLATTENED for nested
			root.objectField( "nested", ObjectFieldStorage.FLATTENED ).toReference();

			// Use NESTED for flattened
			root.objectField( "flattened", ObjectFieldStorage.NESTED ).toReference();
		}
	}

	private static class DifferentFieldsMapping {
		DifferentFieldsMapping(IndexSchemaElement root) {
			IndexSchemaObjectField nestedObject = root.objectField( "nested", ObjectFieldStorage.NESTED );
			nestedObject.toReference();

			// change field string into stringDifferentName
			nestedObject.field( "stringDifferentName", f -> f.asString() ).toReference();
			// change field numeric into numericDifferentName
			nestedObject.field( "numericDifferentName", f -> f.asInteger() ).toReference();

			nestedObject.objectField( "nestedX2", ObjectFieldStorage.NESTED ).toReference();

			IndexSchemaObjectField flattenedObject = root.objectField( "flattened", ObjectFieldStorage.FLATTENED );
			flattenedObject.toReference();

			// change field string into stringDifferentName
			flattenedObject.field( "stringDifferentName", f -> f.asString() ).toReference();
			// change field numeric into numericDifferentName
			flattenedObject.field( "numericDifferentName", f -> f.asInteger() ).toReference();

			nestedObject.objectField( "flattenedX2", ObjectFieldStorage.FLATTENED ).toReference();
		}
	}

	private static class IncompatibleFieldsMapping {
		IncompatibleFieldsMapping(IndexSchemaElement root) {
			IndexSchemaObjectField nestedObject = root.objectField( "nested", ObjectFieldStorage.NESTED );
			nestedObject.toReference();

			// field has same name, but with an incompatible exists predicates: string vs BigDecimal
			nestedObject.field( "string", f -> f.asBigDecimal().decimalScale( 3 ) ).toReference();
			nestedObject.field( "numeric", f -> f.asInteger() ).toReference();

			nestedObject.objectField( "nestedX2", ObjectFieldStorage.NESTED ).toReference();

			IndexSchemaObjectField flattenedObject = root.objectField( "flattened", ObjectFieldStorage.FLATTENED );
			flattenedObject.toReference();

			flattenedObject.field( "string", f -> f.asString() ).toReference();
			// field has same name, but with an incompatible exists predicates: unSortable vs Sortable
			flattenedObject.field( "numeric", f -> f.asInteger().sortable( Sortable.YES ) ).toReference();

			nestedObject.objectField( "flattenedX2", ObjectFieldStorage.FLATTENED ).toReference();
		}
	}
}
