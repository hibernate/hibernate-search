/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.document.model.impl;

import org.hibernate.search.engine.backend.document.model.dsl.ObjectFieldStorage;
import org.hibernate.search.backend.lucene.util.impl.LuceneFields;

/**
 * @author Guillaume Smet
 */
public class LuceneIndexSchemaObjectNode {

	private static final LuceneIndexSchemaObjectNode ROOT =
			new LuceneIndexSchemaObjectNode( null, null, null, false );

	public static LuceneIndexSchemaObjectNode root() {
		return ROOT;
	}

	private final LuceneIndexSchemaObjectNode parent;

	private final String absolutePath;

	private final ObjectFieldStorage storage;

	private final boolean multiValued;

	public LuceneIndexSchemaObjectNode(LuceneIndexSchemaObjectNode parent, String absolutePath,
			ObjectFieldStorage storage, boolean multiValued) {
		this.parent = parent;
		this.absolutePath = absolutePath;
		this.storage = storage;
		this.multiValued = multiValued;
	}

	public LuceneIndexSchemaObjectNode getParent() {
		return parent;
	}

	public String getAbsolutePath() {
		return absolutePath;
	}

	public String getAbsolutePath(String relativeFieldName) {
		return LuceneFields.compose( absolutePath, relativeFieldName );
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
