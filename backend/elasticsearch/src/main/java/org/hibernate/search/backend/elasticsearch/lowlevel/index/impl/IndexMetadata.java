/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.lowlevel.index.impl;

import java.util.Map;

import org.hibernate.search.backend.elasticsearch.lowlevel.index.aliases.impl.IndexAliasDefinition;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.mapping.impl.RootTypeMapping;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.settings.impl.IndexSettings;

import com.google.gson.GsonBuilder;

/**
 * An object representing metadata of an Elasticsearch index: aliases, mapping, settings, ...
 */
public class IndexMetadata {

	private Map<String, IndexAliasDefinition> aliases;

	private RootTypeMapping mapping;

	private IndexSettings settings;

	public Map<String, IndexAliasDefinition> getAliases() {
		return aliases;
	}

	public void setAliases(Map<String, IndexAliasDefinition> aliases) {
		this.aliases = aliases;
	}

	public RootTypeMapping getMapping() {
		return mapping;
	}

	public void setMapping(RootTypeMapping mapping) {
		this.mapping = mapping;
	}

	public IndexSettings getSettings() {
		return settings;
	}

	public void setSettings(IndexSettings settings) {
		this.settings = settings;
	}

	@Override
	public String toString() {
		return new GsonBuilder().setPrettyPrinting().create().toJson( this );
	}
}
