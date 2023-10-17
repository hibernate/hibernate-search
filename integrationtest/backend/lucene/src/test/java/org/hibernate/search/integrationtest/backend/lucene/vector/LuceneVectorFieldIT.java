/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.lucene.vector;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.search.integrationtest.backend.lucene.testsupport.util.DocumentAssert.containsDocument;

import java.util.List;

import org.hibernate.search.backend.lucene.LuceneExtension;
import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.types.Projectable;
import org.hibernate.search.engine.backend.types.VectorSimilarity;
import org.hibernate.search.engine.search.query.SearchQuery;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.extension.SearchSetupHelper;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.SimpleMappedIndex;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.apache.lucene.document.Document;

class LuceneVectorFieldIT {

	private static final byte[] BYTE_VECTOR_1 = new byte[] { 1, 2, 3, 4 };
	private static final byte[] BYTE_VECTOR_2 = new byte[] { 1, 1, 1, 1 };

	private static final float[] FLOAT_VECTOR_1 = new float[] { 1.0f, 2.0f, 3.0f, 4.0f, 1.0f, 2.0f, 3.0f, 4.0f };
	private static final float[] FLOAT_VECTOR_2 = new float[] { 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f };

	@RegisterExtension
	public final SearchSetupHelper setupHelper = SearchSetupHelper.create();

	private final SimpleMappedIndex<IndexBinding> index = SimpleMappedIndex.of( IndexBinding::new );

	@BeforeEach
	void setup() {
		setupHelper.start()
				.withIndex( index )
				.setup();
		initData();
	}

	@Test
	void simpleVectorSavedAndRetrieved() {
		SearchQuery<Document> query = index.createScope().query()
				.select(
						f -> f.extension( LuceneExtension.get() ).document()
				)
				.where( f -> f.matchAll() )
				.toQuery();

		List<Document> result = query.fetchAll().hits();
		assertThat( result )
				.hasSize( 2 )
				.satisfies( containsDocument(
						doc -> doc.hasField( "string", "keyword1" )
								.hasVectorField( "byteVector", BYTE_VECTOR_1 )
								.hasVectorField( "floatVector", FLOAT_VECTOR_1 )
								.andOnlyInternalFields()
				) )
				.satisfies( containsDocument(
						doc -> doc.hasField( "string", "keyword2" )
								.hasVectorField( "byteVector", BYTE_VECTOR_2 )
								.hasVectorField( "floatVector", FLOAT_VECTOR_2 )
								.andOnlyInternalFields()
				) );
	}

	private void initData() {
		index.bulkIndexer()
				.add( "ID:1", document -> {
					document.addValue( index.binding().string, "keyword1" );
					document.addValue( index.binding().byteVector, BYTE_VECTOR_1 );
					document.addValue( index.binding().floatVector, FLOAT_VECTOR_1 );
				} )
				.add( "ID:2", document -> {
					document.addValue( index.binding().string, "keyword2" );
					document.addValue( index.binding().byteVector, BYTE_VECTOR_2 );
					document.addValue( index.binding().floatVector, FLOAT_VECTOR_2 );
				} )
				.join();
	}

	private static class IndexBinding {
		final IndexFieldReference<String> string;
		final IndexFieldReference<byte[]> byteVector;
		final IndexFieldReference<float[]> floatVector;

		IndexBinding(IndexSchemaElement root) {
			string = root.field( "string", f -> f.asString().projectable( Projectable.YES ) ).toReference();
			byteVector = root
					.field( "byteVector",
							f -> f.asByteVector( 4 ).projectable( Projectable.YES ).maxConnections( 16 )
									.vectorSimilarity( VectorSimilarity.L2 ) )
					.toReference();
			floatVector = root
					.field( "floatVector",
							f -> f.asFloatVector( 8 ).projectable( Projectable.YES ).maxConnections( 48 )
									.beamWidth( 256 ).vectorSimilarity( VectorSimilarity.INNER_PRODUCT ) )
					.toReference();
		}
	}
}
