/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.common.stub.backend.document.model.impl;

import java.time.LocalDate;

import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaFieldContext;
import org.hibernate.search.engine.backend.document.model.dsl.StandardIndexSchemaFieldTypedContext;
import org.hibernate.search.engine.spatial.GeoPoint;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.document.model.StubIndexSchemaNode;

class StubIndexSchemaFieldContext implements IndexSchemaFieldContext {

	private final StubIndexSchemaNode.Builder builder;
	private final boolean included;

	StubIndexSchemaFieldContext(StubIndexSchemaNode.Builder builder, boolean included) {
		this.builder = builder;
		this.included = included;
	}

	@Override
	public <F> StandardIndexSchemaFieldTypedContext<F> as(Class<F> inputType) {
		builder.inputType( inputType );
		return new StubIndexSchemaFieldTypedContext<>( builder, inputType, included );
	}

	@Override
	public StandardIndexSchemaFieldTypedContext<String> asString() {
		return as( String.class );
	}

	@Override
	public StandardIndexSchemaFieldTypedContext<Integer> asInteger() {
		return as( Integer.class );
	}

	@Override
	public StandardIndexSchemaFieldTypedContext<LocalDate> asLocalDate() {
		return as( LocalDate.class );
	}

	@Override
	public StandardIndexSchemaFieldTypedContext<GeoPoint> asGeoPoint() {
		return as( GeoPoint.class );
	}

}
