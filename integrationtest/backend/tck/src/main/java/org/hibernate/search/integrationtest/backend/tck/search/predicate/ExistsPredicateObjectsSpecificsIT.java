/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.search.predicate;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hibernate.search.util.impl.integrationtest.common.assertion.SearchResultAssert.assertThatQuery;
import static org.junit.Assume.assumeFalse;

import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.engine.backend.document.IndexObjectFieldReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaObjectField;
import org.hibernate.search.engine.backend.types.ObjectStructure;
import org.hibernate.search.engine.backend.types.Sortable;
import org.hibernate.search.engine.reporting.spi.EventContexts;
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.TckConfiguration;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.common.FailureReportUtils;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.SimpleMappedIndex;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappedIndex;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappingScope;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

public class ExistsPredicateObjectsSpecificsIT {

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

	@ClassRule
	public static final SearchSetupHelper setupHelper = new SearchSetupHelper();

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
	private static final SimpleMappedIndex<IncompatibleFieldsIndexBinding> incompatibleFieldsIndex =
			SimpleMappedIndex.of( IncompatibleFieldsIndexBinding::new ).name( "incompatibleFields" );

	@BeforeClass
	public static void setup() {
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
		assertThatQuery( mainIndex.query()
				.where( f -> f.exists().field( "nested" ) ) )
				// DOCUMENT_2 won't be matched either, since it hasn't got any not-null field
				// DOCUMENT_4 won't be matched either, since we use a nested structure for the inner object field
				.hasDocRefHitsAnyOrder( mainIndex.typeName(), DOCUMENT_3 );
	}

	@Test
	public void nested_noChild() {
		assertThatQuery( mainIndex.query()
				.where( f -> f.exists().field( "nestedNoChild" ) ) )
				.hasNoHits();
	}

	@Test
	public void nested_multiIndexes_compatibleIndexBinding() {
		StubMappingScope scope = mainIndex.createScope( compatibleIndex );

		assertThatQuery( scope.query()
				.where( f -> f.exists().field( "nested" ) ) )
				// DOCUMENT_2 won't be matched either, since it hasn't got any not-null field
				// DOCUMENT_4 won't be matched either, since we use a nested structure for the inner object field
				.hasDocRefHitsAnyOrder( mainIndex.typeName(), DOCUMENT_3 );
	}

	@Test
	public void nested_multiIndexes_incompatibleIndexBinding() {
		assumeFullMultiIndexCompatibilityCheck();
		SearchPredicateFactory f = mainIndex.createScope( incompatibleIndex ).predicate();

		assertThatThrownBy( () -> f.exists().field( "nested" ) )
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

		assertThatQuery( scope.query()
				.where( f -> f.exists().field( "nested" ) ) )
				// DOCUMENT_2 won't be matched either, since it hasn't got any not-null field
				// DOCUMENT_4 won't be matched either, since we use a nested structure for the inner object field
				.hasDocRefHitsAnyOrder( mainIndex.typeName(), DOCUMENT_3 );
	}

