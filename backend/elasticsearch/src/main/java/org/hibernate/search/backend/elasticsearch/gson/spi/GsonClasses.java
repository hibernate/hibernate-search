/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.gson.spi;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.hibernate.search.backend.elasticsearch.lowlevel.index.aliases.impl.IndexAliasDefinition;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.aliases.impl.IndexAliasDefinitionJsonAdapterFactory;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.analysis.impl.AnalysisDefinition;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.analysis.impl.AnalysisDefinitionJsonAdapterFactory;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.analysis.impl.AnalyzerDefinition;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.analysis.impl.AnalyzerDefinitionJsonAdapterFactory;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.analysis.impl.CharFilterDefinition;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.analysis.impl.NormalizerDefinition;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.analysis.impl.NormalizerDefinitionJsonAdapterFactory;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.analysis.impl.TokenFilterDefinition;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.analysis.impl.TokenizerDefinition;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.mapping.impl.AbstractTypeMapping;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.mapping.impl.DynamicTemplate;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.mapping.impl.DynamicTemplateJsonAdapterFactory;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.mapping.impl.DynamicType;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.mapping.impl.FormatJsonAdapter;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.mapping.impl.NamedDynamicTemplate;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.mapping.impl.NamedDynamicTemplateJsonAdapterFactory;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.mapping.impl.PropertyMapping;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.mapping.impl.PropertyMappingJsonAdapterFactory;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.mapping.impl.RootTypeMapping;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.mapping.impl.RootTypeMappingJsonAdapterFactory;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.mapping.impl.RoutingType;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.mapping.impl.RoutingTypeJsonAdapter;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.settings.impl.Analysis;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.settings.impl.AnalysisJsonAdapterFactory;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.settings.impl.IndexSettings;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.settings.impl.IndexSettingsJsonAdapterFactory;

public final class GsonClasses {

	private GsonClasses() {
	}

	/**
	 * @return A set of all classes that will be involved in GSON serialization and will require reflection support.
	 * Useful to enable reflection for these classes in GraalVM-based native images.
	 */
	public static Set<Class<?>> typesRequiringReflection() {
		List<Class<?>> base = Arrays.asList(
				AbstractTypeMapping.class,
				DynamicType.class,
				FormatJsonAdapter.class,
				RoutingTypeJsonAdapter.class,
				PropertyMapping.class,
				PropertyMappingJsonAdapterFactory.class,
				RootTypeMapping.class,
				RootTypeMappingJsonAdapterFactory.class,
				RoutingType.class,
				IndexSettings.class,
				IndexSettingsJsonAdapterFactory.class,
				Analysis.class,
				AnalysisJsonAdapterFactory.class,
				AnalysisDefinition.class,
				AnalyzerDefinition.class,
				AnalyzerDefinitionJsonAdapterFactory.class,
				NormalizerDefinition.class,
				NormalizerDefinitionJsonAdapterFactory.class,
				TokenizerDefinition.class,
				TokenFilterDefinition.class,
				CharFilterDefinition.class,
				AnalysisDefinitionJsonAdapterFactory.class,
				IndexAliasDefinition.class,
				IndexAliasDefinitionJsonAdapterFactory.class,
				DynamicTemplate.class,
				DynamicTemplateJsonAdapterFactory.class,
				NamedDynamicTemplate.class,
				NamedDynamicTemplateJsonAdapterFactory.class
		);
		Set<Class<?>> result = new LinkedHashSet<>();
		for ( Class<?> clazz : base ) {
			Class<?> currentClass = clazz;
			while ( currentClass != Object.class ) {
				result.add( currentClass );
				currentClass = currentClass.getSuperclass();
			}
		}
		return result;
	}
}
