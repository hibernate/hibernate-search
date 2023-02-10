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

import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.types.Projectable;
import org.hibernate.search.engine.search.highlighter.dsl.HighlighterTagSchema;
import org.hibernate.search.engine.search.query.SearchQuery;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.AnalyzedStringFieldTypeDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.SimpleFieldModel;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.SimpleMappedIndex;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappingScope;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

public class FieldHighlightIT {

	@ClassRule
	public static final SearchSetupHelper setupHelper = new SearchSetupHelper();

	private static final SimpleMappedIndex<IndexBinding> index = SimpleMappedIndex.of( IndexBinding::new );

	@BeforeClass
	public static void setup() {
		setupHelper.start().withIndex( index ).setup();

		index.bulkIndexer().add( "1", d -> d.addValue( "string", "some value" ) )
				.add( "2", d -> {
					d.addValue( "string", "some other value" );
					d.addValue(
							"anotherString",
							"Lorem ipsum dolor sit amet, consectetur adipiscing elit. Duis tristique dui vitae ullamcorper volutpat. Vestibulum."
					);
				} )
				.add( "3", d -> {
					d.addValue( "string", "some another value" );
					d.addValue(
							"anotherString",
							"Lorem ipsum dolor sit amet, consectetur adipiscing elit. Quisque orci leo, consequat id diam vitae, mollis rhoncus diam. Vestibulum ante ipsum primis in faucibus orci."
					);
				} )
				.add( "4", d -> {
					d.addValue( "string", "some yet another value" );
					d.addValue(
							"anotherString",
							"Lorem ipsum dolor sit amet, consectetur adipiscing elit. Morbi et aliquet dui. In ultricies sed sem vitae congue. Nulla facilisi. Nullam faucibus a ligula consectetur efficitur. Donec maximus maximus ligula, ac aliquam purus porttitor semper. Phasellus elementum placerat ligula eget consequat. Proin gravida nulla vel faucibus lacinia. Pellentesque nec gravida velit. Fusce vehicula sollicitudin ex."
					);
				} )
				.add( "5", d -> {
					d.addValue( "string", "foo and foo and foo much more times" );
					d.addValue(
							"anotherString",
							"Lorem ipsum dolor sit amet, consectetur adipiscing elit. Proin nec ipsum ultricies, blandit velit vitae, lacinia tellus. Fusce elementum ultricies felis, ut molestie orci lacinia a. In eget euismod nulla. Praesent euismod orci vitae sapien cursus aliquet. Aenean velit ex, consequat in magna eu, ornare facilisis tellus. Ut vel diam nec sem lobortis lacinia. Sed nisi ex, faucibus nec ante pulvinar, congue feugiat mauris. Curabitur efficitur arcu et neque condimentum, vel convallis elit ultricies. Suspendisse a odio augue. Aliquam lorem turpis, molestie at sollicitudin quis, convallis id dolor. Quisque ultricies libero at consequat ornare.\n" +
									"Praesent vel accumsan lectus. Fusce tristique pulvinar pulvinar. Sed ac leo sodales, dictum sapien non, feugiat urna. Quisque dignissim id massa ut dictum. Nam nec erat luctus, sodales lorem in, congue leo. Aliquam erat volutpat. Fusce dapibus consequat dui at lobortis. Suspendisse iaculis pellentesque lacus, eu tincidunt nisi ullamcorper molestie. Vivamus ullamcorper pulvinar commodo. Vivamus at justo in risus pretium malesuada."
					);
				} )
				.join();
	}

	@Test
	public void customTag() {
		StubMappingScope scope = index.createScope();

		SearchQuery<List<String>> highlights = scope.query().select(
						f -> f.highlight( "string" ).highlighter( "strong-tag-highlighter" )
				)
				.where( f -> f.match().field( "string" ).matching( "another" ) )
				.highlighter( h -> h.defaultType().tagSchema( HighlighterTagSchema.STYLED ) )
				.highlighter( "strong-tag-highlighter", h2 -> h2.defaultType().tag( "<strong>", "</strong>" ) )
				.toQuery();

		assertThatHits( highlights.fetchAllHits() )
				.hasHitsAnyOrder(
						Arrays.asList( "some <strong>another</strong> value" ),
						Arrays.asList( "some yet <strong>another</strong> value" )
				);
	}

