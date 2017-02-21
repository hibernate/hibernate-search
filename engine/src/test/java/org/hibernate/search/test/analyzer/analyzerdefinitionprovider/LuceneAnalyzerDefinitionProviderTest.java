/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.analyzer.analyzerdefinitionprovider;

import static org.fest.assertions.Assertions.assertThat;

import java.util.List;

import org.apache.lucene.analysis.core.KeywordTokenizerFactory;
import org.apache.lucene.analysis.core.LowerCaseFilterFactory;
import org.apache.lucene.analysis.core.WhitespaceTokenizerFactory;
import org.apache.lucene.analysis.pattern.PatternReplaceCharFilterFactory;
import org.apache.lucene.analysis.pattern.PatternTokenizerFactory;
import org.apache.lucene.analysis.standard.StandardTokenizerFactory;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.hibernate.search.analyzer.definition.LuceneAnalyzerDefinitionRegistryBuilder;
import org.hibernate.search.analyzer.definition.spi.LuceneAnalyzerDefinitionProvider;
import org.hibernate.search.analyzer.definition.spi.LuceneAnalyzerDefinitionSourceService;
import org.hibernate.search.annotations.Analyze;
import org.hibernate.search.annotations.Analyzer;
import org.hibernate.search.annotations.AnalyzerDef;
import org.hibernate.search.annotations.DocumentId;
import org.hibernate.search.annotations.Factory;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.TokenizerDef;
import org.hibernate.search.backend.spi.Work;
import org.hibernate.search.backend.spi.WorkType;
import org.hibernate.search.cfg.Environment;
import org.hibernate.search.engine.integration.impl.ExtendedSearchIntegrator;
import org.hibernate.search.exception.SearchException;
import org.hibernate.search.indexes.spi.LuceneEmbeddedIndexManagerType;
import org.hibernate.search.query.engine.spi.EntityInfo;
import org.hibernate.search.query.engine.spi.HSQuery;
import org.hibernate.search.spi.SearchIntegrator;
import org.hibernate.search.spi.SearchIntegratorBuilder;
import org.hibernate.search.testsupport.TestForIssue;
import org.hibernate.search.testsupport.junit.SkipOnElasticsearch;
import org.hibernate.search.testsupport.setup.SearchConfigurationForTest;
import org.hibernate.search.testsupport.setup.TransactionContextForTest;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.ExpectedException;

/**
 * @author Yoann Rodiere
 */
@Category(SkipOnElasticsearch.class) // LuceneAnalyzerDefinitionProvider is Lucene-specific
@TestForIssue(jiraKey = "HSEARCH-2418")
public class LuceneAnalyzerDefinitionProviderTest {

	private static final String CUSTOM_ANALYZER_NAME = "custom-analyzer";

	private static final String CUSTOM_ANALYZER_2_NAME = "custom-analyzer-2";

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Test
	public void simple() {
		ExtendedSearchIntegrator integrator = init( CustomAnalyzerProvider.class, CustomAnalyzerEntity.class );

		assertThat( integrator.getAnalyzer( CUSTOM_ANALYZER_NAME ) )
				.as( "Analyzer for '" + CUSTOM_ANALYZER_NAME + "' fetched from the integrator" )
				.isNotNull();

		assertThat( integrator.getAnalyzerRegistry( LuceneEmbeddedIndexManagerType.INSTANCE )
						.getAnalyzerReference( CUSTOM_ANALYZER_NAME ) )
				.as( "Analyzer reference for '" + CUSTOM_ANALYZER_NAME + "' fetched from the integrator" )
				.isNotNull();

		CustomAnalyzerEntity entity = new CustomAnalyzerEntity();
		entity.id = 0;
		entity.field = "charFilterShouldReplace|foo";
		index( integrator, entity );
		assertMatchesExactly( integrator, entity, "field", "charfilterdidreplace" );
	}

