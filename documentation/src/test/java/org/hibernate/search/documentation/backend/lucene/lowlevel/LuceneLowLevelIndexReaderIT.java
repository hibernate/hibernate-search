/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.documentation.backend.lucene.lowlevel;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.EntityManagerFactory;

import org.hibernate.search.backend.lucene.LuceneExtension;
import org.hibernate.search.backend.lucene.scope.LuceneIndexScope;
import org.hibernate.search.documentation.backend.lucene.analyzer.Book;
import org.hibernate.search.documentation.testsupport.BackendConfigurations;
import org.hibernate.search.documentation.testsupport.DocumentationSetupHelper;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.mapping.SearchMapping;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import org.apache.lucene.index.IndexReader;

public class LuceneLowLevelIndexReaderIT {

	@Rule
	public DocumentationSetupHelper setupHelper =
			DocumentationSetupHelper.withSingleBackend( BackendConfigurations.simple() );

	private EntityManagerFactory entityManagerFactory;

	@Before
	public void setup() {
		entityManagerFactory = setupHelper.start().setup( Book.class );
	}

	@Test
	public void indexReader() throws Exception {
		int numDocs;
		//tag::indexReader[]
		SearchMapping mapping = /* ... */ // <1>
				//end::indexReader[]
				Search.mapping( entityManagerFactory );
		//tag::indexReader[]
		LuceneIndexScope indexScope = mapping
				.scope( Book.class ).extension( LuceneExtension.get() ); // <2>
		try ( IndexReader indexReader = indexScope.openIndexReader() ) { // <3>
			// work with the low-level index reader:
			numDocs = indexReader.numDocs();
		}
		//end::indexReader[]

		assertThat( numDocs ).isZero();
	}
}
