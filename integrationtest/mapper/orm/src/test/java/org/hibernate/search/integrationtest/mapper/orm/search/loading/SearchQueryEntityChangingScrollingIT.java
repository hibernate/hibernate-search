/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.orm.search.loading;

import static org.hibernate.search.util.impl.integrationtest.common.assertion.SearchResultAssert.assertThat;
import static org.hibernate.search.util.impl.integrationtest.common.stub.backend.StubBackendUtils.reference;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.query.Query;
import org.hibernate.search.engine.backend.common.DocumentReference;
import org.hibernate.search.engine.search.query.SearchQuery;
import org.hibernate.search.engine.search.query.SearchScroll;
import org.hibernate.search.engine.search.query.SearchScrollResult;
import org.hibernate.search.integrationtest.mapper.orm.search.loading.model.singletype.SimpleEntity;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.session.SearchSession;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendMock;
import org.hibernate.search.util.impl.integrationtest.common.rule.StubSearchWorkBehavior;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmSetupHelper;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmUtils;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import org.assertj.core.api.Assertions;

public class SearchQueryEntityChangingScrollingIT {

	public static final String NEW_NAME = "new-name";
	@Rule
	public BackendMock backendMock = new BackendMock();

	@Rule
	public OrmSetupHelper ormSetupHelper = OrmSetupHelper.withBackendMock( backendMock );

	private SessionFactory sessionFactory;

	@Before
	public void setup() {
		backendMock.expectAnySchema( SimpleEntity.NAME );
		sessionFactory = ormSetupHelper.start().setup( SimpleEntity.class );
		backendMock.verifyExpectationsMet();
	}

	@Test
	public void test() {
		backendMock.inLenientMode( () -> OrmUtils.withinTransaction( sessionFactory, session -> {
			for ( int i = 0; i < 12; i++ ) {
				session.persist( new SimpleEntity( i ) );
			}
		} ) );

		try ( Session session = sessionFactory.openSession() ) {
			Transaction trx = session.beginTransaction();
			SearchSession searchSession = Search.session( session );

			SearchQuery<SimpleEntity> query = searchSession.search( SimpleEntity.class )
					.where( f -> f.matchAll() )
					.toQuery();

			List<String> targetIndexes = Collections.singletonList( SimpleEntity.NAME );

			backendMock.expectScrollObjects( targetIndexes, 3, b -> {
			} );
			for ( int base = 0; base < 12; base += 3 ) {
				backendMock.expectNextScroll( targetIndexes, StubSearchWorkBehavior.of( 3, documentReferences( base, base + 1, base + 2 ) ) );
			}
			backendMock.expectNextScroll( targetIndexes, StubSearchWorkBehavior.of( 0, documentReferences() ) );
			backendMock.expectCloseScroll( targetIndexes );

			int index = 0;

			try ( SearchScroll<SimpleEntity> scroll = query.scroll( 3 ) ) {
				for ( SearchScrollResult<SimpleEntity> next = scroll.next(); next.hasHits(); next = scroll.next() ) {
					assertThat( next.hits() ).hasHitsAnyOrder( new SimpleEntity( index++ ), new SimpleEntity( index++ ), new SimpleEntity( index++ ) );
					changeNames( next.hits() );

					session.flush();
					session.clear();
				}
			}

			trx.commit();
		}

		// checking that changes above had an effect
		try ( Session session = sessionFactory.openSession() ) {
			Query<SimpleEntity> query = session.createQuery( "from SimpleEntity", SimpleEntity.class );
			Assertions.assertThat( query.getResultList() ).extracting( "name" ).contains( NEW_NAME );
		}
	}

	private static List<DocumentReference> documentReferences(int... ids) {
		ArrayList<DocumentReference> result = new ArrayList<>();
		for ( int id : ids ) {
			result.add( reference( SimpleEntity.NAME, id + "" ) );
		}
		return result;
	}

	private static void changeNames(List<SimpleEntity> entities) {
		for ( SimpleEntity entity : entities ) {
			entity.setName( NEW_NAME );
		}
	}
}