	@Test
	public void usingServiceOverride() {
		ExtendedSearchIntegrator integrator = initUsingService( new CustomAnalyzerProvider(), CustomAnalyzerEntity.class );

		assertThat( integrator.getAnalyzer( CUSTOM_ANALYZER_NAME ) )
				.as( "Analyzer for '" + CUSTOM_ANALYZER_NAME + "' fetched from the integrator" )
				.isNotNull();

		assertThat( integrator.getAnalyzerRegistry( LuceneEmbeddedIndexManagerType.INSTANCE )
						.getAnalyzerReference( CUSTOM_ANALYZER_NAME ) )
				.as( "Analyzer reference for '" + CUSTOM_ANALYZER_NAME + "' fetched from the integrator" )
				.isNotNull();

		CustomAnalyzerEntity entity = new CustomAnalyzerEntity();
		entity.id = 0;
		entity.field = "charFilterShouldReplace|foo";
		index( integrator, entity );
		assertMatchesExactly( integrator, entity, "field", "charfilterdidreplace" );
	}

	@Test
	public void override() {
		ExtendedSearchIntegrator integrator = init( CustomAnalyzerProvider.class, AnalyzerDefAnnotationEntity.class );

		assertThat( integrator.getAnalyzer( CUSTOM_ANALYZER_NAME ) )
				.as( "Analyzer for '" + CUSTOM_ANALYZER_NAME + "' fetched from the integrator" )
				.isNotNull();

		assertThat( integrator.getAnalyzerRegistry( LuceneEmbeddedIndexManagerType.INSTANCE )
						.getAnalyzerReference( CUSTOM_ANALYZER_NAME ) )
				.as( "Analyzer reference for '" + CUSTOM_ANALYZER_NAME + "' fetched from the integrator" )
				.isNotNull();

		AnalyzerDefAnnotationEntity entity = new AnalyzerDefAnnotationEntity();
		entity.id = 0;
		entity.field = "charFilterShouldReplace|foo";
		index( integrator, entity );
		assertMatchesExactly( integrator, entity, "field", "charFilterShouldReplace|foo" );
	}

	/**
	 * Test that even when the analyzer isn't referenced from the mapping,
	 * it is available in the registry.
	 */
	@Test
	public void unreferencedAnalyzer() {
		ExtendedSearchIntegrator integrator = init( CustomAnalyzerProvider.class, NoAnalyzerEntity.class );

		assertThat( integrator.getAnalyzer( CUSTOM_ANALYZER_NAME ) )
				.as( "Analyzer for '" + CUSTOM_ANALYZER_NAME + "' fetched from the integrator" )
				.isNotNull();

		assertThat( integrator.getAnalyzerRegistry( LuceneEmbeddedIndexManagerType.INSTANCE )
						.getAnalyzerReference( CUSTOM_ANALYZER_NAME ) )
				.as( "Analyzer reference for '" + CUSTOM_ANALYZER_NAME + "' fetched from the integrator" )
				.isNotNull();
	}

	@Test
	public void instantiation_factorymethod() {
		ExtendedSearchIntegrator integrator = init( ProviderFactory.class, CustomAnalyzerEntity.class );

		assertThat( integrator.getAnalyzer( CUSTOM_ANALYZER_NAME ) )
				.as( "Analyzer for '" + CUSTOM_ANALYZER_NAME + "' fetched from the integrator" )
				.isNotNull();
	}

	@Test
	public void invalid() {
		SearchConfigurationForTest cfg = new SearchConfigurationForTest();
		cfg.addClass( CustomAnalyzerEntity.class );
		cfg.addProperty( Environment.ANALYZER_DEFINITION_PROVIDER, "invalidValue" );

		thrown.expect( SearchException.class );
		thrown.expectMessage( "HSEARCH000329" );

		new SearchIntegratorBuilder().configuration( cfg ).buildSearchIntegrator();
	}

	@Test
	public void namingConflict_withinProvider() {
		thrown.expect( SearchException.class );
		thrown.expectMessage( "HSEARCH000330" );

		init( ProviderWithInternalNamingConflict.class, CustomAnalyzerEntity.class );
	}

	private ExtendedSearchIntegrator initUsingService(LuceneAnalyzerDefinitionProvider analyzerProvider, Class<?> ... entityClasses) {
		SearchConfigurationForTest cfg = new SearchConfigurationForTest();
		for ( Class<?> entityClass : entityClasses ) {
			cfg.addClass( entityClass );
		}
		cfg.getProvidedServices().put( LuceneAnalyzerDefinitionSourceService.class, new LuceneAnalyzerDefinitionSourceService() {

					@Override
					public LuceneAnalyzerDefinitionProvider getLuceneAnalyzerDefinitionProvider() {
						return analyzerProvider;
					}

				});
		return new SearchIntegratorBuilder().configuration( cfg ).buildSearchIntegrator()
				.unwrap( ExtendedSearchIntegrator.class );
	}

