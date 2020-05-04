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


public class ElasticsearchIndexSchemaObjectFieldNode implements ElasticsearchIndexSchemaObjectNode {

	private final ElasticsearchIndexSchemaObjectNode parent;
	private final JsonAccessor<JsonElement> relativeAccessor;
	private final String absolutePath;
	private final IndexFieldInclusion inclusion;

	private final List<String> nestedPathHierarchy;

	private final ObjectFieldStorage storage;

	private final boolean multiValued;

	public ElasticsearchIndexSchemaObjectFieldNode(ElasticsearchIndexSchemaObjectNode parent, String relativeFieldName,
			IndexFieldInclusion inclusion, ObjectFieldStorage storage, boolean multiValued) {
		this.parent = parent;
		this.absolutePath = parent.getAbsolutePath( relativeFieldName );
		this.relativeAccessor = JsonAccessor.root().property( relativeFieldName );
		this.inclusion = parent.getInclusion().compose( inclusion );
		// at the root object level the nestedPathHierarchy is empty
		List<String> theNestedPathHierarchy = parent.getNestedPathHierarchy();
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

	@Override
	public String getAbsolutePath() {
		return absolutePath;
	}

	@Override
	public String getAbsolutePath(String relativeFieldName) {
		return FieldPaths.compose( absolutePath, relativeFieldName );
	}

	@Override
	public IndexFieldInclusion getInclusion() {
		return inclusion;
	}

	@Override
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
