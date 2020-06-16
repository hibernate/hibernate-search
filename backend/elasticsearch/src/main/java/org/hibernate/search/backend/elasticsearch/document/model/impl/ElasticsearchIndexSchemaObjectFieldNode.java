/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.document.model.impl;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.hibernate.search.backend.elasticsearch.logging.impl.Log;
import org.hibernate.search.engine.backend.common.spi.FieldPaths;
import org.hibernate.search.engine.backend.types.ObjectStructure;
import org.hibernate.search.engine.backend.document.model.spi.IndexFieldInclusion;
import org.hibernate.search.engine.backend.metamodel.IndexObjectFieldDescriptor;
import org.hibernate.search.engine.backend.metamodel.IndexObjectFieldTypeDescriptor;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;


public class ElasticsearchIndexSchemaObjectFieldNode extends AbstractElasticsearchIndexSchemaFieldNode
		implements IndexObjectFieldDescriptor, ElasticsearchIndexSchemaObjectNode, IndexObjectFieldTypeDescriptor {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final List<String> nestedPathHierarchy;

	private final ObjectStructure structure;

	private final List<AbstractElasticsearchIndexSchemaFieldNode> staticChildren;

	public ElasticsearchIndexSchemaObjectFieldNode(ElasticsearchIndexSchemaObjectNode parent, String relativeFieldName,
			IndexFieldInclusion inclusion, ObjectStructure structure, boolean multiValued,
			List<AbstractElasticsearchIndexSchemaFieldNode> notYetInitializedStaticChildren) {
		super( parent, relativeFieldName, inclusion, multiValued );
		// at the root object level the nestedPathHierarchy is empty
		List<String> theNestedPathHierarchy = parent.nestedPathHierarchy();
		if ( ObjectStructure.NESTED.equals( structure ) ) {
			// if we found a nested object, we add it to the nestedPathHierarchy
			theNestedPathHierarchy = new ArrayList<>( theNestedPathHierarchy );
			theNestedPathHierarchy.add( absolutePath );
		}
		this.nestedPathHierarchy = Collections.unmodifiableList( theNestedPathHierarchy );
		this.structure = ObjectStructure.DEFAULT.equals( structure ) ? ObjectStructure.FLATTENED : structure;
		// We expect the children to be added to the list externally, just after the constructor call.
		this.staticChildren = Collections.unmodifiableList( notYetInitializedStaticChildren );
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "[absolutePath=" + absolutePath + "]";
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
	public ElasticsearchIndexSchemaObjectFieldNode toObjectField() {
		return this;
	}

	@Override
	public ElasticsearchIndexSchemaFieldNode<?> toValueField() {
		throw log.invalidIndexElementTypeObjectFieldIsNotValueField( absolutePath );
	}

	@Override
	public String absolutePath(String relativeFieldName) {
		return FieldPaths.compose( absolutePath, relativeFieldName );
	}

	@Override
	public List<String> nestedPathHierarchy() {
		return nestedPathHierarchy;
	}

	@Override
	public IndexObjectFieldTypeDescriptor type() {
		// We don't bother creating a dedicated object to represent the type, which is very simple.
		return this;
	}

	@Override
	public Collection<? extends AbstractElasticsearchIndexSchemaFieldNode> staticChildren() {
		return staticChildren;
	}

	@Override
	public boolean nested() {
		return ObjectStructure.NESTED.equals( structure );
	}

	public ObjectStructure structure() {
		return structure;
	}

}
