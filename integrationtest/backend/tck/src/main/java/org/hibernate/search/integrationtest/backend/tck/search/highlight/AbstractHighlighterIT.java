/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.backend.tck.search.highlight;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hibernate.search.util.impl.integrationtest.common.assertion.SearchHitsAssert.assertThatHits;
import static org.junit.jupiter.api.Assumptions.assumeFalse;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.engine.backend.document.IndexObjectFieldReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaObjectField;
import org.hibernate.search.engine.backend.types.Highlightable;
import org.hibernate.search.engine.backend.types.TermVector;
import org.hibernate.search.engine.search.highlighter.SearchHighlighter;
import org.hibernate.search.engine.search.highlighter.dsl.HighlighterEncoder;
import org.hibernate.search.engine.search.highlighter.dsl.HighlighterOptionsStep;
import org.hibernate.search.engine.search.highlighter.dsl.SearchHighlighterFactory;
import org.hibernate.search.engine.search.query.SearchQuery;
import org.hibernate.search.integrationtest.backend.tck.testsupport.configuration.DefaultAnalysisDefinitions;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.TckConfiguration;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.extension.SearchSetupHelper;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.SimpleMappedIndex;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappingScope;
import org.hibernate.search.util.impl.test.extension.ExpectedLog4jLog;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.apache.logging.log4j.Level;
import org.assertj.core.api.ThrowableAssert;

abstract class AbstractHighlighterIT {

	@RegisterExtension
	public static final SearchSetupHelper setupHelper = SearchSetupHelper.create();

	@RegisterExtension
	public final ExpectedLog4jLog logged = ExpectedLog4jLog.create();

	protected static final SimpleMappedIndex<IndexBinding> index = SimpleMappedIndex.of( IndexBinding::new );

	protected static final SimpleMappedIndex<IndexBinding> indexMatching = SimpleMappedIndex.of( IndexBinding::new )
			.name( "matching" );
	private static final SimpleMappedIndex<NotMatchingTypeIndexBinding> notMatchingTypeIndex =
			SimpleMappedIndex.of( NotMatchingTypeIndexBinding::new )
					.name( "notMatchingTypeIndex" );

	@BeforeAll
	static void setup() {
		setupHelper.start().withIndex( index )
				.withIndex( notMatchingTypeIndex )
				.withIndex( indexMatching )
				.setup();

		index.bulkIndexer()
				.add( "1", d -> d.addValue( "string", "some value" ) )
				.add( "2", d -> {
					d.addValue( "string", "some other value" );
					d.addValue( "anotherString", "The quick brown fox jumps right over the little lazy dog" );
				} )
				.add( "3", d -> {
					d.addValue( "string", "some another value" );
				} )
				.add( "4", d -> {
					d.addValue( "string", "some yet another value" );
				} )
				.add( "5", d -> {
					d.addValue( "string", "foo and foo and foo much more times" );
					d.addValue(
							"anotherString",
							"Lorem ipsum dolor sit amet, consectetur adipiscing elit. Proin nec ipsum ultricies, blandit velit vitae, lacinia tellus. Fusce elementum ultricies felis, ut molestie orci lacinia a. In eget euismod nulla. Praesent euismod orci vitae sapien cursus aliquet. Aenean velit ex, consequat in magna eu, ornare facilisis tellus. Ut vel diam nec sem lobortis lacinia. Sed nisi ex, faucibus nec ante pulvinar, congue feugiat mauris. Curabitur efficitur arcu et neque condimentum, vel convallis elit ultricies. Suspendisse a odio augue. Aliquam lorem turpis, molestie at sollicitudin quis, convallis id dolor. Quisque ultricies libero at consequat ornare.\n"
									+
									"Praesent vel accumsan lectus. Fusce tristique pulvinar pulvinar. Sed ac leo sodales, dictum sapien non, feugiat urna. Quisque dignissim id massa ut dictum. Nam nec erat luctus, sodales lorem in, congue leo. Aliquam erat volutpat. Fusce dapibus consequat dui at lobortis. Suspendisse iaculis pellentesque lacus, eu tincidunt nisi ullamcorper molestie. Vivamus ullamcorper pulvinar commodo. Vivamus at justo in risus pretium malesuada."
					);
				} )
				.add( "6", d -> {
					d.addValue( "string", "This string mentions a dog" );
					d.addValue( "anotherString", "The quick brown fox jumps right over the little lazy dog" );
				} )
				.add( "7", d -> {
					d.addValue( "string", "This string mentions a dog too" );
				} )
				.add( "8", d -> {
					d.addValue( "string", "<body><h1>This is a Heading</h1><p>This is a paragraph</p></body>" );
				} )
				.add( "9", d -> {
					d.addObject( "objectFlattened" )
							.addValue( "string", "The quick brown fox jumps right over the little lazy dog" );
					d.addValue( "notAnalyzedString", "The quick brown fox jumps right over the little lazy dog" );
					d.addValue( "multiValuedString", "The quick brown fox jumps right over the little lazy dog" );
					d.addValue( "multiValuedString", "This string mentions a dog" );
					d.addValue( "multiValuedString", "This string mentions a fox" );
				} )
				.add( "10", d -> {
					d.addValue( "string",
							"Scorpions are a German rock band formed in Hanover in 1965 by guitarist Rudolf Schenker. Since the band's inception, its musical style has ranged from hard rock, heavy metal and glam metal to soft rock." );
				} )
				.add( "11", d -> {
					d.addValue( "string",
							"text that has - dash in - it from time - to some useless text in between time to see - how - boundary_chars - works" );
				} )
				.add( "12", d -> {
					d.addValue( "stringNoTermVector", "boo and boo and boo much more times" );
				} )
				.join();
	}

