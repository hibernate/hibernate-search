/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.test;

import java.util.ArrayList;
import java.io.Serializable;
import java.util.List;

import org.apache.lucene.search.Query;
import org.hibernate.search.annotations.Analyze;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.IndexedEmbedded;
import org.hibernate.search.annotations.SortableField;
import org.hibernate.search.annotations.Store;
import org.hibernate.search.backend.spi.Work;
import org.hibernate.search.backend.spi.WorkType;
import org.hibernate.search.cfg.Environment;
import org.hibernate.search.elasticsearch.cfg.ElasticsearchEnvironment;
import org.hibernate.search.elasticsearch.cfg.IndexSchemaManagementStrategy;
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

public class ElasticsearchImplicitProvidedIdIT {

	@Rule
	public SearchFactoryHolder sfHolder = new SearchFactoryHolder( User.class, UserWithAddress.class )
			.withProperty( "hibernate.search.default." + Environment.INDEX_MANAGER_IMPL_NAME, "elasticsearch" )
			.withProperty( "hibernate.search.default." + ElasticsearchEnvironment.REFRESH_AFTER_WRITE, "true" )
			.withProperty( "hibernate.search.default." + ElasticsearchEnvironment.INDEX_SCHEMA_MANAGEMENT_STRATEGY,
					IndexSchemaManagementStrategy.RECREATE_DELETE.name() )
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

	@Test
	@TestForIssue(jiraKey = "HSEARCH-2433")
	public void testWithEmbedded() throws Exception {
		ExtendedSearchIntegrator searchIntegrator = sfHolder.getSearchFactory();

		UserWithAddress user = new UserWithAddress();
		user.setId( 1 );
		user.setSurname( "Smith" );

		Address address = new Address();
		address.setStreet( "21st steet" );
		address.setNumber( 42 );
		address.setPostCode( "8964" );
		user.addAddress( address );

		Work work = new Work( user, "S:1", WorkType.ADD, false );
		TransactionContextForTest tc = new TransactionContextForTest();
		searchIntegrator.getWorker().performWork( work, tc );
		tc.end();

		QueryBuilder queryBuilder = searchIntegrator.buildQueryBuilder().forEntity( UserWithAddress.class ).get();
		Query query = queryBuilder.keyword().onField( "surname" ).matching( "smith" ).createQuery();

		List<EntityInfo> entityInfoList = searchIntegrator.createHSQuery( query, UserWithAddress.class )
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

	@Indexed
	private static class User {

		@Field
		private int id;

		@Field(store = Store.YES)
		private String surname;

		public void setId(int id) {
			this.id = id;
		}

		public void setSurname(String surname) {
			this.surname = surname;
		}

	}

	@Indexed
	private static class UserWithAddress {

		@Field(store = Store.YES, analyze = Analyze.NO)
		@SortableField
		private int id;

		@IndexedEmbedded(targetElement = Address.class, indexNullAs = Field.DEFAULT_NULL_TOKEN)
		private List<Address> addresses = new ArrayList<>();

		@Field(store = Store.YES)
		private String surname;

		public void setId(int id) {
			this.id = id;
		}

		public void setSurname(String surname) {
			this.surname = surname;
		}

		public void addAddress(Address address) {
			this.addresses.add( address );
		}
	}

	private static class Address {

		@Field(store = Store.YES, analyze = Analyze.NO)
		private String street;

		@Field(store = Store.YES, analyze = Analyze.NO)
		private String postCode;

		@Field(store = Store.YES, analyze = Analyze.NO)
		private int number;

		public void setStreet(String street) {
			this.street = street;
		}

		public void setPostCode(String postCode) {
			this.postCode = postCode;
		}

		public void setNumber(int number) {
			this.number = number;
		}
	}

}
