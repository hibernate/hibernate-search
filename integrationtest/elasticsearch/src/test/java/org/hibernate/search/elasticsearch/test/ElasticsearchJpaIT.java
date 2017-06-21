/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.test;

import java.util.List;

import javax.persistence.EntityManager;

import org.hibernate.search.elasticsearch.ElasticsearchProjectionConstants;
import org.hibernate.search.elasticsearch.ElasticsearchQueries;
import org.hibernate.search.jpa.FullTextEntityManager;
import org.hibernate.search.jpa.Search;
import org.hibernate.search.query.engine.spi.QueryDescriptor;
import org.hibernate.search.test.jpa.JPATestCase;
import org.hibernate.search.test.util.JsonHelper;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Gunnar Morling
 */
public class ElasticsearchJpaIT extends JPATestCase {

	@Before
	public void setupTestData() {
		EntityManager em = factory.createEntityManager();
		em.getTransaction().begin();

		GolfPlayer hergesheimer = new GolfPlayer.Builder()
			.firstName( "Klaus" )
			.lastName( "Hergesheimer" )
			.build();
		em.persist( hergesheimer );

		em.getTransaction().commit();
		em.close();
	}

	@After
	public void deleteTestData() {
		EntityManager em = factory.createEntityManager();
		FullTextEntityManager ftem = Search.getFullTextEntityManager( em );
		ftem.getTransaction().begin();

		QueryDescriptor query = ElasticsearchQueries.fromJson( "{ 'query': { 'match_all' : {} } }" );
		List<?> result = ftem.createFullTextQuery( query ).getResultList();

		for ( Object entity : result ) {
			ftem.remove( entity );
		}

		ftem.getTransaction().commit();
		ftem.close();
	}

	@Test
	public void testQueryViaEntityManager() {
		EntityManager em = factory.createEntityManager();
		FullTextEntityManager ftem = Search.getFullTextEntityManager( em );
		ftem.getTransaction().begin();

		QueryDescriptor query = ElasticsearchQueries.fromJson( "{ 'query': { 'match' : { 'lastName' : 'Hergesheimer' } } }" );

		Object[] result = (Object[]) ftem.createFullTextQuery( query, GolfPlayer.class )
				.setProjection( ElasticsearchProjectionConstants.SOURCE )
				.getSingleResult();

		String source = (String) result[0];

		JsonHelper.assertJsonEqualsIgnoringUnknownFields(
				"{" +
					"'lastName': 'Hergesheimer'," +
					"'fullName': 'Klaus Hergesheimer'," +
				"}",
				source
		);

		ftem.getTransaction().commit();
		ftem.close();
	}

	@Override
	public Class<?>[] getAnnotatedClasses() {
		return new Class[] { GolfPlayer.class, GolfCourse.class, Hole.class };
	}
}