	abstract HighlighterOptionsStep<?> highlighter(SearchHighlighterFactory factory);

	@Test
	void highlighterNoConfigurationAtAll() {
		StubMappingScope scope = index.createScope();

		SearchQuery<List<String>> highlights = scope.query().select(
				f -> f.highlight( "string" )
		)
				.where( f -> f.match().field( "string" ).matching( "another" ) )
				.toQuery();

		assertThatHits( highlights.fetchAllHits() )
				.hasHitsAnyOrder(
						Arrays.asList( "some <em>another</em> value" ),
						Arrays.asList( "some yet <em>another</em> value" )
				);
	}

	@Test
	void highlighterNoSettings() {
		StubMappingScope scope = index.createScope();

		SearchQuery<List<String>> highlights = scope.query().select(
				f -> f.highlight( "string" )
		)
				.where( f -> f.match().field( "string" ).matching( "another" ) )
				.highlighter( h -> highlighter( h ) )
				.toQuery();

		assertThatHits( highlights.fetchAllHits() )
				.hasHitsAnyOrder(
						Arrays.asList( "some <em>another</em> value" ),
						Arrays.asList( "some yet <em>another</em> value" )
				);
	}

	@Test
	void highlighterNoSettingsMultipleOccurrencesWithinSameLine() {
		StubMappingScope scope = index.createScope();

		SearchQuery<List<String>> highlights = scope.query().select(
				f -> f.highlight( "string" )
		)
				.where( f -> f.match().field( "string" ).matching( "foo" ) )
				.highlighter( h -> highlighter( h ) )
				.toQuery();

		assertThatHits( highlights.fetchAllHits() )
				.hasHitsAnyOrder(
						Arrays.asList( "<em>foo</em> and <em>foo</em> and <em>foo</em> much more times" )
				);
	}

	@Test
	void customTagGlobal() {
		StubMappingScope scope = index.createScope();

		SearchQuery<List<String>> highlights = scope.query().select(
				f -> f.highlight( "string" )
		)
				.where( f -> f.match().field( "string" ).matching( "another" ) )
				.highlighter( h2 -> highlighter( h2 ).tag( "<strong>", "</strong>" ) )
				.toQuery();

		assertThatHits( highlights.fetchAllHits() )
				.hasHitsAnyOrder(
						Arrays.asList( "some <strong>another</strong> value" ),
						Arrays.asList( "some yet <strong>another</strong> value" )
				);
	}

	@Test
	void customTagField() {
		StubMappingScope scope = index.createScope();

		SearchQuery<List<String>> highlights = scope.query().select(
				f -> f.highlight( "string" ).highlighter( "strong-tag-highlighter" )
		)
				.where( f -> f.match().field( "string" ).matching( "another" ) )
				.highlighter( "strong-tag-highlighter", h2 -> highlighter( h2 ).tag( "<strong>", "</strong>" ) )
				.toQuery();

		assertThatHits( highlights.fetchAllHits() )
				.hasHitsAnyOrder(
						Arrays.asList( "some <strong>another</strong> value" ),
						Arrays.asList( "some yet <strong>another</strong> value" )
				);
	}

	@Test
	void lastHighlighterWins() {
		StubMappingScope scope = index.createScope();

		SearchQuery<List<String>> highlights = scope.query().select(
				f -> f.highlight( "string" )
						.highlighter( "does-not-exist" )
						.highlighter( "some-unused-highlighter" )
						.highlighter( "strong-tag-highlighter" )
		)
				.where( f -> f.match().field( "string" ).matching( "another" ) )
				.highlighter( "strong-tag-highlighter", h2 -> highlighter( h2 ).tag( "<strong>", "</strong>" ) )
				.highlighter( "some-unused-highlighter",
						h2 -> highlighter( h2 ).tag( "<strong>", "</strong>" ).numberOfFragments( 1 ) )
				.toQuery();

		assertThatHits( highlights.fetchAllHits() )
				.hasHitsAnyOrder(
						Arrays.asList( "some <strong>another</strong> value" ),
						Arrays.asList( "some yet <strong>another</strong> value" )
				);
	}

