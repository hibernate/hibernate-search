/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.elasticsearch.management;

import java.util.List;
import java.util.stream.Collectors;

import org.hibernate.search.backend.elasticsearch.analysis.ElasticsearchAnalysisConfigurer;
import org.hibernate.search.backend.elasticsearch.analysis.model.dsl.ElasticsearchAnalysisDefinitionContainerContext;
import org.hibernate.search.backend.elasticsearch.cfg.ElasticsearchBackendSettings;
import org.hibernate.search.backend.elasticsearch.cfg.ElasticsearchIndexLifecycleStrategyName;
import org.hibernate.search.backend.elasticsearch.cfg.ElasticsearchIndexSettings;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.types.Projectable;
import org.hibernate.search.integrationtest.backend.elasticsearch.testsupport.util.TestElasticsearchClient;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.common.FailureReportUtils;
import org.hibernate.search.util.impl.test.SubTest;

import org.junit.Assume;
import org.junit.Rule;
import org.junit.Test;

public class ElasticsearchSchemaAttributeValidationIT {

	private static final String BACKEND_NAME = "myElasticsearchBackend";
	private static final String INDEX_NAME = "IndexName";
	private static final String SCHEMA_VALIDATION_CONTEXT = "schema validation";

	@Rule
	public SearchSetupHelper setupHelper = new SearchSetupHelper();

	@Rule
	public TestElasticsearchClient elasticSearchClient = new TestElasticsearchClient();

	@Test
	public void attribute_dynamic_missing() {
		elasticSearchClient.index( INDEX_NAME ).deleteAndCreate();
		elasticSearchClient.index( INDEX_NAME ).type().putMapping(
				"{"
					+ "'properties': {"
							+ "'myField': {"
									+ "'type': 'integer',"
									+ "'index': true"
							+ "}"
					+ "}"
				+ "}"
		);

		SubTest.expectException( () ->
				validateSchemaConfig()
						.withIndex( INDEX_NAME, ctx -> {
									IndexSchemaElement root = ctx.getSchemaElement();
									root.field( "myField", f -> f.asInteger() ).toReference();
								}
						)
						.setup() )
				.assertThrown()
				.isInstanceOf( SearchException.class )
				.hasMessageMatching( FailureReportUtils.buildFailureReportPattern()
						.indexContext( INDEX_NAME )
						.contextLiteral( SCHEMA_VALIDATION_CONTEXT )
						.failure( "Invalid value for attribute 'dynamic'. Expected 'STRICT', actual is 'null'" )
						.build() );
	}

	@Test
	public void attribute_dynamic_invalid() {
		elasticSearchClient.index( INDEX_NAME ).deleteAndCreate();
		elasticSearchClient.index( INDEX_NAME ).type().putMapping(
				"{"
					+ "'dynamic': false,"
					+ "'properties': {"
							+ "'myField': {"
									+ "'type': 'integer',"
									+ "'index': true"
							+ "}"
					+ "}"
				+ "}"
		);

		SubTest.expectException( () ->
				validateSchemaConfig()
						.withIndex( INDEX_NAME, ctx -> {
									IndexSchemaElement root = ctx.getSchemaElement();
									root.field( "myField", f -> f.asInteger() ).toReference();
								}
						)
						.setup() )
				.assertThrown()
				.isInstanceOf( SearchException.class )
				.hasMessageMatching( FailureReportUtils.buildFailureReportPattern()
						.indexContext( INDEX_NAME )
						.contextLiteral( SCHEMA_VALIDATION_CONTEXT )
						.failure( "Invalid value for attribute 'dynamic'. Expected 'STRICT', actual is 'FALSE'" )
						.build() );

	}

