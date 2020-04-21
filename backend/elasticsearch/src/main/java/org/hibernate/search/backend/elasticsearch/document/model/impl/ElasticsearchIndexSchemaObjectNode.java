/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.document.model.impl;

import java.util.Collections;
import java.util.List;

import org.hibernate.search.engine.backend.common.spi.FieldPaths;
import org.hibernate.search.engine.backend.document.model.dsl.ObjectFieldStorage;


public class ElasticsearchIndexSchemaObjectNode {

	private static final ElasticsearchIndexSchemaObjectNode ROOT =
			// at the root object level the nestedPathHierarchy is empty
			new ElasticsearchIndexSchemaObjectNode( null, null, Collections.emptyList(), null, false );

	public static ElasticsearchIndexSchemaObjectNode root() {
		return ROOT;
	}

	private final ElasticsearchIndexSchemaObjectNode parent;

	private final String absolutePath;

	private final List<String> nestedPathHierarchy;

	private final ObjectFieldStorage storage;

	private final boolean multiValued;

	public ElasticsearchIndexSchemaObjectNode(ElasticsearchIndexSchemaObjectNode parent, String absolutePath, List<String> nestedPathHierarchy,
			ObjectFieldStorage storage,
			boolean multiValued) {
		this.parent = parent;
		this.absolutePath = absolutePath;
		this.nestedPathHierarchy = Collections.unmodifiableList( nestedPathHierarchy );
		this.storage = ObjectFieldStorage.DEFAULT.equals( storage ) ? ObjectFieldStorage.FLATTENED : storage;
		this.multiValued = multiValued;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "[absolutePath=" + absolutePath + "]";
	}

	public ElasticsearchIndexSchemaObjectNode getParent() {
		return parent;
	}

	public String getAbsolutePath() {
		return absolutePath;
	}

	public String getAbsolutePath(String relativeFieldName) {
		return FieldPaths.compose( absolutePath, relativeFieldName );
	}

	public List<String> getNestedPathHierarchy() {
		return nestedPathHierarchy;
	}

	public ObjectFieldStorage getStorage() {
		return storage;
	}

	/**
	 * @return {@code true} if this node is multi-valued in its parent object.
	 */
	public boolean isMultiValued() {
		return multiValued;
	}
}
