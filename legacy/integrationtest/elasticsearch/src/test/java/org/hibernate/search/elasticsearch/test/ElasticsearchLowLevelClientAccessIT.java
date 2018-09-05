/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.test;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import org.hibernate.search.SearchFactory;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.elasticsearch.indexes.ElasticsearchIndexFamily;
import org.hibernate.search.elasticsearch.indexes.ElasticsearchIndexFamilyType;
import org.hibernate.search.exception.SearchException;
import org.hibernate.search.indexes.IndexFamily;
import org.hibernate.search.test.SearchTestBase;
import org.hibernate.search.testsupport.TestForIssue;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.apache.http.nio.client.HttpAsyncClient;
import org.assertj.core.api.Assertions;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.RestClient;

/**
 * Test access to the low-level Elasticsearch client
 */
@TestForIssue( jiraKey = "HSEARCH-3125" )
public class ElasticsearchLowLevelClientAccessIT extends SearchTestBase {

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Test
	public void indexFamily_getClient() throws Exception {
		SearchFactory searchFactory = getSearchFactory();
		IndexFamily indexFamily = searchFactory.getIndexFamily( ElasticsearchIndexFamilyType.get() );
		ElasticsearchIndexFamily elasticsearchIndexFamily = indexFamily.unwrap( ElasticsearchIndexFamily.class );
		RestClient restClient = elasticsearchIndexFamily.getClient( RestClient.class );

		// Test that the client actually works
		Response response = restClient.performRequest( "GET", "/" );
		Assertions.assertThat( response.getStatusLine().getStatusCode() ).isEqualTo( 200 );
	}

	@Test
	public void indexFamily_getClient_error_invalidClass() {
		SearchFactory searchFactory = getSearchFactory();
		IndexFamily indexFamily = searchFactory.getIndexFamily( ElasticsearchIndexFamilyType.get() );
		ElasticsearchIndexFamily elasticsearchIndexFamily = indexFamily.unwrap( ElasticsearchIndexFamily.class );

		thrown.expect( SearchException.class );
		thrown.expectMessage( HttpAsyncClient.class.getName() );
		thrown.expectMessage( "the client can only be unwrapped to" );
		thrown.expectMessage( RestClient.class.getName() );

		elasticsearchIndexFamily.getClient( HttpAsyncClient.class );
	}

	@Override
	public Class<?>[] getAnnotatedClasses() {
		return new Class<?>[]{ IndexedEntity.class };
	}

	@Entity
	@Indexed
	public static class IndexedEntity {
		@Id
		@GeneratedValue
		private Integer id;

		@Field
		private String text;

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public String getText() {
			return text;
		}
	}
}
