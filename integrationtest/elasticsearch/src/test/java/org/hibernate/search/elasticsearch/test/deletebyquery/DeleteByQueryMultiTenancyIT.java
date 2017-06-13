/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.test.deletebyquery;

import static org.fest.assertions.Assertions.assertThat;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.backend.TransactionContext;
import org.hibernate.search.backend.spi.DeleteByQueryWork;
import org.hibernate.search.backend.spi.SingularTermDeletionQuery;
import org.hibernate.search.elasticsearch.ElasticsearchQueries;
import org.hibernate.search.engine.integration.impl.ExtendedSearchIntegrator;
import org.hibernate.search.query.engine.spi.QueryDescriptor;
import org.hibernate.search.spi.impl.PojoIndexedTypeIdentifier;
import org.hibernate.search.test.SearchTestBase;
import org.hibernate.search.testsupport.setup.TransactionContextForTest;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Integration test for delete-by-query with multi-tenancy.
 *
 * @author Gunnar Morling
 */
public class DeleteByQueryMultiTenancyIT extends SearchTestBase {

	private static final String EXTRA_TERRESTRIAL = "Extraterrestrial";
	private static final String DOWN_TO_THE_EARTH = "DownToTheEarth";

	@Before
	public void setupTestData() {
		Session s = getSessionFactory()
			.withOptions()
			.tenantIdentifier( DOWN_TO_THE_EARTH )
			.openSession();

		Transaction tx = s.beginTransaction();

		HockeyPlayer hergesheimer = new HockeyPlayer();
		hergesheimer.name = "Hergesheimer";
		hergesheimer.active = true;
		s.persist( hergesheimer );

		HockeyPlayer galore = new HockeyPlayer();
		galore.name = "Galore";
		galore.active = false;
		s.persist( galore );

		HockeyPlayer kidd = new HockeyPlayer();
		kidd.name = "Kidd";
		kidd.active = false;
		s.persist( kidd );

		HockeyPlayer brand = new HockeyPlayer();
		brand.name = "Brand";
		brand.active = true;
		s.persist( brand );

		tx.commit();
		s.close();

		s = getSessionFactory()
			.withOptions()
			.tenantIdentifier( EXTRA_TERRESTRIAL )
			.openSession();

		tx = s.beginTransaction();

		HockeyPlayer metz = new HockeyPlayer();
		metz.name = "Metz";
		metz.active = false;
		s.persist( metz );

		HockeyPlayer plenty = new HockeyPlayer();
		plenty.name = "Plenty";
		plenty.active = true;
		s.persist( plenty );

		tx.commit();
		s.close();
	}

	@After
	public void deleteTestData() {
		for ( String tenantId : multiTenantIds() ) {
			Session s = getSessionFactory()
				.withOptions()
				.tenantIdentifier( tenantId )
				.openSession();

			FullTextSession session = Search.getFullTextSession( s );
			Transaction tx = s.beginTransaction();

			QueryDescriptor query = ElasticsearchQueries.fromJson( "{ 'query': { 'match_all' : {} } }" );
			List<?> result = session.createFullTextQuery( query ).list();

			for ( Object entity : result ) {
				session.delete( entity );
			}

			tx.commit();
			s.close();
		}
	}

	@Test
	public void deleteByQueryOnlyAffectsEntitiesOfCurrentTenant() throws Exception {
		// when deleting some entities of DOWN_TO_THE_EARTH

		Session s = getSessionFactory()
			.withOptions()
			.tenantIdentifier( DOWN_TO_THE_EARTH )
			.openSession();

		FullTextSession session = Search.getFullTextSession( s );

		ExtendedSearchIntegrator integrator = session.getSearchFactory()
				.unwrap( ExtendedSearchIntegrator.class );

		DeleteByQueryWork queryWork = new DeleteByQueryWork(
				DOWN_TO_THE_EARTH,
				new PojoIndexedTypeIdentifier( HockeyPlayer.class ),
				new SingularTermDeletionQuery( "active", "false" )
		);

		TransactionContext tc = new TransactionContextForTest();

		integrator.getWorker().performWork( queryWork, tc );
		integrator.getWorker().flushWorks( tc );

		Transaction tx = s.beginTransaction();

		// then DOWN_TO_THE_EARTH entities should be gone
		QueryDescriptor query = ElasticsearchQueries.fromJson( "{ 'query': { 'match_all' : {} } }" );

		@SuppressWarnings("unchecked")
		List<HockeyPlayer> result = session.createFullTextQuery( query, HockeyPlayer.class ).list();

		assertThat( result ).onProperty( "name" ).containsOnly( "Hergesheimer", "Brand" );

		tx.commit();

		s.close();

		// and then EXTRA_TERRESTRIAL should remain unchanged
		s = getSessionFactory()
			.withOptions()
			.tenantIdentifier( EXTRA_TERRESTRIAL )
			.openSession();

		session = Search.getFullTextSession( s );

		tx = s.beginTransaction();

		query = ElasticsearchQueries.fromJson( "{ 'query': { 'match_all' : {} } }" );

		result = session.createFullTextQuery( query, HockeyPlayer.class ).list();

		assertThat( result )
			.describedAs( "Running delete-by-query for other tenant should not affect entities of this entity" )
			.onProperty( "name" )
			.containsOnly( "Metz", "Plenty" );

		tx.commit();
		s.close();
	}

	@Override
	public Class<?>[] getAnnotatedClasses() {
		return new Class[] { HockeyPlayer.class };
	}

	@Override
	public Set<String> multiTenantIds() {
		return new HashSet<>( Arrays.asList( EXTRA_TERRESTRIAL, DOWN_TO_THE_EARTH ) );
	}
}
