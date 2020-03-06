/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.elasticsearch.tmp;

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
import org.hibernate.search.integrationtest.backend.tck.testsupport.configuration.DefaultAnalysisDefinitions;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappingIndexManager;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappingScope;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class ImplicitNestedSearchPredicateIT {

	private static final String INDEX_NAME = "IndexName";
	private static final String DOCUMENT_1 = "1";

	private static final String ANY_STRING = "Any String";
	private static final int ANY_INTEGER = 173173;

	private static final String SOME_PHRASE_KEY = "quick fox";
	private static final String SOME_PHRASE_TEXT = "Once upon a time, there was a quick fox in a big house.";

	private static final String SOME_WILDCARD_PATTERN = "f*x";

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
				.where( p -> p.nested().objectField( "nested" )
						.nest( f -> f.nested().objectField( "nested.nested" )
								.nest( g -> g.match().field( "nested.nested.numeric" ).matching( ANY_INTEGER ) )
						) )
				.fetchAllHits();

		assertThat( docs ).hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_1 );
	}

	@Test
	public void nested_X2_implicit() {
		StubMappingScope scope = indexManager.createScope();

		List<DocumentReference> docs = scope.query()
				.where( f -> f.match().field( "nested.nested.numeric" ).matching( ANY_INTEGER ) )
				.fetchAllHits();

		assertThat( docs ).hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_1 );
	}

	@Test
	public void nested_X2_explicit_implicit() {
		StubMappingScope scope = indexManager.createScope();

		List<DocumentReference> docs = scope.query()
				.where( p -> p.nested().objectField( "nested" )
						.nest( g -> g.match().field( "nested.nested.numeric" ).matching( ANY_INTEGER ) )
				)
				.fetchAllHits();

		assertThat( docs ).hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_1 );
	}

	@Test
	public void predicate_match_string() {
		verify_implicit_nest( p -> p.match().field( "nested.string" ).matching( ANY_STRING ) );
	}

	@Test
	public void predicate_match_numeric() {
		verify_implicit_nest( p -> p.match().field( "nested.numeric" ).matching( ANY_INTEGER ) );
	}

	@Test
	public void predicate_phrase() {
		verify_implicit_nest( p -> p.phrase().field( "nested.text" ).matching( SOME_PHRASE_KEY ) );
	}

	@Test
	public void predicate_matchAll() {
		verify_implicit_nest( p -> p.matchAll() );
	}

	@Test
	public void predicate_wildcard() {
		verify_implicit_nest( p -> p.wildcard().field( "nested.text" ).matching( SOME_WILDCARD_PATTERN ) );
	}

	private void verify_implicit_nest(Function<? super SearchPredicateFactory, ? extends PredicateFinalStep> implicitPredicate) {
		StubMappingScope scope = indexManager.createScope();
		SearchPredicate explicitPredicate = scope.predicate().nested().objectField( "nested" ).nest( implicitPredicate ).toPredicate();

		// test the explicit form
		List<DocumentReference> docs = scope.query().selectEntityReference()
				.where( explicitPredicate )
				.fetchAllHits();

		assertThat( docs ).hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_1 );

		// test the implicit form
		docs = scope.query().selectEntityReference()
				.where( implicitPredicate )
				.fetchAllHits();

		assertThat( docs ).hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_1 );
	}

	private void initData() {
		IndexIndexingPlan plan = indexManager.createIndexingPlan();
		plan.add( referenceProvider( DOCUMENT_1 ), document -> {
			DocumentElement nestedDocument = document.addObject( indexMapping.nested );
			nestedDocument.addValue( indexMapping.nestedString, ANY_STRING );
			nestedDocument.addValue( indexMapping.nestedNumeric, ANY_INTEGER );
			nestedDocument.addValue( indexMapping.nestedText, SOME_PHRASE_TEXT );

			DocumentElement nestedDocumentX2 = nestedDocument.addObject( indexMapping.nestedX2 );
			nestedDocumentX2.addValue( indexMapping.nestedX2Numeric, ANY_INTEGER );
		} );
		plan.execute().join();
	}

	private static class IndexMapping {
		final IndexObjectFieldReference nested;
		final IndexFieldReference<String> nestedString;
		final IndexFieldReference<Integer> nestedNumeric;
		final IndexFieldReference<String> nestedText;

		final IndexObjectFieldReference nestedX2;
		final IndexFieldReference<Integer> nestedX2Numeric;

		IndexMapping(IndexSchemaElement root) {
			IndexSchemaObjectField nestedObject = root.objectField( "nested", ObjectFieldStorage.NESTED );
			this.nested = nestedObject.toReference();
			this.nestedString = nestedObject.field( "string", f -> f.asString().projectable( Projectable.YES ).sortable( Sortable.YES ) ).toReference();
			this.nestedNumeric = nestedObject.field( "numeric", f -> f.asInteger() ).toReference();
			this.nestedText = nestedObject.field( "text", f -> f.asString().analyzer( DefaultAnalysisDefinitions.ANALYZER_STANDARD_ENGLISH.name ) ).toReference();

			IndexSchemaObjectField nestedObjectX2 = nestedObject.objectField( "nested", ObjectFieldStorage.NESTED );
			this.nestedX2 = nestedObjectX2.toReference();
			this.nestedX2Numeric = nestedObjectX2.field( "numeric", f -> f.asInteger() ).toReference();
		}
	}

}
