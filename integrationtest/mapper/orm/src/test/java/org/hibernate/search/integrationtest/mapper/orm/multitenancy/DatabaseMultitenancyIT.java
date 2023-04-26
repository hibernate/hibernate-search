/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.orm.multitenancy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.search.util.impl.integrationtest.common.stub.backend.StubBackendUtils.reference;

import java.util.Collections;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.search.engine.backend.common.DocumentReference;
import org.hibernate.search.engine.common.EntityReference;
import org.hibernate.search.engine.search.query.SearchQuery;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendMock;
import org.hibernate.search.util.impl.integrationtest.common.rule.StubSearchWorkBehavior;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmSetupHelper;
import org.hibernate.search.util.impl.test.annotation.PortedFromSearch5;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

@TestForIssue(jiraKey = "HSEARCH-4034")
@PortedFromSearch5(original = "org.hibernate.search.test.batchindexing.DatabaseMultitenancyTest")
public class DatabaseMultitenancyIT {

	@Rule
	public BackendMock backendMock = new BackendMock();

	@Rule
	public OrmSetupHelper ormSetupHelper = OrmSetupHelper.withBackendMock( backendMock );

	/**
	 * Metamec tenant identifier
	 */
	private static final String METAMEC_TID = "metamec";

	/**
	 * Geochron tenant identifier
	 */
	private static final String GEOCHRON_TID = "geochron";

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

	@Before
	public void setup() {
		backendMock.expectSchema( Clock.INDEX, b -> b.field( "brand", String.class ) );
		sessionFactory = ormSetupHelper
				.start()
				.tenants( METAMEC_TID, GEOCHRON_TID )
				.setup( Clock.class );
		backendMock.verifyExpectationsMet();

		// init data:
		persist( METAMEC_TID, METAMEC_MODELS );
		persist( GEOCHRON_TID, GEOCHRON_MODELS );
	}

	@Test
	public void shouldOnlyFindMetamecModels() {
		List<Clock> list = searchAll( METAMEC_TID, METAMEC_MODELS );
		assertThat( list ).containsExactlyInAnyOrder( METAMEC_MODELS );
	}

	@Test
	public void shouldOnlyFindGeochronModels() {
		List<Clock> list = searchAll( GEOCHRON_TID, GEOCHRON_MODELS );
		assertThat( list ).containsExactlyInAnyOrder( GEOCHRON_MODELS );
	}

	@Test
	public void shouldMatchOnlyElementsFromOneTenant() {
		List<Clock> list = searchModel( "model", GEOCHRON_TID, GEOCHRON_MODELS );
		assertThat( list ).containsExactlyInAnyOrder( GEOCHRON_MODELS );
	}

	@Test
	public void searchReferences() {
		assertThat( searchReferences( GEOCHRON_TID, GEOCHRON_MODELS ) ).hasSize( GEOCHRON_MODELS.length );
	}

	private void persist(String tenantId, Clock[] models) {
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

	private List<Clock> searchAll(String tenantId, Clock[] models) {
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

	private List<Clock> searchModel(String searchString, String tenantId, Clock[] models) {
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

	private List<? extends EntityReference> searchReferences(String tenantId, Clock[] models) {
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

	private Session openSessionWithTenantId(String tid) {
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
