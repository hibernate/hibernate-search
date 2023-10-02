/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.lowlevel.index.analysis.impl;

import java.util.Map;

import org.hibernate.search.backend.elasticsearch.gson.impl.SerializeExtraProperties;

import com.google.gson.JsonElement;

/**
 * An abstract base class for analysis-related definitions.
 *
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
