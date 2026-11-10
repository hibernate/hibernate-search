package org.hibernate.search.backend.elasticsearch.lowlevel.index.mapping.impl;

import java.util.Map;

import org.hibernate.search.backend.elasticsearch.json.impl.JsonName;
import org.hibernate.search.backend.elasticsearch.json.impl.JsonSerializable;
import org.hibernate.search.backend.elasticsearch.json.impl.SerializeExtraProperties;
import org.hibernate.search.backend.elasticsearch.json.internal.spi.JsonProviderHelper;

import tools.jackson.databind.JsonNode;

/**
 * An object representing an Elasticsearch dynamic template.
 * <p>
 * This is the inner object in the array of templates, wrapped in a {@link NamedDynamicTemplate}.
 * See https://www.elastic.co/guide/en/elasticsearch/reference/current/dynamic-templates.html
 */
@JsonSerializable
public class DynamicTemplate {

	@JsonName("match_mapping_type")
	private String matchMappingType;

	@JsonName("path_match")
	private String pathMatch;

	private PropertyMapping mapping;

	@SerializeExtraProperties
	private Map<String, JsonNode> extraAttributes;

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
