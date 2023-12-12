/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.types.codec.impl;

import org.hibernate.search.engine.backend.types.VectorSimilarity;

import com.google.gson.JsonArray;

public class ElasticsearchFloatVectorFieldCodec extends AbstractElasticsearchVectorFieldCodec<float[]> {

	public ElasticsearchFloatVectorFieldCodec(VectorSimilarity similarity, int dimension, Integer maxConnections,
			Integer beamWidth) {
		super( similarity, dimension, maxConnections, beamWidth );
	}

	@Override
	protected JsonArray toJsonArray(float[] value) {
		JsonArray array = new JsonArray( value.length );
		for ( float element : value ) {
			array.add( element );
		}
		return array;
	}

	@Override
	protected float[] fromJsonArray(JsonArray jsonElements) {
		int size = jsonElements.size();
		float[] result = new float[size];
		for ( int i = 0; i < size; i++ ) {
			result[i] = jsonElements.get( i ).getAsFloat();
		}
		return result;
	}
}
