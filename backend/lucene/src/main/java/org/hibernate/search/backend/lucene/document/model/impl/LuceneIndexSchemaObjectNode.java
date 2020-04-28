/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.document.model.impl;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.hibernate.search.engine.backend.common.spi.FieldPaths;
import org.hibernate.search.engine.backend.document.model.dsl.ObjectFieldStorage;


public class LuceneIndexSchemaObjectNode {

	private static final LuceneIndexSchemaObjectNode ROOT = new LuceneIndexSchemaObjectNode(
			null, null, Collections.emptyList(),
			null, false,
			// we do not store childrenAbsolutePaths for the root node
			Collections.emptyList()
	);

	public static LuceneIndexSchemaObjectNode root() {
		return ROOT;
	}

	private final LuceneIndexSchemaObjectNode parent;

	private final String absolutePath;

	private final List<String> nestedPathHierarchy;

	private final ObjectFieldStorage storage;

	private final boolean multiValued;

	private final List<String> childrenAbsolutePaths;

	public LuceneIndexSchemaObjectNode(LuceneIndexSchemaObjectNode parent, String absolutePath, List<String> nestedPathHierarchy,
			ObjectFieldStorage storage, boolean multiValued,
			List<String> childrenRelativeNames) {
		this.parent = parent;
		this.absolutePath = absolutePath;
		this.nestedPathHierarchy = Collections.unmodifiableList( nestedPathHierarchy );
		this.storage = storage;
		this.multiValued = multiValued;
		this.childrenAbsolutePaths = childrenRelativeNames.stream()
				.map( relativeFieldName -> FieldPaths.compose( absolutePath, relativeFieldName ) )
				.collect( Collectors.toList() );
	}

	public LuceneIndexSchemaObjectNode getParent() {
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

	public List<String> getChildrenAbsolutePaths() {
		return childrenAbsolutePaths;
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

	@Override
	public String toString() {
		return getClass().getSimpleName() + "[absolutePath=" + absolutePath + ", storage=" + storage + "]";
	}
}
