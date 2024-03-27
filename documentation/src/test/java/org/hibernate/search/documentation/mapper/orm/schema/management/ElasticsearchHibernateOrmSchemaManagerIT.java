/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.documentation.mapper.orm.schema.management;

import static org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmUtils.with;

import java.util.List;
import java.util.Optional;

import jakarta.persistence.EntityManagerFactory;

import org.hibernate.search.backend.elasticsearch.ElasticsearchExtension;
import org.hibernate.search.documentation.testsupport.BackendConfigurations;
import org.hibernate.search.documentation.testsupport.DocumentationSetupHelper;
import org.hibernate.search.engine.common.schema.management.SchemaExport;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.cfg.HibernateOrmMapperSettings;
import org.hibernate.search.mapper.orm.schema.management.SchemaManagementStrategyName;
import org.hibernate.search.mapper.orm.schema.management.SearchSchemaManager;
import org.hibernate.search.mapper.orm.session.SearchSession;
import org.hibernate.search.mapper.pojo.schema.management.SearchSchemaCollector;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.google.gson.JsonObject;

class ElasticsearchHibernateOrmSchemaManagerIT {

	@RegisterExtension
	public DocumentationSetupHelper setupHelper = DocumentationSetupHelper.withSingleBackend(
			BackendConfigurations.simple() );

	private EntityManagerFactory entityManagerFactory;

	@BeforeEach
	void setup() {
		this.entityManagerFactory = setupHelper.start()
				.withProperty(
						HibernateOrmMapperSettings.SCHEMA_MANAGEMENT_STRATEGY,
						SchemaManagementStrategyName.NONE
				)
				.withProperty( HibernateOrmMapperSettings.INDEXING_LISTENERS_ENABLED, false )
				.setup( Book.class, Author.class );
	}

	@Test
	void walkingTheSchemaWithExtension() {
		with( entityManagerFactory ).runNoTransaction( entityManager -> {
			SearchSession searchSession = Search.session( entityManager );
			SearchSchemaManager schemaManager = searchSession.schemaManager();
			schemaManager.exportExpectedSchema(
					// tag::walking-the-schema-extension[]
					new SearchSchemaCollector() {

						@Override
						public void indexSchema(Optional<String> backendName, String indexName, SchemaExport export) {
							List<JsonObject> bodyParts = export
									.extension( ElasticsearchExtension.get() ) // <1>
									.bodyParts(); // <2>
						}
					}
			// end::walking-the-schema-extension[]
			);
		} );
	}
}
