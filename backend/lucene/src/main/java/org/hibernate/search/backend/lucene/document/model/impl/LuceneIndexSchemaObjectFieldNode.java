/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.document.model.impl;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.hibernate.search.backend.lucene.logging.impl.Log;
import org.hibernate.search.engine.backend.common.spi.FieldPaths;
import org.hibernate.search.engine.backend.document.model.dsl.ObjectFieldStorage;
import org.hibernate.search.engine.backend.document.model.spi.IndexFieldInclusion;
import org.hibernate.search.engine.backend.metamodel.IndexCompositeElementDescriptor;
import org.hibernate.search.engine.backend.metamodel.IndexObjectFieldDescriptor;
import org.hibernate.search.engine.backend.metamodel.IndexObjectFieldTypeDescriptor;
import org.hibernate.search.engine.backend.metamodel.IndexValueFieldDescriptor;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;


public class LuceneIndexSchemaObjectFieldNode
		implements IndexObjectFieldDescriptor, LuceneIndexSchemaObjectNode, IndexObjectFieldTypeDescriptor {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final LuceneIndexSchemaObjectNode parent;
	private final String absolutePath;
	private final String relativeName;
	private final IndexFieldInclusion inclusion;

	private final List<String> nestedPathHierarchy;

	private final ObjectFieldStorage storage;

	private final boolean multiValued;

	private final List<String> childrenAbsolutePaths;

	public LuceneIndexSchemaObjectFieldNode(LuceneIndexSchemaObjectNode parent, String relativeName,
			IndexFieldInclusion inclusion, ObjectFieldStorage storage, boolean multiValued,
			List<String> childrenRelativeNames) {
		this.parent = parent;
		this.absolutePath = parent.getAbsolutePath( relativeName );
		this.relativeName = relativeName;
		this.inclusion = parent.getInclusion().compose( inclusion );
		List<String> theNestedPathHierarchy = parent.getNestedPathHierarchy();
		if ( ObjectFieldStorage.NESTED.equals( storage ) ) {
			// if we found a nested object, we add it to the nestedPathHierarchy
			theNestedPathHierarchy = new ArrayList<>( theNestedPathHierarchy );
			theNestedPathHierarchy.add( absolutePath );
		}
		this.nestedPathHierarchy = Collections.unmodifiableList( theNestedPathHierarchy );
		this.storage = storage;
		this.multiValued = multiValued;
		this.childrenAbsolutePaths = childrenRelativeNames.stream()
				.map( childName -> FieldPaths.compose( absolutePath, childName ) )
				.collect( Collectors.toList() );
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "[absolutePath=" + absolutePath + ", storage=" + storage + "]";
	}

	@Override
	public boolean isRoot() {
		return false;
	}

	@Override
	public boolean isObjectField() {
		return true;
	}

	@Override
	public boolean isValueField() {
		return false;
	}

	@Override
	public IndexObjectFieldDescriptor toObjectField() {
		return this;
	}

	@Override
	public IndexValueFieldDescriptor toValueField() {
		throw log.invalidIndexElementTypeObjectFieldIsNotValueField( absolutePath );
	}

	@Override
	public IndexCompositeElementDescriptor parent() {
		return parent;
	}

	public LuceneIndexSchemaObjectNode getParent() {
		return parent;
	}

	@Override
	public String absolutePath() {
		return absolutePath;
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
	public String relativeName() {
		return relativeName;
	}

	@Override
	public IndexFieldInclusion getInclusion() {
		return inclusion;
	}

	@Override
	public List<String> getNestedPathHierarchy() {
		return nestedPathHierarchy;
	}

	public List<String> getChildrenAbsolutePaths() {
		return childrenAbsolutePaths;
	}

	@Override
	public boolean isMultiValued() {
		return multiValued;
	}

	@Override
	public IndexObjectFieldTypeDescriptor type() {
		// We don't bother creating a dedicated object to represent the type, which is very simple.
		return this;
	}

	@Override
	public boolean isNested() {
		return ObjectFieldStorage.NESTED.equals( storage );
	}

	public ObjectFieldStorage getStorage() {
		return storage;
	}
}
