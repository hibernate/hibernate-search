/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.search.predicate;

import static org.hibernate.search.util.impl.integrationtest.common.assertion.SearchResultAssert.assertThat;
import static org.hibernate.search.util.impl.integrationtest.common.stub.mapper.StubMapperUtils.referenceProvider;

import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.index.spi.IndexWorkPlan;
import org.hibernate.search.engine.reporting.spi.EventContexts;
import org.hibernate.search.engine.search.DocumentReference;
import org.hibernate.search.engine.search.query.spi.IndexSearchQuery;
import org.hibernate.search.integrationtest.backend.tck.testsupport.configuration.OverrideAnalysisDefinitions;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.common.FailureReportUtils;
import org.hibernate.search.util.impl.integrationtest.common.stub.mapper.StubMappingIndexManager;
import org.hibernate.search.util.impl.integrationtest.common.stub.mapper.StubMappingSearchScope;
import org.hibernate.search.util.impl.test.SubTest;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class MultiIndexSearchPredicateIT {

	public static final String CONFIGURATION_ID = "analysis-override";

	private static final String INDEX_NAME = "IndexName";
	private static final String INCOMPATIBLE_ANALYZER_INDEX_NAME = "IndexWithIncompatibleAnalyzer";

	@Rule
	public SearchSetupHelper setupHelper = new SearchSetupHelper();

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	private IndexMapping indexMapping;
	private StubMappingIndexManager indexManager;

	private IncompatibleAnalyzerMapping incompatibleAnalyzerMapping;
	private StubMappingIndexManager incompatibleAnalyzerManager;

	@Before
	public void setup() {
		setupHelper.withConfiguration( CONFIGURATION_ID )
				.withIndex(
						INDEX_NAME,
						ctx -> this.indexMapping = new IndexMapping( ctx.getSchemaElement() ),
						indexManager -> this.indexManager = indexManager
				)
				.withIndex(
						INCOMPATIBLE_ANALYZER_INDEX_NAME,
						ctx -> this.incompatibleAnalyzerMapping = new IncompatibleAnalyzerMapping( ctx.getSchemaElement() ),
						indexManager -> this.incompatibleAnalyzerManager = indexManager
				)
				.setup();

		initData();
	}

	@Test
	public void multiIndex_incompatibleAnalyzer_match() {
		StubMappingSearchScope scope = indexManager.createSearchScope( incompatibleAnalyzerManager );

		SubTest.expectException(
				() -> {
					scope.query().asReference()
							.predicate( f -> f.match().onField( "multiIndexTextField" ).matching( "indexValue" ) )
							.toQuery();
				}
		)
				.assertThrown()
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "Multiple conflicting types to build a predicate" )
				.hasMessageContaining( "'multiIndexTextField'" )
				.satisfies( FailureReportUtils.hasContext(
						EventContexts.fromIndexNames( INDEX_NAME, INCOMPATIBLE_ANALYZER_INDEX_NAME )
				) )
		;
	}

	@Test
	public void multiIndex_incompatibleAnalyzer_match_overrideAnalyzer() {
		StubMappingSearchScope scope = indexManager.createSearchScope( incompatibleAnalyzerManager );

		IndexSearchQuery<DocumentReference> query = scope.query().asReference()
				.predicate( f -> f.match().onField( "multiIndexTextField" ).matching( "indexValue anotherWord" )
						.analyzer( OverrideAnalysisDefinitions.ANALYZER_WHITESPACE_LOWERCASE.name ) )
				.toQuery();

		assertThat( query ).assertThat( query ).hasDocRefHitsAnyOrder( b -> {
			b.doc( INDEX_NAME, "1" );
			b.doc( INCOMPATIBLE_ANALYZER_INDEX_NAME, "1" );
		} );
	}

	@Test
	public void multiIndex_incompatibleAnalyzer_match_skipAnalysis() {
		StubMappingSearchScope scope = indexManager.createSearchScope( incompatibleAnalyzerManager );

		IndexSearchQuery<DocumentReference> query = scope.query().asReference()
				.predicate( f -> f.match().onField( "multiIndexTextField" ).matching( "indexvalue" )
						.skipAnalysis() )
				.toQuery();

		assertThat( query ).assertThat( query ).hasDocRefHitsAnyOrder( b -> {
			b.doc( INDEX_NAME, "1" );
			b.doc( INCOMPATIBLE_ANALYZER_INDEX_NAME, "1" );
		} );
	}

	@Test
	public void multiIndex_incompatibleAnalyzer_phrase() {
		StubMappingSearchScope scope = indexManager.createSearchScope( incompatibleAnalyzerManager );

		SubTest.expectException(
				() -> {
					scope.query().asReference()
							.predicate( f -> f.phrase().onField( "multiIndexTextField" ).matching( "indexValue" ) )
							.toQuery();
				}
		)
				.assertThrown()
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "Multiple conflicting types to build a predicate" )
				.hasMessageContaining( "'multiIndexTextField'" )
				.satisfies( FailureReportUtils.hasContext(
						EventContexts.fromIndexNames( INDEX_NAME, INCOMPATIBLE_ANALYZER_INDEX_NAME )
				) )
		;
	}

	@Test
	public void multiIndex_incompatibleAnalyzer_phrase_overrideAnalyzer() {
		StubMappingSearchScope scope = indexManager.createSearchScope( incompatibleAnalyzerManager );

		IndexSearchQuery<DocumentReference> query = scope.query().asReference()
				.predicate( f -> f.phrase().onField( "multiIndexTextField" ).matching( "indexValue" )
						.analyzer( OverrideAnalysisDefinitions.ANALYZER_WHITESPACE_LOWERCASE.name ) )
				.toQuery();

		assertThat( query ).assertThat( query ).hasDocRefHitsAnyOrder( b -> {
			b.doc( INDEX_NAME, "1" );
			b.doc( INCOMPATIBLE_ANALYZER_INDEX_NAME, "1" );
		} );
	}

	@Test
	public void multiIndex_incompatibleAnalyzer_phrase_skipAnalysis() {
		StubMappingSearchScope scope = indexManager.createSearchScope( incompatibleAnalyzerManager );

		IndexSearchQuery<DocumentReference> query = scope.query().asReference()
				.predicate( f -> f.phrase().onField( "multiIndexTextField" ).matching( "indexvalue" )
						.skipAnalysis() )
				.toQuery();

		assertThat( query ).assertThat( query ).hasDocRefHitsAnyOrder( b -> {
			b.doc( INDEX_NAME, "1" );
			b.doc( INCOMPATIBLE_ANALYZER_INDEX_NAME, "1" );
		} );
	}

	@Test
	public void multiIndex_incompatibleAnalyzer_simpleQuery() {
		StubMappingSearchScope scope = indexManager.createSearchScope( incompatibleAnalyzerManager );

		SubTest.expectException(
				() -> {
					scope.query().asReference()
							.predicate( f -> f.simpleQueryString().onField( "multiIndexTextField" ).matching( "indexValue | indexvalue" ) )
							.toQuery();
				}
		)
				.assertThrown()
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "Multiple conflicting types to build a predicate" )
				.hasMessageContaining( "'multiIndexTextField'" )
				.satisfies( FailureReportUtils.hasContext(
						EventContexts.fromIndexNames( INDEX_NAME, INCOMPATIBLE_ANALYZER_INDEX_NAME )
				) )
		;
	}

	@Test
	public void multiIndex_incompatibleAnalyzer_simpleQuery_overrideAnalyzer() {
		StubMappingSearchScope scope = indexManager.createSearchScope( incompatibleAnalyzerManager );

		IndexSearchQuery<DocumentReference> query = scope.query().asReference()
				.predicate( f -> f.simpleQueryString().onField( "multiIndexTextField" ).matching( "indexValue | indexvalue" )
						.analyzer( OverrideAnalysisDefinitions.ANALYZER_WHITESPACE_LOWERCASE.name ) )
				.toQuery();

		assertThat( query ).assertThat( query ).hasDocRefHitsAnyOrder( b -> {
			b.doc( INDEX_NAME, "1" );
			b.doc( INCOMPATIBLE_ANALYZER_INDEX_NAME, "1" );
		} );
	}

	@Test
	public void multiIndex_incompatibleAnalyzer_simpleQuery_skipAnalysis() {
		StubMappingSearchScope scope = indexManager.createSearchScope( incompatibleAnalyzerManager );

		IndexSearchQuery<DocumentReference> query = scope.query().asReference()
				.predicate( f -> f.simpleQueryString().onField( "multiIndexTextField" ).matching( "indexValue | indexvalue" )
						.skipAnalysis() )
				.toQuery();

		assertThat( query ).assertThat( query ).hasDocRefHitsAnyOrder( b -> {
			b.doc( INDEX_NAME, "1" );
			b.doc( INCOMPATIBLE_ANALYZER_INDEX_NAME, "1" );
		} );
	}

	private void initData() {
		IndexWorkPlan<? extends DocumentElement> workPlan = indexManager.createWorkPlan();
		workPlan.add( referenceProvider( "1" ), document -> {
			document.addValue( indexMapping.multiIndexTextField, "indexvalue" );
		} );
		workPlan.execute().join();

		workPlan = incompatibleAnalyzerManager.createWorkPlan();
		workPlan.add( referenceProvider( "1" ), document ->
				document.addValue( incompatibleAnalyzerMapping.multiIndexTextField, "indexValue" )
		);
		workPlan.execute().join();
	}

	private static class IndexMapping {
		final IndexFieldReference<String> multiIndexTextField;

		IndexMapping(IndexSchemaElement root) {
			multiIndexTextField = root.field( "multiIndexTextField", c -> c
					.asString().analyzer( OverrideAnalysisDefinitions.ANALYZER_WHITESPACE.name )
			).toReference();
		}
	}

	private static class IncompatibleAnalyzerMapping {
		final IndexFieldReference<String> multiIndexTextField;

		IncompatibleAnalyzerMapping(IndexSchemaElement root) {
			multiIndexTextField = root.field( "multiIndexTextField", c -> c
					.asString().analyzer( OverrideAnalysisDefinitions.ANALYZER_WHITESPACE_LOWERCASE.name )
			).toReference();
		}
	}
}
