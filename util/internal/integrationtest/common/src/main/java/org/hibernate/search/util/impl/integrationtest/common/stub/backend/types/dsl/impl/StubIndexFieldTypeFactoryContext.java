/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.common.stub.backend.types.dsl.impl;

import java.net.URI;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZonedDateTime;

import org.hibernate.search.engine.backend.types.dsl.IndexFieldTypeFactoryContext;
import org.hibernate.search.engine.backend.types.dsl.StandardIndexFieldTypeContext;
import org.hibernate.search.engine.backend.types.dsl.StringIndexFieldTypeContext;
import org.hibernate.search.engine.spatial.GeoPoint;

public class StubIndexFieldTypeFactoryContext implements IndexFieldTypeFactoryContext {

	@Override
	@SuppressWarnings("unchecked")
	public <F> StandardIndexFieldTypeContext<?, F> as(Class<F> inputType) {
		if ( String.class.isAssignableFrom( inputType ) ) {
			return (StandardIndexFieldTypeContext<?, F>) asString();
		}
		else {
			return new StubGenericIndexFieldTypeContext<>( inputType );
		}
	}

	@Override
	public StringIndexFieldTypeContext<?> asString() {
		return new StubStringIndexFieldTypeContext();
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
	public StandardIndexFieldTypeContext<?, ZonedDateTime> asZonedDateTime() {
		return as( ZonedDateTime.class );
	}

	@Override
	public StandardIndexFieldTypeContext<?, GeoPoint> asGeoPoint() {
		return as( GeoPoint.class );
	}

	@Override
	public StandardIndexFieldTypeContext<?, URI> asUri() {
		return as( URI.class );
	}
}
