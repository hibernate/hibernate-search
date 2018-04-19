/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.document.model.impl;

import java.time.LocalDate;

import org.hibernate.search.engine.backend.document.model.IndexSchemaFieldContext;
import org.hibernate.search.engine.backend.document.model.IndexSchemaFieldTypedContext;
import org.hibernate.search.backend.lucene.types.dsl.impl.IndexSchemaFieldGeoPointContext;
import org.hibernate.search.backend.lucene.types.dsl.impl.IndexSchemaFieldIntegerContext;
import org.hibernate.search.backend.lucene.types.dsl.impl.IndexSchemaFieldLocalDateContext;
import org.hibernate.search.backend.lucene.types.dsl.impl.IndexSchemaFieldStringContext;
import org.hibernate.search.engine.backend.spatial.GeoPoint;
import org.hibernate.search.util.SearchException;


/**
 * @author Guillaume Smet
 */
public class LuceneIndexSchemaFieldContextImpl implements IndexSchemaFieldContext, LuceneIndexSchemaNodeContributor {

	private final String relativeName;

	private LuceneIndexSchemaNodeContributor delegate;

	public LuceneIndexSchemaFieldContextImpl(String relativeName) {
		this.relativeName = relativeName;
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
		return setDelegate( new IndexSchemaFieldStringContext( relativeName ) );
	}

	@Override
	public IndexSchemaFieldTypedContext<Integer> asInteger() {
		return setDelegate( new IndexSchemaFieldIntegerContext( relativeName ) );
	}

	@Override
	public IndexSchemaFieldTypedContext<LocalDate> asLocalDate() {
		return setDelegate( new IndexSchemaFieldLocalDateContext( relativeName ) );
	}

	@Override
	public IndexSchemaFieldTypedContext<GeoPoint> asGeoPoint() {
		return setDelegate( new IndexSchemaFieldGeoPointContext( relativeName ) );
	}

	@Override
	public void contribute(LuceneIndexSchemaNodeCollector collector, LuceneIndexSchemaObjectNode parentNode) {
		// TODO error if delegate is null
		delegate.contribute( collector, parentNode );
	}

	private <T extends LuceneIndexSchemaNodeContributor> T setDelegate(T context) {
		if ( delegate != null ) {
			throw new SearchException( "You cannot set the type of a field more than once" );
		}
		delegate = context;
		return context;
	}

}
