/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.mapper.orm.model;

import static org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmUtils.with;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;

import org.hibernate.SessionFactory;
import org.hibernate.search.engine.backend.analysis.AnalyzerNames;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FullTextField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.util.impl.integrationtest.common.extension.BackendMock;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmSetupHelper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class LobFieldIT {

	@RegisterExtension
	public BackendMock backendMock = BackendMock.create();

	@RegisterExtension
	public OrmSetupHelper ormSetupHelper = OrmSetupHelper.withBackendMock( backendMock );

	private SessionFactory sessionFactory;

	@BeforeEach
	void setup() {
		backendMock.expectSchema( LobEntity.INDEX, b -> b
				.field( "lobText", String.class, b2 -> b2.analyzerName( AnalyzerNames.DEFAULT ) )
		);

		sessionFactory = ormSetupHelper.start().setup( LobEntity.class );
		backendMock.verifyExpectationsMet();
	}

	@Test
	void index() {
		with( sessionFactory ).runInTransaction( session -> {
			LobEntity entity = new LobEntity();
			entity.lobText = "this text is very long ...";

			session.persist( entity );

			backendMock.expectWorks( LobEntity.INDEX )
					.add( String.valueOf( entity.id ), b -> b
							.field( "lobText", "this text is very long ..." )
					);
		} );
		backendMock.verifyExpectationsMet();
	}

	@Test
	void update() {
		with( sessionFactory ).runInTransaction( session -> {
			LobEntity entity = new LobEntity();
			entity.lobText = "initial text";

			session.persist( entity );

			backendMock.expectWorks( LobEntity.INDEX )
					.add( String.valueOf( entity.id ), b -> b
							.field( "lobText", "initial text" )
					);
		} );
		backendMock.verifyExpectationsMet();

		with( sessionFactory ).runInTransaction( session -> {
			LobEntity entity = session.find( LobEntity.class, 1 );
			entity.lobText = "updated text";

			backendMock.expectWorks( LobEntity.INDEX )
					.addOrUpdate( "1", b -> b
							.field( "lobText", "updated text" )
					);
		} );
		backendMock.verifyExpectationsMet();
	}

	@Entity(name = LobEntity.NAME)
	@Indexed(index = LobEntity.INDEX)
	public static class LobEntity {
		static final String NAME = "LobEntity";
		static final String INDEX = "LobEntity";

		@Id
		@GeneratedValue
		public Integer id;

		@FullTextField
		@Lob
		public String lobText;
	}
}
