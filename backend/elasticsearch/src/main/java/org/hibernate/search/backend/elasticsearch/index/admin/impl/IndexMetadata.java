/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.index.admin.impl;

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

	private URLEncodedString typeName;

	private RootTypeMapping mapping;

	private IndexSettings settings;

	public URLEncodedString getName() {
		return name;
	}

	public void setName(URLEncodedString name) {
		this.name = name;
	}

	public URLEncodedString getTypeName() {
		return typeName;
	}

	public void setTypeName(URLEncodedString typeName) {
		this.typeName = typeName;
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
