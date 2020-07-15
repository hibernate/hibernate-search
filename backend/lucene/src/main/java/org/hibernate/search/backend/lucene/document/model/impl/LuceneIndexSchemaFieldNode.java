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
import org.hibernate.search.backend.lucene.search.impl.LuceneSearchContext;
import org.hibernate.search.backend.lucene.search.impl.LuceneSearchFieldContext;
import org.hibernate.search.backend.lucene.search.impl.LuceneSearchFieldQueryElementFactory;
import org.hibernate.search.backend.lucene.search.impl.SearchQueryElementTypeKey;
import org.hibernate.search.backend.lucene.types.impl.LuceneIndexFieldType;
import org.hibernate.search.engine.backend.document.model.spi.IndexFieldInclusion;
import org.hibernate.search.engine.backend.metamodel.IndexValueFieldDescriptor;
import org.hibernate.search.engine.reporting.spi.EventContexts;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;
import org.hibernate.search.util.common.reporting.EventContext;


public class LuceneIndexSchemaFieldNode<F> extends AbstractLuceneIndexSchemaFieldNode
		implements IndexValueFieldDescriptor, LuceneSearchFieldContext<F> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final List<String> nestedPathHierarchy;

	private final LuceneIndexFieldType<F> type;

	public LuceneIndexSchemaFieldNode(LuceneIndexSchemaObjectNode parent, String relativeName,
			IndexFieldInclusion inclusion, boolean multiValued, LuceneIndexFieldType<F> type) {
		super( parent, relativeName, inclusion, multiValued );
		this.nestedPathHierarchy = parent.nestedPathHierarchy();
		this.type = type;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "[absolutePath=" + absolutePath + ", type=" + type + "]";
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
	public LuceneIndexSchemaObjectFieldNode toObjectField() {
		throw log.invalidIndexElementTypeValueFieldIsNotObjectField( absolutePath );
	}

	@Override
	public LuceneIndexSchemaFieldNode<F> toValueField() {
		return this;
	}

	@Override
	public String nestedDocumentPath() {
		return ( nestedPathHierarchy.isEmpty() ) ? null :
				// nested path is the LAST element on the path hierarchy
				nestedPathHierarchy.get( nestedPathHierarchy.size() - 1 );
	}

	@Override
	public List<String> nestedPathHierarchy() {
		return nestedPathHierarchy;
	}

	@Override
	public LuceneIndexFieldType<F> type() {
		return type;
	}

	@Override
	public EventContext eventContext() {
		return EventContexts.fromIndexFieldAbsolutePath( absolutePath );
	}

	@Override
	public <T> T queryElement(SearchQueryElementTypeKey<T> key, LuceneSearchContext searchContext) {
		LuceneSearchFieldQueryElementFactory<T, F> factory = type().queryElementFactory( key );
		if ( factory == null ) {
			throw log.cannotUseQueryElementForField( absolutePath(), key.toString(), eventContext() );
		}
		return factory.create( searchContext, this );
	}

	@SuppressWarnings("unchecked")
	public <T> LuceneIndexSchemaFieldNode<? super T> withValueType(Class<T> expectedSubType, EventContext eventContext) {
		if ( !type.valueClass().isAssignableFrom( expectedSubType ) ) {
			throw log.invalidFieldValueType( type.valueClass(), expectedSubType,
					eventContext.append( EventContexts.fromIndexFieldAbsolutePath( absolutePath ) ) );
		}
		return (LuceneIndexSchemaFieldNode<? super T>) this;
	}
}
