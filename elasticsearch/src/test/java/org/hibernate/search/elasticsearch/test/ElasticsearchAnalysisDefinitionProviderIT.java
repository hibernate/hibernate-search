/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.test;

import static org.fest.assertions.Assertions.assertThat;

import java.util.List;

import org.apache.lucene.analysis.core.KeywordTokenizerFactory;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.hibernate.search.analyzer.definition.LuceneAnalyzerDefinitionProvider;
import org.hibernate.search.analyzer.definition.LuceneAnalyzerDefinitionRegistryBuilder;
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
import org.hibernate.search.elasticsearch.analyzer.definition.ElasticsearchAnalysisDefinitionProvider;
import org.hibernate.search.elasticsearch.analyzer.definition.ElasticsearchAnalysisDefinitionRegistryBuilder;
import org.hibernate.search.elasticsearch.cfg.ElasticsearchEnvironment;
import org.hibernate.search.elasticsearch.spi.ElasticsearchIndexManagerType;
import org.hibernate.search.engine.integration.impl.ExtendedSearchIntegrator;
import org.hibernate.search.exception.SearchException;
import org.hibernate.search.query.engine.spi.EntityInfo;
import org.hibernate.search.query.engine.spi.HSQuery;
import org.hibernate.search.spi.SearchIntegrator;
import org.hibernate.search.testsupport.TestForIssue;
import org.hibernate.search.testsupport.junit.SearchIntegratorResource;
import org.hibernate.search.testsupport.setup.SearchConfigurationForTest;
import org.hibernate.search.testsupport.setup.TransactionContextForTest;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 * @author Yoann Rodiere
 */
@TestForIssue(jiraKey = "HSEARCH-2418")
public class ElasticsearchAnalysisDefinitionProviderIT {

	private static final String CUSTOM_ANALYZER_NAME = "custom-analyzer";

	private static final String CUSTOM_ANALYZER_2_NAME = "custom-analyzer-2";

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Rule
	public SearchIntegratorResource integratorResource = new SearchIntegratorResource();

