package org.hibernate.search.backend.elasticsearch.lowlevel.index.analysis.impl;

import java.util.Map;

import org.hibernate.search.backend.elasticsearch.json.impl.SerializeExtraProperties;
import org.hibernate.search.backend.elasticsearch.json.internal.spi.JsonProviderHelper;

import tools.jackson.databind.JsonNode;

/**
 * An abstract base class for analysis-related definitions.
 *
 */
public abstract class AnalysisDefinition {

	private String type;

	@SerializeExtraProperties
	private Map<String, JsonNode> parameters;

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public Map<String, JsonNode> getParameters() {
		return parameters;
	}

	public void setParameters(Map<String, JsonNode> parameters) {
		this.parameters = parameters;
	}

	@Override
	public String toString() {
		return JsonProviderHelper.toPrettyJson( this );
	}
}