	@Test
	public void attribute_properties_missing() {
		elasticSearchClient.index( INDEX_NAME ).deleteAndCreate();
		elasticSearchClient.index( INDEX_NAME ).type().putMapping(
				"{"
					+ "'dynamic': 'strict'"
				+ "}"
		);


		SubTest.expectException( () ->
				validateSchemaConfig()
						.withIndex( INDEX_NAME, ctx -> {
									IndexSchemaElement root = ctx.getSchemaElement();
									root.field( "myField", f -> f.asInteger() ).toReference();
								}
						)
						.setup() )
				.assertThrown()
				.isInstanceOf( SearchException.class )
				.hasMessageMatching( FailureReportUtils.buildFailureReportPattern()
						.indexContext( INDEX_NAME )
						.contextLiteral( SCHEMA_VALIDATION_CONTEXT )
						.indexFieldContext( "myField" )
						.failure( "Missing property mapping" )
						.build() );
	}

	@Test
	public void attribute_properties_empty() {
		elasticSearchClient.index( INDEX_NAME ).deleteAndCreate();
		elasticSearchClient.index( INDEX_NAME ).type().putMapping(
				"{"
					+ "'dynamic': 'strict',"
					+ "'properties': {"
					+ "}"
				+ "}"
		);


		SubTest.expectException( () ->
				validateSchemaConfig()
						.withIndex( INDEX_NAME, ctx -> {
									IndexSchemaElement root = ctx.getSchemaElement();
									root.field( "myField", f -> f.asInteger() ).toReference();
								}
						)
						.setup() )
				.assertThrown()
				.isInstanceOf( SearchException.class )
				.hasMessageMatching( FailureReportUtils.buildFailureReportPattern()
						.indexContext( INDEX_NAME )
						.contextLiteral( SCHEMA_VALIDATION_CONTEXT )
						.indexFieldContext( "myField" )
						.failure( "Missing property mapping" )
						.build() );
	}

	@Test
	public void attribute_type_invalid() {
		elasticSearchClient.index( INDEX_NAME ).deleteAndCreate();
		elasticSearchClient.index( INDEX_NAME ).type().putMapping(
				"{"
					+ "'dynamic': 'strict',"
					+ "'properties': {"
							+ "'myField': {"
									+ "'type': 'keyword',"
									+ "'index': true"
							+ "}"
					+ "}"
				+ "}"
		);

		SubTest.expectException( () ->
				validateSchemaConfig()
						.withIndex( INDEX_NAME, ctx -> {
									IndexSchemaElement root = ctx.getSchemaElement();
									root.field( "myField", f -> f.asInteger() ).toReference();
								}
						)
						.setup() )
				.assertThrown()
				.isInstanceOf( Exception.class )
				.hasMessageMatching( FailureReportUtils.buildFailureReportPattern()
						.indexContext( INDEX_NAME )
						.contextLiteral( SCHEMA_VALIDATION_CONTEXT )
						.indexFieldContext( "myField" )
						.failure( "Invalid value for attribute 'type'. Expected 'INTEGER', actual is 'KEYWORD'" )
						.build() );
	}

	@Test
	public void attribute_index_missing() {
		elasticSearchClient.index( INDEX_NAME ).deleteAndCreate();
		elasticSearchClient.index( INDEX_NAME ).type().putMapping(
				"{"
					+ "'dynamic': 'strict',"
					+ "'properties': {"
							+ "'myField': {"
									+ "'type': 'integer'"
							+ "}"
					+ "}"
				+ "}"
		);

		// the expected value true is the default
		validateSchemaConfig()
				.withIndex( INDEX_NAME, ctx -> {
							IndexSchemaElement root = ctx.getSchemaElement();
							root.field( "myField", f -> f.asInteger() ).toReference();
						}
				)
				.setup();
	}

