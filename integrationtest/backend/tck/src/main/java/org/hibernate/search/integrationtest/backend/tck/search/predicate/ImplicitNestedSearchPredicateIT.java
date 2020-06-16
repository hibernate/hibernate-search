/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.search.predicate;

import static org.hibernate.search.util.impl.integrationtest.common.assertion.SearchResultAssert.assertThat;

import java.util.function.Function;

import org.hibernate.search.engine.backend.common.DocumentReference;
import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.engine.backend.document.IndexObjectFieldReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaObjectField;
import org.hibernate.search.engine.backend.types.ObjectStructure;
import org.hibernate.search.engine.backend.types.Projectable;
import org.hibernate.search.engine.backend.types.Sortable;
import org.hibernate.search.engine.search.predicate.SearchPredicate;
import org.hibernate.search.engine.search.predicate.dsl.PredicateFinalStep;
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory;
import org.hibernate.search.engine.search.query.SearchQuery;
import org.hibernate.search.engine.spatial.GeoBoundingBox;
import org.hibernate.search.engine.spatial.GeoPoint;
import org.hibernate.search.engine.spatial.GeoPolygon;
import org.hibernate.search.integrationtest.backend.tck.testsupport.configuration.DefaultAnalysisDefinitions;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.SimpleMappedIndex;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappingScope;
import org.assertj.core.api.Assertions;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class ImplicitNestedSearchPredicateIT {

	private static final String DOCUMENT_1 = "1";
	private static final String DOCUMENT_2 = "2";

	private static final String NESTED_1 = "nested1";
	private static final String NESTED_2 = "nested2";
	private static final String NESTED_3 = "nested3";
	private static final String NESTED_1_2 = NESTED_1 + "." + NESTED_2;
	private static final String NESTED_1_2_3 = NESTED_1_2 + "." + NESTED_3;

	private static final String FLATTENED = "flattened";
	private static final String NEST_FLAT_NEST = NESTED_1 + "." + FLATTENED + "." + NESTED_2;

	private static final String SOME_STRING = "Any String";
	private static final String OTHER_STRING = "Other String";

	private static final int SOME_INTEGER = 173173;
	private static final int OTHER_INTEGER = 739379;

	private static final String SOME_PHRASE_TEXT = "Once upon a time, there was a quick fox in a big house.";
	private static final String OTHER_PHRASE_TEXT = "The cat is on the table.";

	private static final String SOME_PHRASE_KEY = "quick fox";
	private static final String SOME_WILDCARD_PATTERN = "f*x";
	private static final String SOME_SIMPLE_QUERY_STRING = "quick + fox";

	private static final GeoPoint G00 = GeoPoint.of( 0, 0 );
	private static final GeoPoint G20 = GeoPoint.of( 2, 0 );
	private static final GeoPoint G02 = GeoPoint.of( 0, 2 );
	private static final GeoPoint G22 = GeoPoint.of( 2, 2 );
	private static final GeoPoint G11 = GeoPoint.of( 1, 1 );
	private static final GeoPoint G33 = GeoPoint.of( 3, 3 );

	@Rule
	public final SearchSetupHelper setupHelper = new SearchSetupHelper();

	private final SimpleMappedIndex<IndexBinding> index = SimpleMappedIndex.of( IndexBinding::new );

	@Before
	public void setup() {
		setupHelper.start().withIndex( index ).setup();

		initData();
	}

	@Test
	public void nested_X2_explicit() {
		StubMappingScope scope = index.createScope();

		SearchQuery<DocumentReference> query = scope.query()
				.where( p -> p.nested().objectField( NESTED_1 )
						.nest( f -> f.nested().objectField( NESTED_1_2 )
								.nest( g -> g.match().field( NESTED_1_2 + ".numeric" ).matching( SOME_INTEGER ) )
						) )
				.toQuery();

		assertThat( query ).hasDocRefHitsAnyOrder( index.typeName(), DOCUMENT_1 );
	}

	@Test
	public void nested_X2_implicit() {
		StubMappingScope scope = index.createScope();

		SearchQuery<DocumentReference> query = scope.query()
				.where( p -> p.match().field( NESTED_1_2 + ".numeric" ).matching( SOME_INTEGER ) )
				.toQuery();

		assertThat( query ).hasDocRefHitsAnyOrder( index.typeName(), DOCUMENT_1 );
	}

	@Test
	public void nested_X2_explicit_implicit() {
		StubMappingScope scope = index.createScope();

		SearchQuery<DocumentReference> query = scope.query()
				.where( p -> p.nested().objectField( NESTED_1 )
						.nest( f -> f.match().field( NESTED_1_2 + ".numeric" ).matching( SOME_INTEGER ) )
				)
				.toQuery();

		assertThat( query ).hasDocRefHitsAnyOrder( index.typeName(), DOCUMENT_1 );
	}

	@Test
	public void nested_X3_explicitX2_implicit() {
		StubMappingScope scope = index.createScope();

		SearchQuery<DocumentReference> query = scope.query()
				.where( p -> p.nested().objectField( NESTED_1 )
						.nest( f -> f.nested().objectField( NESTED_1_2 )
								.nest( g -> g.match().field( NESTED_1_2_3 + ".string" ).matching( SOME_STRING ) )
						) )
				.toQuery();

		assertThat( query ).hasDocRefHitsAnyOrder( index.typeName(), DOCUMENT_1 );
	}

	@Test
	public void nested_X3_explicitX2_implicit_simpleQuery() {
		StubMappingScope scope = index.createScope();

		SearchQuery<DocumentReference> query = scope.query()
				.where( p -> p.nested().objectField( NESTED_1 )
						.nest( f -> f.nested().objectField( NESTED_1_2 )
								.nest( g -> g.simpleQueryString().field( NESTED_1_2_3 + ".text" ).matching( SOME_SIMPLE_QUERY_STRING ) )
						) )
				.toQuery();

		assertThat( query ).hasDocRefHitsAnyOrder( index.typeName(), DOCUMENT_1 );
	}

	@Test
	public void nested_X3_explicit_implicitX2() {
		StubMappingScope scope = index.createScope();

		SearchQuery<DocumentReference> query = scope.query()
				.where( p -> p.nested().objectField( NESTED_1 )
						.nest( f -> f.match().field( NESTED_1_2_3 + ".string" ).matching( SOME_STRING ) )
				)
				.toQuery();

		assertThat( query ).hasDocRefHitsAnyOrder( index.typeName(), DOCUMENT_1 );
	}

	@Test
	public void nested_X3_explicit_implicitX2_simpleQuery() {
		StubMappingScope scope = index.createScope();

		SearchQuery<DocumentReference> query = scope.query()
				.where( p -> p.nested().objectField( NESTED_1 )
							.nest( f -> f.simpleQueryString().field( NESTED_1_2_3 + ".text" ).matching( SOME_SIMPLE_QUERY_STRING ) )
				)
				.toQuery();

		assertThat( query ).hasDocRefHitsAnyOrder( index.typeName(), DOCUMENT_1 );
	}

	@Test
	public void flattenedStepIsSkipped() {
		StubMappingScope scope = index.createScope();

		SearchQuery<DocumentReference> query = scope.query()
				.where( p -> p.match().field( NEST_FLAT_NEST + ".numeric" ).matching( SOME_INTEGER ) )
				.toQuery();

		assertThat( query ).hasDocRefHitsAnyOrder( index.typeName(), DOCUMENT_1 );
	}

	@Test
	public void flattenedStepIsSkipped_simpleQuery() {
		StubMappingScope scope = index.createScope();

		SearchQuery<DocumentReference> query = scope.query()
				.where( p -> p.simpleQueryString().field( NEST_FLAT_NEST + ".text" ).matching( SOME_SIMPLE_QUERY_STRING ) )
				.toQuery();

		assertThat( query ).hasDocRefHitsAnyOrder( index.typeName(), DOCUMENT_1 );
	}

	@Test
	public void predicate_match_string() {
		verify_implicit_nest( p -> p.match().field( NESTED_1 + ".string" ).matching( SOME_STRING ) );
	}

	@Test
	public void predicate_match_numeric() {
		verify_implicit_nest( p -> p.match().field( NESTED_1 + ".numeric" ).matching( SOME_INTEGER ) );
	}

	@Test
	public void predicate_phrase() {
		verify_implicit_nest( p -> p.phrase().field( NESTED_1 + ".text" ).matching( SOME_PHRASE_KEY ) );
	}

	@Test
	public void predicate_matchAll() {
		verify_implicit_nest( p -> p.matchAll(), true );
	}

	@Test
	public void predicate_wildcard() {
		verify_implicit_nest( p -> p.wildcard().field( NESTED_1 + ".text" ).matching( SOME_WILDCARD_PATTERN ) );
	}

	@Test
	public void predicate_simpleQueryString() {
		verify_implicit_nest( p -> p.simpleQueryString().field( NESTED_1 + ".text" ).matching( SOME_SIMPLE_QUERY_STRING ) );
	}

	@Test
	public void predicate_range() {
		verify_implicit_nest( p -> p.range().field( NESTED_1 + ".numeric" ).atMost( SOME_INTEGER ) );
	}

	@Test
	public void predicate_geoPoly() {
		verify_implicit_nest( p -> p.spatial().within().field( NESTED_1 + ".geo" ).polygon( GeoPolygon.of( G00, G02, G22, G20, G00 ) ) );
	}

	@Test
	public void predicate_geoBox() {
		verify_implicit_nest( p -> p.spatial().within().field( NESTED_1 + ".geo" ).boundingBox( GeoBoundingBox.of( G20, G02 ) ) );
	}

	@Test
	public void predicate_geoCircle() {
		verify_implicit_nest( p -> p.spatial().within().field( NESTED_1 + ".geo" ).circle( G11, 1 ) );
	}

	@Test
	public void predicate_exists_field() {
		verify_implicit_nest( p -> p.exists().field( NESTED_1 + ".geo" ), true );
	}

	@Test
	public void predicate_exists_object() {
		verify_implicit_nest( p -> p.exists().field( NESTED_1 ), true );
	}

	@Test
	public void predicate_simpleQueryString_multipleNestedPaths() {
		Assertions.assertThatThrownBy( () -> index.createScope()
				.predicate().simpleQueryString().field( NESTED_1 + ".text" ).field( "text" )
		)
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "Simple query string targets fields" )
				.hasMessageContaining( "spanning multiple nested paths" )
				.hasMessageContaining( NESTED_1 + ".text" );
	}

	private void verify_implicit_nest(Function<? super SearchPredicateFactory, ? extends PredicateFinalStep> implicitPredicate) {
		verify_implicit_nest( implicitPredicate, false );
	}

	private void verify_implicit_nest(Function<? super SearchPredicateFactory, ? extends PredicateFinalStep> implicitPredicate, boolean allMatch) {
		StubMappingScope scope = index.createScope();
		SearchPredicate explicitPredicate = scope.predicate().nested().objectField( NESTED_1 ).nest( implicitPredicate ).toPredicate();

		// test the explicit form
		SearchQuery<DocumentReference> query = scope.query().selectEntityReference()
				.where( explicitPredicate )
				.toQuery();

		if ( allMatch ) {
			assertThat( query ).hasDocRefHitsAnyOrder( index.typeName(), DOCUMENT_1, DOCUMENT_2 );
		}
		else {
			assertThat( query ).hasDocRefHitsAnyOrder( index.typeName(), DOCUMENT_1 );
		}

		// test the implicit form
		query = scope.query().selectEntityReference()
				.where( implicitPredicate )
				.toQuery();

		if ( allMatch ) {
			assertThat( query ).hasDocRefHitsAnyOrder( index.typeName(), DOCUMENT_1, DOCUMENT_2 );
		}
		else {
			assertThat( query ).hasDocRefHitsAnyOrder( index.typeName(), DOCUMENT_1 );
		}
	}

	private void initData() {
		index.bulkIndexer()
				.add( DOCUMENT_1, document -> {
					DocumentElement nestedDocument = document.addObject( index.binding().nested );
					nestedDocument.addValue( index.binding().nestedString, SOME_STRING );
					nestedDocument.addValue( index.binding().nestedNumeric, SOME_INTEGER );
					nestedDocument.addValue( index.binding().nestedText, SOME_PHRASE_TEXT );
					nestedDocument.addValue( index.binding().nestedGeo, G11 );

					DocumentElement nestedDocumentX2 = nestedDocument.addObject( index.binding().nestedX2 );
					nestedDocumentX2.addValue( index.binding().nestedX2Numeric, SOME_INTEGER );

					DocumentElement nestedDocumentX3 = nestedDocumentX2.addObject( index.binding().nestedX3 );
					nestedDocumentX3.addValue( index.binding().nestedX3String, SOME_STRING );
					nestedDocumentX3.addValue( index.binding().nestedX3Text, SOME_PHRASE_TEXT );

					DocumentElement nestFlatNestDocument = nestedDocument.addObject( index.binding().nestFlat ).addObject( index.binding().nestFlatNest );
					nestFlatNestDocument.addValue( index.binding().nestFlatNestNumeric, SOME_INTEGER );
					nestFlatNestDocument.addValue( index.binding().nestFlatNestText, SOME_PHRASE_KEY );
				} )
				.add( DOCUMENT_2, document -> {
					DocumentElement nestedDocument = document.addObject( index.binding().nested );
					nestedDocument.addValue( index.binding().nestedString, OTHER_STRING );
					nestedDocument.addValue( index.binding().nestedNumeric, OTHER_INTEGER );
					nestedDocument.addValue( index.binding().nestedText, OTHER_PHRASE_TEXT );
					nestedDocument.addValue( index.binding().nestedGeo, G33 );

					DocumentElement nestedDocumentX2 = nestedDocument.addObject( index.binding().nestedX2 );
					nestedDocumentX2.addValue( index.binding().nestedX2Numeric, OTHER_INTEGER );

					DocumentElement nestedDocumentX3 = nestedDocumentX2.addObject( index.binding().nestedX3 );
					nestedDocumentX3.addValue( index.binding().nestedX3String, OTHER_STRING );
					nestedDocumentX3.addValue( index.binding().nestedX3Text, OTHER_PHRASE_TEXT );

					DocumentElement nestFlatNestDocument = nestedDocument.addObject( index.binding().nestFlat ).addObject( index.binding().nestFlatNest );
					nestFlatNestDocument.addValue( index.binding().nestFlatNestNumeric, OTHER_INTEGER );
					nestFlatNestDocument.addValue( index.binding().nestFlatNestText, OTHER_STRING );
				} )
				.join();
	}

	private static class IndexBinding {
		final IndexFieldReference<String> text;

		final IndexObjectFieldReference nested;
		final IndexFieldReference<String> nestedString;
		final IndexFieldReference<Integer> nestedNumeric;
		final IndexFieldReference<String> nestedText;
		final IndexFieldReference<GeoPoint> nestedGeo;

		final IndexObjectFieldReference nestedX2;
		final IndexFieldReference<Integer> nestedX2Numeric;

		final IndexObjectFieldReference nestedX3;
		final IndexFieldReference<String> nestedX3String;
		final IndexFieldReference<String> nestedX3Text;

		final IndexObjectFieldReference nestFlat;

		final IndexObjectFieldReference nestFlatNest;
		final IndexFieldReference<Integer> nestFlatNestNumeric;
		final IndexFieldReference<String> nestFlatNestText;

		IndexBinding(IndexSchemaElement root) {
			text = root.field( "text", f -> f.asString().analyzer( DefaultAnalysisDefinitions.ANALYZER_STANDARD_ENGLISH.name ) ).toReference();

			IndexSchemaObjectField nestedObject = root.objectField( NESTED_1, ObjectStructure.NESTED );
			this.nested = nestedObject.toReference();
			this.nestedString = nestedObject.field( "string", f -> f.asString().projectable( Projectable.YES ).sortable( Sortable.YES ) ).toReference();
			this.nestedNumeric = nestedObject.field( "numeric", f -> f.asInteger() ).toReference();
			this.nestedText = nestedObject.field( "text", f -> f.asString().analyzer( DefaultAnalysisDefinitions.ANALYZER_STANDARD_ENGLISH.name ) ).toReference();
			this.nestedGeo = nestedObject.field( "geo", f -> f.asGeoPoint() ).toReference();

			IndexSchemaObjectField nestedObjectX2 = nestedObject.objectField( NESTED_2, ObjectStructure.NESTED );
			this.nestedX2 = nestedObjectX2.toReference();
			this.nestedX2Numeric = nestedObjectX2.field( "numeric", f -> f.asInteger() ).toReference();

			IndexSchemaObjectField nestedObjectX3 = nestedObjectX2.objectField( NESTED_3, ObjectStructure.NESTED );
			this.nestedX3 = nestedObjectX3.toReference();
			this.nestedX3String = nestedObjectX3.field( "string", f -> f.asString() ).toReference();
			this.nestedX3Text = nestedObjectX3.field( "text", f -> f.asString().analyzer( DefaultAnalysisDefinitions.ANALYZER_STANDARD_ENGLISH.name ) ).toReference();

			IndexSchemaObjectField nestFlatObject = nestedObject.objectField( FLATTENED, ObjectStructure.FLATTENED );
			this.nestFlat = nestFlatObject.toReference();

			IndexSchemaObjectField nestFlatNestObject = nestFlatObject.objectField( NESTED_2 );
			this.nestFlatNest = nestFlatNestObject.toReference();
			this.nestFlatNestNumeric = nestFlatNestObject.field( "numeric", f -> f.asInteger() ).toReference();
			this.nestFlatNestText = nestFlatNestObject.field( "text", f -> f.asString().analyzer( DefaultAnalysisDefinitions.ANALYZER_STANDARD_ENGLISH.name ) ).toReference();
		}
	}
}
