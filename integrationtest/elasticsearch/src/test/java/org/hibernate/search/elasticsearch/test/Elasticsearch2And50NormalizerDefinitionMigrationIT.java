/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.test;

import static org.hibernate.search.test.util.JsonHelper.assertJsonEquals;

import java.util.HashMap;
import java.util.Map;

import javax.persistence.Entity;
import javax.persistence.Id;

import org.hibernate.search.annotations.CharFilterDef;
import org.hibernate.search.annotations.DocumentId;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.Normalizer;
import org.hibernate.search.annotations.NormalizerDef;
import org.hibernate.search.annotations.Parameter;
import org.hibernate.search.annotations.TokenFilterDef;
import org.hibernate.search.elasticsearch.analyzer.ElasticsearchCharFilterFactory;
import org.hibernate.search.elasticsearch.analyzer.ElasticsearchTokenFilterFactory;
import org.hibernate.search.elasticsearch.cfg.ElasticsearchEnvironment;
import org.hibernate.search.elasticsearch.cfg.IndexSchemaManagementStrategy;
import org.hibernate.search.elasticsearch.impl.ElasticsearchIndexManager;
import org.hibernate.search.elasticsearch.testutil.TestElasticsearchClient;
import org.hibernate.search.elasticsearch.testutil.junit.SkipFromElasticsearch52;
import org.hibernate.search.elasticsearch.testutil.junit.SkipOnAWS;
import org.hibernate.search.test.SearchInitializationTestBase;
import org.hibernate.search.test.util.ImmutableTestConfiguration;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.ExpectedException;

/**
 * Tests for {@link ElasticsearchIndexManager}'s normalizer definition migration feature.
 *
 * @author Yoann Rodiere
 */
@Category({
		SkipFromElasticsearch52.class,
		SkipOnAWS.class // Cannot alter Elasticsearch settings on AWS, because indexes cannot be closed.
})
public class Elasticsearch2And50NormalizerDefinitionMigrationIT extends SearchInitializationTestBase {

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Rule
	public TestElasticsearchClient elasticSearchClient = new TestElasticsearchClient();

	@Override
	protected void init(Class<?>... annotatedClasses) {
		Map<String, Object> settings = new HashMap<>();
		settings.put(
				"hibernate.search.default." + ElasticsearchEnvironment.INDEX_SCHEMA_MANAGEMENT_STRATEGY,
				IndexSchemaManagementStrategy.UPDATE.getExternalName()
		);
		init( new ImmutableTestConfiguration( settings, annotatedClasses ) );
	}

	@Test
	public void nothingToDo() throws Exception {
		elasticSearchClient.index( AnalyzedEntity.class ).deleteAndCreate(
				"index.analysis",
				"{"
					+ "'analyzer': {"
							+ "'normalizerWithElasticsearchFactories': {"
									+ "'char_filter': ['custom-char-mapping-esFactory'],"
									+ "'tokenizer': 'keyword',"
									+ "'filter': ['custom-elision-esFactory']"
							+ "}"
					+ "},"
					+ "'char_filter': {"
							+ "'custom-char-mapping-esFactory': {"
									+ "'type': 'mapping',"
									+ "'mappings': ['foo => bar']"
							+ "}"
					+ "},"
					+ "'filter': {"
							+ "'custom-elision-esFactory': {"
									+ "'type': 'elision',"
									+ "'articles': ['l', 'd']"
							+ "}"
					+ "}"
				+ "}"
				);

		init( AnalyzedEntity.class );

		assertJsonEquals(
				"{"
					+ "'analyzer': {"
							+ "'normalizerWithElasticsearchFactories': {"
									+ "'char_filter': ['custom-char-mapping-esFactory'],"
									+ "'tokenizer': 'keyword',"
									+ "'filter': ['custom-elision-esFactory']"
							+ "}"
					+ "},"
					+ "'char_filter': {"
							+ "'custom-char-mapping-esFactory': {"
									+ "'type': 'mapping',"
									+ "'mappings': ['foo => bar']"
							+ "}"
					+ "},"
					+ "'filter': {"
							+ "'custom-elision-esFactory': {"
									+ "'type': 'elision',"
									+ "'articles': ['l', 'd']"
							+ "}"
					+ "}"
				+ "}",
				elasticSearchClient.index( AnalyzedEntity.class ).settings( "index.analysis" ).get()
				);
	}

