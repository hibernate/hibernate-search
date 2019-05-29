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
import org.hibernate.search.engine.backend.types.Norms;
import org.hibernate.search.engine.backend.types.Projectable;
import org.hibernate.search.engine.backend.types.Searchable;
import org.hibernate.search.engine.backend.types.Sortable;
import org.hibernate.search.integrationtest.backend.elasticsearch.testsupport.configuration.ElasticsearchNormalizerManagementITAnalysisConfigurer;
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
	public void attribute_index_false_scalar() {
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

		validateSchemaConfig()
				.withIndex( INDEX_NAME, ctx -> {
							IndexSchemaElement root = ctx.getSchemaElement();
							// Searchable.NO allows to have index false
							root.field( "myField", f -> f.asInteger().searchable( Searchable.NO ) ).toReference();
						}
				)
				.setup();
	}

	@Test
	public void attribute_index_false_text() {
		elasticSearchClient.index( INDEX_NAME ).deleteAndCreate();
		elasticSearchClient.index( INDEX_NAME ).type().putMapping(
				"{"
					+ "'dynamic': 'strict',"
					+ "'properties': {"
							+ "'myField': {"
									+ "'type': 'text',"
									+ "'analyzer': 'keyword',"
									+ "'index': false"
							+ "}"
					+ "}"
				+ "}"
		);

		validateSchemaConfig()
				.withIndex( INDEX_NAME, ctx -> {
							IndexSchemaElement root = ctx.getSchemaElement();
							// Searchable.NO allows to have index false
							root.field( "myField", f -> f.asString().analyzer( "keyword" ).searchable( Searchable.NO ) ).toReference();
						}
				)
				.setup();
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

	@Test
	public void property_norms_valid() {
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

		validateSchemaConfig()
				.withIndex( INDEX_NAME, ctx -> {
							IndexSchemaElement root = ctx.getSchemaElement();
							root.field( "myField", f -> f.asString().analyzer( "default" ).norms( Norms.NO ) ).toReference();
						}
				)
				.setup();
	}

	@Test
	public void property_norms_invalid() {
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
									root.field( "myField", f -> f.asString().analyzer( "default" ).norms( Norms.YES ) ).toReference();
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
	public void property_norms_missing_textField() {
		elasticSearchClient.index( INDEX_NAME ).deleteAndCreate();
		elasticSearchClient.index( INDEX_NAME ).type().putMapping(
				"{"
					+ "'dynamic': 'strict',"
					+ "'properties': {"
							+ "'myField': {"
									+ "'type': 'text'"
							+ "}"
					+ "}"
				+ "}"
		);

		validateSchemaConfig()
				.withIndex( INDEX_NAME, ctx -> {
							IndexSchemaElement root = ctx.getSchemaElement();
							root.field( "myField", f -> f.asString().analyzer( "default" ).norms( Norms.YES ) ).toReference();
						}
				)
				.setup();
	}

	@Test
	public void property_norms_missing_keywordField() {
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
							root.field( "myField", f -> f.asString().norms( Norms.NO ) ).toReference();
						}
				)
				.setup();
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
	public void attribute_docValues_valid() {
		elasticSearchClient.index( INDEX_NAME ).deleteAndCreate();
		elasticSearchClient.index( INDEX_NAME ).type().putMapping(
				"{"
					+ "'dynamic': 'strict',"
					+ "'properties': {"
							+ "'myField': {"
									+ "'type': 'integer',"
									+ "'doc_values': true"
							+ "}"
					+ "}"
				+ "}"
		);

		validateSchemaConfig()
				.withIndex( INDEX_NAME, ctx -> {
							IndexSchemaElement root = ctx.getSchemaElement();
							root.field( "myField", f -> f.asInteger().sortable( Sortable.YES ) ).toReference();
						}
				)
				.setup();
	}

	@Test
	public void attribute_docValues_default() {
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

		validateSchemaConfig()
				.withIndex( INDEX_NAME, ctx -> {
							IndexSchemaElement root = ctx.getSchemaElement();
							root.field( "myField", f -> f.asInteger().sortable( Sortable.YES ) ).toReference();
						}
				)
				.setup();
	}

	@Test
	public void attribute_docValues_invalid() {
		elasticSearchClient.index( INDEX_NAME ).deleteAndCreate();
		elasticSearchClient.index( INDEX_NAME ).type().putMapping(
				"{"
					+ "'dynamic': 'strict',"
					+ "'properties': {"
							+ "'myField': {"
									+ "'type': 'integer',"
									+ "'doc_values': false"
							+ "}"
					+ "}"
				+ "}"
		);

		SubTest.expectException( () -> validateSchemaConfig()
				.withIndex( INDEX_NAME, ctx -> {
							IndexSchemaElement root = ctx.getSchemaElement();
							root.field( "myField", f -> f.asInteger().sortable( Sortable.YES ) ).toReference();
						}
				)
				.setup() )
				.assertThrown()
				.isInstanceOf( Exception.class )
				.hasMessageMatching( FailureReportUtils.buildFailureReportPattern()
						.indexContext( INDEX_NAME )
						.contextLiteral( SCHEMA_VALIDATION_CONTEXT )
						.indexFieldContext( "myField" )
						.failure( "Invalid value for attribute 'doc_values'. Expected 'true', actual is 'false'" )
						.build() );
	}

	@Test
	public void attribute_docValues_false() {
		elasticSearchClient.index( INDEX_NAME ).deleteAndCreate();
		elasticSearchClient.index( INDEX_NAME ).type().putMapping(
				"{"
					+ "'dynamic': 'strict',"
					+ "'properties': {"
							+ "'myField': {"
									+ "'type': 'integer',"
									+ "'doc_values': false"
							+ "}"
					+ "}"
				+ "}"
		);

		validateSchemaConfig()
				.withIndex( INDEX_NAME, ctx -> {
							IndexSchemaElement root = ctx.getSchemaElement();
							// Sortable.NO and Sortable.DEFAULT allow to have doc_values false
							root.field( "myField", f -> f.asInteger() ).toReference();
						}
				)
				.setup();
	}

	@Test
	public void attribute_docValues_skip() {
		elasticSearchClient.index( INDEX_NAME ).deleteAndCreate();
		elasticSearchClient.index( INDEX_NAME ).type().putMapping(
				"{"
					+ "'dynamic': 'strict',"
					+ "'properties': {"
							+ "'myField': {"
									+ "'type': 'keyword',"
									+ "'doc_values': true"
							+ "}"
					+ "}"
				+ "}"
		);

		validateSchemaConfig()
				.withIndex( INDEX_NAME, ctx -> {
							IndexSchemaElement root = ctx.getSchemaElement();
							// Sortable.NO or Sortable.DEFAULT does not impose doc_values false
							root.field( "myField", f -> f.asString().sortable( Sortable.NO ) ).toReference();
						}
				)
				.setup();
	}

	@Test
	public void attribute_scaling_factor_valid() {
		elasticSearchClient.index( INDEX_NAME ).deleteAndCreate();
		elasticSearchClient.index( INDEX_NAME ).type().putMapping(
				"{"
					+ "'dynamic': 'strict',"
					+ "'properties': {"
							+ "'myField': {"
									+ "'type': 'scaled_float',"
									+ "'scaling_factor': 100"
							+ "}"
					+ "}"
				+ "}"
		);

		validateSchemaConfig()
				.withIndex( INDEX_NAME, ctx -> {
							IndexSchemaElement root = ctx.getSchemaElement();
							root.field( "myField", f -> f.asBigDecimal().decimalScale( 2 ) ).toReference();
						}
				)
				.setup();
	}

	@Test
	public void attribute_scaling_factor_invalid() {
		elasticSearchClient.index( INDEX_NAME ).deleteAndCreate();
		elasticSearchClient.index( INDEX_NAME ).type().putMapping(
				"{"
					+ "'dynamic': 'strict',"
					+ "'properties': {"
							+ "'myField': {"
									+ "'type': 'scaled_float',"
									+ "'scaling_factor': 2"
							+ "}"
					+ "}"
				+ "}"
		);

		SubTest.expectException( () -> validateSchemaConfig()
				.withIndex( INDEX_NAME, ctx -> {
							IndexSchemaElement root = ctx.getSchemaElement();
							root.field( "myField", f -> f.asBigDecimal().decimalScale( 2 ) ).toReference();
						}
				)
				.setup() )
				.assertThrown()
				.isInstanceOf( Exception.class )
				.hasMessageMatching( FailureReportUtils.buildFailureReportPattern()
						.indexContext( INDEX_NAME )
						.contextLiteral( SCHEMA_VALIDATION_CONTEXT )
						.indexFieldContext( "myField" )
						.failure( "Invalid value for attribute 'scaling_factor'. Expected '100.0', actual is '2.0'" )
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

	@Test
	public void attribute_normalizer_valid() {
		elasticSearchClient.index( INDEX_NAME ).deleteAndCreate(
				"index.analysis",
				"{"
					+ "'normalizer': {"
							+ "'custom-normalizer': {"
									+ "'char_filter': ['custom-char-mapping'],"
									+ "'filter': ['custom-elision']"
							+ "}"
					+ "},"
					+ "'char_filter': {"
							+ "'custom-char-mapping': {"
									+ "'type': 'mapping',"
									+ "'mappings': ['foo => bar']"
							+ "}"
					+ "},"
					+ "'filter': {"
							+ "'custom-elision': {"
									+ "'type': 'elision',"
									+ "'articles': ['l', 'd']"
							+ "}"
					+ "}"
				+ "}"
		);
		elasticSearchClient.index( INDEX_NAME ).type().putMapping(
				"{"
					+ "'dynamic': 'strict',"
					+ "'properties': {"
							+ "'myField': {"
									+ "'type': 'keyword',"
									+ "'index': true,"
									+ "'normalizer': 'custom-normalizer'"
							+ "}"
					+ "}"
				+ "}"
		);

		validateSchemaWithAnalyzerConfig()
				.withIndex( INDEX_NAME, ctx -> {
							IndexSchemaElement root = ctx.getSchemaElement();
							root.field( "myField", f -> f.asString().normalizer( "custom-normalizer" ) ).toReference();
						}
				)
				.setup();
	}

	@Test
	public void attribute_normalizer_invalid() {
		elasticSearchClient.index( INDEX_NAME ).deleteAndCreate(
				"index.analysis",
				"{"
					+ "'normalizer': {"
							+ "'custom-normalizer': {"
									+ "'char_filter': ['custom-char-mapping'],"
									+ "'filter': ['custom-elision']"
							+ "}"
					+ "},"
					+ "'char_filter': {"
							+ "'custom-char-mapping': {"
									+ "'type': 'mapping',"
									+ "'mappings': ['foo => bar']"
							+ "}"
					+ "},"
					+ "'filter': {"
							+ "'custom-elision': {"
									+ "'type': 'elision',"
									+ "'articles': ['l', 'd']"
							+ "}"
					+ "}"
					+ "}"
		);
		elasticSearchClient.index( INDEX_NAME ).type().putMapping(
				"{"
					+ "'dynamic': 'strict',"
					+ "'properties': {"
							+ "'myField': {"
									+ "'type': 'keyword',"
									+ "'index': true,"
									+ "'normalizer': 'custom-normalizer'"
							+ "}"
					+ "}"
				+ "}"
		);

		SubTest.expectException( () ->
				validateSchemaWithAnalyzerConfig()
						.withIndex( INDEX_NAME, ctx -> {
									IndexSchemaElement root = ctx.getSchemaElement();
									root.field( "myField", f -> f.asString().normalizer( "another-normalizer" ) ).toReference();
								}
						)
						.setup() )
				.assertThrown()
				.isInstanceOf( Exception.class )
				.hasMessageMatching( FailureReportUtils.buildFailureReportPattern()
						.indexContext( INDEX_NAME )
						.contextLiteral( SCHEMA_VALIDATION_CONTEXT )
						.indexFieldContext( "myField" )
						.failure( "Invalid value for attribute 'normalizer'. Expected 'another-normalizer', actual is 'custom-normalizer'" )
						.build() );
	}

	private SearchSetupHelper.SetupContext validateSchemaWithAnalyzerConfig() {
		return setupHelper.withDefaultConfiguration( BACKEND_NAME )
				.withIndexDefaultsProperty(
						BACKEND_NAME,
						ElasticsearchIndexSettings.LIFECYCLE_STRATEGY,
						ElasticsearchIndexLifecycleStrategyName.VALIDATE.getExternalRepresentation()
				)
				.withBackendProperty(
						BACKEND_NAME,
						ElasticsearchBackendSettings.ANALYSIS_CONFIGURER,
						new ElasticsearchNormalizerManagementITAnalysisConfigurer()
				);
	}
}
