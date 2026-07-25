/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.mapper.orm.mapping.definition;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import org.hibernate.search.mapper.orm.cfg.HibernateOrmMapperSettings;
import org.hibernate.search.mapper.orm.mapping.HibernateOrmSearchMappingConfigurer;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.SearchEntity;
import org.hibernate.search.util.impl.integrationtest.common.extension.BackendMock;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmSetupHelper;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class SearchEntityWithOrmMapperIT {

	@RegisterExtension
	public BackendMock backendMock = BackendMock.create();

	@RegisterExtension
	public OrmSetupHelper ormSetupHelper = OrmSetupHelper.withBackendMock( backendMock );

	@Test
	void searchEntityIgnoredByOrmMapper() {
		backendMock.expectSchema( OrmIndexedEntity.INDEX, b -> b
				.field( "text", String.class )
		);

		ormSetupHelper.start()
				.withProperty(
						HibernateOrmMapperSettings.MAPPING_CONFIGURER,
						(HibernateOrmSearchMappingConfigurer) context -> {
							context.annotationMapping().add( StandaloneOnlyEntity.class );
						}
				)
				.setup( OrmIndexedEntity.class );
		backendMock.verifyExpectationsMet();
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
	@Indexed(index = StandaloneOnlyEntity.INDEX)
	public static class StandaloneOnlyEntity {
		public static final String INDEX = "StandaloneOnlyEntity";

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