	@Test
	void customTagOverride() {
		StubMappingScope scope = index.createScope();

		SearchQuery<List<String>> highlights = scope.query().select(
				f -> f.highlight( "string" ).highlighter( "strong-tag-highlighter" )
		)
				.where( f -> f.match().field( "string" ).matching( "another" ) )
				.highlighter( h -> highlighter( h ).tag( "<custom>", "</custom>" ) )
				.highlighter( "strong-tag-highlighter", h2 -> highlighter( h2 ).tag( "<strong>", "</strong>" ) )
				.toQuery();

		assertThatHits( highlights.fetchAllHits() )
				.hasHitsAnyOrder(
						Arrays.asList( "some <strong>another</strong> value" ),
						Arrays.asList( "some yet <strong>another</strong> value" )
				);
	}

	@Test
	void lastTagWins() {
		StubMappingScope scope = index.createScope();

		SearchQuery<List<String>> highlights = scope.query().select(
				f -> f.highlight( "string" )
		)
				.where( f -> f.match().field( "string" ).matching( "foo" ) )
				.highlighter( h -> highlighter( h )
						.tag( "*", "*" )
						.tag( "**", "**" )
						.tag( "***", "***" )
						.tag( "****", "****" )
				)
				.toQuery();

		assertThatHits( highlights.fetchAllHits() )
				.hasHitsAnyOrder(
						Arrays.asList( "****foo**** and ****foo**** and ****foo**** much more times" )
				);
	}

	@Test
	void encoderGlobalHtml() {
		encoderGlobal(
				HighlighterEncoder.HTML,
				"&lt;body&gt;&lt;h1&gt;This is a <em>Heading</em>&lt;&#x2F;h1&gt;&lt;p&gt;This is a paragraph&lt;&#x2F;p&gt;&lt;&#x2F;body&gt;"
		);
	}

	@Test
	void encoderGlobalDefault() {
		encoderGlobal(
				HighlighterEncoder.DEFAULT,
				"<body><h1>This is a <em>Heading</em></h1><p>This is a paragraph</p></body>"
		);
	}

	protected void encoderGlobal(HighlighterEncoder encoder, String result) {
		StubMappingScope scope = index.createScope();

		SearchQuery<List<String>> highlights = scope.query().select(
				f -> f.highlight( "string" )
		)
				.where( f -> f.match().field( "string" ).matching( "Heading" ) )
				.highlighter( h -> highlighter( h ).encoder( encoder ) )
				.toQuery();

		assertThatHits( highlights.fetchAllHits() )
				.hasHitsAnyOrder(
						Arrays.asList( result )
				);
	}

	@Test
	void encoderFieldHtml() {
		encoderField(
				HighlighterEncoder.HTML,
				"&lt;body&gt;&lt;h1&gt;This is a <em>Heading</em>&lt;&#x2F;h1&gt;&lt;p&gt;This is a paragraph&lt;&#x2F;p&gt;&lt;&#x2F;body&gt;"
		);
	}

	@Test
	void encoderFieldDefault() {
		encoderField(
				HighlighterEncoder.DEFAULT,
				"<body><h1>This is a <em>Heading</em></h1><p>This is a paragraph</p></body>"
		);
	}

	void encoderField(HighlighterEncoder encoder, String result) {
		assumeTrue(
				TckConfiguration.get().getBackendFeatures().supportsHighlighterEncoderAtFieldLevel(),
				"This test only make sense for backends that support encoder override at field level."
		);
		StubMappingScope scope = index.createScope();

		SearchQuery<List<String>> highlights = scope.query().select(
				f -> f.highlight( "string" ).highlighter( "encoder" )
		)
				.where( f -> f.match().field( "string" ).matching( "Heading" ) )
				.highlighter( "encoder", h -> highlighter( h ).encoder( encoder ) )
				.toQuery();

		assertThatHits( highlights.fetchAllHits() )
				.hasHitsAnyOrder(
						Arrays.asList( result )
				);
	}

	@Test
	void encoderOverrideHtml() {
		encoderOverride(
				HighlighterEncoder.DEFAULT,
				HighlighterEncoder.HTML,
				"&lt;body&gt;&lt;h1&gt;This is a <em>Heading</em>&lt;&#x2F;h1&gt;&lt;p&gt;This is a paragraph&lt;&#x2F;p&gt;&lt;&#x2F;body&gt;"
		);
	}