	@Test
	public void multipleOccurrencesWithinSameLine() {
		StubMappingScope scope = index.createScope();

		SearchQuery<List<String>> highlights = scope.query().select(
						f -> f.highlight( "string" )
				)
				.where( f -> f.match().field( "string" ).matching( "foo" ) )
				.toQuery();

		assertThatHits( highlights.fetchAllHits() )
				.hasHitsAnyOrder(
						Arrays.asList( "<em>foo</em> and <em>foo</em> and <em>foo</em> much more times" )
				);
	}

	@Test
	public void setQueryLevelHighlightSetting() {
		StubMappingScope scope = index.createScope();

		SearchQuery<List<String>> highlights = scope.query().select(
						f -> f.highlight( "string" )
				)
				.where( f -> f.match().field( "string" ).matching( "foo" ) )
				.highlighter( h -> h.defaultType().tag( "<em class=\"hlt1\">", "</em>" ) )
				.toQuery();

		assertThatHits( highlights.fetchAllHits() )
				.hasHitsAnyOrder(
						Arrays.asList( "<em class=\"hlt1\">foo</em> and <em class=\"hlt1\">foo</em> and <em class=\"hlt1\">foo</em> much more times" )
				);
	}

	@Test
	public void setQueryLevelHighlightSettingWithHighlighterConfig() {
		StubMappingScope scope = index.createScope();

		SearchQuery<List<?>> highlights = scope.query().select(
						f -> f.composite().from(
								f.highlight( "string" ),
								f.highlight( "anotherString" ).highlighter( "lorem-ipsum" )
						).asList()
				)
				.where( f -> f.bool()
						.should( f.match().field( "anotherString" ).matching( "lorem" ) )
						.should( f.match().field( "string" ).matching( "foo" ) )
				)
				.highlighter( h -> h.unified().tag( "<em class=\"hlt1\">", "</em>" ) )
				.highlighter( "lorem-ipsum", h2 -> h2.unified()
						.fragmentSize( 50 )
						.numberOfFragments( 2 ) )
				.toQuery();

		assertThatHits( highlights.fetchAllHits() )
				.hasHitsAnyOrder(
						Arrays.asList(
								Arrays.asList(
										Arrays.asList(
												"<em class=\"hlt1\">foo</em> and <em class=\"hlt1\">foo</em> and <em class=\"hlt1\">foo</em> much more times" ),
										Arrays.asList(
												"<em class=\"hlt1\">Lorem</em> ipsum dolor sit amet, consectetur adipiscing",
												"Aliquam <em class=\"hlt1\">lorem</em> turpis, molestie at sollicitudin quis"
												// this one won't be included as we limited the number of fragments
												// , "Nam nec erat luctus, sodales <em class=\"hlt1\">lorem</em> in, congue leo."
										)
								),
								Arrays.asList(
										Arrays.asList(),
										Arrays.asList(
												"<em class=\"hlt1\">Lorem</em> ipsum dolor sit amet, consectetur adipiscing" )
								),
								Arrays.asList(
										Arrays.asList(),
										Arrays.asList(
												"<em class=\"hlt1\">Lorem</em> ipsum dolor sit amet, consectetur adipiscing" )
								),
								Arrays.asList(
										Arrays.asList(),
										Arrays.asList(
												"<em class=\"hlt1\">Lorem</em> ipsum dolor sit amet, consectetur adipiscing" )
								)
						)
				);
	}

	private static class IndexBinding {
		final SimpleFieldModel<String> stringField;
		final SimpleFieldModel<String> anotherStringField;

		IndexBinding(IndexSchemaElement root) {
			stringField = SimpleFieldModel.mapper( AnalyzedStringFieldTypeDescriptor.INSTANCE, c -> {
					} )
					.map( root, "string", c -> c.projectable( Projectable.YES ) );
			anotherStringField = SimpleFieldModel.mapper( AnalyzedStringFieldTypeDescriptor.INSTANCE, c -> {
					} )
					.map( root, "anotherString", c -> c.projectable( Projectable.YES ) );
		}
	}
}