	@Test
	public void normalizer_missing() throws Exception {
		elasticSearchClient.index( AnalyzedEntity.class ).deleteAndCreate(
				"index.analysis",
				"{"
					+ "'char_filter': {"
							+ "'custom-char-mapping-esFactory': {"
									+ "'type': 'mapping',"
									+ "'mappings': ['foo => bar']"
							+ "}"
					+ "},"
					+ "'filter': {"
							+ "'custom-elision-esFactory': {"
									+ "'type': 'elision',"
									+ "'articles': ['l', 'd']"
							+ "}"
					+ "}"
				+ "}"
				);

		init( AnalyzedEntity.class );

		assertJsonEquals(
				"{"
					+ "'analyzer': {"
							+ "'normalizerWithElasticsearchFactories': {"
									+ "'char_filter': ['custom-char-mapping-esFactory'],"
									+ "'tokenizer': 'keyword',"
									+ "'filter': ['custom-elision-esFactory']"
							+ "}"
					+ "},"
					+ "'char_filter': {"
							+ "'custom-char-mapping-esFactory': {"
									+ "'type': 'mapping',"
									+ "'mappings': ['foo => bar']"
							+ "}"
					+ "},"
					+ "'filter': {"
							+ "'custom-elision-esFactory': {"
									+ "'type': 'elision',"
									+ "'articles': ['l', 'd']"
							+ "}"
					+ "}"
				+ "}",
				elasticSearchClient.index( AnalyzedEntity.class ).settings( "index.analysis" ).get()
				);
	}

	@Test
	public void normalizer_componentDefinition_missing() throws Exception {
		elasticSearchClient.index( AnalyzedEntity.class ).deleteAndCreate(
				"index.analysis",
				"{"
					/*
					 * We don't add the analyzer here: since a component is missing
					 * the analyzer can't reference it and thus it must be missing too.
					 */
					// missing: 'char_filter'
					+ "'filter': {"
							+ "'custom-elision-esFactory': {"
									+ "'type': 'elision',"
									+ "'articles': ['l', 'd']"
							+ "}"
					+ "}"
				+ "}"
				);

		init( AnalyzedEntity.class );

		assertJsonEquals(
				"{"
					+ "'analyzer': {"
							+ "'normalizerWithElasticsearchFactories': {"
									+ "'char_filter': ['custom-char-mapping-esFactory'],"
									+ "'tokenizer': 'keyword',"
									+ "'filter': ['custom-elision-esFactory']"
							+ "}"
					+ "},"
					+ "'char_filter': {"
							+ "'custom-char-mapping-esFactory': {"
									+ "'type': 'mapping',"
									+ "'mappings': ['foo => bar']"
							+ "}"
					+ "},"
					+ "'filter': {"
							+ "'custom-elision-esFactory': {"
									+ "'type': 'elision',"
									+ "'articles': ['l', 'd']"
							+ "}"
					+ "}"
				+ "}",
				elasticSearchClient.index( AnalyzedEntity.class ).settings( "index.analysis" ).get()
				);
	}