	@Test
	void encoderOverrideDefault() {
		encoderOverride(
				HighlighterEncoder.HTML,
				HighlighterEncoder.DEFAULT,
				"<body><h1>This is a <em>Heading</em></h1><p>This is a paragraph</p></body>"
		);
	}

	void encoderOverride(HighlighterEncoder globalEncoder, HighlighterEncoder encoder, String result) {
		assumeTrue(
				TckConfiguration.get().getBackendFeatures().supportsHighlighterEncoderAtFieldLevel(),
				"This test only make sense for backends that support encoder override at field level."
		);
		StubMappingScope scope = index.createScope();

		SearchQuery<List<String>> highlights = scope.query().select(
				f -> f.highlight( "string" ).highlighter( "encoder" )
		)
				.where( f -> f.match().field( "string" ).matching( "Heading" ) )
				.highlighter( h -> highlighter( h ).encoder( globalEncoder ) )
				.highlighter( "encoder", h -> highlighter( h ).encoder( encoder ) )
				.toQuery();

		assertThatHits( highlights.fetchAllHits() )
				.hasHitsAnyOrder(
						Arrays.asList( result )
				);
	}

	@Test
	void fragmentSize() {
		assumeTrue( supportsFragmentSize() );
		StubMappingScope scope = index.createScope();

		SearchQuery<List<String>> highlights = scope.query().select(
				f -> f.highlight( "anotherString" )
		)
				.where( f -> f.match().field( "anotherString" ).matching( "ipsum" ) )
				.highlighter( h -> highlighter( h ).fragmentSize( 18 ) )
				.toQuery();

		assertThatHits( highlights.fetchAllHits() )
				.hasHitsAnyOrder(
						fragmentSizeResult()
				);
	}

	@Test
	void fragmentSizeNotSupported() {
		assumeFalse( supportsFragmentSize() );

		assertThatThrownBy(
				() -> index.createScope().query().select(
						f -> f.highlight( "anotherString" )
				)
						.where( f -> f.match().field( "anotherString" ).matching( "ipsum" ) )
						.highlighter( h -> highlighter( h ).fragmentSize( 18 ) )
						.toQuery()
		).isInstanceOf( SearchException.class )
				.hasMessageContainingAll(
						"unified highlighter does not support the size fragment setting",
						"Either use a plain or fast vector highlighters, or do not set this setting"
				);
	}

	protected boolean supportsFragmentSize() {
		return true;
	}

	protected abstract List<String> fragmentSizeResult();

	@Test
	void numberOfFragments() {
		StubMappingScope scope = index.createScope();

		SearchQuery<List<String>> highlights = scope.query().select(
				f -> f.highlight( "anotherString" )
		)
				.where( f -> f.match().field( "anotherString" ).matching( "ipsum" ) )
				.highlighter( h -> highlighter( h ).numberOfFragments( 1 ) )
				.toQuery();

		assertThatHits( highlights.fetchAllHits() )
				.hasHitsAnyOrder(
						numberOfFragmentsResult()
				);
	}

	@Test
	void numberOfFragmentsSingle() {
		StubMappingScope scope = index.createScope();

		SearchQuery<String> highlights = scope.query().select(
				f -> f.highlight( "anotherString" ).single()
		)
				.where( f -> f.match().field( "anotherString" ).matching( "ipsum" ) )
				.highlighter( h -> highlighter( h ).numberOfFragments( 1 ) )
				.toQuery();

		assertThatHits( highlights.fetchAllHits() )
				.hasHitsAnyOrder(
						numberOfFragmentsResult()
				);
	}

	@Test
	void numberOfFragmentsSingleNamedHighlighter() {
		StubMappingScope scope = index.createScope();

		SearchQuery<String> highlights = scope.query().select(
				f -> f.highlight( "anotherString" ).highlighter( "single" ).single()
		)
				.where( f -> f.match().field( "anotherString" ).matching( "ipsum" ) )
				.highlighter( "single", h -> highlighter( h ).numberOfFragments( 1 ) )
				.toQuery();

		assertThatHits( highlights.fetchAllHits() )
				.hasHitsAnyOrder(
						numberOfFragmentsResult()
				);
	}

	@Test
	void numberOfFragmentsSingleError() {
		StubMappingScope scope = index.createScope();

		// numberOfFragments > 1
		assertThatThrownBy( () -> scope.query().select(
				f -> f.highlight( "anotherString" ).single()
		)
				.where( f -> f.match().field( "anotherString" ).matching( "ipsum" ) )
				.highlighter( h -> highlighter( h ).numberOfFragments( 2 ) )
				.toQuery()
		).isInstanceOf( SearchException.class )
				.hasMessageContainingAll(
						"A single-valued highlight projection requested, but the corresponding highlighter does not set number of fragments to 1" );

		// numberOfFragments not defined
		assertThatThrownBy( () -> scope.query().select(
				f -> f.highlight( "anotherString" ).single()
		)
				.where( f -> f.match().field( "anotherString" ).matching( "ipsum" ) )
				.highlighter( h -> highlighter( h ) )
				.toQuery()
		).isInstanceOf( SearchException.class )
				.hasMessageContainingAll(
						"A single-valued highlight projection requested, but the corresponding highlighter does not set number of fragments to 1" );
	}

