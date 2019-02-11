/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.index.admin.impl;

import org.hibernate.search.backend.elasticsearch.orchestration.impl.ElasticsearchWorkOrchestrator;
import org.hibernate.search.backend.elasticsearch.util.spi.URLEncodedString;
import org.hibernate.search.backend.elasticsearch.work.builder.factory.impl.ElasticsearchWorkBuilderFactory;
import org.hibernate.search.engine.logging.spi.ContextualFailureCollector;

/**
 * The administration client for a given Elasticsearch index.
 * <p>
 * This interface is split from the rest of the code because we may one day expose these operations to users
 * (so that they can manage index updates more finely).
 */
public class ElasticsearchIndexAdministrationClient {

	private final ElasticsearchSchemaCreator schemaCreator;
	private final ElasticsearchSchemaDropper schemaDropper;
	private final ElasticsearchSchemaValidator schemaValidator;
	private final ElasticsearchSchemaMigrator schemaMigrator;

	private final URLEncodedString elasticsearchIndexName;
	private final IndexMetadata expectedMetadata;

	public ElasticsearchIndexAdministrationClient(ElasticsearchWorkBuilderFactory workFactory,
			ElasticsearchWorkOrchestrator workOrchestrator,
			URLEncodedString elasticsearchIndexName,
			IndexMetadata expectedMetadata) {
		ElasticsearchSchemaAccessor schemaAccessor = new ElasticsearchSchemaAccessor( workFactory, workOrchestrator );

		this.schemaCreator = new ElasticsearchSchemaCreatorImpl( schemaAccessor );
		this.schemaDropper = new ElasticsearchSchemaDropperImpl( schemaAccessor );
		this.schemaValidator = new ElasticsearchSchemaValidatorImpl( schemaAccessor );
		this.schemaMigrator = new ElasticsearchSchemaMigratorImpl( schemaAccessor, schemaValidator );

		this.elasticsearchIndexName = elasticsearchIndexName;
		this.expectedMetadata = expectedMetadata;
	}

	public void createIfAbsent(ElasticsearchIndexLifecycleExecutionOptions executionOptions) {
		schemaCreator.createIndexIfAbsent( expectedMetadata, executionOptions );
	}

	public void dropAndCreate(ElasticsearchIndexLifecycleExecutionOptions executionOptions) {
		schemaDropper.dropIfExisting( elasticsearchIndexName );
		schemaCreator.createIndex( expectedMetadata, executionOptions );
	}

	public void dropIfExisting(ElasticsearchIndexLifecycleExecutionOptions executionOptions) {
		schemaDropper.dropIfExisting( elasticsearchIndexName );
	}

	public void update(ElasticsearchIndexLifecycleExecutionOptions executionOptions) {
		boolean createdIndex = schemaCreator.createIndexIfAbsent( expectedMetadata, executionOptions );
		if ( !createdIndex ) {
			schemaMigrator.migrate( expectedMetadata );
		}
	}

	public void validate(ElasticsearchIndexLifecycleExecutionOptions executionOptions,
			ContextualFailureCollector failureCollector) {
		schemaCreator.checkIndexExists( elasticsearchIndexName, executionOptions );
		schemaValidator.validate( expectedMetadata, failureCollector );
	}
}