	@Test
	public void attribute_index_invalid() {
		elasticSearchClient.index( INDEX_NAME ).deleteAndCreate();
		elasticSearchClient.index( INDEX_NAME ).type().putMapping(
				"{"
					+ "'dynamic': 'strict',"
					+ "'properties': {"
							+ "'myField': {"
									+ "'type': 'integer',"
									+ "'index': false"
							+ "}"
					+ "}"
				+ "}"
		);

		SubTest.expectException( () ->
				validateSchemaConfig()
						.withIndex( INDEX_NAME, ctx -> {
									IndexSchemaElement root = ctx.getSchemaElement();
									root.field( "myField", f -> f.asInteger() ).toReference();
								}
						)
						.setup() )
				.assertThrown()
				.isInstanceOf( Exception.class )
				.hasMessageMatching( FailureReportUtils.buildFailureReportPattern()
						.indexContext( INDEX_NAME )
						.contextLiteral( SCHEMA_VALIDATION_CONTEXT )
						.indexFieldContext( "myField" )
						.failure( "Invalid value for attribute 'index'. Expected 'true', actual is 'false'" )
						.build() );
	}

	@Test
	public void attribute_format_missing() {
		List<String> allFormats = elasticSearchClient.getDialect().getAllLocalDateDefaultMappingFormats();

		elasticSearchClient.index( INDEX_NAME ).deleteAndCreate();
		elasticSearchClient.index( INDEX_NAME ).type().putMapping(
				"{"
					+ "'dynamic': 'strict',"
					+ "'properties': {"
							+ "'myField': {"
									+ "'type': 'date'"
							+ "}"
					+ "}"
				+ "}"
		);

		SubTest.expectException( () ->
				validateSchemaConfig()
						.withIndex( INDEX_NAME, ctx -> {
									IndexSchemaElement root = ctx.getSchemaElement();
									root.field( "myField", f -> f.asLocalDate() ).toReference();
								}
						)
						.setup() )
				.assertThrown()
				.isInstanceOf( Exception.class )
				.hasMessageMatching( FailureReportUtils.buildFailureReportPattern()
						.indexContext( INDEX_NAME )
						.contextLiteral( SCHEMA_VALIDATION_CONTEXT )
						.indexFieldContext( "myField" )
						.failure( "The output format (the first format in the 'format' attribute) is invalid." )
						.failure(
								"Invalid formats for attribute 'format'",
								"missing elements are '" + allFormats + "'"
						)
						.build() );
	}

	@Test
	public void attribute_format_valid() {
		String allFormats = elasticSearchClient.getDialect().getConcatenatedLocalDateDefaultMappingFormats();

		elasticSearchClient.index( INDEX_NAME ).deleteAndCreate();
		elasticSearchClient.index( INDEX_NAME ).type().putMapping(
				"{"
					+ "'dynamic': 'strict',"
					+ "'properties': {"
							+ "'myField': {"
									+ "'type': 'date',"
									+ "'format': '" + allFormats + "'"
							+ "}"
					+ "}"
				+ "}"
		);

		validateSchemaConfig()
				.withIndex( INDEX_NAME, ctx -> {
							IndexSchemaElement root = ctx.getSchemaElement();
							root.field( "myField", f -> f.asLocalDate() ).toReference();
						}
				)
				.setup();
	}

	@Test
	public void attribute_format_incomplete() {
		String firstFormat = elasticSearchClient.getDialect().getFirstLocalDateDefaultMappingFormat();
		List<String> nextFormats = elasticSearchClient.getDialect().getAllLocalDateDefaultMappingFormats()
				.stream().skip( 1 ).collect( Collectors.toList() );
		Assume.assumeFalse(
				"Skipping this test as we don't have a type with multiple default formats in " + elasticSearchClient.getDialect(),
				nextFormats.isEmpty()
		);

		elasticSearchClient.index( INDEX_NAME ).deleteAndCreate();
		elasticSearchClient.index( INDEX_NAME ).type().putMapping(
				"{"
					+ "'dynamic': 'strict',"
					+ "'properties': {"
							+ "'myField': {"
									+ "'type': 'date',"
									+ "'format': '" + firstFormat + "'"
							+ "}"
					+ "}"
				+ "}"
		);

		SubTest.expectException( () ->
				validateSchemaConfig()
						.withIndex( INDEX_NAME, ctx -> {
									IndexSchemaElement root = ctx.getSchemaElement();
									root.field( "myField", f -> f.asLocalDate() ).toReference();
								}
						)
						.setup() )
				.assertThrown()
				.isInstanceOf( Exception.class )
				.hasMessageMatching( FailureReportUtils.buildFailureReportPattern()
						.indexContext( INDEX_NAME )
						.contextLiteral( SCHEMA_VALIDATION_CONTEXT )
						.indexFieldContext( "myField" )
						.failure(
								"Invalid formats for attribute 'format'",
								"missing elements are '" + nextFormats + "'"
						)
						.build() );
	}

