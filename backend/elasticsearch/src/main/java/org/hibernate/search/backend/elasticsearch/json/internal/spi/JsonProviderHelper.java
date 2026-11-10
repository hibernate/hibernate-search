package org.hibernate.search.backend.elasticsearch.json.internal.spi;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

import org.hibernate.search.backend.elasticsearch.client.common.json.internal.spi.JsonProvider;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.aliases.impl.GeneratedIndexAliasDefinitionJsonCodec;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.aliases.impl.IndexAliasDefinition;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.analysis.impl.AnalyzerDefinition;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.analysis.impl.CharFilterDefinition;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.analysis.impl.GeneratedAnalyzerDefinitionJsonCodec;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.analysis.impl.GeneratedCharFilterDefinitionJsonCodec;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.analysis.impl.GeneratedNormalizerDefinitionJsonCodec;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.analysis.impl.GeneratedTokenFilterDefinitionJsonCodec;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.analysis.impl.GeneratedTokenizerDefinitionJsonCodec;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.analysis.impl.NormalizerDefinition;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.analysis.impl.TokenFilterDefinition;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.analysis.impl.TokenizerDefinition;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.impl.GeneratedIndexMetadataJsonCodec;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.impl.IndexMetadata;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.mapping.impl.DynamicTemplate;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.mapping.impl.DynamicType;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.mapping.impl.DynamicTypeJsonCodec;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.mapping.impl.ElasticsearchDenseVectorIndexOptions;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.mapping.impl.GeneratedDynamicTemplateJsonCodec;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.mapping.impl.GeneratedElasticsearchDenseVectorIndexOptionsJsonCodec;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.mapping.impl.GeneratedOpenSearchVectorTypeMethodJsonCodec;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.mapping.impl.GeneratedOpenSearchVectorTypeMethod_ParametersJsonCodec;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.mapping.impl.GeneratedPropertyMappingJsonCodec;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.mapping.impl.GeneratedRootTypeMappingJsonCodec;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.mapping.impl.NamedDynamicTemplate;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.mapping.impl.NamedDynamicTemplateJsonCodec;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.mapping.impl.OpenSearchVectorTypeMethod;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.mapping.impl.PropertyMapping;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.mapping.impl.RootTypeMapping;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.mapping.impl.RoutingType;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.mapping.impl.RoutingTypeJsonCodec;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.settings.impl.Analysis;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.settings.impl.GeneratedAnalysisJsonCodec;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.settings.impl.GeneratedIndexSettingsJsonCodec;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.settings.impl.IndexSettings;
import org.hibernate.search.util.common.annotation.Incubating;
import org.hibernate.search.util.json.internal.spi.JsonCodec;
import org.hibernate.search.util.json.internal.spi.JsonCodecRegistry;
import org.hibernate.search.util.json.internal.spi.JsonParsing;
import org.hibernate.search.util.json.internal.spi.JsonValidation;

/** Explicit registrations for the backend's known schema model types. */
@Incubating
public final class JsonProviderHelper {
	private static final JsonCodecRegistry REGISTRY = registry();

	private JsonProviderHelper() {
	}

	private static JsonCodecRegistry registry() {
		Map<Class<?>, Function<JsonCodecRegistry, JsonCodec<?>>> factories = new LinkedHashMap<>();
		factories.put( IndexAliasDefinition.class,
				GeneratedIndexAliasDefinitionJsonCodec::new );
		factories.put( AnalyzerDefinition.class,
				GeneratedAnalyzerDefinitionJsonCodec::new );
		factories.put( CharFilterDefinition.class,
				GeneratedCharFilterDefinitionJsonCodec::new );
		factories.put( NormalizerDefinition.class,
				GeneratedNormalizerDefinitionJsonCodec::new );
		factories.put( TokenFilterDefinition.class,
				GeneratedTokenFilterDefinitionJsonCodec::new );
		factories.put( TokenizerDefinition.class,
				GeneratedTokenizerDefinitionJsonCodec::new );
		factories.put( Analysis.class,
				GeneratedAnalysisJsonCodec::new );
		factories.put( IndexSettings.class,
				GeneratedIndexSettingsJsonCodec::new );
		factories.put( DynamicTemplate.class,
				GeneratedDynamicTemplateJsonCodec::new );
		factories.put(
				ElasticsearchDenseVectorIndexOptions.class,
				GeneratedElasticsearchDenseVectorIndexOptionsJsonCodec::new );
		factories.put( OpenSearchVectorTypeMethod.class,
				GeneratedOpenSearchVectorTypeMethodJsonCodec::new );
		factories.put(
				OpenSearchVectorTypeMethod.Parameters.class,
				GeneratedOpenSearchVectorTypeMethod_ParametersJsonCodec::new );
		factories.put( PropertyMapping.class,
				GeneratedPropertyMappingJsonCodec::new );
		factories.put( RootTypeMapping.class,
				GeneratedRootTypeMappingJsonCodec::new );
		factories.put( DynamicType.class, r -> new DynamicTypeJsonCodec() );
		factories.put( RoutingType.class, r -> new RoutingTypeJsonCodec() );
		factories.put( NamedDynamicTemplate.class, NamedDynamicTemplateJsonCodec::new );
		factories.put( IndexMetadata.class,
				GeneratedIndexMetadataJsonCodec::new );
		return new JsonCodecRegistry( factories );
	}

	public static JsonProvider create(boolean logPrettyPrinting) {
		return new JsonProvider( REGISTRY, logPrettyPrinting );
	}

	public static JsonProvider create(boolean logPrettyPrinting, JsonValidation validation) {
		return new JsonProvider( REGISTRY, logPrettyPrinting, validation );
	}

	public static JsonProvider create(boolean logPrettyPrinting, JsonValidation validation, JsonParsing parsing) {
		return new JsonProvider( REGISTRY, logPrettyPrinting, validation, parsing );
	}

	public static JsonCodecService createUserFacingCodec(JsonValidation validation) {
		return new JsonCodecService( REGISTRY, false, true, validation );
	}

	public static JsonCodecService createUserFacingCodec(JsonValidation validation, JsonParsing parsing) {
		return new JsonCodecService( REGISTRY, false, true, validation, parsing );
	}

	public static JsonCodecService createUserFacingCodec() {
		return new JsonCodecService( REGISTRY, false, true );
	}

	public static String toPrettyJson(Object value) {
		return createUserFacingCodec().write( value );
	}
}
