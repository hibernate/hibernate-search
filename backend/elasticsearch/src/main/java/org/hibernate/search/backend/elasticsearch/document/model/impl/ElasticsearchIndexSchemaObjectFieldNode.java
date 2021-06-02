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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.search.backend.elasticsearch.logging.impl.Log;
import org.hibernate.search.backend.elasticsearch.search.impl.ElasticsearchSearchCompositeIndexSchemaElementContext;
import org.hibernate.search.backend.elasticsearch.search.impl.ElasticsearchSearchCompositeIndexSchemaElementQueryElementFactory;
import org.hibernate.search.backend.elasticsearch.search.impl.ElasticsearchSearchIndexScope;
import org.hibernate.search.backend.elasticsearch.search.predicate.impl.ElasticsearchNestedPredicate;
import org.hibernate.search.engine.search.common.spi.SearchQueryElementTypeKey;
import org.hibernate.search.backend.elasticsearch.search.predicate.impl.ElasticsearchExistsPredicate;
import org.hibernate.search.engine.search.predicate.spi.PredicateTypeKeys;
import org.hibernate.search.engine.backend.common.spi.FieldPaths;
import org.hibernate.search.engine.backend.document.model.spi.IndexFieldInclusion;
import org.hibernate.search.engine.backend.metamodel.IndexObjectFieldDescriptor;
import org.hibernate.search.engine.backend.metamodel.IndexObjectFieldTypeDescriptor;
import org.hibernate.search.engine.backend.types.ObjectStructure;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;
import org.hibernate.search.util.common.reporting.EventContext;


public class ElasticsearchIndexSchemaObjectFieldNode extends AbstractElasticsearchIndexSchemaFieldNode
		implements IndexObjectFieldDescriptor, ElasticsearchIndexSchemaObjectNode,
				IndexObjectFieldTypeDescriptor, ElasticsearchSearchCompositeIndexSchemaElementContext {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final List<String> nestedPathHierarchy;

	private final ObjectStructure structure;

	private final Map<String, AbstractElasticsearchIndexSchemaFieldNode> staticChildrenByName;
	private final Map<SearchQueryElementTypeKey<?>, ElasticsearchSearchCompositeIndexSchemaElementQueryElementFactory<?>> queryElementFactories;

	public ElasticsearchIndexSchemaObjectFieldNode(ElasticsearchIndexSchemaObjectNode parent, String relativeFieldName,
			IndexFieldInclusion inclusion, ObjectStructure structure, boolean multiValued,
			Map<String, AbstractElasticsearchIndexSchemaFieldNode> notYetInitializedStaticChildren,
			Map<SearchQueryElementTypeKey<?>, ElasticsearchSearchCompositeIndexSchemaElementQueryElementFactory<?>> queryElementFactories) {
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
		this.queryElementFactories = new HashMap<>();
		this.queryElementFactories.put( PredicateTypeKeys.EXISTS, new ElasticsearchExistsPredicate.ObjectFieldFactory() );
		if ( ObjectStructure.NESTED.equals( structure ) ) {
			this.queryElementFactories.put( PredicateTypeKeys.NESTED, new ElasticsearchNestedPredicate.Factory() );
		}
		this.queryElementFactories.putAll( queryElementFactories );
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
	public <T> T queryElement(SearchQueryElementTypeKey<T> key, ElasticsearchSearchIndexScope scope) {
		ElasticsearchSearchCompositeIndexSchemaElementQueryElementFactory<T> factory = queryElementFactory( key );
		if ( factory == null ) {
			EventContext eventContext = eventContext();
			throw log.cannotUseQueryElementForCompositeIndexElement( eventContext, key.toString(), eventContext );
		}
		try {
			return factory.create( scope, this );
		}
		catch (SearchException e) {
			EventContext eventContext = eventContext();
			throw log.cannotUseQueryElementForCompositeIndexElementBecauseCreationException( eventContext, key.toString(),
					e.getMessage(), e, eventContext );
		}
	}

	@Override
	@SuppressWarnings("unchecked") // The cast is safe because the key type always matches the value type.
	public <T> ElasticsearchSearchCompositeIndexSchemaElementQueryElementFactory<T> queryElementFactory(SearchQueryElementTypeKey<T> key) {
		return (ElasticsearchSearchCompositeIndexSchemaElementQueryElementFactory<T>) queryElementFactories.get( key );
	}

}
