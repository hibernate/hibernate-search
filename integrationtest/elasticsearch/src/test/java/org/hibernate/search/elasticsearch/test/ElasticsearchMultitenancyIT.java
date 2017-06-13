/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.test;

import org.hibernate.search.annotations.DocumentId;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.Store;
import org.hibernate.search.testsupport.TestForIssue;
import org.hibernate.search.testsupport.junit.SearchFactoryHolder;
import org.hibernate.search.testsupport.junit.SearchITHelper;
import org.junit.Rule;
import org.junit.Test;

public class ElasticsearchMultitenancyIT {

	@Rule
	public final SearchFactoryHolder sfHolder = new SearchFactoryHolder( User.class )
			.withMultitenancyEnabled( true );

	private final SearchITHelper helper = new SearchITHelper( sfHolder );

	@Test
	@TestForIssue(jiraKey = "HSEARCH-2420")
	public void testWithCustomId() throws Exception {
		String customId = "S:custom_id_325";

		User user = new User();
		user.setId( customId );
		user.setSurname( "Lee" );

		helper.executor( "tenant_id_with_underscores" ).add()
				.push( user, customId )
				.execute();

		helper.assertThat( "surname", "lee" )
				.from( User.class )
				.matchesExactlyIds( customId );
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
