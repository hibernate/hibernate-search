/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.test;

import java.io.Serializable;
import java.util.List;

import org.apache.lucene.search.Query;
import org.hibernate.search.annotations.DocumentId;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.Store;
import org.hibernate.search.backend.spi.Work;
import org.hibernate.search.backend.spi.WorkType;
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

public class ElasticsearchMultitenancyIT {

	@Rule
	public SearchFactoryHolder sfHolder = new SearchFactoryHolder( User.class )
			.withMultitenancyEnabled( true );

	@Test
	@TestForIssue(jiraKey = "HSEARCH-2420")
	public void testWithCustomId() throws Exception {
		String customId = "S:custom_id_325";

		ExtendedSearchIntegrator searchIntegrator = sfHolder.getSearchFactory();
		User user = new User();
		user.setId( customId );
		user.setSurname( "Lee" );

		indexUser( "tenant_id_with_underscores", user, customId, searchIntegrator );

		QueryBuilder queryBuilder = searchIntegrator.buildQueryBuilder().forEntity( User.class ).get();
		Query query = queryBuilder.keyword().onField( "surname" ).matching( "Lee" ).createQuery();

		List<EntityInfo> entityInfoList = searchIntegrator.createHSQuery( query, User.class ).queryEntityInfos();

		Assert.assertEquals( 1, entityInfoList.size() );
		Assert.assertEquals( customId, entityInfoList.iterator().next().getId() );
	}

	private void indexUser(String tenantId, User user, Serializable id, SearchIntegrator searchIntegrator) {
		Work work = new Work( tenantId, user, id, WorkType.ADD, false );
		TransactionContextForTest tc = new TransactionContextForTest();
		searchIntegrator.getWorker().performWork( work, tc );
		tc.end();
	}

	@Indexed
	private static class User {

		@DocumentId
		private String id;

		@Field(store = Store.YES)
		private String surname;

		public void setId(String id) {
			this.id = id;
		}

		public void setSurname(String surname) {
			this.surname = surname;
		}

	}
}