	@Test
	void numberOfFragmentsSingleButNoExpectedValuesReturned() {
		StubMappingScope scope = index.createScope();

		SearchQuery<String> highlights = scope.query().select(
				f -> f.highlight( "anotherString" ).single()
		)
				.where( f -> f.match().field( "anotherString" ).matching( "thisCannotBeMatchedToAnythingInTheText" ) )
				.highlighter( h -> highlighter( h ).numberOfFragments( 1 ) )
				.toQuery();

		assertThatHits( highlights.fetchAllHits() )
				.hasHitsAnyOrder( List.of() );
	}

	protected List<String> numberOfFragmentsResult() {
		return Arrays.asList(
				"Lorem <em>ipsum</em> dolor sit amet, consectetur adipiscing elit."
		);
	}

	@Test
	void defaultNoMatchSize() {
		StubMappingScope scope = index.createScope();

		SearchQuery<List<String>> highlights = scope.query().select(
				f -> f.highlight( "anotherString" )
		)
				.where( f -> f.match().field( "string" ).matching( "foo" ) )
				.highlighter( h -> highlighter( h ) )
				.toQuery();

		assertThatHits( highlights.fetchAllHits() )
				.hasHitsAnyOrder(
						// by default no match size  == 0, nothing is returned
						Collections.singletonList( Collections.emptyList() )
				);
	}

	@Test
	void noMatchSize() {
		assumeTrue( supportsNoMatchSize() );
		StubMappingScope scope = index.createScope();

		SearchQuery<List<String>> highlights = scope.query().select(
				f -> f.highlight( "anotherString" )
		)
				.where( f -> f.match().field( "string" ).matching( "foo" ) )
				.highlighter( h -> highlighter( h ).noMatchSize( 11 ) )
				.toQuery();

		assertThatHits( highlights.fetchAllHits() )
				.hasHitsAnyOrder(
						Collections.singletonList( "Lorem ipsum" )
				);
	}

	@Test
	void noMatchSizeNotSupported() {
		assumeFalse( supportsNoMatchSize() );
		StubMappingScope scope = index.createScope();

		logged.expectEvent(
				Level.WARN,
				"Lucene's unified highlighter cannot limit the size of a fragment returned when no match is found. Instead if no match size was set to any positive integer - all text will be returned. Configured value '11' will be ignored, and the fragment will not be limited. If you don't want to see this warning set the value to Integer.MAX_VALUE."
		);

		SearchQuery<List<String>> highlights = scope.query().select(
				f -> f.highlight( "anotherString" )
		)
				.where( f -> f.match().field( "string" ).matching( "foo" ) )
				.highlighter( h -> highlighter( h ).noMatchSize( 11 ) )
				.toQuery();
		highlights.fetchAllHits();
	}

	protected boolean supportsNoMatchSize() {
		return true;
	}

	@Test
	void noMatchSizeMultiField() {
		assumeTrue( supportsNoMatchSizeOnMultivaluedFields() );

		StubMappingScope scope = index.createScope();

		SearchQuery<List<String>> highlights = scope.query().select(
				f -> f.highlight( "multiValuedString" )
		)
				.where( f -> f.match().field( "objectFlattened.string" ).matching( "dog" ) )
				// set to max possible value so that all highlighters can return something:
				.highlighter( h -> highlighter( h ).noMatchSize( Integer.MAX_VALUE ) )
				.toQuery();

		assertThatHits( highlights.fetchAllHits() )
				.hasHitsAnyOrder(
						// expect first value if none match in a multi-value field:
						Collections.singletonList( "The quick brown fox jumps right over the little lazy dog" )
				);
	}

	protected boolean supportsNoMatchSizeOnMultivaluedFields() {
		return true;
	}

