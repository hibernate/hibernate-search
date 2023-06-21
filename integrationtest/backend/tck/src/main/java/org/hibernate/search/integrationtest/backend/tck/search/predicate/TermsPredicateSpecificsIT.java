/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.search.predicate;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hibernate.search.util.impl.integrationtest.common.assertion.SearchResultAssert.assertThatQuery;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.hibernate.search.engine.backend.common.DocumentReference;
import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.search.query.SearchQuery;
import org.hibernate.search.integrationtest.backend.tck.testsupport.configuration.DefaultAnalysisDefinitions;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.TckConfiguration;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.BulkIndexer;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.SimpleMappedIndex;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappingScope;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class TermsPredicateSpecificsIT {

	// it should be more than the default max clause count of the boolean queries
	private static final int LOT_OF_TERMS_SIZE = 2000;
	private static final String DOC_ID = "id0";

	@Rule
	public final SearchSetupHelper setupHelper = new SearchSetupHelper();

	private final SimpleMappedIndex<IndexBinding> index = SimpleMappedIndex.of( IndexBinding::new );
	private final List<String> lotsOfTerms = new ArrayList<>();

	public TermsPredicateSpecificsIT() {
		for ( int i = 0; i < LOT_OF_TERMS_SIZE; i++ ) {
			if ( i == 7 ) {
				lotsOfTerms.add( "term" + i );
			}
			lotsOfTerms.add( "wrong-term" + i );
		}
	}

	@Before
	public void before() {
		setupHelper.start().withIndex( index ).setup();
		BulkIndexer indexer = index.bulkIndexer();
		indexer.add( "id0", f -> {
			StringBuilder text = new StringBuilder();
			for ( int i = 0; i < LOT_OF_TERMS_SIZE; i++ ) {
				String value = "term" + i;
				text.append( value );
				text.append( " " );

				f.addValue( index.binding().multiValued, value );
			}
			f.addValue( index.binding().analyzed, text.toString() );
			f.addValue( index.binding().normalized, "TERM" );
			f.addValue( index.binding().lowercaseWhitespaceAnalyzed, "TERM" );
		} );
		indexer.join();
	}

	@Test
	public void emptyTerms_matchingAny() {
		StubMappingScope scope = index.createScope();

		assertThatThrownBy( () -> scope.query()
				.where( f -> f.terms().field( "multiValued" ).matchingAny( Collections.emptyList() ) )
		)
				.isInstanceOf( IllegalArgumentException.class )
				.hasMessageContaining( "'terms' must not be null or empty" );
	}

	@Test
	public void emptyTerms_matchingAll() {
		StubMappingScope scope = index.createScope();

		assertThatThrownBy( () -> scope.query()
				.where( f -> f.terms().field( "multiValued" ).matchingAll( Collections.emptyList() ) )
		)
				.isInstanceOf( IllegalArgumentException.class )
				.hasMessageContaining( "'terms' must not be null or empty" );
	}

	@Test
	public void lotsOfTerms_matchingAny() {
		StubMappingScope scope = index.createScope();

		SearchQuery<DocumentReference> query = scope.query().where(
				f -> f.terms().field( "multiValued" ).matchingAny( lotsOfTerms ) ).toQuery();

		assertThatQuery( query )
				.hasDocRefHitsAnyOrder( c -> c
						.doc( index.typeName(), DOC_ID )
				);
	}

	@Test
	public void analyzedField_termIsNotAnalyzed() {
		StubMappingScope scope = index.createScope();

		SearchQuery<DocumentReference> query = scope.query().where(
				f -> f.terms().field( "analyzed" ).matchingAny( "term1" ) ).toQuery();

		assertThatQuery( query )
				.hasDocRefHitsAnyOrder( c -> c
						.doc( index.typeName(), DOC_ID )
				);

		query = scope.query().where(
				f -> f.terms().field( "analyzed" ).matchingAny( "blablabla term1 gogogo ---" ) ).toQuery();

		assertThatQuery( query ).hasNoHits();
	}

	@Test
	public void normalizedField_termIsNotNormalized() {
		StubMappingScope scope = index.createScope();

		SearchQuery<DocumentReference> query = scope.query().where(
				f -> f.terms().field( "normalized" ).matchingAny( "TERM" ) ).toQuery();

		if ( TckConfiguration.get().getBackendFeatures().termsArgumentsAreNormalized() ) {
			assertThatQuery( query ).hasDocRefHitsAnyOrder( c -> c.doc( index.typeName(), DOC_ID ) );
		}
		else {
			assertThatQuery( query ).hasNoHits();
		}

		query = scope.query().where(
				f -> f.terms().field( "normalized" ).matchingAny( "term" ) ).toQuery();

		assertThatQuery( query )
				.hasDocRefHitsAnyOrder( c -> c
						.doc( index.typeName(), DOC_ID )
				);
	}

	@Test
	public void lowercaseWhitespaceAnalyzedField_termIsNotNormalized() {
		StubMappingScope scope = index.createScope();

		SearchQuery<DocumentReference> query = scope.query().where(
				f -> f.terms().field( "lowercaseWhitespaceAnalyzed" ).matchingAny( "TERM" ) ).toQuery();

		assertThatQuery( query ).hasNoHits();

		query = scope.query().where(
				f -> f.terms().field( "lowercaseWhitespaceAnalyzed" ).matchingAny( "term" ) ).toQuery();

		assertThatQuery( query )
				.hasDocRefHitsAnyOrder( c -> c
						.doc( index.typeName(), DOC_ID )
				);
	}

	private static final class IndexBinding {
		private final IndexFieldReference<String> multiValued;
		private final IndexFieldReference<String> analyzed;
		private final IndexFieldReference<String> normalized;
		private final IndexFieldReference<String> lowercaseWhitespaceAnalyzed;

		IndexBinding(IndexSchemaElement root) {
			multiValued = root.field( "multiValued", f -> f.asString() ).multiValued().toReference();
			analyzed = root.field( "analyzed", f -> f.asString()
					.analyzer( DefaultAnalysisDefinitions.ANALYZER_WHITESPACE.name ) ).toReference();
			normalized = root.field( "normalized", f -> f.asString()
					.normalizer( DefaultAnalysisDefinitions.NORMALIZER_LOWERCASE.name ) ).toReference();
			lowercaseWhitespaceAnalyzed = root.field( "lowercaseWhitespaceAnalyzed", f -> f.asString()
					.analyzer( DefaultAnalysisDefinitions.ANALYZER_WHITESPACE_LOWERCASE.name ) ).toReference();
		}
	}
}
