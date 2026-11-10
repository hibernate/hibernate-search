package org.hibernate.search.backend.elasticsearch.types.codec.impl;

import org.hibernate.search.backend.elasticsearch.json.impl.JsonScalarConversions;
import org.hibernate.search.backend.elasticsearch.json.internal.spi.JsonCodecService;
import org.hibernate.search.engine.backend.types.VectorSimilarity;
import org.hibernate.search.util.common.AssertionFailure;

import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.JsonNodeFactory;

public class ElasticsearchByteVectorFieldCodec extends AbstractElasticsearchVectorFieldCodec<byte[]> {

	public ElasticsearchByteVectorFieldCodec(JsonCodecService json, VectorSimilarity similarity, int dimension, Integer m,
			Integer efConstruction, byte[] indexNullAs) {
		super( json, similarity, dimension, m, efConstruction, indexNullAs );
	}

	@Override
	protected ArrayNode toJsonArray(byte[] value) {
		ArrayNode array = JsonNodeFactory.instance.arrayNode( value.length );
		for ( byte element : value ) {
			array.add( element );
		}
		return array;
	}

	@Override
	protected byte[] fromJsonArray(ArrayNode jsonElements) {
		int size = jsonElements.size();
		byte[] result = new byte[size];
		for ( int i = 0; i < size; i++ ) {
			result[i] = JsonScalarConversions.asByte( jsonElements.get( i ) );
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
