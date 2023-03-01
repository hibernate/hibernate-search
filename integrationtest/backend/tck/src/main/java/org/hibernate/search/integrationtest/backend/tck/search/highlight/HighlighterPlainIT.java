/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.search.highlight;

import static org.hibernate.search.util.impl.integrationtest.common.assertion.SearchHitsAssert.assertThatHits;

import java.util.Arrays;
import java.util.List;

import org.hibernate.search.engine.search.highlighter.dsl.HighlighterFragmenter;
import org.hibernate.search.engine.search.highlighter.dsl.SearchHighlighterFactory;
import org.hibernate.search.engine.search.highlighter.dsl.HighlighterPlainOptionsStep;
import org.hibernate.search.engine.search.query.SearchQuery;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappingScope;

import org.junit.Test;

public class HighlighterPlainIT extends AbstractHighlighterIT {

	@Override
	HighlighterPlainOptionsStep highlighter(SearchHighlighterFactory factory) {
		return factory.plain();
	}

	@Override
	protected List<String> fragmentSizeResult() {
		return Arrays.asList( "Lorem <em>ipsum</em> dolor", " <em>ipsum</em> ultricies" );
	}

	protected List<String> numberOfFragmentsResult() {
		return Arrays.asList(
				"Lorem <em>ipsum</em> dolor sit amet, consectetur adipiscing elit. Proin nec <em>ipsum</em> ultricies, blandit velit"
		);
	}

	@Test
	public void plainMaxAnalyzedOffset() {
		StubMappingScope scope = index.createScope();

		SearchQuery<List<String>> highlights = scope.query().select(
						f -> f.highlight( "string" )
				)
				.where( f -> f.match().field( "string" ).matching( "foo" ) )
				.highlighter( h -> h.plain()
						.maxAnalyzedOffset( 1 )
				)
				.toQuery();

		assertThatHits( highlights.fetchAllHits() )
				.hasHitsAnyOrder(
						Arrays.asList( "<em>foo</em> and foo and foo much more times" )
				);

		highlights = scope.query().select(
						f -> f.highlight( "string" )
				)
				.where( f -> f.match().field( "string" ).matching( "foo" ) )
				.highlighter( h -> h.plain()
						.maxAnalyzedOffset( "foo and foo".length() )
				)
				.toQuery();

		assertThatHits( highlights.fetchAllHits() )
				.hasHitsAnyOrder(
						Arrays.asList( "<em>foo</em> and <em>foo</em> and foo much more times" )
				);
	}

	@Test
	public void plainFragmenterSimple() {
		StubMappingScope scope = index.createScope();

		SearchQuery<List<String>> highlights = scope.query().select(
						f -> f.highlight( "string" )
				)
				.where( f -> f.match().field( "string" ).matching( "foo" ) )
				.highlighter( h -> h.plain()
						.fragmenter( HighlighterFragmenter.SIMPLE )
						.fragmentSize( 15 )
				)
				.toQuery();

		assertThatHits( highlights.fetchAllHits() )
				.hasHitsAnyOrder(
						Arrays.asList( "<em>foo</em> and <em>foo</em>", " and <em>foo</em> much more" )
				);
	}

	@Test
	public void plainFragmenterSpan() {
		StubMappingScope scope = index.createScope();

		SearchQuery<List<String>> highlights = scope.query().select(
						f -> f.highlight( "string" )
				)
				.where( f -> f.match().field( "string" ).matching( "foo" ) )
				.highlighter( h -> h.plain()
						.fragmenter( HighlighterFragmenter.SPAN )
						.fragmentSize( 15 )
				)
				.toQuery();

		assertThatHits( highlights.fetchAllHits() )
				.hasHitsAnyOrder(
						Arrays.asList( "<em>foo</em> and <em>foo</em>", " and <em>foo</em> much more times" )
				);
	}
}
