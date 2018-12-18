/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.common.stub.backend.types.dsl.impl;

import java.time.Instant;
import java.time.LocalDate;

import org.hibernate.search.engine.backend.types.dsl.IndexFieldTypeFactoryContext;
import org.hibernate.search.engine.backend.types.dsl.StandardIndexFieldTypeContext;
import org.hibernate.search.engine.backend.types.dsl.StringIndexFieldTypeContext;
import org.hibernate.search.engine.spatial.GeoPoint;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.document.model.StubIndexSchemaNode;

public class StubIndexFieldTypeFactoryContext implements IndexFieldTypeFactoryContext {

	private final StubIndexSchemaNode.Builder builder;
	private final boolean included;

	public StubIndexFieldTypeFactoryContext(StubIndexSchemaNode.Builder builder, boolean included) {
		this.builder = builder;
		this.included = included;
	}

	@Override
	public <F> StandardIndexFieldTypeContext<?, F> as(Class<F> inputType) {
		if ( String.class.isAssignableFrom( inputType ) ) {
			return (StandardIndexFieldTypeContext<?, F>) asString();
		}
		else {
			return new StubGenericIndexFieldTypeContext<>( builder, inputType, included );
		}
	}

	@Override
	public StringIndexFieldTypeContext<?> asString() {
		return new StubStringIndexFieldTypeContext( builder, included );
	}

	@Override
	public StandardIndexFieldTypeContext<?, Integer> asInteger() {
		return as( Integer.class );
	}

	@Override
	public StandardIndexFieldTypeContext<?, Long> asLong() {
		return as( Long.class );
	}

	@Override
	public StandardIndexFieldTypeContext<?, Boolean> asBoolean() {
		return as( Boolean.class );
	}

	@Override
	public StandardIndexFieldTypeContext<?, LocalDate> asLocalDate() {
		return as( LocalDate.class );
	}

	@Override
	public StandardIndexFieldTypeContext<?, Instant> asInstant() {
		return as( Instant.class );
	}

	@Override
	public StandardIndexFieldTypeContext<?, GeoPoint> asGeoPoint() {
		return as( GeoPoint.class );
	}

}
