/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.test;

import java.io.Serializable;
import java.util.Collections;
import java.util.Locale;

import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.backend.LuceneWork;
import org.hibernate.search.bridge.util.impl.ContextualExceptionBridgeHelper;
import org.hibernate.search.cfg.Environment;
import org.hibernate.search.elasticsearch.cfg.ElasticsearchEnvironment;
import org.hibernate.search.elasticsearch.cfg.IndexSchemaManagementStrategy;
import org.hibernate.search.elasticsearch.client.impl.JestClient;
import org.hibernate.search.engine.integration.impl.ExtendedSearchIntegrator;
import org.hibernate.search.engine.service.spi.ServiceManager;
import org.hibernate.search.engine.service.spi.ServiceReference;
import org.hibernate.search.engine.spi.DocumentBuilderIndexedEntity;
import org.hibernate.search.engine.spi.EntityIndexBinding;
import org.hibernate.search.query.engine.spi.HSQuery;
import org.hibernate.search.spi.DefaultInstanceInitializer;
import org.hibernate.search.testsupport.TestForIssue;
import org.hibernate.search.testsupport.junit.SearchFactoryHolder;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;

import io.searchbox.indices.settings.UpdateSettings;
import org.apache.lucene.search.MatchAllDocsQuery;

public class ElasticsearchFlushIT {

	@Rule
	public SearchFactoryHolder sfHolder = new SearchFactoryHolder( Entity1.class, Entity2.class )
			.withProperty( "hibernate.search.default." + Environment.INDEX_MANAGER_IMPL_NAME, "elasticsearch" )
			.withProperty( "hibernate.search.default." + ElasticsearchEnvironment.REFRESH_AFTER_WRITE, "false" )
			.withProperty(
					"hibernate.search.default." + ElasticsearchEnvironment.INDEX_SCHEMA_MANAGEMENT_STRATEGY,
					IndexSchemaManagementStrategy.RECREATE_DELETE.name()
			)
			.withIdProvidedImplicit( true );


	@Test
	@TestForIssue(jiraKey = "HSEARCH-2491")
	public void testFlushByEntity() throws Exception {

		increaseRefreshTime( Entity1.class, Entity2.class );

		Entity1 entity1 = new Entity1( 1 );
		Entity2 entity2 = new Entity2( 1 );

		indexAsStream( "E1:1", entity1 );
		indexAsStream( "E2:1", entity2 );

		assertIndexed( Entity1.class, 0 );
		assertIndexed( Entity2.class, 0 );

		flush( Entity1.class );

		assertIndexed( Entity1.class, 1 );
		assertIndexed( Entity2.class, 0 );

		flush( Entity2.class );

		assertIndexed( Entity1.class, 1 );
		assertIndexed( Entity2.class, 1 );
	}


	private void increaseRefreshTime(Class<?>... indexes) {
		ServiceManager serviceManager = sfHolder.getSearchFactory().getServiceManager();
		try (ServiceReference<JestClient> client = serviceManager.requestReference( JestClient.class )) {
			JestClient jestClient = client.get();
			String body = "{ \"index\" : { \"refresh_interval\" : \"3600s\"} }";
			UpdateSettings.Builder builder = new UpdateSettings.Builder( body );
			for ( Class<?> index : indexes ) {
				builder.addIndex( index.getName().toLowerCase( Locale.getDefault() ) );
			}
			jestClient.executeRequest( builder.build() );
		}
	}

	private void flush(Class<?> clazz) {
		sfHolder.getBatchBackend().flush( Collections.<Class<?>>singleton( clazz ) );
	}

	private void indexAsStream(Serializable id, Object entity) throws InterruptedException {
		LuceneWork work = createUpdateWork( id, entity );
		sfHolder.getBatchBackend().enqueueAsyncWork( work );
	}

	private LuceneWork createUpdateWork(Serializable id, Object entity) {
		Class clazz = entity.getClass();
		ExtendedSearchIntegrator searchFactory = sfHolder.getSearchFactory();
		EntityIndexBinding entityIndexBinding = searchFactory.getIndexBinding( clazz );
		DocumentBuilderIndexedEntity docBuilder = entityIndexBinding.getDocumentBuilder();
		return docBuilder.createUpdateWork(
				null,
				clazz,
				entity,
				id,
				id.toString(),
				DefaultInstanceInitializer.DEFAULT_INITIALIZER,
				new ContextualExceptionBridgeHelper()
		);
	}

	private void assertIndexed(Class<?> entity, int count) {
		HSQuery hsQuery = sfHolder.getSearchFactory().createHSQuery( new MatchAllDocsQuery(), entity );
		Assert.assertEquals( count, hsQuery.queryResultSize() );
	}

	@Indexed
	@SuppressWarnings("unused")
	private static class Entity1 {

		@Field
		private int id;

		Entity1(int id) {
			this.id = id;
		}
	}

	@Indexed
	@SuppressWarnings("unused")
	private static class Entity2 {

		@Field
		private int id;

		Entity2(int id) {
			this.id = id;
		}
	}

}
