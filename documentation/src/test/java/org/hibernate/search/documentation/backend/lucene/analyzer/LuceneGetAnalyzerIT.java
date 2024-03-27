/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.documentation.backend.lucene.analyzer;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;

import jakarta.persistence.EntityManagerFactory;

import org.hibernate.search.backend.lucene.LuceneBackend;
import org.hibernate.search.backend.lucene.index.LuceneIndexManager;
import org.hibernate.search.documentation.testsupport.BackendConfigurations;
import org.hibernate.search.documentation.testsupport.DocumentationSetupHelper;
import org.hibernate.search.engine.backend.Backend;
import org.hibernate.search.engine.backend.index.IndexManager;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.mapping.SearchMapping;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.apache.lucene.analysis.Analyzer;

class LuceneGetAnalyzerIT {

	@RegisterExtension
	public DocumentationSetupHelper setupHelper =
			DocumentationSetupHelper.withSingleBackend( BackendConfigurations.simple() );

	private EntityManagerFactory entityManagerFactory;

	@BeforeEach
	void setup() {
		entityManagerFactory = setupHelper.start().setup( Book.class );
	}

	@Test
	void fromBackend() {
		//tag::fromBackend[]
		SearchMapping mapping = /* ... */ // <1>
				//end::fromBackend[]
				Search.mapping( entityManagerFactory );
		//tag::fromBackend[]
		Backend backend = mapping.backend(); // <2>
		LuceneBackend luceneBackend = backend.unwrap( LuceneBackend.class ); // <3>
		Optional<? extends Analyzer> analyzer = luceneBackend.analyzer( "english" ); // <4>
		Optional<? extends Analyzer> normalizer = luceneBackend.normalizer( "isbn" ); // <5>
		//end::fromBackend[]
		assertThat( analyzer ).isNotNull();
		assertThat( normalizer ).isNotNull().isNotSameAs( analyzer );
	}

	@Test
	void fromIndexManager() {
		//tag::fromIndexManager[]
		SearchMapping mapping = /* ... */ // <1>
				//end::fromIndexManager[]
				Search.mapping( entityManagerFactory );
		//tag::fromIndexManager[]
		IndexManager indexManager = mapping.indexManager( "Book" ); // <2>
		LuceneIndexManager luceneIndexManager = indexManager.unwrap( LuceneIndexManager.class ); // <3>
		Analyzer indexingAnalyzer = luceneIndexManager.indexingAnalyzer(); // <4>
		Analyzer searchAnalyzer = luceneIndexManager.searchAnalyzer(); // <5>
		//end::fromIndexManager[]
		assertThat( indexingAnalyzer ).isNotNull();
		assertThat( searchAnalyzer ).isNotNull().isNotSameAs( indexingAnalyzer );
	}

}
