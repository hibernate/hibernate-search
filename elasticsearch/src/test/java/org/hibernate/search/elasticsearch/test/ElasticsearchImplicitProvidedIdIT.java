/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.test;

import java.io.Serializable;
import java.util.List;

import org.hibernate.search.backend.spi.Work;
import org.hibernate.search.backend.spi.WorkType;
import org.hibernate.search.cfg.Environment;
import org.hibernate.search.elasticsearch.cfg.ElasticsearchEnvironment;
import org.hibernate.search.engine.integration.impl.ExtendedSearchIntegrator;
import org.hibernate.search.query.dsl.QueryBuilder;
import org.hibernate.search.query.engine.spi.EntityInfo;
import org.hibernate.search.spi.SearchIntegrator;
import org.hibernate.search.testsupport.TestForIssue;
import org.hibernate.search.testsupport.junit.SearchFactoryHolder;
import org.hibernate.search.testsupport.setup.TransactionContextForTest;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;

import org.apache.lucene.search.Query;

public class ElasticsearchImplicitProvidedIdIT {

	@Rule
	public SearchFactoryHolder sfHolder = new SearchFactoryHolder( User.class )
			.withProperty( "hibernate.search.default." + Environment.INDEX_MANAGER_IMPL_NAME, "elasticsearch" )
			.withProperty( "hibernate.search.default." + ElasticsearchEnvironment.REFRESH_AFTER_WRITE, "true" )
			.withIdProvidedImplicit( true );

	@Test
	@TestForIssue(jiraKey = "HSEARCH-2431")
	public void testWithProjection() throws Exception {
		ExtendedSearchIntegrator searchIntegrator = sfHolder.getSearchFactory();
		User user = new User();
		user.setId( 1 );
		user.setSurname( "Smith" );

		indexUser( user, "S:1", searchIntegrator );

		QueryBuilder queryBuilder = searchIntegrator.buildQueryBuilder().forEntity( User.class ).get();
		Query query = queryBuilder.keyword().onField( "surname" ).matching( "smith" ).createQuery();

		List<EntityInfo> entityInfoList = searchIntegrator.createHSQuery( query, User.class )
				.projection( "surname" )
				.queryEntityInfos();

		Assert.assertEquals( 1, entityInfoList.size() );
	}

	private void indexUser(User user, Serializable id, SearchIntegrator searchIntegrator) {
		Work work = new Work( user, id, WorkType.ADD, false );
		TransactionContextForTest tc = new TransactionContextForTest();
		searchIntegrator.getWorker().performWork( work, tc );
		tc.end();
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-2432")
	public void testWithCustomId() throws Exception {
		ExtendedSearchIntegrator searchIntegrator = sfHolder.getSearchFactory();
		User user = new User();
		user.setId( 325 );
		user.setSurname( "Lee" );

		Serializable customId = "S:custom_id_325";

		indexUser( user, customId, searchIntegrator );

		QueryBuilder queryBuilder = searchIntegrator.buildQueryBuilder().forEntity( User.class ).get();
		Query query = queryBuilder.keyword().onField( "surname" ).matching( "Lee" ).createQuery();

		List<EntityInfo> entityInfoList = searchIntegrator.createHSQuery( query, User.class ).queryEntityInfos();

		Assert.assertEquals( 1, entityInfoList.size() );
		Assert.assertEquals( customId, entityInfoList.iterator().next().getId() );
	}
}
