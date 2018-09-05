/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.settings.impl.translation;

import java.util.Map;

import com.google.gson.JsonElement;

public interface ParametersTransformer {

	/**
	 * Extracts Lucene parameters from the given map and returns the corresponding Elasticsearch parameters.
	 * <p>
	 * Lucene parameters that have been taken care of should be removed from {@code luceneParameters}, so that
	 * other transformers won't attempt to transform those.
	 *
	 * @param luceneParameters The parameters of a Lucene analysis factory. Elements can be removed from this
	 * map to indicate they have been taken care of.
	 * @return A set of parameters with the same overall meaning, but following the Elasticsearch syntax.
	 */
	Map<String, JsonElement> transform(Map<String, String> luceneParameters);

}