/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.backend.lucene.mapping;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.types.VectorSimilarity;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.extension.SearchSetupHelper;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.SimpleMappedIndex;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class LuceneVectorFieldCheckIT {

	@RegisterExtension
	public final SearchSetupHelper setupHelper = SearchSetupHelper.create();
	SimpleMappedIndex<IndexBinding> cosine = SimpleMappedIndex.of( r -> new IndexBinding( r, VectorSimilarity.COSINE ) )
			.name( "cosine" );
	SimpleMappedIndex<IndexBinding> dotProduct =
			SimpleMappedIndex.of( r -> new IndexBinding( r, VectorSimilarity.DOT_PRODUCT ) )
					.name( "dotProduct" );

	@BeforeEach
	void setUp() {
		setupHelper.start()
				.withIndex( cosine )
				.withIndex( dotProduct )
				.setup();
	}

	@Test
	void failIndex() {
		assertThatThrownBy( () -> cosine.bulkIndexer().add( "some-id", document -> {
			document.addValue( cosine.binding().bytes, new byte[] { 0, 0, 0, 0 } );
		} ).join() )
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "The cosine vector similarity cannot process vectors with 0 magnitude" );

		assertThatThrownBy( () -> cosine.bulkIndexer().add( "some-id", document -> {
			document.addValue( cosine.binding().floats, new float[] { 0.0f, 0.0f, 0.0f, 0.0f } );
		} ).join() )
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "The cosine vector similarity cannot process vectors with 0 magnitude" );

		assertThatThrownBy( () -> dotProduct.bulkIndexer().add( "some-id", document -> {
			document.addValue( dotProduct.binding().floats, new float[] { 10000.0f, 0.0f, 0.0f, 0.0f } );
		} ).join() )
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "The dot product vector similarity cannot process non-unit magnitude vectors" );
	}

	@Test
	void failQuery() {
		assertThatThrownBy(
				() -> cosine.query().select().where( f -> f.knn( 4 ).field( "bytes" ).matching( new byte[] { 0, 0, 0, 0 } ) )
		).isInstanceOf( SearchException.class )
				.hasMessageContaining( "The cosine vector similarity cannot process vectors with 0 magnitude" );

		assertThatThrownBy(
				() -> cosine.query().select()
						.where( f -> f.knn( 4 ).field( "floats" ).matching( new float[] { 0.0f, 0.0f, 0.0f, 0.0f } ) )
		).isInstanceOf( SearchException.class )
				.hasMessageContaining( "The cosine vector similarity cannot process vectors with 0 magnitude" );

		assertThatThrownBy(
				() -> dotProduct.query().select()
						.where( f -> f.knn( 4 ).field( "floats" ).matching( new float[] { 10000.0f, 0.0f, 0.0f, 0.0f } ) )
		).isInstanceOf( SearchException.class )
				.hasMessageContaining( "The dot product vector similarity cannot process non-unit magnitude vectors" );
	}

	private static class IndexBinding {

		final IndexFieldReference<byte[]> bytes;
		final IndexFieldReference<float[]> floats;

		IndexBinding(IndexSchemaElement root, VectorSimilarity similarity) {
			bytes = root.field( "bytes", f -> f.asByteVector().dimension( 4 ).vectorSimilarity( similarity ) ).toReference();
			floats = root.field( "floats", f -> f.asFloatVector().dimension( 4 ).vectorSimilarity( similarity ) ).toReference();
		}
	}
}
