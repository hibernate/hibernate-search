/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.document.model.impl;

import java.lang.invoke.MethodHandles;

import org.hibernate.search.backend.lucene.logging.impl.Log;
import org.hibernate.search.backend.lucene.search.common.impl.LuceneSearchIndexScope;
import org.hibernate.search.backend.lucene.search.common.impl.LuceneSearchIndexValueFieldContext;
import org.hibernate.search.backend.lucene.types.impl.LuceneIndexValueFieldType;
import org.hibernate.search.engine.backend.document.model.spi.AbstractIndexValueField;
import org.hibernate.search.engine.common.tree.spi.TreeNodeInclusion;
import org.hibernate.search.engine.reporting.spi.EventContexts;
import org.hibernate.search.engine.search.common.spi.SearchIndexSchemaElementContextHelper;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;
import org.hibernate.search.util.common.reporting.EventContext;

public final class LuceneIndexValueField<F>
		extends AbstractIndexValueField<
				LuceneIndexValueField<F>,
				LuceneSearchIndexScope<?>,
				LuceneIndexValueFieldType<F>,
				LuceneIndexCompositeNode,
				F>
		implements LuceneIndexField, LuceneSearchIndexValueFieldContext<F> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final boolean dynamic;

	public LuceneIndexValueField(LuceneIndexCompositeNode parent, String relativeFieldName,
			LuceneIndexValueFieldType<F> type, TreeNodeInclusion inclusion, boolean multiValued,
			boolean dynamic) {
		super( parent, relativeFieldName, type, inclusion, multiValued );
		this.dynamic = dynamic;
	}

	@Override
	protected LuceneIndexValueField<F> self() {
		return this;
	}

	@Override
	public LuceneIndexObjectField toObjectField() {
		return SearchIndexSchemaElementContextHelper.throwingToObjectField( this );
	}

	@Override
	public boolean dynamic() {
		return dynamic;
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
