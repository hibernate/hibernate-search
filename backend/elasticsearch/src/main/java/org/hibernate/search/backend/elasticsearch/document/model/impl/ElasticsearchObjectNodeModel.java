/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.document.model.impl;

import org.hibernate.search.engine.backend.document.model.ObjectFieldStorage;

/**
 * @author Yoann Rodiere
 */
public class ElasticsearchObjectNodeModel {

	private static final ElasticsearchObjectNodeModel ROOT =
			new ElasticsearchObjectNodeModel( null, null, null );

	public static ElasticsearchObjectNodeModel root() {
		return ROOT;
	}

	private final ElasticsearchObjectNodeModel parent;

	private final String absolutePath;

	private final ObjectFieldStorage storage;

	public ElasticsearchObjectNodeModel(ElasticsearchObjectNodeModel parent, String absolutePath,
			ObjectFieldStorage storage) {
		this.parent = parent;
		this.absolutePath = absolutePath;
		this.storage = storage;
	}

	public ElasticsearchObjectNodeModel getParent() {
		return parent;
	}

	public String getAbsolutePath() {
		return absolutePath;
	}

	public String getAbsolutePath(String relativeName) {
		return absolutePath == null ? relativeName : absolutePath + "." + relativeName;
	}

	public ObjectFieldStorage getStorage() {
		return storage;
	}
}