	@Test
	public void simple() {
		ExtendedSearchIntegrator integrator = init( CustomAnalyzerProvider.class, CustomAnalyzerEntity.class );

		assertThat( integrator.getIntegration( ElasticsearchIndexManagerType.INSTANCE )
						.getAnalyzerRegistry()
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

		assertThat( integrator.getIntegration( ElasticsearchIndexManagerType.INSTANCE )
						.getAnalyzerRegistry()
						.getAnalyzerReference( CUSTOM_ANALYZER_NAME ) )
				.as( "Analyzer reference for '" + CUSTOM_ANALYZER_NAME + "' fetched from the integrator" )
				.isNotNull();

		AnalyzerDefAnnotationEntity entity = new AnalyzerDefAnnotationEntity();
		entity.id = 0;
		entity.field = "charFilterShouldReplace|foo";
		index( integrator, entity );
		// Expecting a keyword tokenizer (as per the @AnalyzerDef annotation)
		assertMatchesExactly( integrator, entity, "field", "charFilterShouldReplace|foo" );
	}

	@Test
	public void searchFactoryIncrement() {
		MutatingProviderFactory.provider = new CustomAnalyzerProvider();
		ExtendedSearchIntegrator integrator = init( MutatingProviderFactory.class, CustomAnalyzerEntity.class );

		MutatingProviderFactory.provider = new CustomAnalyzer2Provider();
		integrator.addClasses( CustomAnalyzer2Entity.class );

		assertThat( integrator.getIntegration( ElasticsearchIndexManagerType.INSTANCE )
						.getAnalyzerRegistry()
						.getAnalyzerReference( CUSTOM_ANALYZER_2_NAME ) )
				.as( "Analyzer reference for '" + CUSTOM_ANALYZER_2_NAME + "' fetched from the integrator" )
				.isNotNull();

		CustomAnalyzer2Entity entity = new CustomAnalyzer2Entity();
		entity.id = 0;
		entity.field = "foo bar";
		index( integrator, entity );
		assertMatchesExactly( integrator, entity, "field", "foo" );
	}

	/**
	 * Test that even when the analyzer isn't referenced from the mapping,
	 * it is available in the registry.
	 */
	@Test
	public void unreferencedAnalyzer() {
		ExtendedSearchIntegrator integrator = init( CustomAnalyzerProvider.class, NoAnalyzerEntity.class );

		assertThat( integrator.getIntegration( ElasticsearchIndexManagerType.INSTANCE )
						.getAnalyzerRegistry()
						.getAnalyzerReference( CUSTOM_ANALYZER_NAME ) )
				.as( "Analyzer reference for '" + CUSTOM_ANALYZER_NAME + "' fetched from the integrator" )
				.isNotNull();
	}

	@Test
	public void instantiation_factorymethod() {
		ExtendedSearchIntegrator integrator = init( ProviderFactory.class, CustomAnalyzerEntity.class );

		assertThat( integrator.getIntegration( ElasticsearchIndexManagerType.INSTANCE )
						.getAnalyzerRegistry()
						.getAnalyzerReference( CUSTOM_ANALYZER_NAME ) )
				.as( "Analyzer reference for '" + CUSTOM_ANALYZER_NAME + "' fetched from the integrator" )
				.isNotNull();
	}

	@Test
	public void invalid_notAClass() {
		SearchConfigurationForTest cfg = new SearchConfigurationForTest();
		cfg.addClass( CustomAnalyzerEntity.class );
		cfg.addProperty( ElasticsearchEnvironment.ANALYZER_DEFINITION_PROVIDER, "invalidValue" );

		thrown.expect( SearchException.class );
		thrown.expectMessage( "HSEARCH400075" );

		integratorResource.create( cfg );
	}

	@Test
	public void invalid_luceneProvider() {
		SearchConfigurationForTest cfg = new SearchConfigurationForTest();
		cfg.addClass( CustomAnalyzerEntity.class );
		cfg.addProperty( ElasticsearchEnvironment.ANALYZER_DEFINITION_PROVIDER, LuceneAnalyzerDefinitionProviderImpl.class.getName() );

		thrown.expect( SearchException.class );
		thrown.expectMessage( "HSEARCH400075" );

		integratorResource.create( cfg );
	}

	@Test
	public void namingConflict_withinProvider_analyzer() {
		thrown.expect( SearchException.class );
		thrown.expectMessage( "HSEARCH400074" );

		init( ProviderWithInternalAnalyzerNamingConflict.class, CustomAnalyzerEntity.class );
	}

	@Test
	public void namingConflict_withinProvider_tokenizer() {
		thrown.expect( SearchException.class );
		thrown.expectMessage( "HSEARCH400055" );

		init( ProviderWithInternalTokenizerNamingConflict.class, CustomAnalyzerEntity.class );
	}

	@Test
	public void namingConflict_withinProvider_charFilter() {
		thrown.expect( SearchException.class );
		thrown.expectMessage( "HSEARCH400056" );

		init( ProviderWithInternalCharFilterNamingConflict.class, CustomAnalyzerEntity.class );
	}

	@Test
	public void namingConflict_withinProvider_tokenFilter() {
		thrown.expect( SearchException.class );
		thrown.expectMessage( "HSEARCH400057" );

		init( ProviderWithInternalTokenFilterNamingConflict.class, CustomAnalyzerEntity.class );
	}

	private ExtendedSearchIntegrator init(Class<?> providerClass, Class<?> ... entityClasses) {
		SearchConfigurationForTest cfg = new SearchConfigurationForTest();
		for ( Class<?> entityClass : entityClasses ) {
			cfg.addClass( entityClass );
		}
		cfg.addProperty( ElasticsearchEnvironment.ANALYZER_DEFINITION_PROVIDER, providerClass.getName() );
		return integratorResource.create( cfg );
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

	public static class CustomAnalyzerProvider implements ElasticsearchAnalysisDefinitionProvider {
		@Override
		public void register(ElasticsearchAnalysisDefinitionRegistryBuilder builder) {
			builder.analyzer( CUSTOM_ANALYZER_NAME )
					.withTokenizer( "myPattern" )
					.withCharFilters( "myPattern" )
					.withTokenFilters( "myLowerCase" );

			builder.tokenizer( "myPattern" )
					.type( "pattern" )
					.param( "pattern", "\\|" );
			builder.charFilter( "myPattern" )
					.type( "pattern_replace" )
					.param( "pattern", "charFilterShouldReplace" )
					.param( "replacement", "charFilterDidReplace" );
			builder.tokenFilter( "myLowerCase" )
					.type( "lowercase" );
		}
	}

	public static class ProviderFactory {
		@Factory
		public static CustomAnalyzerProvider create() {
			return new CustomAnalyzerProvider();
		}
	}

	public static class CustomAnalyzer2Provider implements ElasticsearchAnalysisDefinitionProvider {
		@Override
		public void register(ElasticsearchAnalysisDefinitionRegistryBuilder builder) {
			builder.analyzer( CUSTOM_ANALYZER_2_NAME )
					.withTokenizer( "myPattern2" );

			builder.tokenizer( "myPattern2" )
					.type( "pattern" )
					.param( "pattern", " " );
		}
	}

	public static class MutatingProviderFactory {
		private static ElasticsearchAnalysisDefinitionProvider provider;
		@Factory
		public static ElasticsearchAnalysisDefinitionProvider create() {
			return provider;
		}
	}

	public static class ProviderWithInternalAnalyzerNamingConflict implements ElasticsearchAnalysisDefinitionProvider {
		@Override
		public void register(ElasticsearchAnalysisDefinitionRegistryBuilder builder) {
			builder.analyzer( CUSTOM_ANALYZER_NAME )
					.withTokenizer( "standard" );
			builder.analyzer( CUSTOM_ANALYZER_NAME )
					.withTokenizer( "standard" );
		}
	}

	public static class ProviderWithInternalTokenizerNamingConflict implements ElasticsearchAnalysisDefinitionProvider {
		@Override
		public void register(ElasticsearchAnalysisDefinitionRegistryBuilder builder) {
			builder.tokenizer( "foo" )
					.type( "standard" );
			builder.tokenizer( "foo" )
					.type( "standard" );
		}
	}

	public static class ProviderWithInternalCharFilterNamingConflict implements ElasticsearchAnalysisDefinitionProvider {
		@Override
		public void register(ElasticsearchAnalysisDefinitionRegistryBuilder builder) {
			builder.charFilter( "foo" )
					.type( "standard" );
			builder.charFilter( "foo" )
					.type( "standard" );
		}
	}

	public static class ProviderWithInternalTokenFilterNamingConflict implements ElasticsearchAnalysisDefinitionProvider {
		@Override
		public void register(ElasticsearchAnalysisDefinitionRegistryBuilder builder) {
			builder.tokenFilter( "foo" )
					.type( "standard" );
			builder.tokenFilter( "foo" )
					.type( "standard" );
		}
	}

	public static class LuceneAnalyzerDefinitionProviderImpl implements LuceneAnalyzerDefinitionProvider {
		@Override
		public void register(LuceneAnalyzerDefinitionRegistryBuilder builder) {
			// Never called
		}
	}

}
