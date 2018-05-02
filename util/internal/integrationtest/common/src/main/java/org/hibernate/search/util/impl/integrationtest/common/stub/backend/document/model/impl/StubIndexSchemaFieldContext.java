/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.common.stub.backend.document.model.impl;

import java.time.LocalDate;

import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaFieldContext;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaFieldTypedContext;
import org.hibernate.search.engine.backend.spatial.GeoPoint;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.document.model.StubIndexSchemaNode;

class StubIndexSchemaFieldContext implements IndexSchemaFieldContext {

	private final StubIndexSchemaNode.Builder parentBuilder;
	private final String relativeFieldName;
	private final boolean included;

	StubIndexSchemaFieldContext(StubIndexSchemaNode.Builder parentBuilder, String relativeFieldName,
			boolean included) {
		this.parentBuilder = parentBuilder;
		this.relativeFieldName = relativeFieldName;
		this.included = included;
	}

	@Override
	public <T> IndexSchemaFieldTypedContext<T> as(Class<T> inputType) {
		StubIndexSchemaNode.Builder childBuilder =
				StubIndexSchemaNode.field( parentBuilder, relativeFieldName, inputType );
		if ( included ) {
			parentBuilder.child( childBuilder );
		}
		return new StubIndexSchemaFieldTypedContext<>( childBuilder, included );
	}

	@Override
	public IndexSchemaFieldTypedContext<String> asString() {
		return as( String.class );
	}

	@Override
	public IndexSchemaFieldTypedContext<Integer> asInteger() {
		return as( Integer.class );
	}

	@Override
	public IndexSchemaFieldTypedContext<LocalDate> asLocalDate() {
		return as( LocalDate.class );
	}

	@Override
	public IndexSchemaFieldTypedContext<GeoPoint> asGeoPoint() {
		return as( GeoPoint.class );
	}

}
