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
import org.hibernate.search.util.impl.integrationtest.mapper.stub.SimpleMappedIndex;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappedIndex;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappingScope;
import org.assertj.core.api.Assertions;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class ObjectExistsSearchPredicateIT {

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
	public final SearchSetupHelper setupHelper = new SearchSetupHelper();

	private final SimpleMappedIndex<IndexBinding> mainIndex =
			SimpleMappedIndex.of( IndexBinding::new ).name( "main" );
	private final SimpleMappedIndex<IndexBinding> compatibleIndex =
			SimpleMappedIndex.of( IndexBinding::new ).name( "compatible" );
	private final StubMappedIndex emptyIndex =
			SimpleMappedIndex.withoutFields().name( "empty" );
	private final SimpleMappedIndex<IncompatibleIndexBinding> incompatibleIndex =
			SimpleMappedIndex.of( IncompatibleIndexBinding::new ).name( "incompatible" );
	private final SimpleMappedIndex<InvertedIndexBinding> invertedIndex =
			SimpleMappedIndex.of( InvertedIndexBinding::new ).name( "inverted" );
	private final SimpleMappedIndex<DifferentFieldsIndexBinding> differentFieldsIndex =
			SimpleMappedIndex.of( DifferentFieldsIndexBinding::new ).name( "differentFields" );
	private final SimpleMappedIndex<IncompatibleFieldsIndexBinding> incompatibleFieldsIndex =
			SimpleMappedIndex.of( IncompatibleFieldsIndexBinding::new ).name( "incompatibleFields" );

	@Before
	public void setup() {
		setupHelper.start()
				.withIndexes(
						mainIndex, compatibleIndex, emptyIndex,
						incompatibleIndex, invertedIndex, differentFieldsIndex, incompatibleFieldsIndex
				)
				.setup();

		initData();
	}

	@Test
	public void nested() {
		StubMappingScope scope = mainIndex.createScope();

		List<DocumentReference> docs = scope.query().selectEntityReference()
				.where( p -> p.nested().objectField( "nested" ).nest( f -> f.exists().field( "nested" ) ) )
				.fetchAllHits();

		// DOCUMENT_2 won't be matched either, since it hasn't got any not-null field
		// DOCUMENT_4 won't be matched either, since we use a nested storage for the inner object field
		assertThat( docs ).hasDocRefHitsAnyOrder( mainIndex.typeName(), DOCUMENT_3 );
	}

	@Test
	public void nested_noChild() {
		StubMappingScope scope = mainIndex.createScope();

		List<DocumentReference> docs = scope.query().selectEntityReference()
				.where( p -> p.nested().objectField( "nestedNoChild" ).nest( f -> f.exists().field( "nestedNoChild" ) ) )
				.fetchAllHits();

		assertThat( docs ).isEmpty();
	}

	@Test
	public void nested_multiIndexes_compatibleIndexBinding() {
		StubMappingScope scope = mainIndex.createScope( compatibleIndex );

		List<DocumentReference> docs = scope.query().selectEntityReference()
				.where( p -> p.nested().objectField( "nested" ).nest( f -> f.exists().field( "nested" ) ) )
				.fetchAllHits();

		// DOCUMENT_2 won't be matched either, since it hasn't got any not-null field
		// DOCUMENT_4 won't be matched either, since we use a nested storage for the inner object field
		assertThat( docs ).hasDocRefHitsAnyOrder( mainIndex.typeName(), DOCUMENT_3 );
	}

	@Test
	public void nested_multiIndexes_incompatibleIndexBinding() {
		assumeFullMultiIndexCompatibilityCheck();
		StubMappingScope scope = mainIndex.createScope( incompatibleIndex );

		Assertions.assertThatThrownBy(
				() -> scope.predicate().nested().objectField( "nested" ).nest( f -> f.exists().field( "nested" ) )
		)
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "Multiple conflicting models for field" )
				.hasMessageContaining( "'nested'" )
				.satisfies( FailureReportUtils.hasContext(
						EventContexts.fromIndexNames( mainIndex.name(), incompatibleIndex.name() )
				) );
	}

	@Test
	public void nested_multiIndexes_emptyIndexBinding() {
		StubMappingScope scope = mainIndex.createScope( emptyIndex );

		List<DocumentReference> docs = scope.query().selectEntityReference()
				.where( p -> p.nested().objectField( "nested" ).nest( f -> f.exists().field( "nested" ) ) )
				.fetchAllHits();

		// DOCUMENT_2 won't be matched either, since it hasn't got any not-null field
		// DOCUMENT_4 won't be matched either, since we use a nested storage for the inner object field
		assertThat( docs ).hasDocRefHitsAnyOrder( mainIndex.typeName(), DOCUMENT_3 );
	}

	@Test
	public void nested_multiIndexes_wrongStorageType() {
		assumeFullMultiIndexCompatibilityCheck();
		StubMappingScope scope = mainIndex.createScope( invertedIndex );

		Assertions.assertThatThrownBy(
				() -> scope.predicate().exists().field( "nested" )
		)
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "Multiple conflicting models for object field" )
				.hasMessageContaining( "'nested'" )
				.satisfies( FailureReportUtils.hasContext(
						EventContexts.fromIndexNames( mainIndex.name(), invertedIndex.name() )
				) );
	}

	@Test
	public void nested_multiIndexes_differentFields() {
		assumeFullMultiIndexCompatibilityCheck();
		StubMappingScope scope = mainIndex.createScope( differentFieldsIndex );

		Assertions.assertThatThrownBy(
				() -> scope.predicate().nested().objectField( "nested" ).nest( f -> f.exists().field( "nested" ) )
		)
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "Multiple conflicting models for object field" )
				.hasMessageContaining( "'nested'" )
				.satisfies( FailureReportUtils.hasContext(
						EventContexts.fromIndexNames( mainIndex.name(), differentFieldsIndex.name() )
				) );
	}

	@Test
	public void nested_multiIndexes_incompatibleFields() {
		assumeFullMultiIndexCompatibilityCheck();
		StubMappingScope scope = mainIndex.createScope( incompatibleFieldsIndex );

		Assertions.assertThatThrownBy(
				() -> scope.predicate().nested().objectField( "nested" ).nest( f -> f.exists().field( "nested" ) )
		)
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "Multiple conflicting models for object field" )
				.hasMessageContaining( "'nested'" )
				.satisfies( FailureReportUtils.hasContext(
						EventContexts.fromIndexNames( mainIndex.name(), incompatibleFieldsIndex.name() )
				) );
	}

	@Test
	public void flattened() {
		StubMappingScope scope = mainIndex.createScope();

		List<DocumentReference> docs = scope.query().selectEntityReference()
				.where( p -> p.exists().field( "flattened" ) )
				.fetchAllHits();

		// DOCUMENT_2 won't be matched either, since it hasn't got any not-null field
		// DOCUMENT_4 will match, even if the matching field is not a direct child of the targeted path
		assertThat( docs ).hasDocRefHitsAnyOrder( mainIndex.typeName(), DOCUMENT_3, DOCUMENT_4 );
	}

	@Test
	public void flattened_noChild() {
		StubMappingScope scope = mainIndex.createScope();

		List<DocumentReference> docs = scope.query().selectEntityReference()
				.where( p -> p.exists().field( "flattenedNoChild" ) )
				.fetchAllHits();

		assertThat( docs ).isEmpty();
	}

	@Test
	public void flattened_multiIndexes_compatibleIndexBinding() {
		StubMappingScope scope = compatibleIndex.createScope( mainIndex );

		List<DocumentReference> docs = scope.query().selectEntityReference()
				.where( p -> p.exists().field( "flattened" ) )
				.fetchAllHits();

		// DOCUMENT_2 won't be matched either, since it hasn't got any not-null field
		// DOCUMENT_4 will match, even if the matching field is not a direct child of the targeted path
		assertThat( docs ).hasDocRefHitsAnyOrder( mainIndex.typeName(), DOCUMENT_3, DOCUMENT_4 );
	}

	@Test
	public void flattened_multiIndexes_incompatibleIndexBinding() {
		assumeFullMultiIndexCompatibilityCheck();
		StubMappingScope scope = incompatibleIndex.createScope( mainIndex );

		Assertions.assertThatThrownBy(
				() -> scope.predicate().exists().field( "flattened" )
		)
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "Multiple conflicting models for field" )
				.hasMessageContaining( "'flattened'" )
				.satisfies( FailureReportUtils.hasContext(
						EventContexts.fromIndexNames( incompatibleIndex.name(), mainIndex.name() )
				) );
	}

	@Test
	public void flattened_multiIndexes_emptyIndexBinding() {
		StubMappingScope scope = mainIndex.createScope( emptyIndex );

		List<DocumentReference> docs = scope.query().selectEntityReference()
				.where( p -> p.exists().field( "flattened" ) )
				.fetchAllHits();

		// DOCUMENT_2 won't be matched either, since it hasn't got any not-null field
		// DOCUMENT_4 will match, even if the matching field is not a direct child of the targeted path
		assertThat( docs ).hasDocRefHitsAnyOrder( mainIndex.typeName(), DOCUMENT_3, DOCUMENT_4 );
	}

	@Test
	public void flattened_multiIndexes_wrongStorageType() {
		assumeFullMultiIndexCompatibilityCheck();
		StubMappingScope scope = invertedIndex.createScope( mainIndex );

		Assertions.assertThatThrownBy(
				() -> scope.predicate().exists().field( "flattened" )
		)
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "Multiple conflicting models for object field" )
				.hasMessageContaining( "'flattened'" )
				.satisfies( FailureReportUtils.hasContext(
						EventContexts.fromIndexNames( invertedIndex.name(), mainIndex.name() )
				) );
	}

	@Test
	public void flattened_multiIndexes_differentFields() {
		assumeFullMultiIndexCompatibilityCheck();
		StubMappingScope scope = differentFieldsIndex.createScope( mainIndex );

		Assertions.assertThatThrownBy(
				() -> scope.predicate().exists().field( "flattened" )
		)
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "Multiple conflicting models for object field" )
				.hasMessageContaining( "'flattened'" )
				.satisfies( FailureReportUtils.hasContext(
						EventContexts.fromIndexNames( differentFieldsIndex.name(), mainIndex.name() )
				) );
	}

	@Test
	public void flattened_multiIndexes_incompatibleFields() {
		assumeFullMultiIndexCompatibilityCheck();
		StubMappingScope scope = incompatibleFieldsIndex.createScope( mainIndex );

		Assertions.assertThatThrownBy(
				() -> scope.predicate().exists().field( "flattened" )
		)
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "Multiple conflicting models for object field" )
				.hasMessageContaining( "'flattened'" )
				.satisfies( FailureReportUtils.hasContext(
						EventContexts.fromIndexNames( incompatibleFieldsIndex.name(), mainIndex.name() )
				) );
	}

	private void initData() {
		IndexIndexingPlan<?> plan = mainIndex.createIndexingPlan();
		plan.add( referenceProvider( DOCUMENT_0 ), document -> { } );

		plan.add( referenceProvider( DOCUMENT_1 ), document -> {
			document.addValue( mainIndex.binding().string, ANY_STRING );
			document.addValue( mainIndex.binding().numeric, ANY_INTEGER );
		} );
		plan.add( referenceProvider( DOCUMENT_2 ), document -> {
			document.addValue( mainIndex.binding().string, ANY_STRING );
			document.addValue( mainIndex.binding().numeric, ANY_INTEGER );

			document.addObject( mainIndex.binding().nested );
			document.addObject( mainIndex.binding().flattened );
		} );
		plan.add( referenceProvider( DOCUMENT_3 ), document -> {
			document.addValue( mainIndex.binding().string, ANY_STRING );
			document.addValue( mainIndex.binding().numeric, ANY_INTEGER );

			DocumentElement nestedDocument = document.addObject( mainIndex.binding().nested );
			nestedDocument.addValue( mainIndex.binding().nestedString, ANY_STRING );
			nestedDocument.addValue( mainIndex.binding().nestedNumeric, ANY_INTEGER );

			DocumentElement flattedDocument = document.addObject( mainIndex.binding().flattened );
			flattedDocument.addValue( mainIndex.binding().flattenedString, ANY_STRING );
			flattedDocument.addValue( mainIndex.binding().flattenedNumeric, ANY_INTEGER );
		} );
		plan.add( referenceProvider( DOCUMENT_4 ), document -> {
			DocumentElement nestedDocument = document.addObject( mainIndex.binding().nested );
			DocumentElement nestedX2Document = nestedDocument.addObject( mainIndex.binding().nestedX2 );
			nestedX2Document.addValue( mainIndex.binding().nestedX2String, ANY_STRING );

			DocumentElement flattedDocument = document.addObject( mainIndex.binding().flattened );
			DocumentElement flattedX2Document = flattedDocument.addObject( mainIndex.binding().flattenedX2 );
			flattedX2Document.addValue( mainIndex.binding().flattenedX2String, ANY_STRING );
		} );
		plan.add( referenceProvider( DOCUMENT_5 ), document -> {
			document.addObject( mainIndex.binding().nestedNoChild );
			document.addObject( mainIndex.binding().flattenedNoChild );
		} );

		plan.execute().join();
		checkDocumentsCreation();
	}

	private void checkDocumentsCreation() {
		List<DocumentReference> docs = mainIndex.createScope().query().selectEntityReference()
				.where( p -> p.matchAll() )
				.fetchAllHits();

		assertThat( docs ).hasDocRefHitsAnyOrder( mainIndex.typeName(), DOCUMENT_0, DOCUMENT_1, DOCUMENT_2, DOCUMENT_3, DOCUMENT_4, DOCUMENT_5 );
	}

	private void assumeFullMultiIndexCompatibilityCheck() {
		assumeFalse(
				"We do not test some Multi-indexing compatibility checks if the backend allows these",
				TckConfiguration.get().getBackendFeatures().lenientOnMultiIndexesCompatibilityChecks()
		);
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
			root.objectField( "nested", ObjectFieldStorage.FLATTENED ).toReference();

			// Use NESTED for flattened
			root.objectField( "flattened", ObjectFieldStorage.NESTED ).toReference();
		}
	}

	private static class DifferentFieldsIndexBinding {
		DifferentFieldsIndexBinding(IndexSchemaElement root) {
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

	private static class IncompatibleFieldsIndexBinding {
		IncompatibleFieldsIndexBinding(IndexSchemaElement root) {
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
