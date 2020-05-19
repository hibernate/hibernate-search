/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.documentation.backend.lucene.analyzer;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;
import javax.persistence.EntityManagerFactory;

import org.hibernate.search.backend.lucene.LuceneBackend;
import org.hibernate.search.backend.lucene.index.LuceneIndexManager;
import org.hibernate.search.documentation.testsupport.LuceneBackendConfiguration;
import org.hibernate.search.engine.backend.Backend;
import org.hibernate.search.engine.backend.index.IndexManager;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.mapping.SearchMapping;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmSetupHelper;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import org.apache.lucene.analysis.Analyzer;

public class LuceneGetAnalyzerIT {

	@Rule
	public OrmSetupHelper setupHelper =
			OrmSetupHelper.withSingleBackend( "myBackend", new LuceneBackendConfiguration() );

	private EntityManagerFactory entityManagerFactory;

	@Before
	public void setup() {
		entityManagerFactory = setupHelper.start().setup( Book.class );
	}

	@Test
	public void fromBackend() {
		//tag::fromBackend[]
		SearchMapping mapping = Search.mapping( entityManagerFactory ); // <1>
		Backend backend = mapping.backend( "myBackend" ); // <2>
		LuceneBackend luceneBackend = backend.unwrap( LuceneBackend.class ); // <3>
		Optional<? extends Analyzer> analyzer = luceneBackend.analyzer( "english" ); // <4>
		Optional<? extends Analyzer> normalizer = luceneBackend.normalizer( "isbn" ); // <5>
		//end::fromBackend[]
		assertThat( analyzer ).isNotNull();
		assertThat( normalizer ).isNotNull().isNotSameAs( analyzer );
	}

	@Test
	public void fromIndexManager() {
		//tag::fromIndexManager[]
		SearchMapping mapping = Search.mapping( entityManagerFactory ); // <1>
		IndexManager indexManager = mapping.indexManager( "Book" ); // <2>
		LuceneIndexManager luceneIndexManager = indexManager.unwrap( LuceneIndexManager.class ); // <3>
		Analyzer indexingAnalyzer = luceneIndexManager.indexingAnalyzer(); // <4>
		Analyzer searchAnalyzer = luceneIndexManager.searchAnalyzer(); // <5>
		//end::fromIndexManager[]
		assertThat( indexingAnalyzer ).isNotNull();
		assertThat( searchAnalyzer ).isNotNull().isNotSameAs( indexingAnalyzer );
	}

}
