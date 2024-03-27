/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.backend.elasticsearch.schema.management;

import static org.hibernate.search.util.impl.test.JsonHelper.assertJsonEquals;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import org.hibernate.search.backend.elasticsearch.cfg.ElasticsearchIndexSettings;
import org.hibernate.search.engine.backend.work.execution.OperationSubmitter;
import org.hibernate.search.integrationtest.backend.elasticsearch.testsupport.configuration.ElasticsearchIndexSchemaManagerNormalizerITAnalysisConfigurer;
import org.hibernate.search.integrationtest.backend.elasticsearch.testsupport.util.ElasticsearchTckBackendFeatures;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.extension.SearchSetupHelper;
import org.hibernate.search.util.impl.integrationtest.backend.elasticsearch.extension.TestElasticsearchClient;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappedIndex;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappingSchemaManagementStrategy;
import org.hibernate.search.util.impl.test.annotation.PortedFromSearch5;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

/**
 * Tests related to normalizers when updating indexes.
 */
@PortedFromSearch5(original = "org.hibernate.search.elasticsearch.test.ElasticsearchNormalizerDefinitionMigrationIT")
class ElasticsearchIndexSchemaManagerUpdateNormalizerIT {

	@RegisterExtension
	public final SearchSetupHelper setupHelper = SearchSetupHelper.create();

	@RegisterExtension
	public TestElasticsearchClient elasticSearchClient = TestElasticsearchClient.create();

	private final StubMappedIndex index = StubMappedIndex.withoutFields();

	@BeforeEach
	void checkAssumption() {
		assumeTrue(
				ElasticsearchTckBackendFeatures.supportsIndexClosingAndOpening(),
				"This test only is only relevant if we are allowed to open/close Elasticsearch indexes."
		);
	}

	@Test
	void nothingToDo() throws Exception {
		elasticSearchClient.index( index.name() ).deleteAndCreate(
				"index.analysis",
				"{"
						+ " 'normalizer': {"
						+ "   'custom-normalizer': {"
						+ "     'char_filter': ['custom-char-mapping'],"
						+ "     'filter': ['custom-elision']"
						+ "   }"
						+ " },"
						+ " 'char_filter': {"
						+ "   'custom-char-mapping': {"
						+ "     'type': 'mapping',"
						+ "     'mappings': ['foo => bar']"
						+ "   }"
						+ " },"
						+ " 'filter': {"
						+ "   'custom-elision': {"
						+ "     'type': 'elision',"
						+ "     'articles': ['l', 'd']"
						+ "   }"
						+ " }"
						+ "}"
		);

		setupAndUpdateIndex();

		assertJsonEquals(
				"{"
						+ " 'normalizer': {"
						+ "   'custom-normalizer': {"
						+ "     'char_filter': ['custom-char-mapping'],"
						+ "     'filter': ['custom-elision']"
						+ "   }"
						+ " },"
						+ " 'char_filter': {"
						+ "   'custom-char-mapping': {"
						+ "     'type': 'mapping',"
						+ "     'mappings': ['foo => bar']"
						+ "   }"
						+ " },"
						+ " 'filter': {"
						+ "   'custom-elision': {"
						+ "     'type': 'elision',"
						+ "     'articles': ['l', 'd']"
						+ "   }"
						+ " }"
						+ "}",
				elasticSearchClient.index( index.name() ).settings( "index.analysis" ).get()
		);
	}

	@Test
	void normalizer_missing() throws Exception {
		elasticSearchClient.index( index.name() ).deleteAndCreate(
				"index.analysis",
				"{"
						+ " 'char_filter': {"
						+ "   'custom-char-mapping': {"
						+ "     'type': 'mapping',"
						+ "     'mappings': ['foo => bar']"
						+ "   }"
						+ " },"
						+ " 'filter': {"
						+ "   'custom-elision': {"
						+ "     'type': 'elision',"
						+ "     'articles': ['l', 'd']"
						+ "   }"
						+ " }"
						+ "}"
		);

		setupAndUpdateIndex();

		assertJsonEquals(
				"{"
						+ " 'normalizer': {"
						+ "   'custom-normalizer': {"
						+ "     'type': 'custom',"
						+ "     'char_filter': ['custom-char-mapping'],"
						+ "     'filter': ['custom-elision']"
						+ "   }"
						+ " },"
						+ " 'char_filter': {"
						+ "   'custom-char-mapping': {"
						+ "     'type': 'mapping',"
						+ "     'mappings': ['foo => bar']"
						+ "   }"
						+ " },"
						+ " 'filter': {"
						+ "   'custom-elision': {"
						+ "     'type': 'elision',"
						+ "     'articles': ['l', 'd']"
						+ "   }"
						+ " }"
						+ "}",
				elasticSearchClient.index( index.name() ).settings( "index.analysis" ).get()
		);
	}

