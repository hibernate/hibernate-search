/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.document.model.impl;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.hibernate.search.backend.elasticsearch.logging.impl.Log;
import org.hibernate.search.backend.elasticsearch.search.impl.ElasticsearchSearchContext;
import org.hibernate.search.backend.elasticsearch.search.impl.ElasticsearchSearchObjectFieldContext;
import org.hibernate.search.backend.elasticsearch.search.impl.ElasticsearchSearchObjectFieldQueryElementFactory;
import org.hibernate.search.backend.elasticsearch.search.impl.SearchQueryElementTypeKey;
import org.hibernate.search.backend.elasticsearch.search.predicate.impl.ElasticsearchExistsPredicate;
import org.hibernate.search.backend.elasticsearch.search.predicate.impl.PredicateTypeKeys;
import org.hibernate.search.engine.backend.common.spi.FieldPaths;
import org.hibernate.search.engine.backend.types.ObjectStructure;
import org.hibernate.search.engine.backend.document.model.spi.IndexFieldInclusion;
import org.hibernate.search.engine.backend.metamodel.IndexObjectFieldDescriptor;
import org.hibernate.search.engine.backend.metamodel.IndexObjectFieldTypeDescriptor;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;


public class ElasticsearchIndexSchemaObjectFieldNode extends AbstractElasticsearchIndexSchemaFieldNode
		implements IndexObjectFieldDescriptor, ElasticsearchIndexSchemaObjectNode,
				IndexObjectFieldTypeDescriptor, ElasticsearchSearchObjectFieldContext {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final List<String> nestedPathHierarchy;

	private final ObjectStructure structure;

	private final Map<String, AbstractElasticsearchIndexSchemaFieldNode> staticChildrenByName;

	public ElasticsearchIndexSchemaObjectFieldNode(ElasticsearchIndexSchemaObjectNode parent, String relativeFieldName,
			IndexFieldInclusion inclusion, ObjectStructure structure, boolean multiValued,
			Map<String, AbstractElasticsearchIndexSchemaFieldNode> notYetInitializedStaticChildren) {
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
		this.staticChildrenByName = Collections.unmodifiableMap( notYetInitializedStaticChildren );
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
	public ElasticsearchIndexSchemaValueFieldNode<?> toValueField() {
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
	public Map<String, AbstractElasticsearchIndexSchemaFieldNode> staticChildrenByName() {
		return staticChildrenByName;
	}

	@Override
	public boolean nested() {
		return ObjectStructure.NESTED.equals( structure );
	}

	@Override
	public <T> T queryElement(SearchQueryElementTypeKey<T> key, ElasticsearchSearchContext searchContext) {
		ElasticsearchSearchObjectFieldQueryElementFactory<T> factory = queryElementFactory( key );
		if ( factory == null ) {
			throw log.cannotUseQueryElementForObjectField( absolutePath(), key.toString(), eventContext() );
		}
		try {
			return factory.create( searchContext, this );
		}
		catch (SearchException e) {
			throw log.cannotUseQueryElementForObjectFieldBecauseCreationException( absolutePath, key.toString(),
					e.getMessage(), e, null );
		}
	}

	@Override
	@SuppressWarnings("unchecked") // The "equals" condition tells us what T is exactly, so we can cast safely.
	public <T> ElasticsearchSearchObjectFieldQueryElementFactory<T> queryElementFactory(SearchQueryElementTypeKey<T> key) {
		if ( PredicateTypeKeys.EXISTS.equals( key ) ) {
			return (ElasticsearchSearchObjectFieldQueryElementFactory<T>)
					ElasticsearchExistsPredicate.ObjectFieldFactory.INSTANCE;
		}
		// Otherwise: not supported for object fields.
		return null;
	}

	public ObjectStructure structure() {
		return structure;
	}

}
