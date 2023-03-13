/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.documentation.mapper.orm.schema.management;

import static org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmUtils.with;

import java.util.List;
import java.util.Optional;
import javax.persistence.EntityManagerFactory;

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

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import com.google.gson.JsonObject;

public class ElasticsearchHibernateOrmSchemaManagerIT {

	@Rule
	public DocumentationSetupHelper setupHelper = DocumentationSetupHelper.withSingleBackend(
			BackendConfigurations.simple() );

	private EntityManagerFactory entityManagerFactory;

	@Before
	public void setup() {
		this.entityManagerFactory = setupHelper.start()
				.withProperty(
						HibernateOrmMapperSettings.SCHEMA_MANAGEMENT_STRATEGY,
						SchemaManagementStrategyName.NONE
				)
				.withProperty( HibernateOrmMapperSettings.AUTOMATIC_INDEXING_ENABLED, false )
				.setup( Book.class, Author.class );
	}

	@Test
	public void walkingTheSchemaWithExtension() {
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
