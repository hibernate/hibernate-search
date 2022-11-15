/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.elasticsearch.schema.management;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.EnumSet;
import java.util.Map;

import org.hibernate.search.backend.elasticsearch.analysis.ElasticsearchAnalysisConfigurationContext;
import org.hibernate.search.backend.elasticsearch.analysis.ElasticsearchAnalysisConfigurer;
import org.hibernate.search.backend.elasticsearch.cfg.ElasticsearchIndexSettings;
import org.hibernate.search.backend.elasticsearch.index.DynamicMapping;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaObjectField;
import org.hibernate.search.engine.backend.types.ObjectStructure;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.common.impl.Futures;
import org.hibernate.search.util.impl.integrationtest.backend.elasticsearch.rule.TestElasticsearchClient;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappedIndex;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappingSchemaManagementStrategy;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.Rule;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.google.gson.Gson;

/**
 * Tests for the dynamic mapping attribute,
 * for all creating schema management operations.
 */
@RunWith(Parameterized.class)
@TestForIssue(jiraKey = "HSEARCH-3122")
public class ElasticsearchIndexSchemaManagerDynamicMappingIT {

	@Parameters(name = "With operation {0}")
	public static EnumSet<ElasticsearchIndexSchemaManagerOperation> operations() {
		return ElasticsearchIndexSchemaManagerOperation.creating();
	}

	@Rule
	public final SearchSetupHelper setupHelper = new SearchSetupHelper();

	@Rule
	public TestElasticsearchClient elasticSearchClient = new TestElasticsearchClient();

	private final StubMappedIndex index = StubMappedIndex.ofNonRetrievable( root -> {
		root.field( "field", f -> f.asString() ).toReference();

		IndexSchemaObjectField nested = root.objectField( "nested", ObjectStructure.NESTED );
		nested.toReference();
		nested.field( "field", f -> f.asInteger() ).toReference();
	} );

	private final Gson gson = new Gson();
	private final ElasticsearchIndexSchemaManagerOperation operation;

	public ElasticsearchIndexSchemaManagerDynamicMappingIT(ElasticsearchIndexSchemaManagerOperation operation) {
		this.operation = operation;
	}

	@Test
	public void dynamicMapping_default() {
		setupAndInspectIndex( null );
		String mapping = elasticSearchClient.index( index.name() ).type().getMapping();

		verifyDynamicMapping( mapping, DynamicMapping.STRICT );
	}

	@Test
	public void dynamicMapping_strict() {
		setupAndInspectIndex( DynamicMapping.STRICT.externalRepresentation() );
		String mapping = elasticSearchClient.index( index.name() ).type().getMapping();

		verifyDynamicMapping( mapping, DynamicMapping.STRICT );
	}

	@Test
	public void dynamicMapping_false() {
		setupAndInspectIndex( DynamicMapping.FALSE.externalRepresentation() );
		String mapping = elasticSearchClient.index( index.name() ).type().getMapping();

		verifyDynamicMapping( mapping, DynamicMapping.FALSE );
	}

	@Test
	public void dynamicMapping_true() {
		setupAndInspectIndex( DynamicMapping.TRUE.externalRepresentation() );
		String mapping = elasticSearchClient.index( index.name() ).type().getMapping();

		verifyDynamicMapping( mapping, DynamicMapping.TRUE );
	}

	@Test
	public void dynamicMapping_invalid() {
		assertThatThrownBy(
				() -> setupAndInspectIndex( "invalid" )
		)
				.isInstanceOf( SearchException.class )
				.hasMessageContaining(
						"Invalid value for configuration property 'hibernate.search.backend.indexes.indexName.dynamic_mapping': 'invalid'"
				)
				.hasMessageContaining( "Valid values are: [true, false, strict]" );
	}

	private void setupAndInspectIndex(String dynamicMapping) {
		SearchSetupHelper.SetupContext setupContext = setupHelper.start()
				.withBackendProperty(
						// Don't contribute any analysis definitions, validation of those is tested in another test class
						ElasticsearchIndexSettings.ANALYSIS_CONFIGURER,
						(ElasticsearchAnalysisConfigurer) (ElasticsearchAnalysisConfigurationContext context) -> {
							// No-op
						}
				)
				.withSchemaManagement( StubMappingSchemaManagementStrategy.DROP_ON_SHUTDOWN_ONLY )
				.withIndex( index );

		if ( dynamicMapping != null ) {
			setupContext.withIndexProperty( index.name(), "dynamic_mapping", dynamicMapping );
		}

		setupContext.setup();

		Futures.unwrappedExceptionJoin( operation.apply( index.schemaManager() ) );
	}

	private void verifyDynamicMapping(String mapping, DynamicMapping dynamicMapping) {
		@SuppressWarnings("unchecked") // Workaround for assertThat(Map) not taking wildcard type into account like assertThat(Collection) does
		Map<String, Object> map = gson.fromJson( mapping, Map.class );
		assertThat( map ).extractingByKey( "dynamic" )
				.isEqualTo( dynamicMapping.externalRepresentation() );
		assertThat( map ).extractingByKey( "properties" ).extracting( "nested" ).extracting( "dynamic" )
				.isEqualTo( dynamicMapping.externalRepresentation() );
	}
}
