/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.gson.spi;

import java.util.Set;
import java.util.function.Supplier;

import org.hibernate.search.backend.elasticsearch.client.common.gson.spi.GsonProvider;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.mapping.impl.DynamicType;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.mapping.impl.DynamicTypeJsonAdapter;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.mapping.impl.NamedDynamicTemplateJsonAdapterFactory;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.mapping.impl.RootTypeMapping;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.mapping.impl.RoutingType;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.mapping.impl.RoutingTypeJsonAdapter;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.settings.impl.IndexSettings;
import org.hibernate.search.util.common.impl.CollectionHelper;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

/**
 * Centralizes the configuration of the Gson objects.
 */
public final class GsonProviderHelper {

	private GsonProviderHelper() {
	}

	/*
	 * See https://github.com/google/gson/issues/764.
	 *
	 * Only composite type adapters (referring to another type adapter)
	 * should be affected by this bug, so we'll list the corresponding types here.
	 * Maybe it's even narrower, and only type adapters that indirectly refer to themselves are affected,
	 * but I'm not entirely sure about that.
	 *
	 * Note we only need to list "root" types:
	 * all types they refer to will also have their type adapter initialized.
	 */
	private static final Set<TypeToken<?>> TYPES_CAUSING_GSON_CONCURRENT_INITIALIZATION_BUG =
			CollectionHelper.asImmutableSet(
					TypeToken.get( IndexSettings.class ),
					TypeToken.get( RootTypeMapping.class )
			);

	private static final Gson PRETTY_PRINTING_GSON = createConfiguredGsonBuilder()
			.setPrettyPrinting()
			.create();

	public static GsonProvider create(Supplier<GsonBuilder> builderBaseSupplier, boolean logPrettyPrinting) {
		Supplier<GsonBuilder> withAdapters = () -> registerAdapters( builderBaseSupplier.get() );
		return GsonProvider.create( withAdapters, logPrettyPrinting, TYPES_CAUSING_GSON_CONCURRENT_INITIALIZATION_BUG );
	}

	public static Gson createUserFacingGson() {
		return createConfiguredGsonBuilder()
				.setPrettyPrinting()
				.create();
	}

	public static String toPrettyJson(Object obj) {
		return PRETTY_PRINTING_GSON.toJson( obj );
	}

	private static GsonBuilder createConfiguredGsonBuilder() {
		return registerAdapters( new GsonBuilder() );
	}

	private static GsonBuilder registerAdapters(GsonBuilder builder) {
		// Generated reflection-free adapters
		// NOTE: FQCNs are used here on purpose -- javadoc cannot handle correctly generated classes
		builder.registerTypeAdapterFactory(
				new org.hibernate.search.backend.elasticsearch.lowlevel.index.aliases.impl.GeneratedIndexAliasDefinitionTypeAdapterFactory() );
		builder.registerTypeAdapterFactory(
				new org.hibernate.search.backend.elasticsearch.lowlevel.index.analysis.impl.GeneratedAnalyzerDefinitionTypeAdapterFactory() );
		builder.registerTypeAdapterFactory(
				new org.hibernate.search.backend.elasticsearch.lowlevel.index.analysis.impl.GeneratedCharFilterDefinitionTypeAdapterFactory() );
		builder.registerTypeAdapterFactory(
				new org.hibernate.search.backend.elasticsearch.lowlevel.index.analysis.impl.GeneratedNormalizerDefinitionTypeAdapterFactory() );
		builder.registerTypeAdapterFactory(
				new org.hibernate.search.backend.elasticsearch.lowlevel.index.analysis.impl.GeneratedTokenFilterDefinitionTypeAdapterFactory() );
		builder.registerTypeAdapterFactory(
				new org.hibernate.search.backend.elasticsearch.lowlevel.index.analysis.impl.GeneratedTokenizerDefinitionTypeAdapterFactory() );
		builder.registerTypeAdapterFactory(
				new org.hibernate.search.backend.elasticsearch.lowlevel.index.settings.impl.GeneratedAnalysisTypeAdapterFactory() );
		builder.registerTypeAdapterFactory(
				new org.hibernate.search.backend.elasticsearch.lowlevel.index.settings.impl.GeneratedIndexSettingsTypeAdapterFactory() );
		builder.registerTypeAdapterFactory(
				new org.hibernate.search.backend.elasticsearch.lowlevel.index.mapping.impl.GeneratedDynamicTemplateTypeAdapterFactory() );
		builder.registerTypeAdapterFactory(
				new org.hibernate.search.backend.elasticsearch.lowlevel.index.mapping.impl.GeneratedElasticsearchDenseVectorIndexOptionsTypeAdapterFactory() );
		builder.registerTypeAdapterFactory(
				new org.hibernate.search.backend.elasticsearch.lowlevel.index.mapping.impl.GeneratedOpenSearchVectorTypeMethodTypeAdapterFactory() );
		builder.registerTypeAdapterFactory(
				new org.hibernate.search.backend.elasticsearch.lowlevel.index.mapping.impl.GeneratedOpenSearchVectorTypeMethod_ParametersTypeAdapterFactory() );
		builder.registerTypeAdapterFactory(
				new org.hibernate.search.backend.elasticsearch.lowlevel.index.mapping.impl.GeneratedPropertyMappingTypeAdapterFactory() );
		builder.registerTypeAdapterFactory(
				new org.hibernate.search.backend.elasticsearch.lowlevel.index.mapping.impl.GeneratedRootTypeMappingTypeAdapterFactory() );
		// Hand-written adapters (custom serialization logic)
		builder.registerTypeAdapterFactory( new NamedDynamicTemplateJsonAdapterFactory() );
		builder.registerTypeAdapter( DynamicType.class, new DynamicTypeJsonAdapter() );
		builder.registerTypeAdapter( RoutingType.class, new RoutingTypeJsonAdapter() );
		return builder;
	}

}
