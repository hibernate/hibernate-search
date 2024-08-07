/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.types.codec.impl;

import org.hibernate.search.engine.backend.types.VectorSimilarity;
import org.hibernate.search.util.common.AssertionFailure;

import com.google.gson.Gson;
import com.google.gson.JsonArray;

public class ElasticsearchByteVectorFieldCodec extends AbstractElasticsearchVectorFieldCodec<byte[]> {

	public ElasticsearchByteVectorFieldCodec(Gson gson, VectorSimilarity similarity, int dimension, Integer m,
			Integer efConstruction, byte[] indexNullAs) {
		super( gson, similarity, dimension, m, efConstruction, indexNullAs );
	}

	@Override
	protected JsonArray toJsonArray(byte[] value) {
		JsonArray array = new JsonArray( value.length );
		for ( byte element : value ) {
			array.add( element );
		}
		return array;
	}

	@Override
	protected byte[] fromJsonArray(JsonArray jsonElements) {
		int size = jsonElements.size();
		byte[] result = new byte[size];
		for ( int i = 0; i < size; i++ ) {
			result[i] = jsonElements.get( i ).getAsByte();
		}
		return result;
	}

	@Override
	public Class<?> vectorElementsType() {
		return byte.class;
	}

	@Override
	public float scoreToSimilarity(float score) {
		switch ( similarity ) {
			case DEFAULT:
			case L2:
				return (float) Math.sqrt( 1.0f / score - 1.0f );
			case DOT_PRODUCT:
				// 0.5f + similarity / (float) (dim * (1 << 15));
				return ( score - 0.5f ) * (float) ( dimension * ( 1 << 15 ) );
			case COSINE:
				return 2.0f * score - 1.0f;
			case MAX_INNER_PRODUCT:
				if ( score < 1 ) {
					return -1.0f * ( 1.0f / score - 1.0f );
				}
				else {
					return score - 1.0f;
				}
			default:
				throw new AssertionFailure( "Unknown similarity function: " + similarity );
		}
	}
}
