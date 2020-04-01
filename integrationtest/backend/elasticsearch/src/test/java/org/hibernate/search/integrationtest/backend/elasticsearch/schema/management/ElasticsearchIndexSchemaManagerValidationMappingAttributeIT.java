/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.elasticsearch.schema.management;

import static org.hibernate.search.integrationtest.backend.elasticsearch.schema.management.ElasticsearchIndexSchemaManagerTestUtils.defaultMetadataMappingAndCommaForInitialization;
import static org.hibernate.search.integrationtest.backend.elasticsearch.schema.management.ElasticsearchIndexSchemaManagerTestUtils.simpleMappingForInitialization;

import java.util.EnumSet;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.hibernate.search.backend.elasticsearch.analysis.ElasticsearchAnalysisConfigurationContext;
import org.hibernate.search.backend.elasticsearch.analysis.ElasticsearchAnalysisConfigurer;
import org.hibernate.search.backend.elasticsearch.cfg.ElasticsearchBackendSettings;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.types.Norms;
import org.hibernate.search.engine.backend.types.Projectable;
import org.hibernate.search.engine.backend.types.Searchable;
import org.hibernate.search.engine.backend.types.Sortable;
import org.hibernate.search.engine.backend.types.TermVector;
import org.hibernate.search.engine.mapper.mapping.building.spi.IndexedEntityBindingContext;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.common.impl.Futures;
import org.hibernate.search.util.impl.integrationtest.backend.elasticsearch.rule.TestElasticsearchClient;
import org.hibernate.search.util.impl.integrationtest.common.FailureReportUtils;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappingIndexManager;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappingSchemaManagementStrategy;
import org.assertj.core.api.Assertions;

import org.junit.Assume;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

/**
 * Tests related to mapping attributes when validating indexes,
 * for all index-validating schema management operations.
 * <p>
 * These tests are more specific than {@link ElasticsearchIndexSchemaManagerValidationMappingBaseIT}
 * and focus on specific mapping attributes.
 */
@RunWith(Parameterized.class)
public class ElasticsearchIndexSchemaManagerValidationMappingAttributeIT {

	private static final String INDEX_NAME = "IndexName";
	private static final String SCHEMA_VALIDATION_CONTEXT = "schema validation";

	@Parameterized.Parameters(name = "With operation {0}")
	public static EnumSet<ElasticsearchIndexSchemaManagerValidationOperation> operations() {
		return ElasticsearchIndexSchemaManagerValidationOperation.all();
	}

	@Rule
	public SearchSetupHelper setupHelper = new SearchSetupHelper();

	@Rule
	public TestElasticsearchClient elasticSearchClient = new TestElasticsearchClient();

	private final ElasticsearchIndexSchemaManagerValidationOperation operation;

	private StubMappingIndexManager indexManager;

	public ElasticsearchIndexSchemaManagerValidationMappingAttributeIT(
			ElasticsearchIndexSchemaManagerValidationOperation operation) {
		this.operation = operation;
	}

	@Test
	public void attribute_dynamic_missing() {
		elasticSearchClient.index( INDEX_NAME ).deleteAndCreate();
		elasticSearchClient.index( INDEX_NAME ).type().putMapping(
				"{"
					+ "'properties': {"
							+ defaultMetadataMappingAndCommaForInitialization()
							+ "'myField': {"
									+ "'type': 'integer',"
									+ "'index': true"
							+ "}"
					+ "}"
				+ "}"
		);

		Assertions.assertThatThrownBy( () -> setupAndValidate( ctx -> {
			IndexSchemaElement root = ctx.getSchemaElement();
			root.field( "myField", f -> f.asInteger() ).toReference();
		} ) )
				.isInstanceOf( SearchException.class )
				.hasMessageMatching( FailureReportUtils.buildFailureReportPattern()
						.contextLiteral( SCHEMA_VALIDATION_CONTEXT )
						.mappingAttributeContext( "dynamic" )
						.failure( "Invalid value. Expected 'STRICT', actual is 'null'" )
						.build() );
	}

