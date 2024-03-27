/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.lowlevel.index.mapping.impl;

import java.util.Map;

import org.hibernate.search.backend.elasticsearch.gson.impl.SerializeExtraProperties;

import com.google.gson.JsonElement;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.annotations.SerializedName;

/**
 * An object representing OpenSearch K-NN vector Method attributes.
 *
 * See https://opensearch.org/docs/latest/field-types/supported-field-types/knn-vector/
 */
/*
 * CAUTION:
 * 1. JSON serialization is controlled by a specific adapter, which must be
 * updated whenever fields of this class are added, renamed or removed.
 *
 * 2. Whenever adding more properties consider adding property validation to PropertyMappingValidator.
 */
@JsonAdapter(OpenSearchVectorTypeMethodJsonAdapterFactory.class)
public class OpenSearchVectorTypeMethod {

	private String name;

	@SerializedName("space_type")
	private String spaceType;

	private String engine;

	private Parameters parameters;

	@SerializeExtraProperties
	private Map<String, JsonElement> extraAttributes;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getSpaceType() {
		return spaceType;
	}

	public void setSpaceType(String spaceType) {
		this.spaceType = spaceType;
	}

	public String getEngine() {
		return engine;
	}

	public void setEngine(String engine) {
		this.engine = engine;
	}

	public Parameters getParameters() {
		return parameters;
	}

	public void setParameters(Parameters parameters) {
		this.parameters = parameters;
	}

	public Map<String, JsonElement> getExtraAttributes() {
		return extraAttributes;
	}

	public void setExtraAttributes(Map<String, JsonElement> extraAttributes) {
		this.extraAttributes = extraAttributes;
	}

	@JsonAdapter(OpenSearchVectorTypeMethodJsonAdapterFactory.ParametersJsonAdapterFactory.class)
	public static class Parameters {

		@SerializedName("ef_construction")
		private Integer efConstruction;
		private Integer m;

		@SerializeExtraProperties
		private Map<String, JsonElement> extraAttributes;

		public Integer getEfConstruction() {
			return efConstruction;
		}

		public void setEfConstruction(Integer efConstruction) {
			this.efConstruction = efConstruction;
		}

		public Integer getM() {
			return m;
		}

		public void setM(Integer m) {
			this.m = m;
		}

		public Map<String, JsonElement> getExtraAttributes() {
			return extraAttributes;
		}

		public void setExtraAttributes(Map<String, JsonElement> extraAttributes) {
			this.extraAttributes = extraAttributes;
		}
	}
}
