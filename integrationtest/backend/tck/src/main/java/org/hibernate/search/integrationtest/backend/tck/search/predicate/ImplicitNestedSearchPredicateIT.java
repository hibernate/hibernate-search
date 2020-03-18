/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.search.predicate;

import static org.hibernate.search.util.impl.integrationtest.common.assertion.SearchHitsAssert.assertThat;
import static org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMapperUtils.referenceProvider;

import java.util.List;
import java.util.function.Function;

import org.hibernate.search.engine.backend.common.DocumentReference;
import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.engine.backend.document.IndexObjectFieldReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaObjectField;
import org.hibernate.search.engine.backend.document.model.dsl.ObjectFieldStorage;
import org.hibernate.search.engine.backend.types.Projectable;
import org.hibernate.search.engine.backend.types.Sortable;
import org.hibernate.search.engine.backend.work.execution.spi.IndexIndexingPlan;
import org.hibernate.search.engine.search.predicate.SearchPredicate;
import org.hibernate.search.engine.search.predicate.dsl.PredicateFinalStep;
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory;
import org.hibernate.search.engine.spatial.GeoBoundingBox;
import org.hibernate.search.engine.spatial.GeoPoint;
import org.hibernate.search.engine.spatial.GeoPolygon;
import org.hibernate.search.integrationtest.backend.tck.testsupport.configuration.DefaultAnalysisDefinitions;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappingIndexManager;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappingScope;
import org.hibernate.search.util.impl.test.SubTest;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class ImplicitNestedSearchPredicateIT {

	private static final String INDEX_NAME = "IndexName";

	private static final String DOCUMENT_1 = "1";
	private static final String DOCUMENT_2 = "2";

	private static final String NESTED_1 = "nested1";
	private static final String NESTED_2 = "nested2";
	private static final String NESTED_3 = "nested3";
	private static final String NESTED_1_2 = NESTED_1 + "." + NESTED_2;
	private static final String NESTED_1_2_3 = NESTED_1_2 + "." + NESTED_3;

	private static final String ANY_STRING = "Any String";
	private static final String OTHER_STRING = "Other String";

	private static final int ANY_INTEGER = 173173;
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
	public SearchSetupHelper setupHelper = new SearchSetupHelper();

	private IndexMapping indexMapping;
	private StubMappingIndexManager indexManager;

	@Before
	public void setup() {
		setupHelper.start()
				.withIndex(
						INDEX_NAME,
						ctx -> this.indexMapping = new IndexMapping( ctx.getSchemaElement() ),
						indexManager -> this.indexManager = indexManager
				)
				.setup();

		initData();
	}

	@Test
	public void nested_X2_explicit() {
		StubMappingScope scope = indexManager.createScope();

		List<DocumentReference> docs = scope.query()
				.where( p -> p.nested().objectField( NESTED_1 )
						.nest( f -> f.nested().objectField( NESTED_1_2 )
								.nest( g -> g.match().field( NESTED_1_2 + ".numeric" ).matching( ANY_INTEGER ) )
						) )
				.fetchAllHits();

		assertThat( docs ).hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_1 );
	}

	@Test
	public void nested_X2_implicit() {
		StubMappingScope scope = indexManager.createScope();

		List<DocumentReference> docs = scope.query()
				.where( f -> f.match().field( NESTED_1_2 + ".numeric" ).matching( ANY_INTEGER ) )
				.fetchAllHits();

		assertThat( docs ).hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_1 );
	}

	@Test
	public void nested_X2_explicit_implicit() {
		StubMappingScope scope = indexManager.createScope();

		List<DocumentReference> docs = scope.query()
				.where( p -> p.nested().objectField( NESTED_1 )
						.nest( g -> g.match().field( NESTED_1_2 + ".numeric" ).matching( ANY_INTEGER ) )
				)
				.fetchAllHits();

		assertThat( docs ).hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_1 );
	}

	@Test
	public void nested_X3_explicitX2_implicit() {
		StubMappingScope scope = indexManager.createScope();

		List<DocumentReference> docs = scope.query()
				.where( p -> p.nested().objectField( NESTED_1 )
						.nest( f -> f.nested().objectField( NESTED_1_2 )
								.nest( g -> g.match().field( NESTED_1_2_3 + ".string" ).matching( ANY_STRING ) )
						) )
				.fetchAllHits();

		assertThat( docs ).hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_1 );
	}

	@Test
	public void nested_X3_explicitX2_implicit_simpleQuery() {
		StubMappingScope scope = indexManager.createScope();

		List<DocumentReference> docs = scope.query()
				.where( p -> p.nested().objectField( NESTED_1 )
						.nest( f -> f.nested().objectField( NESTED_1_2 )
								.nest( g -> g.simpleQueryString().field( NESTED_1_2_3 + ".text" ).matching( SOME_SIMPLE_QUERY_STRING ) )
						) )
				.fetchAllHits();

		assertThat( docs ).hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_1 );
	}

	@Test
	public void nested_X3_explicit_implicitX2() {
		StubMappingScope scope = indexManager.createScope();

		List<DocumentReference> docs = scope.query()
				.where( p -> p.nested().objectField( NESTED_1 )
						.nest( g -> g.match().field( NESTED_1_2_3 + ".string" ).matching( ANY_STRING ) )
				)
				.fetchAllHits();

		assertThat( docs ).hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_1 );
	}

	@Test
	public void predicate_match_string() {
		verify_implicit_nest( p -> p.match().field( NESTED_1 + ".string" ).matching( ANY_STRING ) );
	}

	@Test
	public void predicate_match_numeric() {
		verify_implicit_nest( p -> p.match().field( NESTED_1 + ".numeric" ).matching( ANY_INTEGER ) );
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
		verify_implicit_nest( p -> p.range().field( NESTED_1 + ".numeric" ).atMost( ANY_INTEGER ) );
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
		SubTest.expectException( () -> indexManager.createScope()
				.predicate().simpleQueryString().field( NESTED_1 + ".text" ).field( "text" )
		)
				.assertThrown()
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "Simple query string targets fields" )
				.hasMessageContaining( "spanning multiple nested paths" )
				.hasMessageContaining( NESTED_1 + ".text" );
	}

	private void verify_implicit_nest(Function<? super SearchPredicateFactory, ? extends PredicateFinalStep> implicitPredicate) {
		verify_implicit_nest( implicitPredicate, false );
	}

	private void verify_implicit_nest(Function<? super SearchPredicateFactory, ? extends PredicateFinalStep> implicitPredicate, boolean allMatch) {
		StubMappingScope scope = indexManager.createScope();
		SearchPredicate explicitPredicate = scope.predicate().nested().objectField( NESTED_1 ).nest( implicitPredicate ).toPredicate();

		// test the explicit form
		List<DocumentReference> docs = scope.query().selectEntityReference()
				.where( explicitPredicate )
				.fetchAllHits();

		if ( allMatch ) {
			assertThat( docs ).hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_1, DOCUMENT_2 );
		}
		else {
			assertThat( docs ).hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_1 );
		}

		// test the implicit form
		docs = scope.query().selectEntityReference()
				.where( implicitPredicate )
				.fetchAllHits();

		if ( allMatch ) {
			assertThat( docs ).hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_1, DOCUMENT_2 );
		}
		else {
			assertThat( docs ).hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_1 );
		}
	}

	private void initData() {
		IndexIndexingPlan plan = indexManager.createIndexingPlan();
		plan.add( referenceProvider( DOCUMENT_1 ), document -> {
			DocumentElement nestedDocument = document.addObject( indexMapping.nested );
			nestedDocument.addValue( indexMapping.nestedString, ANY_STRING );
			nestedDocument.addValue( indexMapping.nestedNumeric, ANY_INTEGER );
			nestedDocument.addValue( indexMapping.nestedText, SOME_PHRASE_TEXT );
			nestedDocument.addValue( indexMapping.nestedGeo, G11 );

			DocumentElement nestedDocumentX2 = nestedDocument.addObject( indexMapping.nestedX2 );
			nestedDocumentX2.addValue( indexMapping.nestedX2Numeric, ANY_INTEGER );

			DocumentElement nestedDocumentX3 = nestedDocumentX2.addObject( indexMapping.nestedX3 );
			nestedDocumentX3.addValue( indexMapping.nestedX3String, ANY_STRING );
			nestedDocumentX3.addValue( indexMapping.nestedX3Text, SOME_PHRASE_TEXT );
		} );
		plan.add( referenceProvider( DOCUMENT_2 ), document -> {
			DocumentElement nestedDocument = document.addObject( indexMapping.nested );
			nestedDocument.addValue( indexMapping.nestedString, OTHER_STRING );
			nestedDocument.addValue( indexMapping.nestedNumeric, OTHER_INTEGER );
			nestedDocument.addValue( indexMapping.nestedText, OTHER_PHRASE_TEXT );
			nestedDocument.addValue( indexMapping.nestedGeo, G33 );

			DocumentElement nestedDocumentX2 = nestedDocument.addObject( indexMapping.nestedX2 );
			nestedDocumentX2.addValue( indexMapping.nestedX2Numeric, OTHER_INTEGER );

			DocumentElement nestedDocumentX3 = nestedDocumentX2.addObject( indexMapping.nestedX3 );
			nestedDocumentX3.addValue( indexMapping.nestedX3String, OTHER_STRING );
			nestedDocumentX3.addValue( indexMapping.nestedX3Text, OTHER_PHRASE_TEXT );
		} );
		plan.execute().join();
	}

	private static class IndexMapping {
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

		IndexMapping(IndexSchemaElement root) {
			text = root.field( "text", f -> f.asString().analyzer( DefaultAnalysisDefinitions.ANALYZER_STANDARD_ENGLISH.name ) ).toReference();

			IndexSchemaObjectField nestedObject = root.objectField( NESTED_1, ObjectFieldStorage.NESTED );
			this.nested = nestedObject.toReference();
			this.nestedString = nestedObject.field( "string", f -> f.asString().projectable( Projectable.YES ).sortable( Sortable.YES ) ).toReference();
			this.nestedNumeric = nestedObject.field( "numeric", f -> f.asInteger() ).toReference();
			this.nestedText = nestedObject.field( "text", f -> f.asString().analyzer( DefaultAnalysisDefinitions.ANALYZER_STANDARD_ENGLISH.name ) ).toReference();
			this.nestedGeo = nestedObject.field( "geo", f -> f.asGeoPoint() ).toReference();

			IndexSchemaObjectField nestedObjectX2 = nestedObject.objectField( NESTED_2, ObjectFieldStorage.NESTED );
			this.nestedX2 = nestedObjectX2.toReference();
			this.nestedX2Numeric = nestedObjectX2.field( "numeric", f -> f.asInteger() ).toReference();

			IndexSchemaObjectField nestedObjectX3 = nestedObjectX2.objectField( NESTED_3, ObjectFieldStorage.NESTED );
			this.nestedX3 = nestedObjectX3.toReference();
			this.nestedX3String = nestedObjectX3.field( "string", f -> f.asString() ).toReference();
			this.nestedX3Text = nestedObjectX3.field( "text", f -> f.asString().analyzer( DefaultAnalysisDefinitions.ANALYZER_STANDARD_ENGLISH.name ) ).toReference();
		}
	}
}
