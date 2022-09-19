/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.documentation.backend.elasticsearch.client;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;

import jakarta.persistence.EntityManagerFactory;

import org.hibernate.search.backend.elasticsearch.ElasticsearchBackend;
import org.hibernate.search.documentation.testsupport.BackendConfigurations;
import org.hibernate.search.documentation.testsupport.DocumentationSetupHelper;
import org.hibernate.search.engine.backend.Backend;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.mapping.SearchMapping;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import org.elasticsearch.client.Request;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.RestClient;

public class ElasticsearchGetClientIT {

	@Rule
	public DocumentationSetupHelper setupHelper =
			DocumentationSetupHelper.withSingleBackend( BackendConfigurations.simple() );

	private EntityManagerFactory entityManagerFactory;

	@Before
	public void setup() {
		entityManagerFactory = setupHelper.start().setup( Book.class );
	}

	@Test
	public void client() throws IOException {
		//tag::client[]
		SearchMapping mapping = /* ... */ // <1>
				//end::client[]
				Search.mapping( entityManagerFactory );
		//tag::client[]
		Backend backend = mapping.backend(); // <2>
		ElasticsearchBackend elasticsearchBackend = backend.unwrap( ElasticsearchBackend.class ); // <3>
		RestClient client = elasticsearchBackend.client( RestClient.class ); // <4>
		//end::client[]

		Response response = client.performRequest( new Request( "GET", "/" ) );
		assertThat( response ).isNotNull();
		assertThat( response.getStatusLine().getStatusCode() ).isEqualTo( 200 );
	}

}
