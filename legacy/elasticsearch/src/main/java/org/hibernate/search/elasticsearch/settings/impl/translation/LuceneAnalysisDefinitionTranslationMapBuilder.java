/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.settings.impl.translation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.lucene.analysis.util.AbstractAnalysisFactory;
import org.hibernate.search.elasticsearch.settings.impl.model.AnalysisDefinition;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

/**
 * A utility that helps {@link DefaultElasticsearchAnalyzerDefinitionTranslator} build its translation maps.
 *
 * @author Yoann Rodiere
 */
class LuceneAnalysisDefinitionTranslationMapBuilder<D extends AnalysisDefinition> {

	private final Class<D> targetClass;
	private final Map<String, AnalysisDefinitionFactory<D>> result = new HashMap<>();

	public LuceneAnalysisDefinitionTranslationMapBuilder(Class<D> targetClass) {
		this.targetClass = targetClass;
	}

	public SimpleAnalysisDefinitionFactoryBuilder<D> builder(Class<? extends AbstractAnalysisFactory> luceneClass, String typeName) {
		return new SimpleAnalysisDefinitionFactoryBuilder<>( this, luceneClass, typeName );
	}

	public LuceneAnalysisDefinitionTranslationMapBuilder<D> add(Class<? extends AbstractAnalysisFactory> luceneClass,
			AnalysisDefinitionFactory<D> definitionFactory) {
		result.put( luceneClass.getName(), definitionFactory );
		return this;
	}

	public LuceneAnalysisDefinitionTranslationMapBuilder<D> addJsonPassThrough(Class<? extends AbstractAnalysisFactory> factoryClass) {
		result.put( factoryClass.getName(), new JsonPassThroughAnalysisDefinitionFactory<>( targetClass, factoryClass ) );
		return this;
	}

	public Map<String, AnalysisDefinitionFactory<D>> build() {
		return Collections.unmodifiableMap( result );
	}

	static class SimpleAnalysisDefinitionFactoryBuilder<D extends AnalysisDefinition> {

		private final LuceneAnalysisDefinitionTranslationMapBuilder<D> parent;
		private final Class<? extends AbstractAnalysisFactory> luceneClass;
		private final String typeName;

		private List<ParametersTransformer> parametersTransformers = new ArrayList<>();
		private Map<String, String> parameterNameTranslations;
		private Map<String, ParameterValueTransformer> parameterValueTranslations;
		private Map<String, JsonElement> staticParameters;

		private SimpleAnalysisDefinitionFactoryBuilder(LuceneAnalysisDefinitionTranslationMapBuilder<D> parent, Class<? extends AbstractAnalysisFactory> luceneClass, String typeName) {
			super();
			this.parent = parent;
			this.luceneClass = luceneClass;
			this.typeName = typeName;
		}

		public SimpleAnalysisDefinitionFactoryBuilder<D> transform(ParametersTransformer transformer) {
			parametersTransformers.add( transformer );
			return this;
		}

		public SimpleAnalysisDefinitionFactoryBuilder<D> add(String elasticsearchParam, String value) {
			if ( staticParameters == null ) {
				staticParameters = new LinkedHashMap<>();
			}
			staticParameters.put( elasticsearchParam, new JsonPrimitive( value ) );
			return this;
		}

		public SimpleAnalysisDefinitionFactoryBuilder<D> disallow(String luceneParam) {
			return transform( luceneParam, new ThrowingUnsupportedParameterValueTransformer( luceneClass, luceneParam ) );
		}

		public SimpleAnalysisDefinitionFactoryBuilder<D> mandateAndStrip(String luceneParam, String luceneValue) {
			return transform( new ThrowingMandatoryStrippedParametersTransformer( luceneClass, luceneParam, luceneValue ) );
		}

		public SimpleAnalysisDefinitionFactoryBuilder<D> rename(String luceneParam, String elasticsearchParam) {
			if ( parameterNameTranslations == null ) {
				parameterNameTranslations = new HashMap<>();
			}
			parameterNameTranslations.put( luceneParam, elasticsearchParam );
			return this;
		}

		public MapParameterValueTransformerBuilder<D> transform(String luceneParam) {
			return new MapParameterValueTransformerBuilder<>( this, luceneClass, luceneParam );
		}

		public SimpleAnalysisDefinitionFactoryBuilder<D> transform(String luceneParam, ParameterValueTransformer transformer) {
			if ( parameterValueTranslations == null ) {
				parameterValueTranslations = new HashMap<>();
			}
			parameterValueTranslations.put( luceneParam, transformer );
			return this;
		}

		public LuceneAnalysisDefinitionTranslationMapBuilder<D> end() {
			if ( staticParameters != null ) {
				/*
				 * Add static parameters first, so that they can be overridden by
				 * both custom transformers and the simple transformer added just below.
				 */
				parametersTransformers.add( 0, new StaticParametersTransformer( staticParameters ) );
			}

			if ( parameterNameTranslations == null ) {
				parameterNameTranslations = Collections.emptyMap();
			}
			if ( parameterValueTranslations == null ) {
				parameterValueTranslations = Collections.emptyMap();
			}
			/*
			 * This transformer will only handle those parameters that were not consumed by
			 * custom transformer, so we can safely execute it last.
			 *
			 * We always add this transformer, even when the maps we pass to the constructor are empty,
			 * because it also handles unknown parameters.
			 */
			parametersTransformers.add( new SimpleParametersTransformer( parameterNameTranslations, parameterValueTranslations ) );

			return parent.add( luceneClass, new SimpleAnalysisDefinitionFactory<>(
					parent.targetClass, typeName, parametersTransformers ) );
		}

	}

	static class MapParameterValueTransformerBuilder<D extends AnalysisDefinition> {
		private final SimpleAnalysisDefinitionFactoryBuilder<D> parent;

		private final Class<?> factoryClass;
		private final String parameterName;

		private final Map<String, JsonElement> translations = new HashMap<>();

		private MapParameterValueTransformerBuilder(SimpleAnalysisDefinitionFactoryBuilder<D> parent, Class<?> factoryClass, String parameterName) {
			super();
			this.parent = parent;
			this.factoryClass = factoryClass;
			this.parameterName = parameterName;
		}

		public MapParameterValueTransformerBuilder<D> add(String luceneValue, String elasticsearchValue) {
			return add( luceneValue, new JsonPrimitive( elasticsearchValue ) );
		}

		public MapParameterValueTransformerBuilder<D> add(String luceneValue, JsonElement elasticsearchValue) {
			translations.put( luceneValue, elasticsearchValue );
			return this;
		}

		public SimpleAnalysisDefinitionFactoryBuilder<D> end() {
			return parent.transform( parameterName, new MapParameterValueTransformer( factoryClass, parameterName, translations ) );
		}

	}
}
