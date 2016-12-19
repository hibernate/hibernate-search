/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.settings.impl;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.hibernate.search.elasticsearch.impl.JsonBuilder;
import org.hibernate.search.util.impl.CollectionHelper;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

class CjkBigramIgnoredScriptsParametersTransformer implements ParametersTransformer {

	private static final Set<String> SCRIPTS = Collections.unmodifiableSet(
			CollectionHelper.asSet( "han", "hiragana", "katakana", "hangul" ) );

	@Override
	public Map<String, JsonElement> transform(Map<String, String> luceneParameters) {
		JsonBuilder.Array ignoredScriptsBuilder = JsonBuilder.array();

		for ( String script : SCRIPTS ) {
			String value = luceneParameters.remove( script );

			if ( value != null && !Boolean.parseBoolean( value ) ) {
				ignoredScriptsBuilder.add( new JsonPrimitive( script ) );
			}
		}

		JsonArray ignoredScripts = ignoredScriptsBuilder.build();
		if ( ignoredScripts.size() > 0 ) {
			Map<String, JsonElement> result = new LinkedHashMap<>();
			result.put( "ignored_scripts", ignoredScripts );
			return result;
		}
		else {
			return Collections.emptyMap();
		}
	}

}