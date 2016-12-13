/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.test;

import static org.fest.assertions.Assertions.assertThat;
import static org.fest.assertions.MapAssert.entry;

import java.lang.annotation.Annotation;
import java.util.Map;

import org.apache.lucene.analysis.charfilter.HTMLStripCharFilterFactory;
import org.apache.lucene.analysis.cjk.CJKBigramFilterFactory;
import org.apache.lucene.analysis.core.WhitespaceTokenizerFactory;
import org.apache.lucene.analysis.no.NorwegianLightStemFilterFactory;
import org.apache.lucene.analysis.pattern.PatternCaptureGroupFilterFactory;
import org.apache.lucene.analysis.pattern.PatternReplaceFilterFactory;
import org.apache.lucene.analysis.standard.StandardFilterFactory;
import org.apache.lucene.analysis.standard.StandardTokenizerFactory;
import org.apache.lucene.analysis.synonym.SynonymFilterFactory;
import org.apache.lucene.analysis.util.TokenizerFactory;
import org.hibernate.annotations.common.annotationfactory.AnnotationDescriptor;
import org.hibernate.annotations.common.annotationfactory.AnnotationFactory;
import org.hibernate.search.annotations.CharFilterDef;
import org.hibernate.search.annotations.Parameter;
import org.hibernate.search.annotations.TokenFilterDef;
import org.hibernate.search.annotations.TokenizerDef;
import org.hibernate.search.elasticsearch.impl.JsonBuilder;
import org.hibernate.search.elasticsearch.settings.impl.DefaultElasticsearchAnalyzerDefinitionTranslator;
import org.hibernate.search.elasticsearch.settings.impl.model.CharFilterDefinition;
import org.hibernate.search.elasticsearch.settings.impl.model.TokenFilterDefinition;
import org.hibernate.search.elasticsearch.settings.impl.model.TokenizerDefinition;
import org.hibernate.search.exception.SearchException;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.google.gson.JsonPrimitive;

/**
 * @author Yoann Rodiere
 */
public class DefaultElasticsearchAnalyzerDefinitionTranslatorTest {

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	private DefaultElasticsearchAnalyzerDefinitionTranslator translator =
			new DefaultElasticsearchAnalyzerDefinitionTranslator();

	@Before
	public void setup() {
		translator.start( null, null ); // Parameters are not used
	}

	@After
	public void tearDown() {
		translator.stop();
	}

	@Test
	public void unknownClass() {
		TokenizerDef annotation = annotation(
				TokenizerDef.class,
				CustomTokenizerFactory.class
				);

		thrown.expect( SearchException.class );
		thrown.expectMessage( "HSEARCH400059" );
		thrown.expectMessage( CustomTokenizerFactory.class.getSimpleName() );

		translator.translate( annotation );
	}

	@Test
	public void translateType() {
		TokenFilterDef annotation = annotation(
				TokenFilterDef.class,
				StandardFilterFactory.class
				);

		TokenFilterDefinition definition = translator.translate( annotation );

		assertThat( definition.getType() ).as( "type" ).isEqualTo( "standard" );
	}

	@Test
	public void renameParameter() {
		TokenizerDef annotation = annotation(
				TokenizerDef.class,
				StandardTokenizerFactory.class,
				param( "maxTokenLength", "5" )
				);

		TokenizerDefinition definition = translator.translate( annotation );

		assertThat( definition.getParameters() ).as( "parameters" )
				.includes( entry( "max_token_length", new JsonPrimitive( "5" ) ) );
		assertThat( definition.getParameters().keySet() ).as( "parameter names" )
				.excludes( "maxTokenLength" );
	}

	@Test
	public void disallowParameter() {
		TokenizerDef annotation = annotation(
				TokenizerDef.class,
				WhitespaceTokenizerFactory.class,
				param( "rule", "foo" )
				);

		thrown.expect( SearchException.class );
		thrown.expectMessage( "HSEARCH400061" );
		thrown.expectMessage( WhitespaceTokenizerFactory.class.getSimpleName() );
		thrown.expectMessage( "'rule'" );

		translator.translate( annotation );
	}

	@Test
	public void transformParameter() {
		CharFilterDef annotation = annotation(
				CharFilterDef.class,
				HTMLStripCharFilterFactory.class,
				param( "escapedTags", "foo,bar" )
				);

		CharFilterDefinition definition = translator.translate( annotation );

		assertThat( definition.getParameters() ).as( "parameters" )
				.includes( entry(
						"escaped_tags",
						JsonBuilder.array().add( new JsonPrimitive( "foo" ) ).add( new JsonPrimitive( "bar" ) ).build()
				) );
	}

	@Test
	public void transformParameter_tokenizerClass() {
		TokenFilterDef annotation = annotation(
				TokenFilterDef.class,
				SynonymFilterFactory.class,
				param( "tokenizerFactory", WhitespaceTokenizerFactory.class.getName() )
				);

		TokenFilterDefinition definition = translator.translate( annotation );

		assertThat( definition.getParameters() ).as( "parameters" )
				.includes( entry(
						"tokenizer",
						new JsonPrimitive( "whitespace" )
				) );
	}

