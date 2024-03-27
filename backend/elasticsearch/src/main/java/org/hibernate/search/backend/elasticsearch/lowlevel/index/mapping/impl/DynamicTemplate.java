/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.lowlevel.index.mapping.impl;

import java.util.Map;

import org.hibernate.search.backend.elasticsearch.gson.impl.SerializeExtraProperties;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.annotations.SerializedName;

/**
 * An object representing an Elasticsearch dynamic template.
 * <p>
 * This is the inner object in the array of templates, wrapped in a {@link NamedDynamicTemplate}.
 * See https://www.elastic.co/guide/en/elasticsearch/reference/current/dynamic-templates.html
 */
@JsonAdapter(DynamicTemplateJsonAdapterFactory.class)
public class DynamicTemplate {

	@SerializedName("match_mapping_type")
	private String matchMappingType;

	@SerializedName("path_match")
	private String pathMatch;

	private PropertyMapping mapping;

	@SerializeExtraProperties
	private Map<String, JsonElement> extraAttributes;

	@Override
	public String toString() {
		return new GsonBuilder().setPrettyPrinting().create().toJson( this );
	}

	public String getMatchMappingType() {
		return matchMappingType;
	}

	public void setMatchMappingType(String matchMappingType) {
		this.matchMappingType = matchMappingType;
	}

	public String getPathMatch() {
		return pathMatch;
	}

	public void setPathMatch(String pathMatch) {
		this.pathMatch = pathMatch;
	}

	public PropertyMapping getMapping() {
		return mapping;
	}

	public void setMapping(PropertyMapping mapping) {
		this.mapping = mapping;
	}

	public Map<String, JsonElement> getExtraAttributes() {
		return extraAttributes;
	}

	public void setExtraAttributes(Map<String, JsonElement> extraAttributes) {
		this.extraAttributes = extraAttributes;
	}
}
