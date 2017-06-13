/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.test;

import static org.hibernate.search.test.util.impl.ExceptionMatcherBuilder.isException;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import org.apache.lucene.document.Document;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.FieldBridge;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.NumericField;
import org.hibernate.search.bridge.LuceneOptions;
import org.hibernate.search.bridge.MetadataProvidingFieldBridge;
import org.hibernate.search.bridge.spi.FieldMetadataBuilder;
import org.hibernate.search.bridge.spi.FieldType;
import org.hibernate.search.elasticsearch.cfg.ElasticsearchEnvironment;
import org.hibernate.search.elasticsearch.cfg.IndexSchemaManagementStrategy;
import org.hibernate.search.elasticsearch.testutil.TestElasticsearchClient;
import org.hibernate.search.exception.SearchException;
import org.hibernate.search.test.SearchInitializationTestBase;
import org.hibernate.search.test.util.ImmutableTestConfiguration;
import org.hibernate.testing.TestForIssue;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

/**
 * Tests for detection of errors in the metadata when generating the Elasticsearch schema.
 *
 * @author Yoann Rodiere
 */
@RunWith(Parameterized.class)
public class ElasticsearchSchemaMetadataErrorsIT extends SearchInitializationTestBase {

	@Parameters(name = "With strategy {0}")
	public static EnumSet<IndexSchemaManagementStrategy> strategies() {
		return EnumSet.complementOf( EnumSet.of( IndexSchemaManagementStrategy.NONE ) );
	}

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Rule
	public TestElasticsearchClient elasticSearchClient = new TestElasticsearchClient();

	private IndexSchemaManagementStrategy strategy;

	public ElasticsearchSchemaMetadataErrorsIT(IndexSchemaManagementStrategy strategy) {
		super();
		this.strategy = strategy;
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-2458")
	public void detectIncompleteNumericType() throws Exception {
		thrown.expect(
				isException( SearchException.class )
						.withMessage( "HSEARCH400018" )
						.withMessage( "Unexpected numeric encoding type for field" )
						.withMessage( "'" + EntityWithIncompleteNumericTypeField.class.getName() + "'" )
						.withMessage( "'numericField'" )
				.build()
		);

		elasticSearchClient.index( EntityWithIncompleteNumericTypeField.class ).registerForCleanup();
		init( strategy, EntityWithIncompleteNumericTypeField.class );
	}

	private void init(IndexSchemaManagementStrategy strategy, Class<?> ... entityClasses) {
		Map<String, Object> settings = new HashMap<>();
		settings.put(
				"hibernate.search.default." + ElasticsearchEnvironment.INDEX_SCHEMA_MANAGEMENT_STRATEGY,
				strategy.getExternalName()
		);

		init( new ImmutableTestConfiguration( settings, entityClasses ) );
	}

	@Entity
	@Indexed
	private static class EntityWithIncompleteNumericTypeField {
		@Id
		@GeneratedValue
		Long id;

		@Field(bridge = @FieldBridge(impl = FieldBridgeNotUsingDefaultField.class))
		@NumericField // We provide incomplete metadata about the default field: it is numeric, but the exact numeric type is not provided.
		String numericField;
	}

	public static class FieldBridgeNotUsingDefaultField implements MetadataProvidingFieldBridge {

		@Override
		public void configureFieldMetadata(String name, FieldMetadataBuilder builder) {
			// We don't provide metadata for the default field (whose name is in the variable "name")
			builder.field( name + "_string", FieldType.STRING );
		}

		@Override
		public void set(String name, Object value, Document document, LuceneOptions luceneOptions) {
			luceneOptions.addFieldToDocument( name, (String) value, document );
		}

	}

}