	@Test
	public void attribute_format_exceeding() {
		String allFormats = elasticSearchClient.getDialect().getConcatenatedLocalDateDefaultMappingFormats();

		elasticSearchClient.index( INDEX_NAME ).deleteAndCreate();
		elasticSearchClient.index( INDEX_NAME ).type().putMapping(
				"{"
					+ "'dynamic': 'strict',"
					+ "'properties': {"
							+ "'myField': {"
									+ "'type': 'date',"
									+ "'format': '" + allFormats + "||yyyy" + "'"
							+ "}"
					+ "}"
				+ "}"
		);

		SubTest.expectException( () ->
				validateSchemaConfig()
						.withIndex( INDEX_NAME, ctx -> {
									IndexSchemaElement root = ctx.getSchemaElement();
									root.field( "myField", f -> f.asLocalDate() ).toReference();
								}
						)
						.setup() )
				.assertThrown()
				.isInstanceOf( Exception.class )
				.hasMessageMatching( FailureReportUtils.buildFailureReportPattern()
						.indexContext( INDEX_NAME )
						.contextLiteral( SCHEMA_VALIDATION_CONTEXT )
						.indexFieldContext( "myField" )
						.failure(
								"Invalid formats for attribute 'format'",
								"unexpected elements are '[yyyy]'"
						)
						.build() );
	}

	@Test
	public void attribute_format_wrong() {
		elasticSearchClient.index( INDEX_NAME ).deleteAndCreate();
		elasticSearchClient.index( INDEX_NAME ).type().putMapping(
				"{"
					+ "'dynamic': 'strict',"
					+ "'properties': {"
							+ "'myField': {"
									+ "'type': 'date',"
									+ "'format': 'epoch_millis||strict_date_time'"
							+ "}"
					+ "}"
				+ "}"
		);

		SubTest.expectException( () ->
				validateSchemaConfig()
						.withIndex( INDEX_NAME, ctx -> {
									IndexSchemaElement root = ctx.getSchemaElement();
									root.field( "myField", f -> f.asLocalDate() ).toReference();
								}
						)
						.setup() )
				.assertThrown()
				.isInstanceOf( Exception.class )
				.hasMessageMatching( FailureReportUtils.buildFailureReportPattern()
						.indexContext( INDEX_NAME )
						.contextLiteral( SCHEMA_VALIDATION_CONTEXT )
						.indexFieldContext( "myField" )
						.failure(
								"The output format (the first format in the 'format' attribute) is invalid. Expected '"
										+ elasticSearchClient.getDialect().getFirstLocalDateDefaultMappingFormat()
										+ "', actual is 'epoch_millis'"
						)
						.build() );
	}

