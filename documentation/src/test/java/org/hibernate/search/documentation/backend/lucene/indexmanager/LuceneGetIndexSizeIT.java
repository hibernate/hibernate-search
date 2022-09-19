/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.documentation.backend.lucene.indexmanager;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.EntityManagerFactory;

import org.hibernate.search.backend.lucene.index.LuceneIndexManager;
import org.hibernate.search.documentation.backend.lucene.analyzer.Book;
import org.hibernate.search.documentation.testsupport.BackendConfigurations;
import org.hibernate.search.documentation.testsupport.DocumentationSetupHelper;
import org.hibernate.search.engine.backend.index.IndexManager;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.mapping.SearchMapping;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class LuceneGetIndexSizeIT {

	@Rule
	public DocumentationSetupHelper setupHelper =
			DocumentationSetupHelper.withSingleBackend( BackendConfigurations.simple() );

	private EntityManagerFactory entityManagerFactory;

	@Before
	public void setup() {
		entityManagerFactory = setupHelper.start().setup( Book.class );
	}

	@Test
	public void computeIndexSize() {
		//tag::computeIndexSize[]
		SearchMapping mapping = /* ... */ // <1>
				//end::computeIndexSize[]
				Search.mapping( entityManagerFactory );
		//tag::computeIndexSize[]
		IndexManager indexManager = mapping.indexManager( "Book" ); // <2>
		LuceneIndexManager luceneIndexManager = indexManager.unwrap( LuceneIndexManager.class ); // <3>
		long size = luceneIndexManager.computeSizeInBytes(); // <4>
		luceneIndexManager.computeSizeInBytesAsync() // <5>
				.thenAccept( sizeInBytes -> {
					// ...
				} );
		//end::computeIndexSize[]
		assertThat( size ).isGreaterThanOrEqualTo( 0L );
	}

}
