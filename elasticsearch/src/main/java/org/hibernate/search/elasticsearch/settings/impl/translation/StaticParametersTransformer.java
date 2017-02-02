/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.settings.impl.translation;

import java.util.Collections;
import java.util.Map;

import com.google.gson.JsonElement;

/**
 * A {@link ParametersTransformer} that returns Elasticsearch parameters independently from the Lucene parameters.
 *
 * @author Yoann Rodiere
 */
class StaticParametersTransformer implements ParametersTransformer {

	private final Map<String, JsonElement> staticParameters;

	public StaticParametersTransformer(Map<String, JsonElement> staticParameters) {
		this.staticParameters = Collections.unmodifiableMap( staticParameters );
	}

	@Override
	public Map<String, JsonElement> transform(Map<String, String> luceneParameters) {
		return staticParameters;
	}

	@Override
	public String toString() {
		return new StringBuilder( getClass().getSimpleName() )
				.append( "[" )
				.append( staticParameters )
				.append( "]" )
				.toString();
	}
}
