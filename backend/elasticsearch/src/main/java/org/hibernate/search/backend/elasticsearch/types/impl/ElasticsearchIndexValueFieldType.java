/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.types.impl;

import org.hibernate.search.backend.elasticsearch.lowlevel.index.mapping.impl.DataTypes;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.mapping.impl.PropertyMapping;
import org.hibernate.search.backend.elasticsearch.search.common.impl.ElasticsearchSearchIndexScope;
import org.hibernate.search.backend.elasticsearch.search.common.impl.ElasticsearchSearchIndexValueFieldContext;
import org.hibernate.search.backend.elasticsearch.search.common.impl.ElasticsearchSearchIndexValueFieldTypeContext;
import org.hibernate.search.backend.elasticsearch.types.codec.impl.ElasticsearchFieldCodec;
import org.hibernate.search.engine.backend.types.spi.AbstractIndexValueFieldType;

import com.google.gson.JsonPrimitive;

public class ElasticsearchIndexValueFieldType<F>
		extends AbstractIndexValueFieldType<
				ElasticsearchSearchIndexScope<?>,
				ElasticsearchSearchIndexValueFieldContext<F>,
				F>
		implements ElasticsearchSearchIndexValueFieldTypeContext<F> {
	private final JsonPrimitive elasticsearchTypeAsJson;
	private final ElasticsearchFieldCodec<F> codec;
	private final PropertyMapping mapping;

	public ElasticsearchIndexValueFieldType(Builder<F> builder) {
		super( builder );
		this.elasticsearchTypeAsJson = builder.elasticsearchTypeAsJson();
		this.codec = builder.codec;
		this.mapping = builder.mapping;
	}

	@Override
	public JsonPrimitive elasticsearchTypeAsJson() {
		return elasticsearchTypeAsJson;
	}

	public ElasticsearchFieldCodec<F> codec() {
		return codec;
	}

	@Override
	public boolean hasNormalizerOnAtLeastOneIndex() {
		return normalizerName().isPresent();
	}

	public PropertyMapping mapping() {
		return mapping;
	}

	public static class Builder<F>
			extends AbstractIndexValueFieldType.Builder<
					ElasticsearchSearchIndexScope<?>,
					ElasticsearchSearchIndexValueFieldContext<F>,
					F> {

		private ElasticsearchFieldCodec<F> codec;
		private final PropertyMapping mapping;

		public Builder(Class<F> valueType, PropertyMapping mapping) {
			super( valueType );
			this.mapping = mapping;
		}

		public void codec(ElasticsearchFieldCodec<F> codec) {
			this.codec = codec;
		}

		public ElasticsearchFieldCodec<F> codec() {
			return codec;
		}

		public PropertyMapping mapping() {
			return mapping;
		}

		@Override
		public ElasticsearchIndexValueFieldType<F> build() {
			return new ElasticsearchIndexValueFieldType<>( this );
		}

		private JsonPrimitive elasticsearchTypeAsJson() {
			String typeName = mapping.getType();
			if ( typeName == null ) {
				// Can happen with user-provided mappings
				typeName = DataTypes.OBJECT;
			}
			return new JsonPrimitive( typeName );
		}
	}
}
