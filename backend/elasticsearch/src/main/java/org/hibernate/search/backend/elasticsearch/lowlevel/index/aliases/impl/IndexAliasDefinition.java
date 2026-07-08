/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.lowlevel.index.aliases.impl;

import java.util.Map;

import org.hibernate.search.backend.elasticsearch.gson.impl.GsonSerializable;
import org.hibernate.search.backend.elasticsearch.gson.impl.SerializeExtraProperties;
import org.hibernate.search.backend.elasticsearch.gson.spi.GsonProviderHelper;

import com.google.gson.JsonElement;
import com.google.gson.annotations.SerializedName;

/**
 * An alias definition for an Elasticsearch index.
 */
@GsonSerializable
public class IndexAliasDefinition {

	@SerializedName("is_write_index")
	private Boolean isWriteIndex;

	@SerializeExtraProperties
	private Map<String, JsonElement> extraAttributes;

	public Boolean getWriteIndex() {
		return isWriteIndex;
	}

	public void setWriteIndex(Boolean writeIndex) {
		isWriteIndex = writeIndex;
	}

	public Map<String, JsonElement> getExtraAttributes() {
		return extraAttributes;
	}

	public void setExtraAttributes(Map<String, JsonElement> extraAttributes) {
		this.extraAttributes = extraAttributes;
	}

	@Override
	public String toString() {
		return GsonProviderHelper.toPrettyJson( this );
	}
}