	@Test
	void normalizer_componentDefinition_missing() throws Exception {
		elasticSearchClient.index( index.name() ).deleteAndCreate(
				"index.analysis",
				"{"
						/*
						 * We don't add the analyzer here: since a component is missing
						 * the analyzer can't reference it and thus it must be missing too.
						 */
						// missing: 'char_filter'
						+ " 'filter': {"
						+ "   'custom-elision': {"
						+ "     'type': 'elision',"
						+ "     'articles': ['l', 'd']"
						+ "   }"
						+ " }"
						+ "}"
		);

		setupAndUpdateIndex();

		assertJsonEquals(
				"{"
						+ " 'normalizer': {"
						+ "   'custom-normalizer': {"
						+ "     'type': 'custom',"
						+ "     'char_filter': ['custom-char-mapping'],"
						+ "     'filter': ['custom-elision']"
						+ "   }"
						+ " },"
						+ " 'char_filter': {"
						+ "   'custom-char-mapping': {"
						+ "     'type': 'mapping',"
						+ "     'mappings': ['foo => bar']"
						+ "   }"
						+ " },"
						+ " 'filter': {"
						+ "   'custom-elision': {"
						+ "     'type': 'elision',"
						+ "     'articles': ['l', 'd']"
						+ "   }"
						+ " }"
						+ "}",
				elasticSearchClient.index( index.name() ).settings( "index.analysis" ).get()
		);
	}

	@Test
	void normalizer_componentReference_invalid() throws Exception {
		elasticSearchClient.index( index.name() ).deleteAndCreate(
				"index.analysis",
				"{"
						+ " 'normalizer': {"
						+ "   'custom-normalizer': {"
						+ "     'char_filter': ['custom-char-mapping2']," // Invalid
						+ "     'filter': ['custom-elision']"
						+ "   }"
						+ " },"
						+ " 'char_filter': {"
						+ "   'custom-char-mapping': {"
						+ "     'type': 'mapping',"
						+ "     'mappings': ['foo => bar']"
						+ "   },"
						+ "   'custom-char-mapping2': {"
						+ "     'type': 'mapping',"
						+ "     'mappings': ['foo => bar2']"
						+ "   }"
						+ " },"
						+ " 'filter': {"
						+ "   'custom-elision': {"
						+ "     'type': 'elision',"
						+ "     'articles': ['l', 'd']"
						+ "   }"
						+ " }"
						+ "}"
		);

		setupAndUpdateIndex();

		assertJsonEquals(
				"{"
						+ " 'normalizer': {"
						+ "   'custom-normalizer': {"
						+ "     'type': 'custom',"
						+ "     'char_filter': ['custom-char-mapping'],"
						+ "     'filter': ['custom-elision']"
						+ "   }"
						+ " },"
						+ " 'char_filter': {"
						+ "   'custom-char-mapping': {"
						+ "     'type': 'mapping',"
						+ "     'mappings': ['foo => bar']"
						+ "   },"
						+ " 'custom-char-mapping2': {"
						+ "   'type': 'mapping',"
						+ "   'mappings': ['foo => bar2']"
						+ " }"
						+ " },"
						+ " 'filter': {"
						+ "   'custom-elision': {"
						+ "     'type': 'elision',"
						+ "     'articles': ['l', 'd']"
						+ "   }"
						+ " }"
						+ "}",
				elasticSearchClient.index( index.name() ).settings( "index.analysis" ).get()
		);
	}

	@Test
	void normalizer_componentDefinition_invalid() throws Exception {
		elasticSearchClient.index( index.name() ).deleteAndCreate(
				"index.analysis",
				"{"
						+ " 'normalizer': {"
						+ "   'custom-normalizer': {"
						+ "     'char_filter': ['custom-char-mapping']," // Correct, but the actual definition is not
						+ "     'filter': ['custom-elision']"
						+ "   }"
						+ " },"
						+ " 'char_filter': {"
						+ "   'custom-char-mapping': {"
						+ "     'type': 'mapping',"
						+ "     'mappings': ['foo => bar2']" // Invalid
						+ "   }"
						+ " },"
						+ " 'filter': {"
						+ "   'custom-elision': {"
						+ "     'type': 'elision',"
						+ "     'articles': ['l', 'd']"
						+ "   }"
						+ " }"
						+ "}"
		);

		setupAndUpdateIndex();

		assertJsonEquals(
				"{"
						+ " 'normalizer': {"
						+ "   'custom-normalizer': {"
						+ "     'type': 'custom',"
						+ "     'char_filter': ['custom-char-mapping'],"
						+ "     'filter': ['custom-elision']"
						+ "   }"
						+ " },"
						+ " 'char_filter': {"
						+ "   'custom-char-mapping': {"
						+ "     'type': 'mapping',"
						+ "     'mappings': ['foo => bar']"
						+ "   }"
						+ " },"
						+ " 'filter': {"
						+ "   'custom-elision': {"
						+ "     'type': 'elision',"
						+ "     'articles': ['l', 'd']"
						+ "   }"
						+ " }"
						+ "}",
				elasticSearchClient.index( index.name() ).settings( "index.analysis" ).get()
		);
	}

	private void setupAndUpdateIndex() {
		setupHelper.start()
				.withSchemaManagement( StubMappingSchemaManagementStrategy.DROP_ON_SHUTDOWN_ONLY )
				.withBackendProperty(
						ElasticsearchIndexSettings.ANALYSIS_CONFIGURER,
						new ElasticsearchIndexSchemaManagerNormalizerITAnalysisConfigurer()
				)
				.withIndex( index )
				.setup();

		index.schemaManager().createOrUpdate( OperationSubmitter.blocking() ).join();
	}

}