	@Test
	public void attribute_analyzer_missing() {
		elasticSearchClient.index( INDEX_NAME ).deleteAndCreate();
		elasticSearchClient.index( INDEX_NAME ).type().putMapping(
				"{"
					+ "'dynamic': 'strict',"
					+ "'properties': {"
							+ "'myField': {"
									+ "'type': 'text',"
									+ "'index': true"
							+ "}"
					+ "}"
				+ "}"
		);

		SubTest.expectException( () ->
				validateSchemaConfig()
						.withIndex( INDEX_NAME, ctx -> {
									IndexSchemaElement root = ctx.getSchemaElement();
									root.field( "myField", f -> f.asString().analyzer( "keyword" ) ).toReference();
								}
						)
						.setup() )
				.assertThrown()
				.isInstanceOf( Exception.class )
				.hasMessageMatching( FailureReportUtils.buildFailureReportPattern()
						.indexContext( INDEX_NAME )
						.contextLiteral( SCHEMA_VALIDATION_CONTEXT )
						.indexFieldContext( "myField" )
						.failure( "Invalid value for attribute 'analyzer'. Expected 'keyword', actual is 'null'" )
						.build() );
	}

	@Test
	public void attribute_analyzer_valid() {
		elasticSearchClient.index( INDEX_NAME ).deleteAndCreate();
		elasticSearchClient.index( INDEX_NAME ).type().putMapping(
				"{"
					+ "'dynamic': 'strict',"
					+ "'properties': {"
							+ "'myField': {"
									+ "'type': 'text',"
									+ "'index': true,"
									+ "'analyzer': 'keyword'"
							+ "}"
					+ "}"
				+ "}"
		);

		validateSchemaConfig()
				.withIndex( INDEX_NAME, ctx -> {
							IndexSchemaElement root = ctx.getSchemaElement();
							root.field( "myField", f -> f.asString().analyzer( "keyword" ) ).toReference();
						}
				)
				.setup();
	}

	@Test
	public void attribute_analyzer_invalid() {
		elasticSearchClient.index( INDEX_NAME ).deleteAndCreate();
		elasticSearchClient.index( INDEX_NAME ).type().putMapping(
				"{"
					+ "'dynamic': 'strict',"
					+ "'properties': {"
							+ "'myField': {"
									+ "'type': 'text',"
									+ "'index': true,"
									+ "'analyzer': 'keyword'"
							+ "}"
					+ "}"
				+ "}"
		);

		SubTest.expectException( () ->
				validateSchemaConfig()
						.withIndex( INDEX_NAME, ctx -> {
									IndexSchemaElement root = ctx.getSchemaElement();
									root.field( "myField", f -> f.asString().analyzer( "default" ) ).toReference();
								}
						)
						.setup() )
				.assertThrown()
				.isInstanceOf( Exception.class )
				.hasMessageMatching( FailureReportUtils.buildFailureReportPattern()
						.indexContext( INDEX_NAME )
						.contextLiteral( SCHEMA_VALIDATION_CONTEXT )
						.indexFieldContext( "myField" )
						.failure( "Invalid value for attribute 'analyzer'. Expected 'default', actual is 'keyword'" )
						.build() );
	}

	// TODO HSEARCH-3048: test attribute norm (same pattern: missing, valid, invalid)
	@Test
	public void property_norms_invalid() throws Exception {
		Assume.assumeTrue( "Norms configuration is not supported yet; see HSEARCH-3048", false );

		elasticSearchClient.index( INDEX_NAME ).deleteAndCreate();
		elasticSearchClient.index( INDEX_NAME ).type().putMapping(
				"{"
					+ "'dynamic': 'strict',"
					+ "'properties': {"
							+ "'myField': {"
									+ "'type': 'text',"
									+ "'norms': false"
							+ "}"
					+ "}"
				+ "}"
		);

		SubTest.expectException( () ->
				validateSchemaConfig()
						.withIndex( INDEX_NAME, ctx -> {
									IndexSchemaElement root = ctx.getSchemaElement();
									// TODO disable norms once the APIs allow it; see HSEARCH-3048
									root.field( "myField", f -> f.asString().analyzer( "default" ) ).toReference();
								}
						)
						.setup() )
				.assertThrown()
				.isInstanceOf( Exception.class )
				.hasMessageMatching( FailureReportUtils.buildFailureReportPattern()
						.indexContext( INDEX_NAME )
						.contextLiteral( SCHEMA_VALIDATION_CONTEXT )
						.indexFieldContext( "myField" )
						.failure( "Invalid value for attribute 'norms'. Expected 'true', actual is 'false'" )
						.build()
		);
	}
	@Test
	public void attribute_store_true() {
		elasticSearchClient.index( INDEX_NAME ).deleteAndCreate();
		elasticSearchClient.index( INDEX_NAME ).type().putMapping(
				"{"
					+ "'dynamic': 'strict',"
					+ "'properties': {"
							+ "'myField': {"
									+ "'type': 'keyword',"
									+ "'store': true"
							+ "}"
					+ "}"
				+ "}"
		);

		validateSchemaConfig()
				.withIndex( INDEX_NAME, ctx -> {
							IndexSchemaElement root = ctx.getSchemaElement();
							root.field( "myField", f -> f.asString().projectable( Projectable.YES ) ).toReference();
						}
				)
				.setup();
	}