	private ExtendedSearchIntegrator init(Class<?> providerClass, Class<?> ... entityClasses) {
		SearchConfigurationForTest cfg = new SearchConfigurationForTest();
		for ( Class<?> entityClass : entityClasses ) {
			cfg.addClass( entityClass );
		}
		cfg.addProperty( Environment.ANALYZER_DEFINITION_PROVIDER, providerClass.getName() );
		return new SearchIntegratorBuilder().configuration( cfg ).buildSearchIntegrator()
				.unwrap( ExtendedSearchIntegrator.class );
	}

	private void index(SearchIntegrator integrator, Identifiable entity) {
		Work work = new Work( entity, entity.getId(), WorkType.ADD, false );
		TransactionContextForTest tc = new TransactionContextForTest();
		integrator.getWorker().performWork( work, tc );
		tc.end();
	}

	private void assertMatchesExactly(SearchIntegrator integrator, Identifiable entity, String fieldName, String termValue) {
		assertMatchesExactly( integrator, entity, new TermQuery( new Term( fieldName, termValue ) ) );
	}

	private void assertMatchesExactly(SearchIntegrator integrator, Identifiable entity, Query luceneQuery) {
		Class<?> entityClass = entity.getClass();
		HSQuery query = integrator.createHSQuery(
				luceneQuery,
				entityClass
				);
		List<EntityInfo> results = query.queryEntityInfos();
		assertThat( results )
				.onProperty( "id" )
				.as( "Results of query '" + luceneQuery + "' on " + entityClass.getSimpleName() )
				.containsExactly( entity.getId() );
	}

	private interface Identifiable {
		long getId();
	}

	@Indexed
	static class NoAnalyzerEntity implements Identifiable {
		@DocumentId
		long id;

		@Field(analyze = Analyze.NO)
		String field;

		@Override
		public long getId() {
			return id;
		}
	}

	@Indexed
	static class CustomAnalyzerEntity implements Identifiable {
		@DocumentId
		long id;

		@Field(analyzer = @Analyzer(definition = CUSTOM_ANALYZER_NAME))
		String field;

		@Override
		public long getId() {
			return id;
		}
	}

	@Indexed
	@AnalyzerDef(name = CUSTOM_ANALYZER_NAME, tokenizer = @TokenizerDef(factory = KeywordTokenizerFactory.class))
	static class AnalyzerDefAnnotationEntity extends CustomAnalyzerEntity {
	}

	@Indexed
	static class CustomAnalyzer2Entity implements Identifiable {
		@DocumentId
		long id;

		@Field(analyzer = @Analyzer(definition = CUSTOM_ANALYZER_2_NAME))
		String field;

		@Override
		public long getId() {
			return id;
		}
	}

	public static class CustomAnalyzerProvider implements LuceneAnalyzerDefinitionProvider {
		@Override
		public void register(LuceneAnalyzerDefinitionRegistryBuilder builder) {
			builder
					.analyzer( CUSTOM_ANALYZER_NAME )
							.tokenizer( PatternTokenizerFactory.class )
									.param( "pattern", "\\|" )
							.charFilter( PatternReplaceCharFilterFactory.class )
									.param( "pattern", "charFilterShouldReplace" )
									.param( "replacement", "charFilterDidReplace" )
							.tokenFilter( LowerCaseFilterFactory.class );
		}
	}

	public static class ProviderFactory {
		@Factory
		public static CustomAnalyzerProvider create() {
			return new CustomAnalyzerProvider();
		}
	}

	public static class CustomAnalyzer2Provider implements LuceneAnalyzerDefinitionProvider {
		@Override
		public void register(LuceneAnalyzerDefinitionRegistryBuilder builder) {
			builder
					.analyzer( CUSTOM_ANALYZER_2_NAME )
							.tokenizer( WhitespaceTokenizerFactory.class );
		}
	}

	public static class ProviderWithInternalNamingConflict implements LuceneAnalyzerDefinitionProvider {
		@Override
		public void register(LuceneAnalyzerDefinitionRegistryBuilder builder) {
			builder
					.analyzer( CUSTOM_ANALYZER_NAME )
							.tokenizer( StandardTokenizerFactory.class )
					.analyzer( CUSTOM_ANALYZER_NAME )
							.tokenizer( StandardTokenizerFactory.class );
		}
	}

}
