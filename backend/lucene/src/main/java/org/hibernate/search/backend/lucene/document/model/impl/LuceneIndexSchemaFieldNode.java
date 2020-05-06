/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.document.model.impl;

import java.lang.invoke.MethodHandles;
import java.util.List;

import org.hibernate.search.backend.lucene.logging.impl.Log;
import org.hibernate.search.backend.lucene.types.impl.LuceneIndexFieldType;
import org.hibernate.search.engine.backend.document.model.spi.IndexFieldInclusion;
import org.hibernate.search.engine.backend.metamodel.IndexCompositeElementDescriptor;
import org.hibernate.search.engine.backend.metamodel.IndexObjectFieldDescriptor;
import org.hibernate.search.engine.backend.metamodel.IndexValueFieldDescriptor;
import org.hibernate.search.engine.backend.metamodel.IndexValueFieldTypeDescriptor;
import org.hibernate.search.engine.reporting.spi.EventContexts;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;
import org.hibernate.search.util.common.reporting.EventContext;


public class LuceneIndexSchemaFieldNode<F> implements IndexValueFieldDescriptor {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final LuceneIndexSchemaObjectNode parent;
	private final String absolutePath;
	private final String relativeName;
	private final IndexFieldInclusion inclusion;

	private final List<String> nestedPathHierarchy;

	private final boolean multiValued;

	private final LuceneIndexFieldType<F> type;

	public LuceneIndexSchemaFieldNode(LuceneIndexSchemaObjectNode parent, String relativeName,
			IndexFieldInclusion inclusion, boolean multiValued, LuceneIndexFieldType<F> type) {
		this.parent = parent;
		this.absolutePath = parent.getAbsolutePath( relativeName );
		this.relativeName = relativeName;
		this.inclusion = parent.getInclusion().compose( inclusion );
		this.nestedPathHierarchy = parent.getNestedPathHierarchy();
		this.multiValued = multiValued;
		this.type = type;
	}

	@Override
	public boolean isObjectField() {
		return false;
	}

	@Override
	public boolean isValueField() {
		return true;
	}

	@Override
	public IndexObjectFieldDescriptor toObjectField() {
		throw log.invalidIndexElementTypeValueFieldIsNotObjectField( absolutePath );
	}

	@Override
	public IndexValueFieldDescriptor toValueField() {
		return this;
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

	public String getAbsolutePath() {
		return absolutePath;
	}

	@Override
	public String relativeName() {
		return relativeName;
	}

	public IndexFieldInclusion getInclusion() {
		return inclusion;
	}

	public String getNestedDocumentPath() {
		return ( nestedPathHierarchy.isEmpty() ) ? null :
				// nested path is the LAST element on the path hierarchy
				nestedPathHierarchy.get( nestedPathHierarchy.size() - 1 );
	}

	public List<String> getNestedPathHierarchy() {
		return nestedPathHierarchy;
	}

	@Override
	public boolean isMultiValued() {
		return multiValued;
	}

	@Override
	public IndexValueFieldTypeDescriptor type() {
		return type;
	}

	public LuceneIndexFieldType<F> getType() {
		return type;
	}

	@SuppressWarnings("unchecked")
	public <T> LuceneIndexSchemaFieldNode<? super T> withValueType(Class<T> expectedSubType, EventContext eventContext) {
		if ( !type.getValueType().isAssignableFrom( expectedSubType ) ) {
			throw log.invalidFieldValueType( type.getValueType(), expectedSubType,
					eventContext.append( EventContexts.fromIndexFieldAbsolutePath( absolutePath ) ) );
		}
		return (LuceneIndexSchemaFieldNode<? super T>) this;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder( getClass().getSimpleName() ).append( "[" )
				.append( ", absolutePath=" ).append( absolutePath )
				.append( ", type=" ).append( type )
				.append( "]" );
		return sb.toString();
	}
}
