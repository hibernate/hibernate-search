/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.documentation.search.converter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmUtils.with;

import java.util.List;

import jakarta.persistence.Basic;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;

import org.hibernate.search.documentation.testsupport.BackendConfigurations;
import org.hibernate.search.documentation.testsupport.DocumentationSetupHelper;
import org.hibernate.search.engine.search.common.ValueConvert;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.session.SearchSession;
import org.hibernate.search.mapper.pojo.bridge.ValueBridge;
import org.hibernate.search.mapper.pojo.bridge.mapping.annotation.ValueBridgeRef;
import org.hibernate.search.mapper.pojo.bridge.runtime.ValueBridgeToIndexedValueContext;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FullTextField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class DslConverterIT {
	@RegisterExtension
	public DocumentationSetupHelper setupHelper = DocumentationSetupHelper.withSingleBackend( BackendConfigurations.simple() );

	private EntityManagerFactory entityManagerFactory;

	@BeforeEach
	void setup() {
		entityManagerFactory = setupHelper.start().setup( AuthenticationEvent.class );
		initData();
	}

	@Test
	void dslConverterEnabled() {
		with( entityManagerFactory ).runInTransaction( entityManager -> {
			SearchSession searchSession = Search.session( entityManager );

			// tag::dsl-converter-enabled[]
			List<AuthenticationEvent> result = searchSession.search( AuthenticationEvent.class )
					.where( f -> f.match().field( "outcome" )
							.matching( AuthenticationOutcome.INVALID_PASSWORD ) )
					.fetchHits( 20 );
			// end::dsl-converter-enabled[]

			assertThat( result )
					.extracting( "id" )
					.containsExactly( 2 );
		} );
	}

	@Test
	void dslConverterDisabled() {
		with( entityManagerFactory ).runInTransaction( entityManager -> {
			SearchSession searchSession = Search.session( entityManager );

			// tag::dsl-converter-disabled[]
			List<AuthenticationEvent> result = searchSession.search( AuthenticationEvent.class )
					.where( f -> f.match().field( "outcome" )
							.matching( "Invalid password", ValueConvert.NO ) )
					.fetchHits( 20 );
			// end::dsl-converter-disabled[]

			assertThat( result )
					.extracting( "id" )
					.containsExactly( 2 );
		} );
	}

	private void initData() {
		with( entityManagerFactory ).runInTransaction( entityManager -> {
			AuthenticationEvent event1 = new AuthenticationEvent( 1 );
			event1.setOutcome( AuthenticationOutcome.USER_NOT_FOUND );
			AuthenticationEvent event2 = new AuthenticationEvent( 2 );
			event2.setOutcome( AuthenticationOutcome.INVALID_PASSWORD );

			entityManager.persist( event1 );
			entityManager.persist( event2 );
		} );
	}

	@Entity(name = "AuthenticationEvent")
	@Indexed
	public static class AuthenticationEvent {
		@Id
		private Integer id;
		@Basic
		@Enumerated
		@FullTextField(
				analyzer = "english",
				valueBridge = @ValueBridgeRef(type = AuthenticationOutcomeBridge.class)
		)
		private AuthenticationOutcome outcome;

		protected AuthenticationEvent() {
			// For Hibernate ORM
		}

		public AuthenticationEvent(int id) {
			this.id = id;
		}

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public AuthenticationOutcome getOutcome() {
			return outcome;
		}

		public void setOutcome(AuthenticationOutcome outcome) {
			this.outcome = outcome;
		}
	}

	private enum AuthenticationOutcome {
		USER_NOT_FOUND( "User not found" ),
		INVALID_PASSWORD( "Invalid password" );

		private final String text;

		private AuthenticationOutcome(String text) {
			this.text = text;
		}
	}

	public static class AuthenticationOutcomeBridge implements ValueBridge<AuthenticationOutcome, String> {
		@Override
		public String toIndexedValue(AuthenticationOutcome value, ValueBridgeToIndexedValueContext context) {
			return value == null ? null : value.text;
		}
	}

}