	@Test
	public void transformParameter_tokenizerClass_unknownClass() {
		TokenFilterDef annotation = annotation(
				TokenFilterDef.class,
				SynonymFilterFactory.class,
				param( "tokenizerFactory", CustomTokenizerFactory.class.getName() )
				);

		thrown.expect( SearchException.class );
		thrown.expectMessage( "HSEARCH400062" );
		thrown.expectMessage( SynonymFilterFactory.class.getSimpleName() );
		thrown.expectMessage( "'tokenizerFactory'" );

		translator.translate( annotation );
	}

	@Test
	public void transformParameter_singleElementArray() {
		TokenFilterDef annotation = annotation(
				TokenFilterDef.class,
				PatternCaptureGroupFilterFactory.class,
				param( "pattern", "foo" )
				);

		TokenFilterDefinition definition = translator.translate( annotation );

		assertThat( definition.getParameters() ).as( "parameters" )
				.includes( entry(
						"patterns",
						JsonBuilder.array().add( new JsonPrimitive( "foo" ) ).build()
				) );
	}

	@Test
	public void transformParameter_norwegianStemmer() {
		TokenFilterDef annotation = annotation(
				TokenFilterDef.class,
				NorwegianLightStemFilterFactory.class
				);

		TokenFilterDefinition definition = translator.translate( annotation );

		assertThat( definition.getType() ).as( "type" ).isEqualTo( "stemmer" );
		assertThat( definition.getParameters() ).as( "parameters" )
				.includes( entry(
						"name", new JsonPrimitive( "light_norwegian" )
				) );
	}

	@Test
	public void transformParameter_norwegianStemmer_bokmal() {
		TokenFilterDef annotation = annotation(
				TokenFilterDef.class,
				NorwegianLightStemFilterFactory.class,
				param( "variant", "nb" )
				);

		TokenFilterDefinition definition = translator.translate( annotation );

		assertThat( definition.getType() ).as( "type" ).isEqualTo( "stemmer" );
		assertThat( definition.getParameters() ).as( "parameters" )
				.includes( entry(
						"name", new JsonPrimitive( "light_norwegian" )
				) );
	}

	@Test
	public void transformParameter_norwegianStemmer_nynorsk() {
		TokenFilterDef annotation = annotation(
				TokenFilterDef.class,
				NorwegianLightStemFilterFactory.class,
				param( "variant", "nn" )
				);

		TokenFilterDefinition definition = translator.translate( annotation );

		assertThat( definition.getType() ).as( "type" ).isEqualTo( "stemmer" );
		assertThat( definition.getParameters() ).as( "parameters" )
				.includes( entry(
						"name", new JsonPrimitive( "light_nynorsk" )
				) );
	}

	@Test
	public void transformParameter_norwegianStemmer_invalid() {
		TokenFilterDef annotation = annotation(
				TokenFilterDef.class,
				NorwegianLightStemFilterFactory.class,
				param( "variant", "invalid" )
				);

		thrown.expect( SearchException.class );
		thrown.expectMessage( "HSEARCH400063" );
		thrown.expectMessage( NorwegianLightStemFilterFactory.class.getSimpleName() );
		thrown.expectMessage( "'variant'" );
		thrown.expectMessage( "'invalid'" );

		translator.translate( annotation );
	}

	@Test
	public void transformParameter_patternReplace() {
		TokenFilterDef annotation = annotation(
				TokenFilterDef.class,
				PatternReplaceFilterFactory.class,
				param( "replace", "first" )
				);

		TokenFilterDefinition definition = translator.translate( annotation );

		assertThat( definition.getParameters() ).as( "parameters" )
				.includes( entry(
						"all", new JsonPrimitive( "false" )
				) );
	}

	@Test
	public void transformParameter_cjkBigramIgnoredScripts() {
		TokenFilterDef annotation = annotation(
				TokenFilterDef.class,
				CJKBigramFilterFactory.class,
				param( "outputUnigrams", "true" ),
				param( "han", "false" ),
				param( "hiragana", "false" ),
				param( "katakana", "false" ),
				param( "hangul", "false" )
				);

		TokenFilterDefinition definition = translator.translate( annotation );

		assertThat( definition.getParameters() ).as( "parameters" )
				.includes(
						entry(
								"ignored_scripts",
								JsonBuilder.array()
										.add( new JsonPrimitive( "han" ) )
										.add( new JsonPrimitive( "hiragana" ) )
										.add( new JsonPrimitive( "katakana" ) )
										.add( new JsonPrimitive( "hangul" ) )
								.build()
						),
						entry(
								"output_unigrams",
								new JsonPrimitive( true )
						)
				);

		assertThat( definition.getParameters().keySet() ).as( "parameter names" )
				.excludes( "han", "hiragana", "katakana", "hangul", "outputUnigrams" );
	}

	private static <T extends Annotation> T annotation(Class<T> annotationType, Class<?> factoryType, Parameter ... parameters) {
		AnnotationDescriptor descriptor = new AnnotationDescriptor( annotationType );
		descriptor.setValue( "factory", factoryType );
		descriptor.setValue( "params", parameters );
		return AnnotationFactory.create( descriptor );
	}

	private static Parameter param(String name, String value) {
		AnnotationDescriptor descriptor = new AnnotationDescriptor( Parameter.class );
		descriptor.setValue( "name", name );
		descriptor.setValue( "value", value );
		return AnnotationFactory.create( descriptor );
	}

	private abstract static class CustomTokenizerFactory extends TokenizerFactory {
		protected CustomTokenizerFactory(Map<String, String> args) {
			super( args );
		}
	}

}
