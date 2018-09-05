/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.settings.impl.model;

import java.util.Map;

import org.hibernate.search.elasticsearch.gson.impl.SerializeExtraProperties;

import com.google.gson.JsonElement;

/**
 * An abstract base class for analysis-related definitions.
 *
 * @author Yoann Rodiere
 */
public abstract class AnalysisDefinition {

	private String type;

	@SerializeExtraProperties
	private Map<String, JsonElement> parameters;

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public Map<String, JsonElement> getParameters() {
		return parameters;
	}

	public void setParameters(Map<String, JsonElement> parameters) {
		this.parameters = parameters;
	}

}