	@Test
	void compositeHighlight() {
		StubMappingScope scope = index.createScope();

		SearchQuery<List<?>> highlights = scope.query().select(
				f -> f.composite().from(
						f.highlight( "string" ),
						f.highlight( "anotherString" )
				).asList()
		)
				.where( f -> f.bool()
						.should( f.match().field( "anotherString" ).matching( "fox" ) )
						.should( f.match().field( "string" ).matching( "dog" ) )
				)
				.highlighter( h -> highlighter( h ) )
				.toQuery();

		assertThatHits( highlights.fetchAllHits() )
				.hasHitsAnyOrder(
						Arrays.asList(
								Collections.singletonList( "This string mentions a <em>dog</em> too" ),
								Collections.emptyList()
						),
						Arrays.asList(
								Collections.singletonList( "This string mentions a <em>dog</em>" ),
								Collections.singletonList(
										"The quick brown <em>fox</em> jumps right over the little lazy dog" )
						),
						Arrays.asList(
								Collections.emptyList(),
								Collections.singletonList(
										"The quick brown <em>fox</em> jumps right over the little lazy dog" )
						)
				);
	}

	@Test
	void compositeHighlightMultipleConfigurations() {
		StubMappingScope scope = index.createScope();

		SearchQuery<List<?>> highlights = scope.query().select(
				f -> f.composite().from(
						f.highlight( "string" ).highlighter( "for-string" ),
						f.highlight( "anotherString" ).highlighter( "for-another-string" )
				).asList()
		)
				.where( f -> f.bool()
						.should( f.match().field( "anotherString" ).matching( "fox" ) )
						.should( f.match().field( "string" ).matching( "dog" ) )
				)
				.highlighter( h -> highlighter( h ).tag( "*", "*" ) )
				.highlighter( "for-string", h -> highlighter( h ).tag( "**", "**" ) )
				.highlighter( "for-another-string", h -> highlighter( h ).tag( "***", "***" ) )
				.toQuery();

		assertThatHits( highlights.fetchAllHits() )
				.hasHitsAnyOrder(
						Arrays.asList(
								Collections.singletonList( "This string mentions a **dog** too" ),
								Collections.emptyList()
						),
						Arrays.asList(
								Collections.singletonList( "This string mentions a **dog**" ),
								Collections.singletonList(
										"The quick brown ***fox*** jumps right over the little lazy dog" )
						),
						Arrays.asList(
								Collections.emptyList(),
								Collections.singletonList(
										"The quick brown ***fox*** jumps right over the little lazy dog" )
						)
				);
	}

	@Test
	void multivaluedField() {
		StubMappingScope scope = index.createScope();

		SearchQuery<List<String>> highlights = scope.query().select(
				f -> f.highlight( "multiValuedString" )
		)
				.where( f -> f.match().field( "multiValuedString" ).matching( "dog" ) )
				.highlighter( h -> highlighter( h ) )
				.toQuery();

		assertThatHits( highlights.fetchAllHits() )
				.hasHitsAnyOrder( Collections.singletonList( multivaluedFieldResult() ) );
	}

	@Test
	void multivaluedFieldDuplicated() {
		StubMappingScope scope = index.createScope();

		SearchQuery<List<?>> highlights = scope.query().select(
				f -> f.composite().from(
						f.highlight( "multiValuedString" ),
						f.highlight( "multiValuedString" )
				).asList()
		)
				.where( f -> f.match().field( "multiValuedString" ).matching( "dog" ) )
				.highlighter( h -> highlighter( h ) )
				.toQuery();

		assertThatHits( highlights.fetchAllHits() )
				.hasHitsAnyOrder(
						Collections.singletonList(
								Arrays.asList(
										multivaluedFieldResult(),
										multivaluedFieldResult()
								)
						)
				);
	}

	protected List<String> multivaluedFieldResult() {
		return Arrays.asList(
				"The quick brown fox jumps right over the little lazy <em>dog</em>",
				"This string mentions a <em>dog</em>"
		);
	}

	@Test
	void inObjectField() {
		StubMappingScope scope = index.createScope();

		SearchQuery<List<String>> highlights = scope.query().select(
				f -> f.highlight( "objectFlattened.string" )
		)
				.where( f -> f.match().field( "objectFlattened.string" ).matching( "fox" ) )
				.highlighter( h -> highlighter( h ) )
				.toQuery();

		assertThatHits( highlights.fetchAllHits() )
				.hasHitsAnyOrder(
						Collections.singletonList(
								Collections.singletonList(
										"The quick brown <em>fox</em> jumps right over the little lazy dog" ) )
				);
	}

	@Test
	void inObjectFieldFieldWildcard() {
		StubMappingScope scope = index.createScope();

		SearchQuery<List<String>> highlights = scope.query().select(
				f -> f.highlight( "objectFlattened.string" )
		)
				.where( f -> f.wildcard().field( "objectFlattened.string" ).matching( "fo?" ) )
				.highlighter( h -> highlighter( h ) )
				.toQuery();

		assertThatHits( highlights.fetchAllHits() )
				.hasHitsAnyOrder(
						Collections.singletonList(
								Collections.singletonList(
										"The quick brown <em>fox</em> jumps right over the little lazy dog" ) )
				);
	}

