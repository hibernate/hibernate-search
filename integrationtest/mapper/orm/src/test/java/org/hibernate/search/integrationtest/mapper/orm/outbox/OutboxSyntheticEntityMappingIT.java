/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.orm.outbox;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.metamodel.EntityType;

import org.hibernate.SessionFactory;
import org.hibernate.search.engine.backend.analysis.AnalyzerNames;
import org.hibernate.search.mapper.orm.cfg.HibernateOrmMapperSettings;
import org.hibernate.search.mapper.orm.mapping.HibernateOrmSearchMappingConfigurer;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.ProgrammaticMappingConfigurationContext;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.TypeMappingStep;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendMock;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmSetupHelper;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmUtils;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class OutboxSyntheticEntityMappingIT {

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
				.withProperty( HibernateOrmMapperSettings.FILL_OUTBOX_TABLE, true )
				.setup( IndexedEntity.class );
		backendMock.verifyExpectationsMet();
	}

	@Test
	public void index_workingAsUsual() {
		OrmUtils.withinTransaction( sessionFactory, session -> {
			IndexedEntity indexedPojo = new IndexedEntity( 1, "Using some text here" );
			session.save( indexedPojo );

			backendMock.expectWorks( IndexedEntity.class.getSimpleName() ).add( "1", b -> b
					.field( "text", "Using some text here" )
			).processedThenExecuted();
		} );
	}

	@Test
	public void saveAndLoadOutboxSyntheticEntity() {
		EntityType<?> entityType = sessionFactory.getMetamodel().getEntities().iterator().next();
		String entityName = entityType.getName();
		AtomicInteger id = new AtomicInteger();

		OrmUtils.withinTransaction( sessionFactory, session -> {
			HashMap<String, Object> entityData = new HashMap<>();
			entityData.put( "entityName", entityName );
			entityData.put( "entityId", "739" );
			entityData.put( "routingKey", "fake-routing-key" );

			session.save( "Outbox", entityData );

			@SuppressWarnings("unchecked") // this field is defined as integer
			Integer generatedId = (Integer) entityData.get( "id" );

			id.set( generatedId );
		} );

		OrmUtils.withinTransaction( sessionFactory, session -> {
			@SuppressWarnings("unchecked") // synthetic entities are loaded as map
			Map<String, Object> load = (Map<String, Object>) session.load( "Outbox", id.get() );
			assertThat( load ).isNotNull();

			assertThat( load ).containsEntry( "entityName", entityName );
			assertThat( load ).containsEntry( "entityId", "739" );
			assertThat( load ).containsEntry( "routingKey", "fake-routing-key" );
		} );
	}

	@Entity
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
