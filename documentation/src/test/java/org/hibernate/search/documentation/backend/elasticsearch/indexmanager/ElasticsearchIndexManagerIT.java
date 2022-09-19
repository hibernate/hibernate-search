/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.documentation.backend.elasticsearch.indexmanager;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.EntityManagerFactory;

import org.hibernate.search.backend.elasticsearch.metamodel.ElasticsearchIndexDescriptor;
import org.hibernate.search.backend.elasticsearch.index.ElasticsearchIndexManager;
import org.hibernate.search.documentation.testsupport.BackendConfigurations;
import org.hibernate.search.documentation.testsupport.DocumentationSetupHelper;
import org.hibernate.search.engine.backend.index.IndexManager;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.mapping.SearchMapping;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class ElasticsearchIndexManagerIT {

	@Rule
	public DocumentationSetupHelper setupHelper =
			DocumentationSetupHelper.withSingleBackend( BackendConfigurations.simple() );

	private EntityManagerFactory entityManagerFactory;

	@Before
	public void setup() {
		entityManagerFactory = setupHelper.start().setup( Book.class );
	}

	@Test
	public void readWriteName() {
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
