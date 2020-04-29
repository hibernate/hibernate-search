/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.document.model.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.hibernate.search.backend.elasticsearch.gson.impl.JsonAccessor;
import org.hibernate.search.engine.backend.common.spi.FieldPaths;
import org.hibernate.search.engine.backend.document.model.dsl.ObjectFieldStorage;
import org.hibernate.search.engine.backend.document.model.spi.IndexFieldInclusion;

import com.google.gson.JsonElement;


public class ElasticsearchIndexSchemaObjectNode {

	private static final ElasticsearchIndexSchemaObjectNode ROOT =
			new ElasticsearchIndexSchemaObjectNode( null, null, IndexFieldInclusion.INCLUDED,
					null, false );

	public static ElasticsearchIndexSchemaObjectNode root() {
		return ROOT;
	}

	private final ElasticsearchIndexSchemaObjectNode parent;
	private final JsonAccessor<JsonElement> relativeAccessor;
	private final String absolutePath;
	private final IndexFieldInclusion inclusion;

	private final List<String> nestedPathHierarchy;

	private final ObjectFieldStorage storage;

	private final boolean multiValued;

	public ElasticsearchIndexSchemaObjectNode(ElasticsearchIndexSchemaObjectNode parent, String relativeFieldName,
			IndexFieldInclusion inclusion, ObjectFieldStorage storage, boolean multiValued) {
		this.parent = parent;
		this.absolutePath = parent == null ? relativeFieldName : parent.getAbsolutePath( relativeFieldName );
		this.relativeAccessor = relativeFieldName == null ? null : JsonAccessor.root().property( relativeFieldName );
		this.inclusion = parent == null ? inclusion : parent.getInclusion().compose( inclusion );
		// at the root object level the nestedPathHierarchy is empty
		List<String> theNestedPathHierarchy = parent == null ? Collections.emptyList() : parent.getNestedPathHierarchy();
		if ( ObjectFieldStorage.NESTED.equals( storage ) ) {
			// if we found a nested object, we add it to the nestedPathHierarchy
			theNestedPathHierarchy = new ArrayList<>( theNestedPathHierarchy );
			theNestedPathHierarchy.add( absolutePath );
		}
		this.nestedPathHierarchy = Collections.unmodifiableList( theNestedPathHierarchy );
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

	public JsonAccessor<JsonElement> getRelativeAccessor() {
		return relativeAccessor;
	}

	public String getAbsolutePath() {
		return absolutePath;
	}

	public String getAbsolutePath(String relativeFieldName) {
		return FieldPaths.compose( absolutePath, relativeFieldName );
	}

	public IndexFieldInclusion getInclusion() {
		return inclusion;
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
