/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.test;

import static org.hibernate.search.test.util.JsonHelper.assertJsonEquals;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

import javax.persistence.Entity;
import javax.persistence.Id;

import org.apache.lucene.analysis.charfilter.MappingCharFilterFactory;
import org.apache.lucene.analysis.core.LowerCaseFilterFactory;
import org.apache.lucene.analysis.util.ElisionFilterFactory;
import org.hibernate.search.annotations.CharFilterDef;
import org.hibernate.search.annotations.DocumentId;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.Normalizer;
import org.hibernate.search.annotations.NormalizerDef;
import org.hibernate.search.annotations.NormalizerDefs;
import org.hibernate.search.annotations.Parameter;
import org.hibernate.search.annotations.TokenFilterDef;
import org.hibernate.search.elasticsearch.analyzer.ElasticsearchCharFilterFactory;
import org.hibernate.search.elasticsearch.analyzer.ElasticsearchTokenFilterFactory;
import org.hibernate.search.elasticsearch.cfg.ElasticsearchEnvironment;
import org.hibernate.search.elasticsearch.cfg.IndexSchemaManagementStrategy;
import org.hibernate.search.elasticsearch.impl.ElasticsearchIndexManager;
import org.hibernate.search.elasticsearch.testutil.TestElasticsearchClient;
import org.hibernate.search.elasticsearch.testutil.junit.SkipBelowElasticsearch52;
import org.hibernate.search.test.SearchInitializationTestBase;
import org.hibernate.search.test.util.ImmutableTestConfiguration;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

/**
 * Tests for {@link ElasticsearchIndexManager}'s normalizer definition creation feature.
 *
 * @author Yoann Rodiere
 */
@RunWith(Parameterized.class)
@Category(SkipBelowElasticsearch52.class)
public class Elasticsearch52NormalizerDefinitionCreationIT extends SearchInitializationTestBase {

	@Parameters(name = "With strategy {0}")
	public static EnumSet<IndexSchemaManagementStrategy> strategies() {
		return EnumSet.complementOf( EnumSet.of(
				// Those strategies don't create the schema, so we don't test those
				IndexSchemaManagementStrategy.NONE, IndexSchemaManagementStrategy.VALIDATE
				) );
	}

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Rule
	public TestElasticsearchClient elasticSearchClient = new TestElasticsearchClient();

	private final IndexSchemaManagementStrategy strategy;

	public Elasticsearch52NormalizerDefinitionCreationIT(IndexSchemaManagementStrategy strategy) {
		super();
		this.strategy = strategy;
	}

	@Override
	protected void init(Class<?>... annotatedClasses) {
		Map<String, Object> settings = new HashMap<>();
		settings.put(
				"hibernate.search.default." + ElasticsearchEnvironment.INDEX_SCHEMA_MANAGEMENT_STRATEGY,
				strategy.getExternalName()
		);
		init( new ImmutableTestConfiguration( settings, annotatedClasses ) );
	}

