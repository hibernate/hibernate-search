/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.document.model.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.hibernate.search.backend.lucene.search.impl.LuceneSearchCompositeIndexSchemaElementContext;
import org.hibernate.search.backend.lucene.search.impl.LuceneSearchCompositeIndexSchemaElementQueryElementFactory;
import org.hibernate.search.backend.lucene.search.impl.LuceneSearchContext;
import org.hibernate.search.backend.lucene.search.impl.SearchQueryElementTypeKey;
import org.hibernate.search.backend.lucene.search.predicate.impl.PredicateTypeKeys;
import org.hibernate.search.backend.lucene.types.predicate.impl.LuceneObjectExistsPredicate;
import org.hibernate.search.engine.backend.common.spi.FieldPaths;
import org.hibernate.search.engine.backend.types.ObjectStructure;
import org.hibernate.search.engine.backend.document.model.spi.IndexFieldInclusion;
import org.hibernate.search.engine.backend.metamodel.IndexObjectFieldDescriptor;
import org.hibernate.search.engine.backend.metamodel.IndexObjectFieldTypeDescriptor;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.common.reporting.EventContext;


public class LuceneIndexSchemaObjectFieldNode extends AbstractLuceneIndexSchemaFieldNode
		implements IndexObjectFieldDescriptor, LuceneIndexSchemaObjectNode,
				IndexObjectFieldTypeDescriptor, LuceneSearchCompositeIndexSchemaElementContext {

	private final List<String> nestedPathHierarchy;

	private final ObjectStructure structure;

	private final Map<String, AbstractLuceneIndexSchemaFieldNode> staticChildrenByName;

	public LuceneIndexSchemaObjectFieldNode(LuceneIndexSchemaObjectNode parent, String relativeName,
			IndexFieldInclusion inclusion, ObjectStructure structure, boolean multiValued, boolean dynamic,
			Map<String, AbstractLuceneIndexSchemaFieldNode> notYetInitializedStaticChildren) {
		super( parent, relativeName, inclusion, multiValued, dynamic );
		List<String> theNestedPathHierarchy = parent.nestedPathHierarchy();
		if ( ObjectStructure.NESTED.equals( structure ) ) {
			// if we found a nested object, we add it to the nestedPathHierarchy
			theNestedPathHierarchy = new ArrayList<>( theNestedPathHierarchy );
			theNestedPathHierarchy.add( absolutePath );
		}
		this.nestedPathHierarchy = Collections.unmodifiableList( theNestedPathHierarchy );
		this.structure = structure;
		// We expect the children to be added to the list externally, just after the constructor call.
		this.staticChildrenByName = Collections.unmodifiableMap( notYetInitializedStaticChildren );
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "[absolutePath=" + absolutePath + ", structure=" + structure + "]";
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
	public LuceneIndexSchemaObjectFieldNode toObjectField() {
		return this;
	}

	@Override
	public LuceneIndexSchemaValueFieldNode<?> toValueField() {
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
	public Map<String, ? extends AbstractLuceneIndexSchemaFieldNode> staticChildrenByName() {
		return staticChildrenByName;
	}

	@Override
	public boolean nested() {
		return ObjectStructure.NESTED.equals( structure );
	}

	@Override
	public <T> T queryElement(SearchQueryElementTypeKey<T> key, LuceneSearchContext searchContext) {
		LuceneSearchCompositeIndexSchemaElementQueryElementFactory<T> factory = queryElementFactory( key );
		if ( factory == null ) {
			EventContext eventContext = eventContext();
			throw log.cannotUseQueryElementForObjectField( absolutePath, key.toString(), eventContext );
		}
		try {
			return factory.create( searchContext, this );
		}
		catch (SearchException e) {
			EventContext eventContext = eventContext();
			throw log.cannotUseQueryElementForObjectFieldBecauseCreationException( absolutePath, key.toString(),
					e.getMessage(), e, eventContext );
		}
	}

	@Override
	@SuppressWarnings("unchecked") // The "equals" condition tells us what T is exactly, so we can cast safely.
	public <T> LuceneSearchCompositeIndexSchemaElementQueryElementFactory<T> queryElementFactory(SearchQueryElementTypeKey<T> key) {
		if ( PredicateTypeKeys.EXISTS.equals( key ) ) {
			return (LuceneSearchCompositeIndexSchemaElementQueryElementFactory<T>)
					LuceneObjectExistsPredicate.Factory.INSTANCE;
		}
		// Otherwise: not supported for object fields.
		return null;
	}

	public ObjectStructure structure() {
		return structure;
	}
}
