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
import org.hibernate.search.backend.lucene.search.common.impl.AbstractLuceneValueFieldSearchQueryElementFactory;
import org.hibernate.search.backend.lucene.search.common.impl.LuceneSearchIndexCompositeNodeContext;
import org.hibernate.search.backend.lucene.search.common.impl.LuceneSearchIndexScope;
import org.hibernate.search.backend.lucene.search.common.impl.LuceneSearchIndexValueFieldContext;
import org.hibernate.search.engine.search.common.spi.SearchQueryElementTypeKey;
import org.hibernate.search.backend.lucene.types.impl.LuceneIndexValueFieldType;
import org.hibernate.search.engine.backend.document.model.spi.IndexFieldInclusion;
import org.hibernate.search.engine.backend.metamodel.IndexValueFieldDescriptor;
import org.hibernate.search.engine.reporting.spi.EventContexts;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;
import org.hibernate.search.util.common.reporting.EventContext;


public class LuceneIndexValueField<F> extends AbstractLuceneIndexField
		implements IndexValueFieldDescriptor, LuceneSearchIndexValueFieldContext<F> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final List<String> nestedPathHierarchy;

	private final LuceneIndexValueFieldType<F> type;

	public LuceneIndexValueField(LuceneIndexCompositeNode parent, String relativeName,
			IndexFieldInclusion inclusion, boolean multiValued, boolean dynamic, LuceneIndexValueFieldType<F> type) {
		super( parent, relativeName, inclusion, multiValued, dynamic );
		this.nestedPathHierarchy = parent.nestedPathHierarchy();
		this.type = type;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "[absolutePath=" + absolutePath + ", type=" + type + "]";
	}

	@Override
	public boolean isComposite() {
		return false;
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
	public LuceneSearchIndexCompositeNodeContext toComposite() {
		throw log.invalidIndexElementTypeValueFieldIsNotObjectField( absolutePath );
	}

	@Override
	public LuceneIndexObjectField toObjectField() {
		throw log.invalidIndexElementTypeValueFieldIsNotObjectField( absolutePath );
	}

	@Override
	public LuceneIndexValueField<F> toValueField() {
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
	public LuceneIndexValueFieldType<F> type() {
		return type;
	}

	@Override
	public <T> T queryElement(SearchQueryElementTypeKey<T> key, LuceneSearchIndexScope scope) {
		AbstractLuceneValueFieldSearchQueryElementFactory<T, F> factory = type().queryElementFactory( key );
		if ( factory == null ) {
			throw log.cannotUseQueryElementForIndexElement( eventContext(), key.toString(),
					log.missingSupportHintForValueField( key.toString() ), eventContext() );
		}
		return factory.create( scope, this );
	}

	@SuppressWarnings("unchecked")
	public <T> LuceneIndexValueField<? super T> withValueType(Class<T> expectedSubType, EventContext eventContext) {
		if ( !type.valueClass().isAssignableFrom( expectedSubType ) ) {
			throw log.invalidFieldValueType( type.valueClass(), expectedSubType,
					eventContext.append( EventContexts.fromIndexFieldAbsolutePath( absolutePath ) ) );
		}
		return (LuceneIndexValueField<? super T>) this;
	}
}