	@Test
	public void success_simple() throws Exception {
		elasticSearchClient.index( SimpleNormalizedEntity.class )
				.ensureDoesNotExist().registerForCleanup();

		init( SimpleNormalizedEntity.class );

		assertJsonEquals(
				"{"
					+ "'normalizer': {"
							+ "'normalizerWithSimpleComponents': {"
									+ "'filter': ['lowercase']"
							+ "},"
							+ "'normalizerWithComplexComponents': {"
									+ "'char_filter': ['normalizerWithComplexComponents_MappingCharFilterFactory'],"
									+ "'filter': ['normalizerWithComplexComponents_ElisionFilterFactory']"
							+ "},"
							+ "'normalizerWithNamedComponents': {"
									+ "'char_filter': ['custom-char-mapping'],"
									+ "'filter': ['custom-elision']"
							+ "},"
							+ "'normalizerWithElasticsearchFactories': {"
									+ "'char_filter': ['custom-char-mapping-esFactory'],"
									+ "'filter': ['custom-elision-esFactory']"
							+ "}"
					+ "},"
					+ "'char_filter': {"
							+ "'normalizerWithComplexComponents_MappingCharFilterFactory': {"
									+ "'type': 'mapping',"
									+ "'mappings': ['foo => bar']"
							+ "},"
							+ "'custom-char-mapping': {"
									+ "'type': 'mapping',"
									+ "'mappings': ['foo => bar']"
							+ "},"
							+ "'custom-char-mapping-esFactory': {"
									+ "'type': 'mapping',"
									+ "'mappings': ['foo => bar']"
							+ "}"
					+ "},"
					+ "'filter': {"
							+ "'normalizerWithComplexComponents_ElisionFilterFactory': {"
									+ "'type': 'elision',"
									+ "'articles': ['l', 'd'],"
							+ "},"
							+ "'custom-elision': {"
									+ "'type': 'elision',"
									+ "'articles': ['l', 'd'],"
							+ "},"
							+ "'custom-elision-esFactory': {"
									+ "'type': 'elision',"
									+ "'articles': ['l', 'd'],"
							+ "}"
					+ "}"
				+ "}",
				elasticSearchClient.index( SimpleNormalizedEntity.class ).settings( "index.analysis" ).get()
				);
	}

	@Indexed
	@Entity
	@NormalizerDefs({
			@NormalizerDef(
					name = "normalizerWithSimpleComponents",
					filters = @TokenFilterDef(factory = LowerCaseFilterFactory.class)
			),
			@NormalizerDef(
					name = "normalizerWithComplexComponents",
					charFilters = @CharFilterDef(
							factory = MappingCharFilterFactory.class,
							params = {
									@Parameter(name = "mapping", value = "org/hibernate/search/elasticsearch/test/mappings.properties")
							}
					),
					filters = @TokenFilterDef(
							factory = ElisionFilterFactory.class,
							params = {
									@Parameter(name = "articles", value = "org/hibernate/search/elasticsearch/test/elision.properties")
							}
					)
			),
			@NormalizerDef(
					name = "normalizerWithNamedComponents",
					charFilters = @CharFilterDef(
							name = "custom-char-mapping",
							factory = MappingCharFilterFactory.class,
							params = {
									@Parameter(name = "mapping", value = "org/hibernate/search/elasticsearch/test/mappings.properties")
							}
					),
					filters = @TokenFilterDef(
							name = "custom-elision",
							factory = ElisionFilterFactory.class,
							params = {
									@Parameter(name = "articles", value = "org/hibernate/search/elasticsearch/test/elision.properties")
							}
					)
			),
			@NormalizerDef(
					name = "normalizerWithElasticsearchFactories",
					charFilters = @CharFilterDef(
							name = "custom-char-mapping-esFactory",
							factory = ElasticsearchCharFilterFactory.class,
							params = {
									@Parameter(name = "type", value = "'mapping'"),
									@Parameter(name = "mappings", value = "['foo => bar']"),
							}
					),
					filters = @TokenFilterDef(
							name = "custom-elision-esFactory",
							factory = ElasticsearchTokenFilterFactory.class,
							params = {
									@Parameter(name = "type", value = "'elision'"),
									@Parameter(name = "articles", value = "['l', 'd']")
							}
					)
			)
	})
	private static class SimpleNormalizedEntity {
		@DocumentId
		@Id
		Long id;

		@Field(name = "myField1", normalizer = @Normalizer(definition = "normalizerWithSimpleComponents"))
		@Field(name = "myField2", normalizer = @Normalizer(definition = "normalizerWithComplexComponents"))
		@Field(name = "myField3", normalizer = @Normalizer(definition = "normalizerWithNamedComponents"))
		@Field(name = "myField4", normalizer = @Normalizer(definition = "normalizerWithElasticsearchFactories"))
		String myField;
	}

}
