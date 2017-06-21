/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.test;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.search.annotations.Analyze;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.IndexedEmbedded;
import org.hibernate.search.annotations.SortableField;
import org.hibernate.search.annotations.Store;
import org.hibernate.search.testsupport.TestForIssue;
import org.hibernate.search.testsupport.junit.SearchFactoryHolder;
import org.hibernate.search.testsupport.junit.SearchITHelper;
import org.junit.Rule;
import org.junit.Test;

public class ElasticsearchImplicitProvidedIdIT {

	@Rule
	public final SearchFactoryHolder sfHolder = new SearchFactoryHolder( User.class, UserWithAddress.class )
			.withIdProvidedImplicit( true );

	private final SearchITHelper helper = new SearchITHelper( sfHolder );

	@Test
	@TestForIssue(jiraKey = "HSEARCH-2431")
	public void testWithProjection() throws Exception {
		User user = new User();
		user.setId( 1 );
		user.setSurname( "Smith" );

		helper.add( user, "S:1" );

		helper.assertThat( "surname", "smith" )
				.from( User.class )
				.projecting( "surname" )
				.matchesExactlySingleProjections( "Smith" );
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-2432")
	public void testWithCustomId() throws Exception {
		User user = new User();
		user.setId( 325 );
		user.setSurname( "Lee" );

		Serializable customId = "S:custom_id_325";

		helper.add( user, customId );

		helper.assertThat( "surname", "lee" )
				.from( User.class )
				.projecting( "surname" )
				.matchesExactlySingleProjections( "Lee" )
				.matchesExactlyIds( customId );
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-2433")
	public void testWithEmbedded() throws Exception {
		UserWithAddress user = new UserWithAddress();
		user.setId( 1 );
		user.setSurname( "Smith" );

		Address address = new Address();
		address.setStreet( "21st steet" );
		address.setNumber( 42 );
		address.setPostCode( "8964" );
		user.addAddress( address );

		helper.add( user, "S:1" );

		helper.assertThat( "surname", "smith" )
				.from( UserWithAddress.class )
				.projecting( "surname" )
				.matchesExactlySingleProjections( "Smith" );
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
