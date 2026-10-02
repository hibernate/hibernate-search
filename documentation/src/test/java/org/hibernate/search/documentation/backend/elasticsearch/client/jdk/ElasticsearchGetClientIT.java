package org.hibernate.search.documentation.backend.elasticsearch.client.jdk;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import jakarta.persistence.EntityManagerFactory;

import org.hibernate.search.backend.elasticsearch.ElasticsearchBackend;
import org.hibernate.search.backend.elasticsearch.client.impl.RestJdkClient;
import org.hibernate.search.documentation.backend.elasticsearch.client.Book;
import org.hibernate.search.documentation.testsupport.BackendConfigurations;
import org.hibernate.search.documentation.testsupport.DocumentationSetupHelper;
import org.hibernate.search.engine.backend.Backend;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.mapping.SearchMapping;
import org.hibernate.search.util.impl.integrationtest.backend.elasticsearch.SearchBackendContainer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class ElasticsearchGetClientIT {

	@RegisterExtension
	public DocumentationSetupHelper setupHelper =
			DocumentationSetupHelper.withSingleBackend( BackendConfigurations.simple() );

	private EntityManagerFactory entityManagerFactory;

	@BeforeEach
	void setup() {
		entityManagerFactory = setupHelper.start().setup( Book.class );
	}

	@Test
	void client() throws IOException, InterruptedException {
		//tag::client[]
		SearchMapping mapping = /* ... */ // <1>
				//end::client[]
				Search.mapping( entityManagerFactory );
		//tag::client[]
		Backend backend = mapping.backend(); // <2>
		ElasticsearchBackend elasticsearchBackend = backend.unwrap( ElasticsearchBackend.class ); // <3>
		RestJdkClient client = elasticsearchBackend.client( RestJdkClient.class ); // <4>
		//end::client[]

		// The configured connection URL may or may not already include a scheme.
		String connectionUrl = SearchBackendContainer.connectionUrl();
		URI baseUri = URI.create( connectionUrl.contains( "://" ) ? connectionUrl : "http://" + connectionUrl );
		HttpRequest request = HttpRequest.newBuilder()
				.uri( baseUri.resolve( "/" ) )
				.GET()
				.build();
		HttpResponse<String> response = client.sendAsync( request, HttpResponse.BodyHandlers.ofString() ).join();
		assertThat( response ).isNotNull();
		assertThat( response.statusCode() ).isEqualTo( 200 );
	}

}
