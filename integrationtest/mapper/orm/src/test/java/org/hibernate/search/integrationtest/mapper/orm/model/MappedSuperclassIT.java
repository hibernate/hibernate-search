/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.mapper.orm.model;

import static org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmUtils.with;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;

import org.hibernate.SessionFactory;
import org.hibernate.search.engine.backend.analysis.AnalyzerNames;
import org.hibernate.search.mapper.orm.cfg.HibernateOrmMapperSettings;
import org.hibernate.search.mapper.orm.mapping.HibernateOrmSearchMappingConfigurer;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.ProgrammaticMappingConfigurationContext;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.TypeMappingStep;
import org.hibernate.search.util.impl.integrationtest.common.extension.BackendMock;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmSetupHelper;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

@TestForIssue(jiraKey = "HSEARCH-1108")
class MappedSuperclassIT {

	private static final String INDEX_NAME = "IndexedEntity";

	@RegisterExtension
	public BackendMock backendMock = BackendMock.create();

	@RegisterExtension
	public OrmSetupHelper ormSetupHelper = OrmSetupHelper.withBackendMock( backendMock );

	private SessionFactory sessionFactory;

	@BeforeEach
	void setup() {
		backendMock.expectSchema(
				INDEX_NAME, b -> b.field( "text", String.class, f -> f.analyzerName( AnalyzerNames.DEFAULT ) ) );

		sessionFactory = ormSetupHelper.start()
				.withProperty( HibernateOrmMapperSettings.MAPPING_CONFIGURER, (HibernateOrmSearchMappingConfigurer) context -> {
					ProgrammaticMappingConfigurationContext mapping = context.programmaticMapping();
					TypeMappingStep indexedEntityMapping = mapping.type( IndexedEntity.class );
					indexedEntityMapping.indexed().index( INDEX_NAME );
					indexedEntityMapping.property( "id" ).documentId();
					indexedEntityMapping.property( "text" ).fullTextField();
				} )
				.setup( IndexedEntity.class );
		backendMock.verifyExpectationsMet();
	}

	@Test
	void index() {
		with( sessionFactory ).runInTransaction( session -> {
			IndexedEntity indexedPojo = new IndexedEntity( 1, "Using some text here" );
			session.persist( indexedPojo );

			backendMock.expectWorks( IndexedEntity.class.getSimpleName() )
					.add( "1", b -> b.field( "text", "Using some text here" ) );
		} );
	}

	@MappedSuperclass
	public abstract static class TextEntity {

		protected String text;

		public String getText() {
			return text;
		}
	}

	@Entity(name = "IndexedEntity")
	public static class IndexedEntity extends TextEntity {

		@Id
		private Integer id;

		private IndexedEntity() {
		}

		public IndexedEntity(Integer id, String text) {
			this.id = id;
			this.text = text;
		}

		public Integer getId() {
			return id;
		}
	}
}
