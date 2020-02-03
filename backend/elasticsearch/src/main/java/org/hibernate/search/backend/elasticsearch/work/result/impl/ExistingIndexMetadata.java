/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.work.result.impl;

import org.hibernate.search.backend.elasticsearch.lowlevel.index.impl.IndexMetadata;

import com.google.gson.GsonBuilder;

/**
 * An object representing an Elasticsearch index that actually exists on the cluster.
 */
public final class ExistingIndexMetadata {

	private final String primaryName;

	private final IndexMetadata metadata;

	public ExistingIndexMetadata(String primaryName, IndexMetadata metadata) {
		this.primaryName = primaryName;
		this.metadata = metadata;
	}

	@Override
	public String toString() {
		return new GsonBuilder().setPrettyPrinting().create().toJson( this );
	}

	/**
	 * @return The primary name of the index, i.e. its un-aliased name.
	 */
	public String getPrimaryName() {
		return primaryName;
	}

	public IndexMetadata getMetadata() {
		return metadata;
	}
}