	@Test
	void simpleFieldWildcard() {
		StubMappingScope scope = index.createScope();

		SearchQuery<List<String>> highlights = scope.query().select(
				f -> f.highlight( "string" )
		)
				.where( f -> f.wildcard().field( "string" ).matching( "fo?" ) )
				.highlighter( h -> highlighter( h ) )
				.toQuery();

		assertThatHits( highlights.fetchAllHits() )
				.hasHitsAnyOrder(
						Collections.singletonList(
								Collections.singletonList(
										"<em>foo</em> and <em>foo</em> and <em>foo</em> much more times" ) )
				);
	}

	@Test
	void orderByScore() {
		assumeTrue(
				supportsOrderByScoreMultivaluedField(),
				"Some versions of the backend have a bug that prevents them from correctly sorting the results."
		);
		StubMappingScope scope = index.createScope();

		SearchQuery<List<String>> highlights = scope.query().select(
				f -> f.highlight( "multiValuedString" )
		)
				.where( f -> f.bool()
						.must( f.match().field( "multiValuedString" ).matching( "dog" ) )
						.should( f.match().field( "multiValuedString" ).matching( "string" ).boost( 10.0f ) ) )
				.highlighter( h -> highlighter( h ).orderByScore( true ) )
				.toQuery();

		assertThatHits( highlights.fetchAllHits() )
				.hasHitsAnyOrder( orderByScoreResult() );
	}

	protected List<List<String>> orderByScoreResult() {
		return Arrays.asList(
				Arrays.asList(
						"This <em>string</em> mentions a <em>dog</em>",
						"This <em>string</em> mentions a fox",
						"The quick brown fox jumps right over the little lazy <em>dog</em>"
				)
		);
	}

	protected boolean supportsOrderByScoreMultivaluedField() {
		return true;
	}

	@Test
	void unknownNamedHighlighter() {
		assertThatThrownBy(
				() -> index.createScope().query().select(
						f -> f.highlight( "string" ).highlighter( "not-configured-highlighter" )
				).where( f -> f.matchAll() )
						.highlighter( "some-config", h -> highlighter( h ) )
						.highlighter( "some-other-config", h -> highlighter( h ) )
						.toQuery()
		).isInstanceOf( SearchException.class )
				.hasMessageContainingAll(
						"Cannot find a highlighter with name 'not-configured-highlighter'.",
						"Available highlighters are:",
						"some-config", "some-other-config"
				);
	}

	@Test
	void prebuiltHighlighter() {
		SearchHighlighter highlighter = highlighter( index.createScope().highlighter() ).tag( "---", "---" )
				.toHighlighter();

		SearchQuery<List<String>> highlights = index.createScope().query().select(
				f -> f.highlight( "string" )
		).where( f -> f.match().field( "string" ).matching( "dog" ) )
				.highlighter( highlighter )
				.toQuery();

		assertThatHits( highlights.fetchAllHits() )
				.hasHitsAnyOrder(
						Arrays.asList(
								Collections.singletonList( "This string mentions a ---dog---" ),
								Collections.singletonList( "This string mentions a ---dog--- too" )
						)
				);
	}

	@Test
	void prebuiltNamedHighlighter() {
		SearchHighlighter highlighter = highlighter( index.createScope().highlighter() ).tag( "---", "---" )
				.toHighlighter();

		SearchQuery<List<String>> highlights = index.createScope().query().select(
				f -> f.highlight( "string" ).highlighter( "named-highlighter" )
		).where( f -> f.match().field( "string" ).matching( "dog" ) )
				.highlighter( "named-highlighter", highlighter )
				.toQuery();

		assertThatHits( highlights.fetchAllHits() )
				.hasHitsAnyOrder(
						Arrays.asList(
								Collections.singletonList( "This string mentions a ---dog---" ),
								Collections.singletonList( "This string mentions a ---dog--- too" )
						)
				);
	}

	@Test
	void prebuiltHighlighterWrongScope() {
		SearchHighlighter highlighter = highlighter( notMatchingTypeIndex.createScope().highlighter() ).tag( "---", "---" )
				.toHighlighter();

		assertFailScope(
				() -> index.createScope().query().select(
						f -> f.highlight( "string" )
				).where( f -> f.match().field( "string" ).matching( "dog" ) )
						.highlighter( highlighter )
						.toQuery(),
				Set.of( index.name() ),
				Set.of( notMatchingTypeIndex.name() ),
				Set.of( index.name() )
		);

		SearchHighlighter highlighter2 = highlighter( index.createScope( indexMatching ).highlighter() ).tag( "---", "---" )
				.toHighlighter();

		assertThatCode( () -> index.createScope().query().select(
				f -> f.highlight( "string" )
		).where( f -> f.match().field( "string" ).matching( "dog" ) )
				.highlighter( highlighter2 )
				.toQuery() )
				.doesNotThrowAnyException();
		assertThatCode( () -> indexMatching.createScope().query().select(
				f -> f.highlight( "string" )
		).where( f -> f.match().field( "string" ).matching( "dog" ) )
				.highlighter( highlighter2 )
				.toQuery() )
				.doesNotThrowAnyException();
	}

