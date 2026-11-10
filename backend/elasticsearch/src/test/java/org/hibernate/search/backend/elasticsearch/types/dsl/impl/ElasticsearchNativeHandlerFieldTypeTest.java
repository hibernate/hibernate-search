package org.hibernate.search.backend.elasticsearch.types.dsl.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.hibernate.search.backend.elasticsearch.gson.GsonJsonHandlers;
import org.hibernate.search.backend.elasticsearch.json.internal.spi.JsonProviderHelper;
import org.hibernate.search.backend.elasticsearch.json.jackson.JacksonJsonHandlers;
import org.hibernate.search.backend.elasticsearch.types.impl.ElasticsearchIndexValueFieldType;
import org.hibernate.search.engine.backend.types.IndexFieldType;

import org.junit.jupiter.api.Test;

import com.google.gson.JsonElement;

import tools.jackson.databind.JsonNode;

class ElasticsearchNativeHandlerFieldTypeTest {
	@Test
	void nativeBindingRetainsIndexTypeAndConverterMetadataAcrossIndependentMappingHandler() {
		ElasticsearchIndexFieldTypeBuildContext context = mock( ElasticsearchIndexFieldTypeBuildContext.class );
		when( context.getUserFacingCodec() ).thenReturn( JsonProviderHelper.create( false ).getCodec() );
		IndexFieldType<JsonNode> jackson =
				new ElasticsearchNativeIndexFieldTypeHandlerMappingStep<>( context, JacksonJsonHandlers.value() )
						.mapping( GsonJsonHandlers.object(), GsonJsonHandlers.object().fromJson( "{\"type\":\"keyword\"}" ) )
						.toIndexFieldType();
		ElasticsearchIndexValueFieldType<JsonNode> jacksonType = (ElasticsearchIndexValueFieldType<JsonNode>) jackson;
		assertThat( jacksonType.valueClass() ).isEqualTo( JsonNode.class );
		assertThat( jacksonType.mappingDslConverter().valueType() ).isEqualTo( JsonNode.class );
		assertThat( jacksonType.mappingProjectionConverter().valueType() ).isEqualTo( JsonNode.class );
		IndexFieldType<JsonElement> gson =
				new ElasticsearchNativeIndexFieldTypeHandlerMappingStep<>( context, GsonJsonHandlers.value() )
						.mapping( "{\"type\":\"keyword\"}" ).toIndexFieldType();
		assertThat( ( (ElasticsearchIndexValueFieldType<JsonElement>) gson ).valueClass() ).isEqualTo( JsonElement.class );
	}

	@Test
	@SuppressWarnings("deprecation")
	void arrayOptionControlsCodecAndCrossIndexCompatibilityForBothLibrariesAndLegacyDsl() {
		var context = mock( ElasticsearchIndexFieldTypeBuildContext.class );
		when( context.getUserFacingCodec() ).thenReturn( JsonProviderHelper.create( false ).getCodec() );
		var legacy =
				(ElasticsearchIndexValueFieldType<
						JsonElement>) new ElasticsearchNativeIndexFieldTypeLegacyMappingStep( context )
								.mapping( "{\"type\":\"keyword\"}" ).toIndexFieldType();
		var gson = (ElasticsearchIndexValueFieldType<
				JsonElement>) new ElasticsearchNativeIndexFieldTypeHandlerMappingStep<>( context, GsonJsonHandlers.value() )
						.mapping( "{\"type\":\"keyword\"}" ).toIndexFieldType();
		var gsonAtomic = (ElasticsearchIndexValueFieldType<
				JsonElement>) new ElasticsearchNativeIndexFieldTypeHandlerMappingStep<>( context, GsonJsonHandlers.value() )
						.mapping( "{\"type\":\"keyword\"}" ).arrayAsValue().toIndexFieldType();
		var legacyAtomic =
				(ElasticsearchIndexValueFieldType<
						JsonElement>) new ElasticsearchNativeIndexFieldTypeLegacyMappingStep( context )
								.mapping( "{\"type\":\"keyword\"}" ).arrayAsValue().toIndexFieldType();
		var jackson = (ElasticsearchIndexValueFieldType<
				JsonNode>) new ElasticsearchNativeIndexFieldTypeHandlerMappingStep<>( context, JacksonJsonHandlers.value() )
						.mapping( "{\"type\":\"keyword\"}" ).toIndexFieldType();
		var jacksonAtomic = (ElasticsearchIndexValueFieldType<
				JsonNode>) new ElasticsearchNativeIndexFieldTypeHandlerMappingStep<>( context, JacksonJsonHandlers.value() )
						.mapping( "{\"type\":\"keyword\"}" ).arrayAsValue().toIndexFieldType();
		assertThat( legacy.codec().isCompatibleWith( gson.codec() ) ).isTrue();
		assertThat( gson.codec().canDecodeArrays() ).isFalse();
		assertThat( jackson.codec().canDecodeArrays() ).isFalse();
		assertThat( gsonAtomic.codec().canDecodeArrays() ).isTrue();
		assertThat( jacksonAtomic.codec().canDecodeArrays() ).isTrue();
		assertThat( legacyAtomic.codec().isCompatibleWith( gsonAtomic.codec() ) ).isTrue();
		assertThat( gsonAtomic.codec().isCompatibleWith( gson.codec() ) ).isFalse();
		assertThat( jacksonAtomic.codec().isCompatibleWith( jackson.codec() ) ).isFalse();
	}

	@Test
	@SuppressWarnings("deprecation")
	void legacyNativeFactoryInterfacesPreserveGsonSpecializationAndOneParameterOptionsArity() {
		ElasticsearchIndexFieldTypeBuildContext context = mock( ElasticsearchIndexFieldTypeBuildContext.class );
		when( context.getUserFacingCodec() ).thenReturn( JsonProviderHelper.create( false ).getCodec() );
		IndexFieldType<JsonElement> legacy = new ElasticsearchNativeIndexFieldTypeLegacyMappingStep( context )
				.mapping( GsonJsonHandlers.object().fromJson( "{\"type\":\"keyword\"}" ) ).toIndexFieldType();
		var legacyType = (ElasticsearchIndexValueFieldType<JsonElement>) legacy;
		assertThat( legacyType.valueClass() ).isEqualTo( JsonElement.class );
		assertThat( legacyType.codec().canDecodeArrays() ).isFalse();
	}
}
