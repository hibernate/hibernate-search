/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.document.model.dsl.impl;

import java.time.LocalDate;

import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaFieldTerminalContext;
import org.hibernate.search.engine.backend.document.model.dsl.StandardIndexSchemaFieldTypedContext;
import org.hibernate.search.backend.lucene.document.model.LuceneFieldContributor;
import org.hibernate.search.backend.lucene.document.model.LuceneFieldValueExtractor;
import org.hibernate.search.backend.lucene.document.model.dsl.LuceneIndexSchemaFieldContext;
import org.hibernate.search.backend.lucene.document.model.impl.LuceneIndexSchemaNodeCollector;
import org.hibernate.search.backend.lucene.document.model.impl.LuceneIndexSchemaNodeContributor;
import org.hibernate.search.backend.lucene.document.model.impl.LuceneIndexSchemaObjectNode;
import org.hibernate.search.backend.lucene.types.dsl.impl.LuceneGeoPointIndexSchemaFieldContextImpl;
import org.hibernate.search.backend.lucene.types.dsl.impl.LuceneIntegerIndexSchemaFieldContextImpl;
import org.hibernate.search.backend.lucene.types.dsl.impl.LuceneLocalDateIndexSchemaFieldContextImpl;
import org.hibernate.search.backend.lucene.types.dsl.impl.LuceneFieldIndexSchemaFieldContextImpl;
import org.hibernate.search.backend.lucene.types.dsl.impl.LuceneStringIndexSchemaFieldContextImpl;
import org.hibernate.search.backend.lucene.util.impl.LuceneFields;
import org.hibernate.search.util.EventContext;
import org.hibernate.search.engine.logging.spi.EventContexts;
import org.hibernate.search.engine.spatial.GeoPoint;
import org.hibernate.search.util.SearchException;
import org.hibernate.search.util.impl.common.Contracts;


/**
 * @author Guillaume Smet
 */
class LuceneIndexSchemaFieldContextImpl
		implements LuceneIndexSchemaFieldContext, LuceneIndexSchemaNodeContributor, LuceneIndexSchemaContext {

	private final AbstractLuceneIndexSchemaObjectNodeBuilder parent;
	private final String relativeFieldName;
	private final String absoluteFieldPath;

	private LuceneIndexSchemaNodeContributor delegate;

	LuceneIndexSchemaFieldContextImpl(AbstractLuceneIndexSchemaObjectNodeBuilder parent,
			String relativeFieldName) {
		this.parent = parent;
		this.relativeFieldName = relativeFieldName;
		this.absoluteFieldPath = LuceneFields.compose( parent.getAbsolutePath(), relativeFieldName );
	}

	@Override
	@SuppressWarnings("unchecked")
	public <F> StandardIndexSchemaFieldTypedContext<F> as(Class<F> inputType) {
		if ( String.class.equals( inputType ) ) {
			return (StandardIndexSchemaFieldTypedContext<F>) asString();
		}
		else if ( Integer.class.equals( inputType ) ) {
			return (StandardIndexSchemaFieldTypedContext<F>) asInteger();
		}
		else if ( LocalDate.class.equals( inputType ) ) {
			return (StandardIndexSchemaFieldTypedContext<F>) asLocalDate();
		}
		else if ( GeoPoint.class.equals( inputType ) ) {
			return (StandardIndexSchemaFieldTypedContext<F>) asGeoPoint();
		}
		else {
			// TODO implement other types
			throw new SearchException( "Cannot guess field type for input type " + inputType );
		}
	}

	@Override
	public StandardIndexSchemaFieldTypedContext<String> asString() {
		return setDelegate( new LuceneStringIndexSchemaFieldContextImpl( this, relativeFieldName ) );
	}

	@Override
	public StandardIndexSchemaFieldTypedContext<Integer> asInteger() {
		return setDelegate( new LuceneIntegerIndexSchemaFieldContextImpl( this, relativeFieldName ) );
	}

	@Override
	public StandardIndexSchemaFieldTypedContext<LocalDate> asLocalDate() {
		return setDelegate( new LuceneLocalDateIndexSchemaFieldContextImpl( this, relativeFieldName ) );
	}

	@Override
	public StandardIndexSchemaFieldTypedContext<GeoPoint> asGeoPoint() {
		return setDelegate( new LuceneGeoPointIndexSchemaFieldContextImpl( this, relativeFieldName ) );
	}

	@Override
	public void contribute(LuceneIndexSchemaNodeCollector collector, LuceneIndexSchemaObjectNode parentNode) {
		Contracts.assertNotNull( delegate, "delegate" );

		delegate.contribute( collector, parentNode );
	}

	@Override
	public <F> IndexSchemaFieldTerminalContext<F> asLuceneField(LuceneFieldContributor<F> fieldContributor,
			LuceneFieldValueExtractor<F> fieldValueExtractor) {
		return setDelegate( new LuceneFieldIndexSchemaFieldContextImpl<>(
				this, relativeFieldName, fieldContributor, fieldValueExtractor
		) );
	}

	@Override
	public EventContext getEventContext() {
		return getRoot().getIndexEventContext()
				.append( EventContexts.fromIndexFieldAbsolutePath( absoluteFieldPath ) );
	}

	@Override
	public LuceneIndexSchemaRootNodeBuilder getRoot() {
		return parent.getRoot();
	}

	private <T extends LuceneIndexSchemaNodeContributor> T setDelegate(T context) {
		if ( delegate != null ) {
			throw new SearchException( "You cannot set the type of a field more than once" );
		}
		delegate = context;
		return context;
	}
}
