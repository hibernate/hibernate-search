/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.orm.outbox;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.search.mapper.orm.outbox.impl.OutboxAdditionalJaxbMappingProducer.ENTITY_ID_PROPERTY_NAME;
import static org.hibernate.search.mapper.orm.outbox.impl.OutboxAdditionalJaxbMappingProducer.ENTITY_NAME_PROPERTY_NAME;
import static org.hibernate.search.mapper.orm.outbox.impl.OutboxAdditionalJaxbMappingProducer.OUTBOX_ENTITY_NAME;
import static org.hibernate.search.mapper.orm.outbox.impl.OutboxAdditionalJaxbMappingProducer.ROUTE_PROPERTY_NAME;

import java.util.List;
import java.util.Map;
import javax.persistence.Entity;
import javax.persistence.Id;

import org.hibernate.SessionFactory;
import org.hibernate.query.Query;
import org.hibernate.search.engine.backend.analysis.AnalyzerNames;
import org.hibernate.search.mapper.orm.cfg.HibernateOrmMapperSettings;
import org.hibernate.search.mapper.orm.mapping.HibernateOrmSearchMappingConfigurer;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.ProgrammaticMappingConfigurationContext;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.TypeMappingStep;
import org.hibernate.search.mapper.pojo.route.DocumentRoutesDescriptor;
import org.hibernate.search.util.common.serialization.spi.SerializationUtils;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendMock;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmSetupHelper;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmUtils;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class OutboxTableAutomaticIndexingStrategyIT {

	private static final String INDEX_NAME = "IndexedEntity";

	@Rule
	public BackendMock backendMock = new BackendMock();

	@Rule
	public OrmSetupHelper ormSetupHelper = OrmSetupHelper.withBackendMock( backendMock );

	private SessionFactory sessionFactory;

	@Before
	public void setup() {
		backendMock.expectSchema(
				INDEX_NAME, b -> b.field( "text", String.class, f -> f.analyzerName( AnalyzerNames.DEFAULT ) ) );

		sessionFactory = ormSetupHelper.start()
				.withProperty(
						HibernateOrmMapperSettings.MAPPING_CONFIGURER,
						(HibernateOrmSearchMappingConfigurer) context -> {
							ProgrammaticMappingConfigurationContext mapping = context.programmaticMapping();
							TypeMappingStep indexedEntityMapping = mapping.type( IndexedEntity.class );
							indexedEntityMapping.indexed().index( INDEX_NAME );
							indexedEntityMapping.property( "id" ).documentId();
							indexedEntityMapping.property( "text" ).fullTextField();
						}
				)
				.withProperty(
						"hibernate.search.automatic_indexing.strategy",
						"org.hibernate.search.mapper.orm.outbox.impl.OutboxTableAutomaticIndexingStrategy"
				)
				.setup( IndexedEntity.class );
		backendMock.verifyExpectationsMet();
	}

	@Test
	public void test() {
		OrmUtils.withinTransaction( sessionFactory, session -> {
			IndexedEntity indexedPojo = new IndexedEntity( 1, "Using some text here" );
			session.save( indexedPojo );
		} );

		OrmUtils.withinTransaction( sessionFactory, session -> {
			Query<Map> query = session.createQuery( "select e from " + OUTBOX_ENTITY_NAME + " e", Map.class );

			List<Map> list = query.list();
			assertThat( list ).hasSize( 1 );

			Map<String, Object> load = list.get( 0 );
			assertThat( load ).containsEntry( ENTITY_NAME_PROPERTY_NAME, "IndexedEntity" );
			assertThat( load ).containsEntry( ENTITY_ID_PROPERTY_NAME, "1" );

			byte[] serializedRoutingKeys = (byte[]) load.get( ROUTE_PROPERTY_NAME );
			DocumentRoutesDescriptor routesDescriptor = SerializationUtils.deserialize(
					DocumentRoutesDescriptor.class, serializedRoutingKeys );

			assertThat( routesDescriptor ).isNotNull();
			assertThat( routesDescriptor.currentRoute().routingKey() ).isNull();
			assertThat( routesDescriptor.previousRoutes() ).isEmpty();
		} );
	}

	@Entity(name = "IndexedEntity")
	public static class IndexedEntity {

		@Id
		private Integer id;
		private String text;

		private IndexedEntity() {
		}

		public IndexedEntity(Integer id, String text) {
			this.id = id;
			this.text = text;
		}

		public Integer getId() {
			return id;
		}

		public String getText() {
			return text;
		}
	}
}
