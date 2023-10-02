/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.types.codec.impl;

import org.hibernate.search.engine.backend.types.VectorSimilarity;

import com.google.gson.JsonArray;

public class ElasticsearchByteVectorFieldCodec extends AbstractElasticsearchVectorFieldCodec<byte[]> {

	public ElasticsearchByteVectorFieldCodec(VectorSimilarity similarity, int dimension, Integer m,
			Integer efConstruction, byte[] indexNullAs) {
		super( similarity, dimension, m, efConstruction, indexNullAs );
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
}
