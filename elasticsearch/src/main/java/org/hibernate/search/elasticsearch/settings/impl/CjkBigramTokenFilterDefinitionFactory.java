/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.settings.impl;

import java.util.LinkedHashMap;
import java.util.Map;

import org.hibernate.search.annotations.Parameter;
import org.hibernate.search.elasticsearch.impl.JsonBuilder;
import org.hibernate.search.elasticsearch.settings.impl.model.TokenFilterDefinition;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

class CjkBigramTokenFilterDefinitionFactory implements AnalysisDefinitionFactory<TokenFilterDefinition> {

	@Override
	public String getType() {
		return "cjk_bigram";
	}

	@Override
	public TokenFilterDefinition create(Parameter[] parameters) {
		TokenFilterDefinition definition = new TokenFilterDefinition();

		Map<String, JsonElement> parameterMap = new LinkedHashMap<>();

		JsonBuilder.Array ignoredScriptsBuilder = JsonBuilder.array();

		for ( Parameter parameter : parameters ) {
			String name = parameter.name();
			String value = parameter.value();

			switch ( name ) {
				case "han":
				case "hiragana":
				case "katakana":
				case "hangul":
					if ( !Boolean.parseBoolean( value ) ) {
						ignoredScriptsBuilder.add( new JsonPrimitive( name ) );
					}
					break;
				case "outputUnigrams":
					parameterMap.put( "output_unigrams", new JsonPrimitive( Boolean.parseBoolean( value ) ) );
					break;
				default:
					parameterMap.put( name, new JsonPrimitive( value ) );
					break;
			}
		}

		JsonArray ignoredScripts = ignoredScriptsBuilder.build();
		if ( ignoredScripts.size() > 0 ) {
			parameterMap.put( "ignored_scripts", ignoredScripts );
		}

		if ( !parameterMap.isEmpty() ) {
			definition.setParameters( parameterMap );
		}

		return definition;
	}

}