package org.hibernate.search.backend.elasticsearch.lowlevel.index.mapping.impl;

import java.util.Map;

import org.hibernate.search.backend.elasticsearch.json.impl.JsonName;
import org.hibernate.search.backend.elasticsearch.json.impl.JsonSerializable;
import org.hibernate.search.backend.elasticsearch.json.impl.SerializeExtraProperties;
import org.hibernate.search.backend.elasticsearch.json.internal.spi.JsonProviderHelper;

import tools.jackson.databind.JsonNode;

/**
 * An object representing OpenSearch K-NN vector Method attributes.
 *
 * See https://opensearch.org/docs/latest/field-types/supported-field-types/knn-vector/
 */
@JsonSerializable
public class OpenSearchVectorTypeMethod {

	private String name;

	@JsonName("space_type")
	private String spaceType;

	private String engine;

	private Parameters parameters;

	@SerializeExtraProperties
	private Map<String, JsonNode> extraAttributes;

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

	public Map<String, JsonNode> getExtraAttributes() {
		return extraAttributes;
	}

	public void setExtraAttributes(Map<String, JsonNode> extraAttributes) {
		this.extraAttributes = extraAttributes;
	}

	@Override
	public String toString() {
		return JsonProviderHelper.toPrettyJson( this );
	}

	@JsonSerializable
	public static class Parameters {

		@JsonName("ef_construction")
		private Integer efConstruction;
		private Integer m;

		@SerializeExtraProperties
		private Map<String, JsonNode> extraAttributes;

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

		public Map<String, JsonNode> getExtraAttributes() {
			return extraAttributes;
		}

		public void setExtraAttributes(Map<String, JsonNode> extraAttributes) {
			this.extraAttributes = extraAttributes;
		}

		@Override
		public String toString() {
			return JsonProviderHelper.toPrettyJson( this );
		}
	}
}
