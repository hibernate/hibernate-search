/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.document.model.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.search.backend.lucene.search.impl.LuceneSearchCompositeIndexSchemaElementContext;
import org.hibernate.search.backend.lucene.search.impl.LuceneSearchCompositeIndexSchemaElementQueryElementFactory;
import org.hibernate.search.backend.lucene.search.impl.LuceneSearchContext;
import org.hibernate.search.backend.lucene.search.predicate.impl.LuceneNestedPredicate;
import org.hibernate.search.engine.search.common.spi.SearchQueryElementTypeKey;
import org.hibernate.search.engine.search.predicate.spi.PredicateTypeKeys;
import org.hibernate.search.backend.lucene.types.predicate.impl.LuceneObjectExistsPredicate;
import org.hibernate.search.engine.backend.common.spi.FieldPaths;
import org.hibernate.search.engine.backend.document.model.spi.IndexFieldInclusion;
import org.hibernate.search.engine.backend.metamodel.IndexObjectFieldDescriptor;
import org.hibernate.search.engine.backend.metamodel.IndexObjectFieldTypeDescriptor;
import org.hibernate.search.engine.backend.types.ObjectStructure;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.common.reporting.EventContext;


public class LuceneIndexSchemaObjectFieldNode extends AbstractLuceneIndexSchemaFieldNode
		implements IndexObjectFieldDescriptor, LuceneIndexSchemaObjectNode,
				IndexObjectFieldTypeDescriptor, LuceneSearchCompositeIndexSchemaElementContext {

	private final List<String> nestedPathHierarchy;

	private final ObjectStructure structure;

	private final Map<String, AbstractLuceneIndexSchemaFieldNode> staticChildrenByName;
	private final Map<SearchQueryElementTypeKey<?>, LuceneSearchCompositeIndexSchemaElementQueryElementFactory<?>> queryElementFactories;

	public LuceneIndexSchemaObjectFieldNode(LuceneIndexSchemaObjectNode parent, String relativeName,
			IndexFieldInclusion inclusion, ObjectStructure structure, boolean multiValued, boolean dynamic,
			Map<String, AbstractLuceneIndexSchemaFieldNode> notYetInitializedStaticChildren,
			Map<SearchQueryElementTypeKey<?>, LuceneSearchCompositeIndexSchemaElementQueryElementFactory<?>> queryElementFactories) {
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
		this.queryElementFactories = new HashMap<>();
		this.queryElementFactories.put( PredicateTypeKeys.EXISTS, LuceneObjectExistsPredicate.Factory.INSTANCE );
		if ( ObjectStructure.NESTED.equals( structure ) ) {
			this.queryElementFactories.put( PredicateTypeKeys.NESTED, LuceneNestedPredicate.Factory.INSTANCE );
		}
		this.queryElementFactories.putAll( queryElementFactories );
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
			throw log.cannotUseQueryElementForCompositeIndexElement( eventContext, key.toString(), eventContext );
		}
		try {
			return factory.create( searchContext, this );
		}
		catch (SearchException e) {
			EventContext eventContext = eventContext();
			throw log.cannotUseQueryElementForCompositeIndexElementBecauseCreationException( eventContext, key.toString(),
					e.getMessage(), e, eventContext );
		}
	}

	@Override
	@SuppressWarnings("unchecked") // The cast is safe because the key type always matches the value type.
	public <T> LuceneSearchCompositeIndexSchemaElementQueryElementFactory<T> queryElementFactory(SearchQueryElementTypeKey<T> key) {
		return (LuceneSearchCompositeIndexSchemaElementQueryElementFactory<T>) queryElementFactories.get( key );
	}

	public ObjectStructure structure() {
		return structure;
	}
}
