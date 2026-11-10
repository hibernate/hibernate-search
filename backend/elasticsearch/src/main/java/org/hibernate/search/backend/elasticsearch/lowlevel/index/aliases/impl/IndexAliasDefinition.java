package org.hibernate.search.backend.elasticsearch.lowlevel.index.aliases.impl;

import java.util.Map;

import org.hibernate.search.backend.elasticsearch.json.impl.JsonName;
import org.hibernate.search.backend.elasticsearch.json.impl.JsonSerializable;
import org.hibernate.search.backend.elasticsearch.json.impl.SerializeExtraProperties;
import org.hibernate.search.backend.elasticsearch.json.internal.spi.JsonProviderHelper;

import tools.jackson.databind.JsonNode;

/**
 * An alias definition for an Elasticsearch index.
 */
@JsonSerializable
public class IndexAliasDefinition {

	@JsonName("is_write_index")
	private Boolean isWriteIndex;

	@SerializeExtraProperties
	private Map<String, JsonNode> extraAttributes;

	public Boolean getWriteIndex() {
		return isWriteIndex;
	}

	public void setWriteIndex(Boolean writeIndex) {
		isWriteIndex = writeIndex;
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
