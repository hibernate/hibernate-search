package org.hibernate.search.backend.elasticsearch.types.codec.impl;

import org.hibernate.search.backend.elasticsearch.json.impl.JsonScalarConversions;
import org.hibernate.search.backend.elasticsearch.json.internal.spi.JsonCodecService;
import org.hibernate.search.engine.backend.types.VectorSimilarity;
import org.hibernate.search.util.common.AssertionFailure;

import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.JsonNodeFactory;

public class ElasticsearchFloatVectorFieldCodec extends AbstractElasticsearchVectorFieldCodec<float[]> {

	public ElasticsearchFloatVectorFieldCodec(JsonCodecService json, VectorSimilarity similarity, int dimension, Integer m,
			Integer efConstruction, float[] indexNullAs) {
		super( json, similarity, dimension, m, efConstruction, indexNullAs );
	}

	@Override
	protected ArrayNode toJsonArray(float[] value) {
		ArrayNode array = JsonNodeFactory.instance.arrayNode( value.length );
		for ( float element : value ) {
			array.add( element );
		}
		return array;
	}

	@Override
	protected float[] fromJsonArray(ArrayNode jsonElements) {
		int size = jsonElements.size();
		float[] result = new float[size];
		for ( int i = 0; i < size; i++ ) {
			result[i] = JsonScalarConversions.asFloat( jsonElements.get( i ) );
		}
		return result;
	}

	@Override
	public Class<?> vectorElementsType() {
		return float.class;
	}

	@Override
	public float scoreToSimilarity(float score) {
		switch ( similarity ) {
			case DEFAULT:
			case L2:
				return (float) Math.sqrt( 1.0f / score - 1.0f );
			case DOT_PRODUCT:
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