	@Test
	public void attribute_dynamic_invalid() {
		elasticSearchClient.index( INDEX_NAME ).deleteAndCreate();
		elasticSearchClient.index( INDEX_NAME ).type().putMapping(
				"{"
					+ "'dynamic': false,"
					+ "'properties': {"
							+ defaultMetadataMappingAndCommaForInitialization()
							+ "'myField': {"
									+ "'type': 'integer',"
									+ "'index': true"
							+ "}"
					+ "}"
				+ "}"
		);

		Assertions.assertThatThrownBy( () -> setupAndValidate( ctx -> {
			IndexSchemaElement root = ctx.getSchemaElement();
			root.field( "myField", f -> f.asInteger() ).toReference();
		} ) )
				.isInstanceOf( SearchException.class )
				.hasMessageMatching( FailureReportUtils.buildFailureReportPattern()
						.contextLiteral( SCHEMA_VALIDATION_CONTEXT )
						.mappingAttributeContext( "dynamic" )
						.failure( "Invalid value. Expected 'STRICT', actual is 'FALSE'" )
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

		Assertions.assertThatThrownBy( () -> setupAndValidate( ctx -> {
			IndexSchemaElement root = ctx.getSchemaElement();
			root.field( "myField", f -> f.asInteger() ).toReference();
		} ) )
				.isInstanceOf( SearchException.class )
				.hasMessageMatching( FailureReportUtils.buildFailureReportPattern()
						.contextLiteral( SCHEMA_VALIDATION_CONTEXT )
						.indexFieldContext( "_entity_type" )
						.failure( "Missing property mapping" )
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

		Assertions.assertThatThrownBy( () -> setupAndValidate( ctx -> {
			IndexSchemaElement root = ctx.getSchemaElement();
			root.field( "myField", f -> f.asInteger() ).toReference();
		} ) )
				.isInstanceOf( SearchException.class )
				.hasMessageMatching( FailureReportUtils.buildFailureReportPattern()
						.contextLiteral( SCHEMA_VALIDATION_CONTEXT )
						.indexFieldContext( "_entity_type" )
						.failure( "Missing property mapping" )
						.indexFieldContext( "myField" )
						.failure( "Missing property mapping" )
						.build() );
	}

	@Test
	public void attribute_type_invalid() {
		elasticSearchClient.index( INDEX_NAME ).deleteAndCreate();
		elasticSearchClient.index( INDEX_NAME ).type().putMapping(
				simpleMappingForInitialization(
					"'myField': {"
							+ "'type': 'keyword',"
							+ "'index': true"
					+ "}"
				)
		);

		Assertions.assertThatThrownBy( () -> setupAndValidate( ctx -> {
			IndexSchemaElement root = ctx.getSchemaElement();
			root.field( "myField", f -> f.asInteger() ).toReference();
		} ) )
				.isInstanceOf( Exception.class )
				.hasMessageMatching( FailureReportUtils.buildFailureReportPattern()
						.contextLiteral( SCHEMA_VALIDATION_CONTEXT )
						.indexFieldContext( "myField" )
						.mappingAttributeContext( "type" )
						.failure( "Invalid value. Expected 'integer', actual is 'keyword'" )
						.build() );
	}

	@Test
	public void attribute_index_missing() {
		elasticSearchClient.index( INDEX_NAME ).deleteAndCreate();
		elasticSearchClient.index( INDEX_NAME ).type().putMapping(
				simpleMappingForInitialization(
					"'myField': {"
							+ "'type': 'integer'"
					+ "}"
				)
		);

		// the expected value true is the default
		setupAndValidate( ctx -> {
			IndexSchemaElement root = ctx.getSchemaElement();
			root.field( "myField", f -> f.asInteger() ).toReference();
		} );
	}

	@Test
	public void attribute_index_invalid() {
		elasticSearchClient.index( INDEX_NAME ).deleteAndCreate();
		elasticSearchClient.index( INDEX_NAME ).type().putMapping(
				simpleMappingForInitialization(
					"'myField': {"
							+ "'type': 'integer',"
							+ "'index': false"
					+ "}"
				)
		);

		Assertions.assertThatThrownBy( () -> setupAndValidate( ctx -> {
			IndexSchemaElement root = ctx.getSchemaElement();
			root.field( "myField", f -> f.asInteger() ).toReference();
		} ) )
				.isInstanceOf( Exception.class )
				.hasMessageMatching( FailureReportUtils.buildFailureReportPattern()
						.contextLiteral( SCHEMA_VALIDATION_CONTEXT )
						.indexFieldContext( "myField" )
						.mappingAttributeContext( "index" )
						.failure( "Invalid value. Expected 'true', actual is 'false'" )
						.build() );
	}

	@Test
	public void attribute_index_false_scalar() {
		elasticSearchClient.index( INDEX_NAME ).deleteAndCreate();
		elasticSearchClient.index( INDEX_NAME ).type().putMapping(
				simpleMappingForInitialization(
					"'myField': {"
							+ "'type': 'integer',"
							+ "'index': false"
					+ "}"
				)
		);

		setupAndValidate( ctx -> {
			IndexSchemaElement root = ctx.getSchemaElement();
			// Searchable.NO allows to have index false
			root.field( "myField", f -> f.asInteger().searchable( Searchable.NO ) ).toReference();
		} );
	}

	@Test
	public void attribute_index_false_text() {
		elasticSearchClient.index( INDEX_NAME ).deleteAndCreate();
		elasticSearchClient.index( INDEX_NAME ).type().putMapping(
				simpleMappingForInitialization(
					"'myField': {"
							+ "'type': 'text',"
							+ "'analyzer': 'keyword',"
							+ "'index': false"
					+ "}"
				)
		);

		setupAndValidate( ctx -> {
			IndexSchemaElement root = ctx.getSchemaElement();
			// Searchable.NO allows to have index false
			root.field( "myField", f -> f.asString().analyzer( "keyword" ).searchable( Searchable.NO ) ).toReference();
		} );
	}

	@Test
	public void attribute_format_missing() {
		List<String> allFormats = elasticSearchClient.getDialect().getAllLocalDateDefaultMappingFormats();

		elasticSearchClient.index( INDEX_NAME ).deleteAndCreate();
		elasticSearchClient.index( INDEX_NAME ).type().putMapping(
				simpleMappingForInitialization(
					"'myField': {"
							+ "'type': 'date'"
					+ "}"
				)
		);

		Assertions.assertThatThrownBy( () -> setupAndValidate( ctx -> {
			IndexSchemaElement root = ctx.getSchemaElement();
			root.field( "myField", f -> f.asLocalDate() ).toReference();
		} ) )
				.isInstanceOf( Exception.class )
				.hasMessageMatching( FailureReportUtils.buildFailureReportPattern()
						.contextLiteral( SCHEMA_VALIDATION_CONTEXT )
						.indexFieldContext( "myField" )
						.mappingAttributeContext( "format" )
						.failure( "The output format (the first element) is invalid." )
						.failure(
								"Invalid formats",
								"missing elements are '" + allFormats + "'"
						)
						.build() );
	}

	@Test
	public void attribute_format_valid() {
		String allFormats = elasticSearchClient.getDialect().getConcatenatedLocalDateDefaultMappingFormats();

		elasticSearchClient.index( INDEX_NAME ).deleteAndCreate();
		elasticSearchClient.index( INDEX_NAME ).type().putMapping(
				simpleMappingForInitialization(
					"'myField': {"
							+ "'type': 'date',"
							+ "'format': '" + allFormats + "'"
					+ "}"
				)
		);

		setupAndValidate( ctx -> {
			IndexSchemaElement root = ctx.getSchemaElement();
			root.field( "myField", f -> f.asLocalDate() ).toReference();
		} );
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
				simpleMappingForInitialization(
					"'myField': {"
							+ "'type': 'date',"
							+ "'format': '" + firstFormat + "'"
					+ "}"
				)
		);

		Assertions.assertThatThrownBy( () -> setupAndValidate( ctx -> {
			IndexSchemaElement root = ctx.getSchemaElement();
			root.field( "myField", f -> f.asLocalDate() ).toReference();
		} ) )
				.isInstanceOf( Exception.class )
				.hasMessageMatching( FailureReportUtils.buildFailureReportPattern()
						.contextLiteral( SCHEMA_VALIDATION_CONTEXT )
						.indexFieldContext( "myField" )
						.mappingAttributeContext( "format" )
						.failure(
								"Invalid formats",
								"missing elements are '" + nextFormats + "'"
						)
						.build() );
	}

	@Test
	public void attribute_format_exceeding() {
		String allFormats = elasticSearchClient.getDialect().getConcatenatedLocalDateDefaultMappingFormats();

		elasticSearchClient.index( INDEX_NAME ).deleteAndCreate();
		elasticSearchClient.index( INDEX_NAME ).type().putMapping(
				simpleMappingForInitialization(
					"'myField': {"
							+ "'type': 'date',"
							+ "'format': '" + allFormats + "||yyyy" + "'"
					+ "}"
				)
		);

		Assertions.assertThatThrownBy( () -> setupAndValidate( ctx -> {
			IndexSchemaElement root = ctx.getSchemaElement();
			root.field( "myField", f -> f.asLocalDate() ).toReference();
		} ) )
				.isInstanceOf( Exception.class )
				.hasMessageMatching( FailureReportUtils.buildFailureReportPattern()
						.contextLiteral( SCHEMA_VALIDATION_CONTEXT )
						.indexFieldContext( "myField" )
						.mappingAttributeContext( "format" )
						.failure(
								"Invalid formats",
								"unexpected elements are '[yyyy]'"
						)
						.build() );
	}

	@Test
	public void attribute_format_wrong() {
		elasticSearchClient.index( INDEX_NAME ).deleteAndCreate();
		elasticSearchClient.index( INDEX_NAME ).type().putMapping(
				simpleMappingForInitialization(
					"'myField': {"
							+ "'type': 'date',"
							+ "'format': 'epoch_millis||strict_date_time'"
					+ "}"
				)
		);

		Assertions.assertThatThrownBy( () -> setupAndValidate( ctx -> {
			IndexSchemaElement root = ctx.getSchemaElement();
			root.field( "myField", f -> f.asLocalDate() ).toReference();
		} ) )
				.isInstanceOf( Exception.class )
				.hasMessageMatching( FailureReportUtils.buildFailureReportPattern()
						.contextLiteral( SCHEMA_VALIDATION_CONTEXT )
						.indexFieldContext( "myField" )
						.mappingAttributeContext( "format" )
						.failure(
								"The output format (the first element) is invalid. Expected '"
										+ elasticSearchClient.getDialect().getFirstLocalDateDefaultMappingFormat()
										+ "', actual is 'epoch_millis'"
						)
						.build() );
	}

	@Test
	public void attribute_analyzer_missing() {
		elasticSearchClient.index( INDEX_NAME ).deleteAndCreate();
		elasticSearchClient.index( INDEX_NAME ).type().putMapping(
				simpleMappingForInitialization(
					"'myField': {"
							+ "'type': 'text',"
							+ "'index': true"
					+ "}"
				)
		);

		Assertions.assertThatThrownBy( () -> setupAndValidate( ctx -> {
			IndexSchemaElement root = ctx.getSchemaElement();
			root.field( "myField", f -> f.asString().analyzer( "keyword" ) ).toReference();
		} ) )
				.isInstanceOf( Exception.class )
				.hasMessageMatching( FailureReportUtils.buildFailureReportPattern()
						.contextLiteral( SCHEMA_VALIDATION_CONTEXT )
						.indexFieldContext( "myField" )
						.mappingAttributeContext( "analyzer" )
						.failure( "Invalid value. Expected 'keyword', actual is 'null'" )
						.build() );
	}

	@Test
	public void attribute_analyzer_valid() {
		elasticSearchClient.index( INDEX_NAME ).deleteAndCreate();
		elasticSearchClient.index( INDEX_NAME ).type().putMapping(
				simpleMappingForInitialization(
					"'myField': {"
							+ "'type': 'text',"
							+ "'index': true,"
							+ "'analyzer': 'keyword'"
					+ "}"
				)
		);

		setupAndValidate( ctx -> {
			IndexSchemaElement root = ctx.getSchemaElement();
			root.field( "myField", f -> f.asString().analyzer( "keyword" ) ).toReference();
		} );
	}

	@Test
	public void attribute_analyzer_invalid() {
		elasticSearchClient.index( INDEX_NAME ).deleteAndCreate();
		elasticSearchClient.index( INDEX_NAME ).type().putMapping(
				simpleMappingForInitialization(
					"'myField': {"
							+ "'type': 'text',"
							+ "'index': true,"
							+ "'analyzer': 'keyword'"
					+ "}"
				)
		);

		Assertions.assertThatThrownBy( () -> setupAndValidate( ctx -> {
			IndexSchemaElement root = ctx.getSchemaElement();
			root.field( "myField", f -> f.asString().analyzer( "default" ) ).toReference();
		} ) )
				.isInstanceOf( Exception.class )
				.hasMessageMatching( FailureReportUtils.buildFailureReportPattern()
						.contextLiteral( SCHEMA_VALIDATION_CONTEXT )
						.indexFieldContext( "myField" )
						.mappingAttributeContext( "analyzer" )
						.failure( "Invalid value. Expected 'default', actual is 'keyword'" )
						.build() );
	}

	@Test
	public void attribute_searchAnalyzer_missing() {
		elasticSearchClient.index( INDEX_NAME ).deleteAndCreate();
		elasticSearchClient.index( INDEX_NAME ).type().putMapping(
				simpleMappingForInitialization(
					"'myField': {"
							+ "'type': 'text',"
							+ "'index': true,"
							+ "'analyzer': 'keyword'"
					+ "}"
				)
		);

		Assertions.assertThatThrownBy( () -> setupAndValidate( ctx -> {
			IndexSchemaElement root = ctx.getSchemaElement();
			root.field( "myField", f -> f.asString()
					.analyzer( "keyword" ).searchAnalyzer( "italian" ) ).toReference();
		} ) )
				.isInstanceOf( Exception.class )
				.hasMessageMatching( FailureReportUtils.buildFailureReportPattern()
						.contextLiteral( SCHEMA_VALIDATION_CONTEXT )
						.indexFieldContext( "myField" )
						.mappingAttributeContext( "search_analyzer" )
						.failure( "Invalid value. Expected 'italian', actual is 'null'" )
						.build() );
	}

	@Test
	public void attribute_searchAnalyzer_valid() {
		elasticSearchClient.index( INDEX_NAME ).deleteAndCreate();
		elasticSearchClient.index( INDEX_NAME ).type().putMapping(
				simpleMappingForInitialization(
					"'myField': {"
							+ "'type': 'text',"
							+ "'index': true,"
							+ "'analyzer': 'keyword',"
							+ "'search_analyzer': 'english'"
					+ "}"
				)
		);

		setupAndValidate( ctx -> {
			IndexSchemaElement root = ctx.getSchemaElement();
			root.field( "myField", f -> f.asString()
					.analyzer( "keyword" ).searchAnalyzer( "english" ) ).toReference();
		} );
	}

	@Test
	public void attribute_searchAnalyzer_invalid() {
		elasticSearchClient.index( INDEX_NAME ).deleteAndCreate();
		elasticSearchClient.index( INDEX_NAME ).type().putMapping(
				simpleMappingForInitialization(
					"'myField': {"
							+ "'type': 'text',"
							+ "'index': true,"
							+ "'analyzer': 'keyword',"
							+ "'search_analyzer': 'english'"
					+ "}"
				)
		);

		Assertions.assertThatThrownBy( () -> setupAndValidate( ctx -> {
			IndexSchemaElement root = ctx.getSchemaElement();
			root.field( "myField", f -> f.asString().analyzer( "keyword" ).searchAnalyzer( "italian" ) ).toReference();
		} ) )
				.isInstanceOf( Exception.class )
				.hasMessageMatching( FailureReportUtils.buildFailureReportPattern()
						.contextLiteral( SCHEMA_VALIDATION_CONTEXT )
						.indexFieldContext( "myField" )
						.mappingAttributeContext( "search_analyzer" )
						.failure( "Invalid value. Expected 'italian', actual is 'english'" )
						.build() );
	}

	@Test
	public void property_norms_valid() {
		elasticSearchClient.index( INDEX_NAME ).deleteAndCreate();
		elasticSearchClient.index( INDEX_NAME ).type().putMapping(
				simpleMappingForInitialization(
					"'myField': {"
							+ "'type': 'text',"
							+ "'norms': false"
					+ "}"
				)
		);

		setupAndValidate( ctx -> {
			IndexSchemaElement root = ctx.getSchemaElement();
			root.field( "myField", f -> f.asString().analyzer( "default" ).norms( Norms.NO ) ).toReference();
		} );
	}

	@Test
	public void property_norms_invalid() {
		elasticSearchClient.index( INDEX_NAME ).deleteAndCreate();
		elasticSearchClient.index( INDEX_NAME ).type().putMapping(
				simpleMappingForInitialization(
					"'myField': {"
							+ "'type': 'text',"
							+ "'norms': false"
					+ "}"
				)
		);

		Assertions.assertThatThrownBy( () -> setupAndValidate( ctx -> {
			IndexSchemaElement root = ctx.getSchemaElement();
			root.field( "myField", f -> f.asString().analyzer( "default" ).norms( Norms.YES ) ).toReference();
		} ) )
				.isInstanceOf( Exception.class )
				.hasMessageMatching( FailureReportUtils.buildFailureReportPattern()
						.contextLiteral( SCHEMA_VALIDATION_CONTEXT )
						.indexFieldContext( "myField" )
						.mappingAttributeContext( "norms" )
						.failure( "Invalid value. Expected 'true', actual is 'false'" )
						.build()
				);
	}

	@Test
	public void property_norms_missing_textField() {
		elasticSearchClient.index( INDEX_NAME ).deleteAndCreate();
		elasticSearchClient.index( INDEX_NAME ).type().putMapping(
				simpleMappingForInitialization(
					"'myField': {"
							+ "'type': 'text'"
					+ "}"
				)
		);

		setupAndValidate( ctx -> {
			IndexSchemaElement root = ctx.getSchemaElement();
			root.field( "myField", f -> f.asString().analyzer( "default" ).norms( Norms.YES ) ).toReference();
		} );
	}

	@Test
	public void property_norms_missing_keywordField() {
		elasticSearchClient.index( INDEX_NAME ).deleteAndCreate();
		elasticSearchClient.index( INDEX_NAME ).type().putMapping(
				simpleMappingForInitialization(
					"'myField': {"
							+ "'type': 'keyword'"
					+ "}"
				)
		);

		setupAndValidate( ctx -> {
			IndexSchemaElement root = ctx.getSchemaElement();
			root.field( "myField", f -> f.asString().norms( Norms.NO ) ).toReference();
		} );
	}

	@Test
	public void property_termVector_valid() {
		elasticSearchClient.index( INDEX_NAME ).deleteAndCreate();
		elasticSearchClient.index( INDEX_NAME ).type().putMapping(
				simpleMappingForInitialization(
					"'myField': {"
							+ "'type': 'text',"
							+ "'analyzer': 'english',"
							+ "'term_vector': 'with_positions_offsets'"
					+ "}"
				)
		);

		setupAndValidate( ctx -> {
			IndexSchemaElement root = ctx.getSchemaElement();
			root.field( "myField", f -> f.asString().analyzer( "english" ).termVector( TermVector.WITH_POSITIONS_OFFSETS ) ).toReference();
		} );
	}

	@Test
	public void property_termVector_missing() {
		elasticSearchClient.index( INDEX_NAME ).deleteAndCreate();
		elasticSearchClient.index( INDEX_NAME ).type().putMapping(
				simpleMappingForInitialization(
					"'myField': {"
							+ "'type': 'text',"
							+ "'analyzer': 'english'"
					+ "}"
				)
		);

		setupAndValidate( ctx -> {
			IndexSchemaElement root = ctx.getSchemaElement();
			root.field( "myField", f -> f.asString().analyzer( "english" ).termVector( TermVector.NO ) ).toReference();
		} );
	}

	@Test
	public void property_termVector_invalid() {
		elasticSearchClient.index( INDEX_NAME ).deleteAndCreate();
		elasticSearchClient.index( INDEX_NAME ).type().putMapping(
				simpleMappingForInitialization(
					"'myField': {"
							+ "'type': 'text',"
							+ "'analyzer': 'english',"
							+ "'term_vector': 'with_offsets'"
					+ "}"
				)
		);

		Assertions.assertThatThrownBy( () -> setupAndValidate( ctx -> {
			IndexSchemaElement root = ctx.getSchemaElement();
			root.field( "myField", f -> f.asString().analyzer( "english" ).termVector( TermVector.YES ) ).toReference();
		} ) )
				.isInstanceOf( SearchException.class )
				.hasMessageMatching( FailureReportUtils.buildFailureReportPattern()
						.contextLiteral( SCHEMA_VALIDATION_CONTEXT )
						.indexFieldContext( "myField" )
						.mappingAttributeContext( "term_vector" )
						.failure( "Invalid value. Expected 'yes', actual is 'with_offsets'" )
						.build() );
	}

	@Test
	public void attribute_store_true() {
		elasticSearchClient.index( INDEX_NAME ).deleteAndCreate();
		elasticSearchClient.index( INDEX_NAME ).type().putMapping(
				simpleMappingForInitialization(
					"'myField': {"
							+ "'type': 'keyword',"
							+ "'store': true"
					+ "}"
				)
		);

		setupAndValidate( ctx -> {
			IndexSchemaElement root = ctx.getSchemaElement();
			root.field( "myField", f -> f.asString().projectable( Projectable.YES ) ).toReference();
		} );
	}

	@Test
	public void attribute_store_default() {
		elasticSearchClient.index( INDEX_NAME ).deleteAndCreate();
		elasticSearchClient.index( INDEX_NAME ).type().putMapping(
				simpleMappingForInitialization(
					"'myField': {"
							+ "'type': 'keyword'"
					+ "}"
				)
		);

		setupAndValidate( ctx -> {
			IndexSchemaElement root = ctx.getSchemaElement();
			root.field( "myField", f -> f.asString().projectable( Projectable.DEFAULT ) ).toReference();
		} );
	}

	@Test
	public void attribute_store_invalid() {
		elasticSearchClient.index( INDEX_NAME ).deleteAndCreate();
		elasticSearchClient.index( INDEX_NAME ).type().putMapping(
				simpleMappingForInitialization(
					"'myField': {"
							+ "'type': 'keyword',"
							+ "'store': false"
					+ "}"
				)
		);

		Assertions.assertThatThrownBy( () -> setupAndValidate( ctx -> {
			IndexSchemaElement root = ctx.getSchemaElement();
			root.field( "myField", f -> f.asString().projectable( Projectable.YES ) ).toReference();
		} ) )
				.isInstanceOf( Exception.class )
				.hasMessageMatching( FailureReportUtils.buildFailureReportPattern()
						.contextLiteral( SCHEMA_VALIDATION_CONTEXT )
						.indexFieldContext( "myField" )
						.mappingAttributeContext( "store" )
						.failure( "Invalid value. Expected 'true', actual is 'null'" )
						.build() );
	}

	@Test
	public void attribute_nullValue_valid() {
		elasticSearchClient.index( INDEX_NAME ).deleteAndCreate();
		elasticSearchClient.index( INDEX_NAME ).type().putMapping(
				simpleMappingForInitialization(
					"'myField': {"
							+ "'type': 'integer',"
							+ "'null_value': 739"
					+ "}"
				)
		);

		setupAndValidate( ctx -> {
			IndexSchemaElement root = ctx.getSchemaElement();
			root.field( "myField", f -> f.asInteger().indexNullAs( 739 ) ).toReference();
		} );
	}

	@Test
	public void attribute_nullValue_missing() {
		elasticSearchClient.index( INDEX_NAME ).deleteAndCreate();
		elasticSearchClient.index( INDEX_NAME ).type().putMapping(
				simpleMappingForInitialization(
					"'myField': {"
							+ "'type': 'integer'"
					+ "}"
				)
		);

		Assertions.assertThatThrownBy( () -> setupAndValidate( ctx -> {
			IndexSchemaElement root = ctx.getSchemaElement();
			root.field( "myField", f -> f.asInteger().indexNullAs( 739 ) ).toReference();
		} ) )
				.isInstanceOf( Exception.class )
				.hasMessageMatching( FailureReportUtils.buildFailureReportPattern()
						.contextLiteral( SCHEMA_VALIDATION_CONTEXT )
						.indexFieldContext( "myField" )
						.mappingAttributeContext( "null_value" )
						.failure( "Invalid value. Expected '739', actual is 'null'" )
						.build() );
	}

	@Test
	public void attribute_nullValue_invalid() {
		elasticSearchClient.index( INDEX_NAME ).deleteAndCreate();
		elasticSearchClient.index( INDEX_NAME ).type().putMapping(
				simpleMappingForInitialization(
					"'myField': {"
							+ "'type': 'integer',"
							+ "'null_value': 777"
					+ "}"
				)
		);

		Assertions.assertThatThrownBy( () -> setupAndValidate( ctx -> {
			IndexSchemaElement root = ctx.getSchemaElement();
			root.field( "myField", f -> f.asInteger().indexNullAs( 739 ) ).toReference();
		} ) )
				.isInstanceOf( Exception.class )
				.hasMessageMatching( FailureReportUtils.buildFailureReportPattern()
						.contextLiteral( SCHEMA_VALIDATION_CONTEXT )
						.indexFieldContext( "myField" )
						.mappingAttributeContext( "null_value" )
						.failure( "Invalid value. Expected '739', actual is '777'" )
						.build() );
	}

	@Test
	public void attribute_docValues_valid() {
		elasticSearchClient.index( INDEX_NAME ).deleteAndCreate();
		elasticSearchClient.index( INDEX_NAME ).type().putMapping(
				simpleMappingForInitialization(
					"'myField': {"
							+ "'type': 'integer',"
							+ "'doc_values': true"
					+ "}"
				)
		);

		setupAndValidate( ctx -> {
			IndexSchemaElement root = ctx.getSchemaElement();
			root.field( "myField", f -> f.asInteger().sortable( Sortable.YES ) ).toReference();
		} );
	}

	@Test
	public void attribute_docValues_default() {
		elasticSearchClient.index( INDEX_NAME ).deleteAndCreate();
		elasticSearchClient.index( INDEX_NAME ).type().putMapping(
				simpleMappingForInitialization(
					"'myField': {"
							+ "'type': 'integer'"
					+ "}"
				)
		);

		setupAndValidate( ctx -> {
			IndexSchemaElement root = ctx.getSchemaElement();
			root.field( "myField", f -> f.asInteger().sortable( Sortable.YES ) ).toReference();
		} );
	}

	@Test
	public void attribute_docValues_invalid() {
		elasticSearchClient.index( INDEX_NAME ).deleteAndCreate();
		elasticSearchClient.index( INDEX_NAME ).type().putMapping(
				simpleMappingForInitialization(
					"'myField': {"
							+ "'type': 'integer',"
							+ "'doc_values': false"
					+ "}"
				)
		);

		Assertions.assertThatThrownBy( () -> setupAndValidate( ctx -> {
			IndexSchemaElement root = ctx.getSchemaElement();
			root.field( "myField", f -> f.asInteger().sortable( Sortable.YES ) ).toReference();
		} ) )
				.isInstanceOf( Exception.class )
				.hasMessageMatching( FailureReportUtils.buildFailureReportPattern()
						.contextLiteral( SCHEMA_VALIDATION_CONTEXT )
						.indexFieldContext( "myField" )
						.mappingAttributeContext( "doc_values" )
						.failure( "Invalid value. Expected 'true', actual is 'false'" )
						.build() );
	}

	@Test
	public void attribute_docValues_false() {
		elasticSearchClient.index( INDEX_NAME ).deleteAndCreate();
		elasticSearchClient.index( INDEX_NAME ).type().putMapping(
				simpleMappingForInitialization(
					"'myField': {"
							+ "'type': 'integer',"
							+ "'doc_values': false"
					+ "}"
				)
		);

		setupAndValidate( ctx -> {
			IndexSchemaElement root = ctx.getSchemaElement();
			// Sortable.NO and Sortable.DEFAULT allow to have doc_values false
			root.field( "myField", f -> f.asInteger() ).toReference();
		} );
	}

	@Test
	public void attribute_docValues_skip() {
		elasticSearchClient.index( INDEX_NAME ).deleteAndCreate();
		elasticSearchClient.index( INDEX_NAME ).type().putMapping(
				simpleMappingForInitialization(
					"'myField': {"
							+ "'type': 'keyword',"
							+ "'doc_values': true"
					+ "}"
				)
		);

		setupAndValidate( ctx -> {
			IndexSchemaElement root = ctx.getSchemaElement();
			// Sortable.NO or Sortable.DEFAULT does not impose doc_values false
			root.field( "myField", f -> f.asString().sortable( Sortable.NO ) ).toReference();
		} );
	}

	@Test
	public void attribute_scaling_factor_valid() {
		elasticSearchClient.index( INDEX_NAME ).deleteAndCreate();
		elasticSearchClient.index( INDEX_NAME ).type().putMapping(
				simpleMappingForInitialization(
					"'myField': {"
							+ "'type': 'scaled_float',"
							+ "'scaling_factor': 100"
					+ "}"
				)
		);

		setupAndValidate( ctx -> {
			IndexSchemaElement root = ctx.getSchemaElement();
			root.field( "myField", f -> f.asBigDecimal().decimalScale( 2 ) ).toReference();
		} );
	}

	@Test
	public void attribute_scaling_factor_invalid() {
		elasticSearchClient.index( INDEX_NAME ).deleteAndCreate();
		elasticSearchClient.index( INDEX_NAME ).type().putMapping(
				simpleMappingForInitialization(
					"'myField': {"
							+ "'type': 'scaled_float',"
							+ "'scaling_factor': 2"
					+ "}"
				)
		);

		Assertions.assertThatThrownBy( () -> setupAndValidate( ctx -> {
			IndexSchemaElement root = ctx.getSchemaElement();
			root.field( "myField", f -> f.asBigDecimal().decimalScale( 2 ) ).toReference();
		} ) )
				.isInstanceOf( Exception.class )
				.hasMessageMatching( FailureReportUtils.buildFailureReportPattern()
						.contextLiteral( SCHEMA_VALIDATION_CONTEXT )
						.indexFieldContext( "myField" )
						.mappingAttributeContext( "scaling_factor" )
						.failure( "Invalid value. Expected '100.0', actual is '2.0'" )
						.build() );
	}

	@Test
	public void attribute_normalizer_missing() {
		elasticSearchClient.index( INDEX_NAME ).deleteAndCreate();
		elasticSearchClient.index( INDEX_NAME ).type().putMapping(
				simpleMappingForInitialization(
					"'myField': {"
							+ "'type': 'keyword',"
							+ "'index': true"
					+ "}"
				)
		);

		Assertions.assertThatThrownBy( () -> setupAndValidate( ctx -> {
			IndexSchemaElement root = ctx.getSchemaElement();
			root.field( "myField", f -> f.asString().normalizer( "default" ) ).toReference();
		} ) )
				.isInstanceOf( Exception.class )
				.hasMessageMatching( FailureReportUtils.buildFailureReportPattern()
						.contextLiteral( SCHEMA_VALIDATION_CONTEXT )
						.indexFieldContext( "myField" )
						.mappingAttributeContext( "normalizer" )
						.failure( "Invalid value. Expected 'default', actual is 'null'" )
						.build() );
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
				simpleMappingForInitialization(
					"'myField': {"
							+ "'type': 'keyword',"
							+ "'index': true,"
							+ "'normalizer': 'custom-normalizer'"
					+ "}"
				)
		);

		setupAndValidate( ctx -> {
			IndexSchemaElement root = ctx.getSchemaElement();
			root.field( "myField", f -> f.asString().normalizer( "custom-normalizer" ) ).toReference();
		} );
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
				simpleMappingForInitialization(
					"'myField': {"
							+ "'type': 'keyword',"
							+ "'index': true,"
							+ "'normalizer': 'custom-normalizer'"
					+ "}"
				)
		);

