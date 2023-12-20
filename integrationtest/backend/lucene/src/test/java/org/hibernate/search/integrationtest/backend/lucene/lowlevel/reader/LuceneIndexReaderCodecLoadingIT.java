/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.lucene.lowlevel.reader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.search.util.impl.integrationtest.common.assertion.SearchResultAssert.assertThatQuery;

import java.util.List;
import java.util.stream.Collectors;

import org.hibernate.search.backend.lucene.LuceneExtension;
import org.hibernate.search.backend.lucene.lowlevel.codec.impl.HibernateSearchKnnVectorsFormat;
import org.hibernate.search.backend.lucene.lowlevel.codec.impl.HibernateSearchLuceneCodec;
import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.extension.SearchSetupHelper;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.SimpleMappedIndex;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMapping;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappingSchemaManagementStrategy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.apache.lucene.codecs.Codec;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.SegmentCommitInfo;
import org.apache.lucene.index.StandardDirectoryReader;

class LuceneIndexReaderCodecLoadingIT {

	@RegisterExtension
	public final SearchSetupHelper setupHelper = SearchSetupHelper.create();

	private final SimpleMappedIndex<IndexBinding> index = SimpleMappedIndex.of( IndexBinding::new );


	@Test
	void smoke() throws Exception {
		// 1. Prepare some index where we have a vector field:
		try ( StubMapping mapping = setupHelper.start()
				.withSchemaManagement( StubMappingSchemaManagementStrategy.DROP_AND_CREATE_ON_STARTUP_ONLY )
				.withIndex( index )
				.setup() ) {
			assertThatQuery( index.query().where( SearchPredicateFactory::matchAll ) ).hasNoHits();
			index.bulkIndexer().add( "id:1", c -> c.addValue( "vector", new byte[] { 1, 2 } ) ).join();
			assertThatQuery( index.query().where( SearchPredicateFactory::matchAll ) ).hasTotalHitCount( 1 );
		}

		try ( StubMapping mapping = setupHelper.start()
				.withSchemaManagement( StubMappingSchemaManagementStrategy.DROP_ON_SHUTDOWN_ONLY )
				.withIndex( index )
				.setup() ) {
			// 2. Make sure that the index is still there and Hibernate Search can access it without any problems
			//		Note: if a wrong codec was written to the index initially then we'd have a failure here.
			assertThatQuery( index.query().where( SearchPredicateFactory::matchAll ) ).hasTotalHitCount( 1 );

			// 3. Now let's double-check that the coded in the segments is not the custom Hibernate Search one, but the one coming from the Lucene itself.
			try ( IndexReader indexReader = index.createScope().extension( LuceneExtension.get() ).openIndexReader() ) {
				int segments = 0;
				for ( StandardDirectoryReader reader : getStandardDirectoryReaders( indexReader ) ) {
					for ( SegmentCommitInfo segmentInfo : reader.getSegmentInfos() ) {
						segments++;
						Codec codec = segmentInfo.info.getCodec();
						assertThat( codec )
								.isNotInstanceOf( HibernateSearchLuceneCodec.class )
								.isInstanceOf( HibernateSearchLuceneCodec.DEFAULT_CODEC.getClass() );
						assertThat( codec.knnVectorsFormat() )
								.isNotInstanceOf( HibernateSearchKnnVectorsFormat.class );
					}
				}
				assertThat( segments ).isPositive();
			}
		}
	}

	@SuppressWarnings("unchecked")
	private List<StandardDirectoryReader> getStandardDirectoryReaders(IndexReader indexReader) {
		return indexReader.getContext().children().stream()
				.map( ctx -> (StandardDirectoryReader) ctx.reader() )
				.collect( Collectors.toList() );
	}

	private static class IndexBinding {
		final IndexFieldReference<byte[]> vectorField;

		IndexBinding(IndexSchemaElement root) {
			vectorField = root.field( "vector", c -> c.asByteVector().dimension( 2 ).maxConnections( 10 ) ).toReference();
		}
	}
}
