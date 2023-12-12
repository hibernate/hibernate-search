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
	private final Integer maxConnections;
	private final Integer beamWidth;

	protected AbstractElasticsearchVectorFieldCodec(VectorSimilarity similarity, int dimension,
			Integer maxConnections, Integer beamWidth) {
		this.similarity = similarity;
		this.dimension = dimension;
		this.maxConnections = maxConnections;
		this.beamWidth = beamWidth;
	}

	@Override
	public JsonElement encode(F value) {
		if ( value == null ) {
			return JsonNull.INSTANCE;
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
				&& Objects.equals( maxConnections, that.maxConnections )
				&& Objects.equals( beamWidth, that.beamWidth );
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "{" +
				"vectorSimilarity=" + similarity +
				", dimension=" + dimension +
				", beamWidth=" + beamWidth +
				", maxConnection=" + maxConnections +
				'}';
	}
}
