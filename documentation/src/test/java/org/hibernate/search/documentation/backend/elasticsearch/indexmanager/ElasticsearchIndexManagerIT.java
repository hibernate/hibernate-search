/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.documentation.backend.elasticsearch.indexmanager;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.EntityManagerFactory;

import org.hibernate.search.backend.elasticsearch.index.ElasticsearchIndexManager;
import org.hibernate.search.backend.elasticsearch.metamodel.ElasticsearchIndexDescriptor;
import org.hibernate.search.documentation.testsupport.BackendConfigurations;
import org.hibernate.search.documentation.testsupport.DocumentationSetupHelper;
import org.hibernate.search.engine.backend.index.IndexManager;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.mapping.SearchMapping;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class ElasticsearchIndexManagerIT {

	@RegisterExtension
	public DocumentationSetupHelper setupHelper =
			DocumentationSetupHelper.withSingleBackend( BackendConfigurations.simple() );

	private EntityManagerFactory entityManagerFactory;

	@BeforeEach
	void setup() {
		entityManagerFactory = setupHelper.start().setup( Book.class );
	}

	@Test
	void readWriteName() {
		//tag::readWriteName[]
		SearchMapping mapping = /* ... */ // <1>
				//end::readWriteName[]
				Search.mapping( entityManagerFactory );
		//tag::readWriteName[]
		IndexManager indexManager = mapping.indexManager( "Book" ); // <2>
		ElasticsearchIndexManager esIndexManager = indexManager.unwrap( ElasticsearchIndexManager.class ); // <3>
		ElasticsearchIndexDescriptor descriptor = esIndexManager.descriptor();// <4>
		String readName = descriptor.readName();// <5>
		String writeName = descriptor.writeName();// <5>
		//end::readWriteName[]
		assertThat( readName ).isEqualTo( "book-read" );
		assertThat( writeName ).isEqualTo( "book-write" );
	}
}
