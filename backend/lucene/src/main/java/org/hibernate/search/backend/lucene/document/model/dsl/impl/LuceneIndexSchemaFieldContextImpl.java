/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.document.model.dsl.impl;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaFieldTerminalContext;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaFieldTypedContext;
import org.hibernate.search.engine.backend.document.model.dsl.spi.IndexSchemaContext;
import org.hibernate.search.backend.lucene.document.model.LuceneFieldContributor;
import org.hibernate.search.backend.lucene.document.model.LuceneFieldValueExtractor;
import org.hibernate.search.backend.lucene.document.model.dsl.LuceneIndexSchemaFieldContext;
import org.hibernate.search.backend.lucene.document.model.impl.LuceneIndexSchemaNodeCollector;
import org.hibernate.search.backend.lucene.document.model.impl.LuceneIndexSchemaNodeContributor;
import org.hibernate.search.backend.lucene.document.model.impl.LuceneIndexSchemaObjectNode;
import org.hibernate.search.backend.lucene.types.dsl.impl.GeoPointIndexSchemaFieldContext;
import org.hibernate.search.backend.lucene.types.dsl.impl.IntegerIndexSchemaFieldContext;
import org.hibernate.search.backend.lucene.types.dsl.impl.LocalDateIndexSchemaFieldContext;
import org.hibernate.search.backend.lucene.types.dsl.impl.LuceneFieldIndexSchemaFieldContext;
import org.hibernate.search.backend.lucene.types.dsl.impl.StringIndexSchemaFieldContext;
import org.hibernate.search.backend.lucene.util.impl.LuceneFields;
import org.hibernate.search.engine.logging.spi.FailureContextElement;
import org.hibernate.search.engine.logging.spi.FailureContexts;
import org.hibernate.search.engine.spatial.GeoPoint;
import org.hibernate.search.util.SearchException;
import org.hibernate.search.util.impl.common.Contracts;


/**
 * @author Guillaume Smet
 */
class LuceneIndexSchemaFieldContextImpl
		implements LuceneIndexSchemaFieldContext, LuceneIndexSchemaNodeContributor, IndexSchemaContext {

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
	public <T> IndexSchemaFieldTypedContext<T> as(Class<T> inputType) {
		if ( String.class.equals( inputType ) ) {
			return (IndexSchemaFieldTypedContext<T>) asString();
		}
		else if ( Integer.class.equals( inputType ) ) {
			return (IndexSchemaFieldTypedContext<T>) asInteger();
		}
		else if ( LocalDate.class.equals( inputType ) ) {
			return (IndexSchemaFieldTypedContext<T>) asLocalDate();
		}
		else if ( GeoPoint.class.equals( inputType ) ) {
			return (IndexSchemaFieldTypedContext<T>) asGeoPoint();
		}
		else {
			// TODO implement other types
			throw new SearchException( "Cannot guess field type for input type " + inputType );
		}
	}

	@Override
	public IndexSchemaFieldTypedContext<String> asString() {
		return setDelegate( new StringIndexSchemaFieldContext( this, relativeFieldName ) );
	}

	@Override
	public IndexSchemaFieldTypedContext<Integer> asInteger() {
		return setDelegate( new IntegerIndexSchemaFieldContext( this, relativeFieldName ) );
	}

	@Override
	public IndexSchemaFieldTypedContext<LocalDate> asLocalDate() {
		return setDelegate( new LocalDateIndexSchemaFieldContext( this, relativeFieldName ) );
	}

	@Override
	public IndexSchemaFieldTypedContext<GeoPoint> asGeoPoint() {
		return setDelegate( new GeoPointIndexSchemaFieldContext( this, relativeFieldName ) );
	}

	@Override
	public void contribute(LuceneIndexSchemaNodeCollector collector, LuceneIndexSchemaObjectNode parentNode) {
		Contracts.assertNotNull( delegate, "delegate" );

		delegate.contribute( collector, parentNode );
	}

	@Override
	public <V> IndexSchemaFieldTerminalContext<V> asLuceneField(LuceneFieldContributor<V> fieldContributor,
			LuceneFieldValueExtractor<V> fieldValueExtractor) {
		return setDelegate( new LuceneFieldIndexSchemaFieldContext<>(
				this, relativeFieldName, fieldContributor, fieldValueExtractor
		) );
	}

	@Override
	public List<FailureContextElement> getFailureContext() {
		return Arrays.asList(
				parent.getRootNodeBuilder().getIndexFailureContextElement(),
				FailureContexts.fromIndexFieldAbsolutePath( absoluteFieldPath )
		);
	}

	private <T extends LuceneIndexSchemaNodeContributor> T setDelegate(T context) {
		if ( delegate != null ) {
			throw new SearchException( "You cannot set the type of a field more than once" );
		}
		delegate = context;
		return context;
	}

}