	private static void assertFailScope(ThrowableAssert.ThrowingCallable query, Set<String> scope,
			Set<String> highlighter, Set<String> differences) {
		List<String> messageParts = new ArrayList<>();
		messageParts.add( "Invalid highlighter" );
		messageParts.add( "You must build the highlighter from a scope targeting indexes " );
		messageParts.addAll( scope );
		messageParts.add( "the given highlighter was built from a scope targeting " );
		messageParts.addAll( highlighter );
		messageParts.add( "where indexes [" );
		messageParts.addAll( differences );
		messageParts.add( "] are missing" );

		assertThatThrownBy( query )
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll( messageParts.toArray( String[]::new ) );
	}

	@Test
	void phraseMatching() {
		SearchQuery<List<String>> highlights = index.createScope().query().select(
				f -> f.highlight( "multiValuedString" )
		).where( f -> f.phrase().field( "multiValuedString" ).matching( "brown fox" ) )
				.highlighter( h -> highlighter( h ) )
				.toQuery();

		assertThatHits( highlights.fetchAllHits() )
				.hasHitsAnyOrder(
						phraseMatchingResult()
				);
	}

	protected boolean supportsPhraseMatching() {
		return true;
	}

	private List<List<String>> phraseMatchingResult() {
		if ( supportsPhraseMatching() ) {
			return Collections.singletonList(
					Collections.singletonList(
							"The quick <em>brown fox</em> jumps right over the little lazy dog" )
			);
		}
		else {
			return Collections.singletonList(
					Collections.singletonList(
							"The quick <em>brown</em> <em>fox</em> jumps right over the little lazy dog" )
			);
		}
	}

	protected static class IndexBinding {
		final IndexFieldReference<String> stringField;
		final IndexFieldReference<String> anotherStringField;
		final IndexFieldReference<String> objectFlattenedString;
		final IndexFieldReference<String> notAnalyzedString;
		final IndexFieldReference<String> multiValuedString;
		final IndexObjectFieldReference objectFlattened;
		final IndexFieldReference<String> stringNoTermVectorField;
		final IndexFieldReference<Integer> intField;

		IndexBinding(IndexSchemaElement root) {
			stringField = root.field( "string", f -> f.asString()
					.highlightable( Collections.singletonList( Highlightable.ANY ) )
					.analyzer( DefaultAnalysisDefinitions.ANALYZER_STANDARD_ENGLISH.name )
					.termVector( TermVector.WITH_POSITIONS_OFFSETS_PAYLOADS )
			).toReference();

			anotherStringField = root.field( "anotherString", f -> f.asString()
					.highlightable( Collections.singletonList( Highlightable.ANY ) )
					.analyzer( DefaultAnalysisDefinitions.ANALYZER_STANDARD_ENGLISH.name )
					.termVector( TermVector.WITH_POSITIONS_OFFSETS )
			).toReference();

			IndexSchemaObjectField objectField = root.objectField( "objectFlattened" );
			objectFlattened = objectField.toReference();

			objectFlattenedString = objectField.field( "string", f -> f.asString()
					.highlightable( Collections.singletonList( Highlightable.ANY ) )
					.analyzer( DefaultAnalysisDefinitions.ANALYZER_STANDARD_ENGLISH.name )
			).toReference();

			notAnalyzedString = root.field( "notAnalyzedString", f -> f.asString() ).toReference();

			multiValuedString = root.field( "multiValuedString", f -> f.asString()
					.highlightable( Collections.singletonList( Highlightable.ANY ) )
					.analyzer( DefaultAnalysisDefinitions.ANALYZER_STANDARD_ENGLISH.name )
			).multiValued().toReference();

			stringNoTermVectorField = root.field( "stringNoTermVector", f -> f.asString()
					.highlightable( Arrays.asList( Highlightable.UNIFIED, Highlightable.PLAIN ) )
					.analyzer( DefaultAnalysisDefinitions.ANALYZER_STANDARD_ENGLISH.name )
			).toReference();

			intField = root.field( "int", f -> f.asInteger() ).toReference();
		}
	}

	private static class NotMatchingTypeIndexBinding {
		final IndexFieldReference<Integer> stringField;

		NotMatchingTypeIndexBinding(IndexSchemaElement root) {
			stringField = root.field( "string", f -> f.asInteger() ).toReference();
		}
	}
}
