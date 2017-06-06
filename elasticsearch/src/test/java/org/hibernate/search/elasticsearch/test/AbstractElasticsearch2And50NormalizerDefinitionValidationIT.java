/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.test;

import static org.hibernate.search.test.util.impl.ExceptionMatcherBuilder.isException;

import java.io.IOException;
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
import org.hibernate.search.elasticsearch.schema.impl.ElasticsearchSchemaValidationException;
import org.hibernate.search.elasticsearch.testutil.TestElasticsearchClient;
import org.hibernate.search.test.SearchInitializationTestBase;
import org.hibernate.search.test.util.ImmutableTestConfiguration;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 * Tests for {@link ElasticsearchIndexManager}'s normalizer definition validation feature.
 *
 * Note: normalizers are implemented as analyzers,
 * so we don't test everything like in {@link AbstractAnalyzerDefinitionValidationIT}.
 *
 * @author Yoann Rodiere
 */
public abstract class AbstractElasticsearch2And50NormalizerDefinitionValidationIT extends SearchInitializationTestBase {

	private static final String VALIDATION_FAILED_MESSAGE_ID = "HSEARCH400033";

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Rule
	public TestElasticsearchClient elasticSearchClient = new TestElasticsearchClient();

	@Override
	protected void init(Class<?>... annotatedClasses) {
		Map<String, Object> settings = new HashMap<>();
		settings.put(
				"hibernate.search.default." + ElasticsearchEnvironment.INDEX_SCHEMA_MANAGEMENT_STRATEGY,
				IndexSchemaManagementStrategy.VALIDATE.getExternalName()
		);
		init( new ImmutableTestConfiguration( settings, annotatedClasses ) );
	}

	@Test
	public void success_simple() throws Exception {
		elasticSearchClient.index( NormalizedEntity.class ).deleteAndCreate(
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

		putMapping();

		init( NormalizedEntity.class );

		// If we get here, it means validation passed (no exception was thrown)
	}

	protected abstract void putMapping() throws IOException;

	@Test
	public void normalizer_missing() throws Exception {
		elasticSearchClient.index( NormalizedEntity.class ).deleteAndCreate(
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

		thrown.expect(
				isException( ElasticsearchSchemaValidationException.class )
						.withMessage( VALIDATION_FAILED_MESSAGE_ID )
						.withMessage( "analyzer 'normalizerWithElasticsearchFactories':\n\tMissing analyzer" )
				.build()
		);

		init( NormalizedEntity.class );
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
	public static class NormalizedEntity {
		@DocumentId
		@Id
		Long id;

		@Field(normalizer = @Normalizer(definition = "normalizerWithElasticsearchFactories"))
		String myField;
	}
}
