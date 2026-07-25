/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.mapper.orm.standalone;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.search.util.impl.integrationtest.common.stub.backend.StubBackendUtils.reference;
import static org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmUtils.with;

import java.lang.invoke.MethodHandles;
import java.util.List;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import org.hibernate.SessionFactory;
import org.hibernate.search.engine.search.query.SearchQuery;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.cfg.HibernateOrmMapperSettings;
import org.hibernate.search.mapper.orm.mapping.HibernateOrmSearchMappingConfigurer;
import org.hibernate.search.mapper.orm.session.SearchSession;
import org.hibernate.search.mapper.pojo.common.spi.PojoEntityReference;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.SearchEntity;
import org.hibernate.search.mapper.pojo.standalone.mapping.SearchMapping;
import org.hibernate.search.util.impl.integrationtest.common.extension.BackendMock;
import org.hibernate.search.util.impl.integrationtest.common.extension.StubSearchWorkBehavior;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmSetupHelper;
import org.hibernate.search.util.impl.integrationtest.mapper.pojo.standalone.StandalonePojoMappingSetupHelper;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class DualMapperIT {

	@RegisterExtension
	public BackendMock ormBackendMock = BackendMock.create();

	@RegisterExtension
	public BackendMock standaloneBackendMock = BackendMock.create();

	@RegisterExtension
	public OrmSetupHelper ormSetupHelper = OrmSetupHelper.withBackendMock( ormBackendMock );

	@RegisterExtension
	public StandalonePojoMappingSetupHelper standaloneSetupHelper =
			StandalonePojoMappingSetupHelper.withBackendMock( MethodHandles.lookup(), standaloneBackendMock );

	@Test
	void bothMappersCoexist() {
		ormBackendMock.expectSchema( OrmIndexedEntity.INDEX, b -> b
				.field( "text", String.class )
		);
		standaloneBackendMock.expectSchema( StandaloneIndexedEntity.INDEX, b -> b
				.field( "name", String.class )
		);

		SessionFactory sessionFactory = ormSetupHelper.start()
				.withProperty(
						HibernateOrmMapperSettings.MAPPING_CONFIGURER,
						(HibernateOrmSearchMappingConfigurer) context -> {
							context.annotationMapping().add( StandaloneIndexedEntity.class );
						}
				)
				.setup( OrmIndexedEntity.class );
		ormBackendMock.verifyExpectationsMet();

		SearchMapping standaloneMapping = standaloneSetupHelper.start()
				.withAnnotatedTypes( StandaloneIndexedEntity.class )
				.setup();
		standaloneBackendMock.verifyExpectationsMet();

		// Index an ORM entity via persist
		with( sessionFactory ).runInTransaction( session -> {
			OrmIndexedEntity entity = new OrmIndexedEntity();
			entity.setId( 1 );
			entity.setText( "hello from ORM" );
			session.persist( entity );

			ormBackendMock.expectWorks( OrmIndexedEntity.INDEX )
					.add( "1", b -> b.field( "text", "hello from ORM" ) );
		} );
		ormBackendMock.verifyExpectationsMet();

		// Index a standalone entity via indexing plan
		try ( org.hibernate.search.mapper.pojo.standalone.session.SearchSession standaloneSession =
				standaloneMapping.createSession() ) {
			StandaloneIndexedEntity entity = new StandaloneIndexedEntity();
			entity.setId( 1 );
			entity.setName( "hello from standalone" );
			standaloneSession.indexingPlan().add( entity );

			standaloneBackendMock.expectWorks( StandaloneIndexedEntity.INDEX )
					.add( "1", b -> b.field( "name", "hello from standalone" ) );
		}
		standaloneBackendMock.verifyExpectationsMet();

		// Search via ORM mapper
		with( sessionFactory ).runInTransaction( session -> {
			SearchSession searchSession = Search.session( session );
			SearchQuery<OrmIndexedEntity> query = searchSession.search( OrmIndexedEntity.class )
					.selectEntity()
					.where( f -> f.matchAll() )
					.toQuery();

			ormBackendMock.expectSearchObjects(
					OrmIndexedEntity.INDEX,
					StubSearchWorkBehavior.of( 1L, reference( OrmIndexedEntity.INDEX, "1" ) )
			);

			List<OrmIndexedEntity> hits = query.fetchAllHits();
			assertThat( hits ).hasSize( 1 );
			assertThat( hits.get( 0 ).getText() ).isEqualTo( "hello from ORM" );
		} );
		ormBackendMock.verifyExpectationsMet();

		// Search via standalone mapper
		try ( org.hibernate.search.mapper.pojo.standalone.session.SearchSession standaloneSession =
				standaloneMapping.createSession() ) {
			SearchQuery<org.hibernate.search.engine.common.EntityReference> query =
					standaloneSession.search( StandaloneIndexedEntity.class )
							.selectEntityReference()
							.where( f -> f.matchAll() )
							.toQuery();

			standaloneBackendMock.expectSearchObjects(
					StandaloneIndexedEntity.INDEX,
					StubSearchWorkBehavior.of( 1L, reference( StandaloneIndexedEntity.INDEX, "1" ) )
			);

			assertThat( query.fetchAllHits() ).containsExactly(
					PojoEntityReference.withName( StandaloneIndexedEntity.class,
							StandaloneIndexedEntity.class.getSimpleName(), 1 )
			);
		}
		standaloneBackendMock.verifyExpectationsMet();
	}

	@Entity(name = "OrmIndexedEntity")
	@Indexed(index = OrmIndexedEntity.INDEX)
	public static class OrmIndexedEntity {
		public static final String INDEX = "OrmIndexedEntity";

		@Id
		private Integer id;

		@GenericField
		private String text;

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public String getText() {
			return text;
		}

		public void setText(String text) {
			this.text = text;
		}
	}

	@SearchEntity
	@Indexed(index = StandaloneIndexedEntity.INDEX)
	public static class StandaloneIndexedEntity {
		public static final String INDEX = "StandaloneIndexedEntity";

		@DocumentId
		private Integer id;

		@GenericField
		private String name;

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}
}