	@Test
	public void attribute_store_default() {
		elasticSearchClient.index( INDEX_NAME ).deleteAndCreate();
		elasticSearchClient.index( INDEX_NAME ).type().putMapping(
				"{"
					+ "'dynamic': 'strict',"
					+ "'properties': {"
							+ "'myField': {"
									+ "'type': 'keyword'"
							+ "}"
					+ "}"
				+ "}"
		);

		validateSchemaConfig()
				.withIndex( INDEX_NAME, ctx -> {
							IndexSchemaElement root = ctx.getSchemaElement();
							root.field( "myField", f -> f.asString().projectable( Projectable.DEFAULT ) ).toReference();
						}
				)
				.setup();
	}

	@Test
	public void attribute_store_invalid() {
		elasticSearchClient.index( INDEX_NAME ).deleteAndCreate();
		elasticSearchClient.index( INDEX_NAME ).type().putMapping(
				"{"
					+ "'dynamic': 'strict',"
					+ "'properties': {"
							+ "'myField': {"
									+ "'type': 'keyword',"
									+ "'store': false"
							+ "}"
					+ "}"
				+ "}"
		);

		SubTest.expectException( () -> validateSchemaConfig()
				.withIndex( INDEX_NAME, ctx -> {
							IndexSchemaElement root = ctx.getSchemaElement();
							root.field( "myField", f -> f.asString().projectable( Projectable.YES ) ).toReference();
						}
				)
				.setup() )
				.assertThrown()
				.isInstanceOf( Exception.class )
				.hasMessageMatching( FailureReportUtils.buildFailureReportPattern()
						.indexContext( INDEX_NAME )
						.contextLiteral( SCHEMA_VALIDATION_CONTEXT )
						.indexFieldContext( "myField" )
						.failure( "Invalid value for attribute 'store'. Expected 'true', actual is 'null'" )
						.build() );
	}

	@Test
	public void attribute_nullValue_valid() {
		elasticSearchClient.index( INDEX_NAME ).deleteAndCreate();
		elasticSearchClient.index( INDEX_NAME ).type().putMapping(
				"{"
					+ "'dynamic': 'strict',"
					+ "'properties': {"
							+ "'myField': {"
									+ "'type': 'integer',"
									+ "'null_value': 739"
							+ "}"
					+ "}"
				+ "}"
		);

		validateSchemaConfig()
				.withIndex( INDEX_NAME, ctx -> {
							IndexSchemaElement root = ctx.getSchemaElement();
							root.field( "myField", f -> f.asInteger().indexNullAs( 739 ) ).toReference();
						}
				)
				.setup();
	}

