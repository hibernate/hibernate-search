/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.apache.lucene.index.IndexReader;

class LuceneLowLevelIndexReaderIT {

	@RegisterExtension
	public DocumentationSetupHelper setupHelper =
			DocumentationSetupHelper.withSingleBackend( BackendConfigurations.simple() );

	private EntityManagerFactory entityManagerFactory;

	@BeforeEach
	void setup() {
		entityManagerFactory = setupHelper.start().setup( Book.class );
	}

	@Test
	void indexReader() throws Exception {
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
