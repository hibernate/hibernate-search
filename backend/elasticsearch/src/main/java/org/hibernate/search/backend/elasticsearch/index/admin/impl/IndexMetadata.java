/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.index.admin.impl;

import java.util.Map;
import java.util.TreeMap;

import org.hibernate.search.backend.elasticsearch.document.model.impl.esnative.RootTypeMapping;
import org.hibernate.search.backend.elasticsearch.index.settings.impl.esnative.IndexSettings;
import org.hibernate.search.backend.elasticsearch.util.spi.URLEncodedString;

import com.google.gson.GsonBuilder;

/**
 * An object representing an Elasticsearch index.
 *
 * @author Yoann Rodiere
 */
public class IndexMetadata {

	private URLEncodedString name;

	private Map<String, RootTypeMapping> mappings = new TreeMap<>();

	private IndexSettings settings;

	public URLEncodedString getName() {
		return name;
	}

	public void setName(URLEncodedString name) {
		this.name = name;
	}

	public Map<String, RootTypeMapping> getMappings() {
		return mappings;
	}

	public void setMappings(Map<String, RootTypeMapping> mappings) {
		this.mappings = mappings;
	}

	public void putMapping(String name, RootTypeMapping mapping) {
		this.mappings.put( name, mapping );
	}

	public void removeMapping(String name) {
		this.mappings.remove( name );
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