	@Test
	public void attribute_nullValue_missing() {
		elasticSearchClient.index( INDEX_NAME ).deleteAndCreate();
		elasticSearchClient.index( INDEX_NAME ).type().putMapping(
				"{"
					+ "'dynamic': 'strict',"
					+ "'properties': {"
							+ "'myField': {"
									+ "'type': 'integer'"
							+ "}"
					+ "}"
				+ "}"
		);

		SubTest.expectException( () -> validateSchemaConfig()
				.withIndex( INDEX_NAME, ctx -> {
							IndexSchemaElement root = ctx.getSchemaElement();
							root.field( "myField", f -> f.asInteger().indexNullAs( 739 ) ).toReference();
						}
				)
				.setup() )
				.assertThrown()
				.isInstanceOf( Exception.class )
				.hasMessageMatching( FailureReportUtils.buildFailureReportPattern()
						.indexContext( INDEX_NAME )
						.contextLiteral( SCHEMA_VALIDATION_CONTEXT )
						.indexFieldContext( "myField" )
						.failure( "Invalid value for attribute 'null_value'. Expected '739', actual is 'null'" )
						.build() );
	}

	@Test
	public void attribute_nullValue_invalid() {
		elasticSearchClient.index( INDEX_NAME ).deleteAndCreate();
		elasticSearchClient.index( INDEX_NAME ).type().putMapping(
				"{"
					+ "'dynamic': 'strict',"
					+ "'properties': {"
							+ "'myField': {"
									+ "'type': 'integer',"
									+ "'null_value': 777"
							+ "}"
					+ "}"
				+ "}"
		);

		SubTest.expectException( () -> validateSchemaConfig()
				.withIndex( INDEX_NAME, ctx -> {
							IndexSchemaElement root = ctx.getSchemaElement();
							root.field( "myField", f -> f.asInteger().indexNullAs( 739 ) ).toReference();
						}
				)
				.setup() )
				.assertThrown()
				.isInstanceOf( Exception.class )
				.hasMessageMatching( FailureReportUtils.buildFailureReportPattern()
						.indexContext( INDEX_NAME )
						.contextLiteral( SCHEMA_VALIDATION_CONTEXT )
						.indexFieldContext( "myField" )
						.failure( "Invalid value for attribute 'null_value'. Expected '739', actual is '777'" )
						.build() );
	}

	@Test
	public void attribute_normalizer_missing() {
		elasticSearchClient.index( INDEX_NAME ).deleteAndCreate();
		elasticSearchClient.index( INDEX_NAME ).type().putMapping(
				"{"
					+ "'dynamic': 'strict',"
					+ "'properties': {"
							+ "'myField': {"
									+ "'type': 'keyword',"
									+ "'index': true"
							+ "}"
					+ "}"
				+ "}"
		);

		SubTest.expectException( () ->
				validateSchemaConfig()
						.withIndex( INDEX_NAME, ctx -> {
									IndexSchemaElement root = ctx.getSchemaElement();
									root.field( "myField", f -> f.asString().normalizer( "default" ) ).toReference();
								}
						)
						.setup() )
				.assertThrown()
				.isInstanceOf( Exception.class )
				.hasMessageMatching( FailureReportUtils.buildFailureReportPattern()
						.indexContext( INDEX_NAME )
						.contextLiteral( SCHEMA_VALIDATION_CONTEXT )
						.indexFieldContext( "myField" )
						.failure( "Invalid value for attribute 'normalizer'. Expected 'default', actual is 'null'" )
						.build() );
	}

	private SearchSetupHelper.SetupContext validateSchemaConfig() {
		return setupHelper.withDefaultConfiguration( BACKEND_NAME )
				.withIndexDefaultsProperty(
						BACKEND_NAME,
						ElasticsearchIndexSettings.LIFECYCLE_STRATEGY,
						ElasticsearchIndexLifecycleStrategyName.VALIDATE.getExternalRepresentation()
				)
				.withBackendProperty(
						BACKEND_NAME,
						// Don't contribute any analysis definitions, migration of those is tested in another test class
						ElasticsearchBackendSettings.ANALYSIS_CONFIGURER,
						new ElasticsearchAnalysisConfigurer() {
							@Override
							public void configure(ElasticsearchAnalysisDefinitionContainerContext context) {
								// No-op
							}
						}
				);
	}
}
