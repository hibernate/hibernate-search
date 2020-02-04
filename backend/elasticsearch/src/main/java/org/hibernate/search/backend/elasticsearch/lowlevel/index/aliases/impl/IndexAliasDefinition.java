/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.lowlevel.index.aliases.impl;

import java.util.Map;

import org.hibernate.search.backend.elasticsearch.gson.impl.SerializeExtraProperties;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.annotations.SerializedName;

/**
 * An alias definition for an Elasticsearch index.
 */
/*
 * CAUTION: JSON serialization is controlled by this specific adapter, which must be
 * updated whenever fields of this class are added, renamed or removed.
 */
@JsonAdapter(IndexAliasDefinitionJsonAdapterFactory.class)
public class IndexAliasDefinition {

	@SerializedName("is_write_index")
	private Boolean isWriteIndex;

	@SerializeExtraProperties
	private Map<String, JsonElement> extraAttributes;

	@Override
	public String toString() {
		return new GsonBuilder().setPrettyPrinting().create().toJson( this );
	}

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
}
