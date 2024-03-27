/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.gson.spi;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public final class GsonClasses {

	private GsonClasses() {
	}

	/**
	 * @return A set of names of all classes that will be involved in GSON serialization and will require reflection support.
	 * Useful to enable reflection for these classes in GraalVM-based native images.
	 */
	public static Set<String> typesRequiringReflection() {
		return new HashSet<>( Arrays.asList(
				"org.hibernate.search.backend.elasticsearch.gson.impl.AbstractConfiguredExtraPropertiesJsonAdapterFactory",
				"org.hibernate.search.backend.elasticsearch.gson.impl.AbstractConfiguredExtraPropertiesJsonAdapterFactory$Adapter",
				"org.hibernate.search.backend.elasticsearch.gson.impl.AbstractExtraPropertiesJsonAdapter",
				"org.hibernate.search.backend.elasticsearch.lowlevel.index.aliases.impl.IndexAliasDefinition",
				"org.hibernate.search.backend.elasticsearch.lowlevel.index.analysis.impl.AbstractCompositeAnalysisDefinition",
				"org.hibernate.search.backend.elasticsearch.lowlevel.index.analysis.impl.AnalysisDefinition",
				"org.hibernate.search.backend.elasticsearch.lowlevel.index.analysis.impl.AnalyzerDefinition",
				"org.hibernate.search.backend.elasticsearch.lowlevel.index.analysis.impl.CharFilterDefinition",
				"org.hibernate.search.backend.elasticsearch.lowlevel.index.analysis.impl.NormalizerDefinition",
				"org.hibernate.search.backend.elasticsearch.lowlevel.index.analysis.impl.TokenFilterDefinition",
				"org.hibernate.search.backend.elasticsearch.lowlevel.index.analysis.impl.TokenizerDefinition",
				"org.hibernate.search.backend.elasticsearch.lowlevel.index.mapping.impl.AbstractTypeMapping",
				"org.hibernate.search.backend.elasticsearch.lowlevel.index.mapping.impl.DynamicTemplate",
				"org.hibernate.search.backend.elasticsearch.lowlevel.index.mapping.impl.DynamicType",
				"org.hibernate.search.backend.elasticsearch.lowlevel.index.mapping.impl.FormatJsonAdapter",
				"org.hibernate.search.backend.elasticsearch.lowlevel.index.mapping.impl.NamedDynamicTemplate",
				"org.hibernate.search.backend.elasticsearch.lowlevel.index.mapping.impl.NamedDynamicTemplateJsonAdapterFactory$Adapter",
				"org.hibernate.search.backend.elasticsearch.lowlevel.index.mapping.impl.PropertyMapping",
				"org.hibernate.search.backend.elasticsearch.lowlevel.index.mapping.impl.RootTypeMapping",
				"org.hibernate.search.backend.elasticsearch.lowlevel.index.mapping.impl.RoutingType",
				"org.hibernate.search.backend.elasticsearch.lowlevel.index.mapping.impl.RoutingTypeJsonAdapter",
				"org.hibernate.search.backend.elasticsearch.lowlevel.index.settings.impl.Analysis",
				"org.hibernate.search.backend.elasticsearch.lowlevel.index.settings.impl.IndexSettings",
				"org.hibernate.search.backend.elasticsearch.lowlevel.index.aliases.impl.IndexAliasDefinitionJsonAdapterFactory",
				"org.hibernate.search.backend.elasticsearch.lowlevel.index.analysis.impl.AnalysisDefinitionJsonAdapterFactory",
				"org.hibernate.search.backend.elasticsearch.lowlevel.index.analysis.impl.AnalyzerDefinitionJsonAdapterFactory",
				"org.hibernate.search.backend.elasticsearch.lowlevel.index.analysis.impl.NormalizerDefinitionJsonAdapterFactory",
				"org.hibernate.search.backend.elasticsearch.lowlevel.index.mapping.impl.AbstractTypeMappingJsonAdapterFactory",
				"org.hibernate.search.backend.elasticsearch.lowlevel.index.mapping.impl.DynamicTemplateJsonAdapterFactory",
				"org.hibernate.search.backend.elasticsearch.lowlevel.index.mapping.impl.NamedDynamicTemplateJsonAdapterFactory",
				"org.hibernate.search.backend.elasticsearch.lowlevel.index.mapping.impl.PropertyMappingJsonAdapterFactory",
				"org.hibernate.search.backend.elasticsearch.lowlevel.index.mapping.impl.RootTypeMappingJsonAdapterFactory",
				"org.hibernate.search.backend.elasticsearch.lowlevel.index.settings.impl.AnalysisJsonAdapterFactory",
				"org.hibernate.search.backend.elasticsearch.lowlevel.index.settings.impl.IndexSettingsJsonAdapterFactory",
				"org.hibernate.search.backend.elasticsearch.lowlevel.index.mapping.impl.ElasticsearchDenseVectorIndexOptions",
				"org.hibernate.search.backend.elasticsearch.lowlevel.index.mapping.impl.OpenSearchVectorTypeMethod",
				"org.hibernate.search.backend.elasticsearch.lowlevel.index.mapping.impl.OpenSearchVectorTypeMethod$Parameters",
				"org.hibernate.search.backend.elasticsearch.lowlevel.index.mapping.impl.ElasticsearchDenseVectorIndexOptionsJsonAdapterFactory",
				"org.hibernate.search.backend.elasticsearch.lowlevel.index.mapping.impl.OpenSearchVectorTypeMethodJsonAdapterFactory",
				"org.hibernate.search.backend.elasticsearch.lowlevel.index.mapping.impl.OpenSearchVectorTypeMethodJsonAdapterFactory$ParametersJsonAdapterFactory"
		) );
	}

}