	@Test
	public void nested_multiIndexes_wrongStructure() {
		assumeFullMultiIndexCompatibilityCheck();
		SearchPredicateFactory f = mainIndex.createScope( invertedIndex ).predicate();

		assertThatThrownBy( () -> f.exists().field( "nested" ) )
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
		SearchPredicateFactory f = mainIndex.createScope( differentFieldsIndex ).predicate();

		assertThatThrownBy( () -> f.exists().field( "nested" ) )
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
		SearchPredicateFactory f = mainIndex.createScope( incompatibleFieldsIndex ).predicate();

		assertThatThrownBy( () -> f.exists().field( "nested" ) )
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "Multiple conflicting models for object field" )
				.hasMessageContaining( "'nested'" )
				.satisfies( FailureReportUtils.hasContext(
						EventContexts.fromIndexNames( mainIndex.name(), incompatibleFieldsIndex.name() )
				) );
	}

	@Test
	public void flattened() {
		assertThatQuery( mainIndex.query()
				.where( p -> p.exists().field( "flattened" ) ) )
				// DOCUMENT_2 won't be matched either, since it hasn't got any not-null field
				// DOCUMENT_4 will match, even if the matching field is not a direct child of the targeted path
				.hasDocRefHitsAnyOrder( mainIndex.typeName(), DOCUMENT_3, DOCUMENT_4 );
	}

	@Test
	public void flattened_noChild() {
		assertThatQuery( mainIndex.query()
				.where( p -> p.exists().field( "flattenedNoChild" ) ) )
				.hasNoHits();
	}

	@Test
	public void flattened_multiIndexes_compatibleIndexBinding() {
		assertThatQuery( mainIndex.query()
				.where( p -> p.exists().field( "flattened" ) ) )
				// DOCUMENT_2 won't be matched either, since it hasn't got any not-null field
				// DOCUMENT_4 will match, even if the matching field is not a direct child of the targeted path
				.hasDocRefHitsAnyOrder( mainIndex.typeName(), DOCUMENT_3, DOCUMENT_4 );
	}

	@Test
	public void flattened_multiIndexes_incompatibleIndexBinding() {
		assumeFullMultiIndexCompatibilityCheck();
		SearchPredicateFactory f = incompatibleIndex.createScope( mainIndex ).predicate();

		assertThatThrownBy( () -> f.exists().field( "flattened" ) )
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

		assertThatQuery( scope.query()
				.where( p -> p.exists().field( "flattened" ) ) )
				// DOCUMENT_2 won't be matched either, since it hasn't got any not-null field
				// DOCUMENT_4 will match, even if the matching field is not a direct child of the targeted path
				.hasDocRefHitsAnyOrder( mainIndex.typeName(), DOCUMENT_3, DOCUMENT_4 );
	}

	@Test
	public void flattened_multiIndexes_wrongStructure() {
		assumeFullMultiIndexCompatibilityCheck();
		SearchPredicateFactory f = invertedIndex.createScope( mainIndex ).predicate();

		assertThatThrownBy( () -> f.exists().field( "flattened" ) )
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
		SearchPredicateFactory f = differentFieldsIndex.createScope( mainIndex ).predicate();

		assertThatThrownBy( () -> f.exists().field( "flattened" ) )
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
		SearchPredicateFactory f = incompatibleFieldsIndex.createScope( mainIndex ).predicate();

		assertThatThrownBy( () -> f.exists().field( "flattened" ) )
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "Multiple conflicting models for object field" )
				.hasMessageContaining( "'flattened'" )
				.satisfies( FailureReportUtils.hasContext(
						EventContexts.fromIndexNames( incompatibleFieldsIndex.name(), mainIndex.name() )
				) );
	}

	private static void initData() {
		mainIndex.bulkIndexer()
				.add( DOCUMENT_0, document -> { } )
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

	private static class IncompatibleFieldsIndexBinding {
		IncompatibleFieldsIndexBinding(IndexSchemaElement root) {
			IndexSchemaObjectField nestedObject = root.objectField( "nested", ObjectStructure.NESTED );
			nestedObject.toReference();

			// field has same name, but with an incompatible exists predicates: string vs BigDecimal
			nestedObject.field( "string", f -> f.asBigDecimal().decimalScale( 3 ) ).toReference();
			nestedObject.field( "numeric", f -> f.asInteger() ).toReference();

			nestedObject.objectField( "nestedX2", ObjectStructure.NESTED ).toReference();

			IndexSchemaObjectField flattenedObject = root.objectField( "flattened", ObjectStructure.FLATTENED );
			flattenedObject.toReference();

			flattenedObject.field( "string", f -> f.asString() ).toReference();
			// field has same name, but with an incompatible exists predicates: unSortable vs Sortable
			flattenedObject.field( "numeric", f -> f.asInteger().sortable( Sortable.YES ) ).toReference();

			nestedObject.objectField( "flattenedX2", ObjectStructure.FLATTENED ).toReference();
		}
	}
}
