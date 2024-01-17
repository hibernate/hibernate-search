/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.types.codec.impl;

import java.util.Objects;

import org.hibernate.search.backend.elasticsearch.gson.impl.JsonElementTypes;
import org.hibernate.search.engine.backend.types.VectorSimilarity;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;

public abstract class AbstractElasticsearchVectorFieldCodec<F> implements ElasticsearchVectorFieldCodec<F> {

	private final VectorSimilarity similarity;
	private final int dimension;
	private final Integer m;
	private final Integer efConstruction;
	private final F indexNullAs;

	protected AbstractElasticsearchVectorFieldCodec(VectorSimilarity similarity, int dimension,
			Integer m, Integer efConstruction, F indexNullAs) {
		this.similarity = similarity;
		this.dimension = dimension;
		this.m = m;
		this.efConstruction = efConstruction;
		this.indexNullAs = indexNullAs;
	}

	@Override
	public JsonElement encode(F value) {
		if ( value == null ) {
			if ( indexNullAs == null ) {
				return JsonNull.INSTANCE;
			}
			value = indexNullAs;
		}
		return toJsonArray( value );
	}

	protected abstract JsonArray toJsonArray(F value);

	@Override
	public F decode(JsonElement element) {
		if ( element == null || element.isJsonNull() ) {
			return null;
		}
		return fromJsonArray( JsonElementTypes.ARRAY.fromElement( element ) );
	}

	protected abstract F fromJsonArray(JsonArray jsonElements);

	@Override
	public int getConfiguredDimensions() {
		return dimension;
	}

	@Override
	public boolean isCompatibleWith(ElasticsearchFieldCodec<?> other) {

		if ( this == other ) {
			return true;
		}
		if ( other == null || getClass() != other.getClass() ) {
			return false;
		}
		AbstractElasticsearchVectorFieldCodec<?> that = (AbstractElasticsearchVectorFieldCodec<?>) other;
		return dimension == that.dimension
				&& Objects.equals( similarity, that.similarity )
				&& Objects.equals( m, that.m )
				&& Objects.equals( efConstruction, that.efConstruction );
	}

	@Override
	public boolean canDecodeArrays() {
		return true;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "{" +
				"vectorSimilarity=" + similarity +
				", dimension=" + dimension +
				", efConstruction=" + efConstruction +
				", m=" + m +
				'}';
	}
}