	@Test
	public void normalizer_componentReference_invalid() throws Exception {
		elasticSearchClient.index( AnalyzedEntity.class ).deleteAndCreate(
				"index.analysis",
				"{"
					+ "'analyzer': {"
							+ "'normalizerWithElasticsearchFactories': {"
									+ "'char_filter': ['custom-char-mapping-esFactory2']," // Invalid
									+ "'tokenizer': 'keyword',"
									+ "'filter': ['custom-elision-esFactory']"
							+ "}"
					+ "},"
					+ "'char_filter': {"
							+ "'custom-char-mapping-esFactory': {"
									+ "'type': 'mapping',"
									+ "'mappings': ['foo => bar']"
							+ "},"
							+ "'custom-char-mapping-esFactory2': {"
									+ "'type': 'mapping',"
									+ "'mappings': ['foo => bar2']"
							+ "}"
					+ "},"
					+ "'filter': {"
							+ "'custom-elision-esFactory': {"
									+ "'type': 'elision',"
									+ "'articles': ['l', 'd']"
							+ "}"
					+ "}"
				+ "}"
				);

		init( AnalyzedEntity.class );

		assertJsonEquals(
				"{"
					+ "'analyzer': {"
							+ "'normalizerWithElasticsearchFactories': {"
									+ "'char_filter': ['custom-char-mapping-esFactory'],"
									+ "'tokenizer': 'keyword',"
									+ "'filter': ['custom-elision-esFactory']"
							+ "}"
					+ "},"
					+ "'char_filter': {"
							+ "'custom-char-mapping-esFactory': {"
									+ "'type': 'mapping',"
									+ "'mappings': ['foo => bar']"
							+ "},"
					+ "'custom-char-mapping-esFactory2': {"
							+ "'type': 'mapping',"
							+ "'mappings': ['foo => bar2']"
					+ "}"
					+ "},"
					+ "'filter': {"
							+ "'custom-elision-esFactory': {"
									+ "'type': 'elision',"
									+ "'articles': ['l', 'd']"
							+ "}"
					+ "}"
				+ "}",
				elasticSearchClient.index( AnalyzedEntity.class ).settings( "index.analysis" ).get()
				);
	}

	@Test
	public void normalizer_componentDefinition_invalid() throws Exception {
		elasticSearchClient.index( AnalyzedEntity.class ).deleteAndCreate(
				"index.analysis",
				"{"
					+ "'analyzer': {"
							+ "'normalizerWithElasticsearchFactories': {"
									+ "'char_filter': ['custom-char-mapping-esFactory']," // Correct, but the actual definition is not
									+ "'tokenizer': 'keyword',"
									+ "'filter': ['custom-elision-esFactory']"
							+ "}"
					+ "},"
					+ "'char_filter': {"
							+ "'custom-char-mapping-esFactory': {"
									+ "'type': 'mapping',"
									+ "'mappings': ['foo => bar2']" // Invalid
							+ "}"
					+ "},"
					+ "'filter': {"
							+ "'custom-elision-esFactory': {"
									+ "'type': 'elision',"
									+ "'articles': ['l', 'd']"
							+ "}"
					+ "}"
				+ "}"
				);

		init( AnalyzedEntity.class );

		assertJsonEquals(
				"{"
					+ "'analyzer': {"
							+ "'normalizerWithElasticsearchFactories': {"
									+ "'char_filter': ['custom-char-mapping-esFactory'],"
									+ "'tokenizer': 'keyword',"
									+ "'filter': ['custom-elision-esFactory']"
							+ "}"
					+ "},"
					+ "'char_filter': {"
							+ "'custom-char-mapping-esFactory': {"
									+ "'type': 'mapping',"
									+ "'mappings': ['foo => bar']"
							+ "}"
					+ "},"
					+ "'filter': {"
							+ "'custom-elision-esFactory': {"
									+ "'type': 'elision',"
									+ "'articles': ['l', 'd']"
							+ "}"
					+ "}"
				+ "}",
				elasticSearchClient.index( AnalyzedEntity.class ).settings( "index.analysis" ).get()
				);
	}

	@Indexed
	@Entity
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
	public static class AnalyzedEntity {
		@DocumentId
		@Id
		Long id;

		@Field(normalizer = @Normalizer(definition = "normalizerWithElasticsearchFactories"))
		String myField;
	}

}
