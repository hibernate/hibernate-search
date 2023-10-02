/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.mapper.orm.multitenancy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.search.util.impl.integrationtest.common.stub.backend.StubBackendUtils.reference;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.search.engine.backend.common.DocumentReference;
import org.hibernate.search.engine.common.EntityReference;
import org.hibernate.search.engine.search.query.SearchQuery;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.util.impl.integrationtest.common.extension.BackendMock;
import org.hibernate.search.util.impl.integrationtest.common.extension.StubSearchWorkBehavior;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmSetupHelper;
import org.hibernate.search.util.impl.test.annotation.PortedFromSearch5;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;
import org.hibernate.search.util.impl.test.extension.parameterized.ParameterizedPerClass;
import org.hibernate.search.util.impl.test.extension.parameterized.ParameterizedSetup;
import org.hibernate.search.util.impl.test.extension.parameterized.ParameterizedSetupBeforeTest;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@TestForIssue(jiraKey = "HSEARCH-4034")
@PortedFromSearch5(original = "org.hibernate.search.test.batchindexing.DatabaseMultitenancyTest")
@ParameterizedPerClass
class DatabaseMultitenancyIT {

	@RegisterExtension
	public static BackendMock backendMock = BackendMock.create();

	@RegisterExtension
	public static OrmSetupHelper ormSetupHelper = OrmSetupHelper.withBackendMock( backendMock );

	private Object tenant1;
	private Object tenant2;

	public static List<? extends Arguments> params() {
		return List.of(
				Arguments.of( "metamec", "geochron" ),
				Arguments.of( 1, 2 ),
				Arguments.of( UUID.fromString( "55555555-7777-6666-9999-000000000001" ),
						UUID.fromString( "55555555-7777-6666-9999-000000000002" ) )
		);
	}

	@ParameterizedSetup
	@MethodSource("params")
	void setup(Object tenant1, Object tenant2) {
		this.tenant1 = tenant1;
		this.tenant2 = tenant2;

		backendMock.expectSchema( Clock.INDEX, b -> b.field( "brand", String.class ) );
		sessionFactory = ormSetupHelper
				.start()
				.tenantsWithHelperEnabled( tenant1, tenant2 )
				.setup( Clock.class );
		backendMock.verifyExpectationsMet();
	}

	private static Clock[] METAMEC_MODELS = {
			new Clock( 1, "Metamec - Model A850" ),
			new Clock( 2, "Metamec - Model 4562" ),
			new Clock( 5, "Metamec - Model 792" )
	};

	private static Clock[] GEOCHRON_MODELS = {
			new Clock( 1, "Geochron - Model The Original Kilburg" ),
			new Clock( 2, "Geochron - Model The Boardroom" ),
			new Clock( 9, "Geochron - Model Designer Series" )
	};

	private SessionFactory sessionFactory;

	@ParameterizedSetupBeforeTest
	void setup() {
		// init data:
		persist( tenant1, METAMEC_MODELS );
		persist( tenant2, GEOCHRON_MODELS );
	}

	@Test
	void shouldOnlyFindMetamecModels() {
		List<Clock> list = searchAll( tenant1, METAMEC_MODELS );
		assertThat( list ).containsExactlyInAnyOrder( METAMEC_MODELS );
	}

	@Test
	void shouldOnlyFindGeochronModels() {
		List<Clock> list = searchAll( tenant2, GEOCHRON_MODELS );
		assertThat( list ).containsExactlyInAnyOrder( GEOCHRON_MODELS );
	}

	@Test
	void shouldMatchOnlyElementsFromOneTenant() {
		List<Clock> list = searchModel( "model", tenant2, GEOCHRON_MODELS );
		assertThat( list ).containsExactlyInAnyOrder( GEOCHRON_MODELS );
	}

	@Test
	void searchReferences() {
		assertThat( searchReferences( tenant2, GEOCHRON_MODELS ) ).hasSize( GEOCHRON_MODELS.length );
	}

	private void persist(Object tenantId, Clock[] models) {
		BackendMock.DocumentWorkCallListContext expectWorks = backendMock.expectWorks( Clock.INDEX, tenantId );
		for ( Clock clock : models ) {
			expectWorks.add( clock.getId() + "", b -> b.field( "brand", clock.getBrand() ) );
		}

		try ( Session session = openSessionWithTenantId( tenantId ) ) {
			session.beginTransaction();
			for ( Object clock : models ) {
				session.persist( clock );
			}
			session.getTransaction().commit();
			session.clear();
		}

		backendMock.verifyExpectationsMet();
	}

	private List<Clock> searchAll(Object tenantId, Clock[] models) {
		try ( Session session = openSessionWithTenantId( tenantId ) ) {
			SearchQuery<Clock> query = Search.session( session )
					.search( Clock.class )
					.where( f -> f.matchAll() )
					.toQuery();

			backendMock.expectSearchObjects( Clock.INDEX,
					StubSearchWorkBehavior.of( 1, documentReferencesOf( models ) )
			);

			return query.fetchAllHits();
		}
	}

	private List<Clock> searchModel(String searchString, Object tenantId, Clock[] models) {
		try ( Session session = openSessionWithTenantId( tenantId ) ) {
			SearchQuery<Clock> query = Search.session( session )
					.search( Clock.class )
					.where( f -> f.match().field( "brand" ).matching( searchString ) )
					.toQuery();

			backendMock.expectSearchObjects( Clock.INDEX,
					StubSearchWorkBehavior.of( 1, documentReferencesOf( models ) )
			);

			return query.fetchAllHits();
		}
	}

	private List<? extends EntityReference> searchReferences(Object tenantId, Clock[] models) {
		try ( Session session = openSessionWithTenantId( tenantId ) ) {
			SearchQuery<? extends EntityReference> query = Search.session( session )
					.search( Clock.class )
					.selectEntityReference()
					.where( f -> f.matchAll() )
					.toQuery();

			backendMock.expectSearchReferences( Collections.singletonList( Clock.INDEX ),
					StubSearchWorkBehavior.of( 1, documentReferencesOf( models ) )
			);

			return query.fetchAllHits();
		}
	}

	private Session openSessionWithTenantId(Object tid) {
		return sessionFactory.withOptions().tenantIdentifier( tid ).openSession();
	}

	private DocumentReference[] documentReferencesOf(Clock[] models) {
		DocumentReference[] result = new DocumentReference[models.length];
		for ( int i = 0; i < models.length; i++ ) {
			result[i] = reference( Clock.INDEX, models[i].getId() + "" );
		}
		return result;
	}
}
