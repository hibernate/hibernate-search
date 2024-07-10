/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.types.impl;

import java.util.Optional;
import java.util.function.Consumer;

import org.hibernate.search.backend.elasticsearch.lowlevel.index.mapping.impl.DataTypes;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.mapping.impl.PropertyMapping;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.settings.impl.PropertyMappingIndexSettingsContributor;
import org.hibernate.search.backend.elasticsearch.search.common.impl.ElasticsearchSearchIndexScope;
import org.hibernate.search.backend.elasticsearch.search.common.impl.ElasticsearchSearchIndexValueFieldContext;
import org.hibernate.search.backend.elasticsearch.search.common.impl.ElasticsearchSearchIndexValueFieldTypeContext;
import org.hibernate.search.backend.elasticsearch.types.codec.impl.ElasticsearchFieldCodec;
import org.hibernate.search.engine.backend.types.converter.ToDocumentValueConverter;
import org.hibernate.search.engine.backend.types.converter.runtime.ToDocumentValueConvertContext;
import org.hibernate.search.engine.backend.types.converter.spi.DslConverter;
import org.hibernate.search.engine.backend.types.converter.spi.ProjectionConverter;
import org.hibernate.search.engine.backend.types.spi.AbstractIndexValueFieldType;

import com.google.gson.JsonElement;
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

	private final Consumer<PropertyMappingIndexSettingsContributor> indexSettingsContributor;
	private final ProjectionConverter<?, ?> rawProjectionConverter;
	private final DslConverter<?, ?> rawDslConverter;


	public ElasticsearchIndexValueFieldType(Builder<F> builder) {
		super( builder );
		this.elasticsearchTypeAsJson = builder.elasticsearchTypeAsJson();
		this.codec = builder.codec;
		this.mapping = builder.mapping;
		this.indexSettingsContributor = builder.indexSettingsContributor;

		this.rawProjectionConverter = ProjectionConverter.passThrough( String.class );
		this.rawDslConverter = new DslConverter<>( String.class, new RawNoParsingDslConverter() );
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

	public Optional<Consumer<PropertyMappingIndexSettingsContributor>> additionalIndexSettings() {
		return Optional.ofNullable( indexSettingsContributor );
	}

	@Override
	public DslConverter<?, ?> rawDslConverter() {
		return rawDslConverter;
	}

	@Override
	public ProjectionConverter<?, ?> rawProjectionConverter() {
		return rawProjectionConverter;
	}

	public static class Builder<F>
			extends AbstractIndexValueFieldType.Builder<
					ElasticsearchSearchIndexScope<?>,
					ElasticsearchSearchIndexValueFieldContext<F>,
					F> {

		private ElasticsearchFieldCodec<F> codec;
		private final PropertyMapping mapping;
		private Consumer<PropertyMappingIndexSettingsContributor> indexSettingsContributor;

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

		public void contributeAdditionalIndexSettings(
				Consumer<PropertyMappingIndexSettingsContributor> indexSettingsContributor) {
			this.indexSettingsContributor = indexSettingsContributor;
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

	private static class RawNoParsingDslConverter implements ToDocumentValueConverter<String, JsonElement> {

		@Override
		public JsonElement toDocumentValue(String value, ToDocumentValueConvertContext context) {
			return new JsonPrimitive( value );
		}
	}
}
