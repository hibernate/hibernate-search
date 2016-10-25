/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.test;

import java.util.List;

import org.hibernate.search.backend.spi.Work;
import org.hibernate.search.backend.spi.WorkType;
import org.hibernate.search.cfg.Environment;
import org.hibernate.search.elasticsearch.cfg.ElasticsearchEnvironment;
import org.hibernate.search.query.dsl.QueryBuilder;
import org.hibernate.search.query.engine.spi.EntityInfo;
import org.hibernate.search.spi.SearchIntegrator;
import org.hibernate.search.spi.SearchIntegratorBuilder;
import org.hibernate.search.testsupport.TestForIssue;
import org.hibernate.search.testsupport.setup.SearchConfigurationForTest;
import org.hibernate.search.testsupport.setup.TransactionContextForTest;

import org.junit.Assert;
import org.junit.Test;

import org.apache.lucene.search.Query;

public class ElasticsearchImplicitProvidedIdIT {

	@Test
	@TestForIssue(jiraKey = "HSEARCH-2431")
	public void testWithProjection() throws Exception {
		SearchConfigurationForTest cfg = new SearchConfigurationForTest()
				.addProperty( "hibernate.search.default." + Environment.INDEX_MANAGER_IMPL_NAME, "elasticsearch" )
				.addProperty( "hibernate.search.default." + ElasticsearchEnvironment.REFRESH_AFTER_WRITE, "true" )
				.addClass( User.class )
				.setIdProvidedImplicit( true );

		SearchIntegratorBuilder searchIntegratorBuilder = new SearchIntegratorBuilder().configuration( cfg );

		try (SearchIntegrator searchIntegrator = searchIntegratorBuilder.buildSearchIntegrator();) {
			User user = new User();
			user.setId( 1 );
			user.setSurname( "Smith" );

			Work work = new Work( user, "S:1", WorkType.ADD, false );
			TransactionContextForTest tc = new TransactionContextForTest();
			searchIntegrator.getWorker().performWork( work, tc );
			tc.end();

			QueryBuilder queryBuilder = searchIntegrator.buildQueryBuilder().forEntity( User.class ).get();
			Query query = queryBuilder.keyword().onField( "surname" ).matching( "smith" ).createQuery();

			List<EntityInfo> entityInfoList = searchIntegrator.createHSQuery( query, User.class )
					.projection( "surname" )
					.queryEntityInfos();

			Assert.assertEquals( 1, entityInfoList.size() );
		}
	}
}