		Assertions.assertThatThrownBy( () -> setupAndValidate( ctx -> {
			IndexSchemaElement root = ctx.getSchemaElement();
			root.field( "myField", f -> f.asString().normalizer( "another-normalizer" ) ).toReference();
		} ) )
				.isInstanceOf( Exception.class )
				.hasMessageMatching( FailureReportUtils.buildFailureReportPattern()
						.contextLiteral( SCHEMA_VALIDATION_CONTEXT )
						.indexFieldContext( "myField" )
						.mappingAttributeContext( "normalizer" )
						.failure( "Invalid value. Expected 'another-normalizer', actual is 'custom-normalizer'" )
						.build() );
	}

	private void setupAndValidate(Consumer<? super IndexedEntityBindingContext> mappingContributor) {
		setupHelper.start()
				.withSchemaManagement( StubMappingSchemaManagementStrategy.DROP_ON_SHUTDOWN_ONLY )
				.withBackendProperty(
						// Don't contribute any analysis definitions, migration of those is tested in another test class
						ElasticsearchBackendSettings.ANALYSIS_CONFIGURER,
						(ElasticsearchAnalysisConfigurer) (ElasticsearchAnalysisConfigurationContext context) -> {
							// No-op
						}
				)
				.withIndex( INDEX_NAME, mappingContributor, indexManager -> this.indexManager = indexManager )
				.setup();

		Futures.unwrappedExceptionJoin( operation.apply( indexManager.getSchemaManager() ) );
	}
}
