/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.util.common.stub.backend.document.model.impl;

import java.time.LocalDate;

import org.hibernate.search.engine.backend.document.model.IndexSchemaElement;
import org.hibernate.search.engine.backend.document.model.IndexSchemaFieldContext;
import org.hibernate.search.engine.backend.document.model.IndexSchemaFieldTypedContext;
import org.hibernate.search.engine.backend.document.model.ObjectFieldStorage;
import org.hibernate.search.engine.backend.document.model.spi.IndexSchemaNestingContext;
import org.hibernate.search.engine.backend.document.model.spi.IndexSchemaObjectField;
import org.hibernate.search.engine.backend.spatial.GeoPoint;
import org.hibernate.search.integrationtest.util.common.stub.backend.document.model.StubIndexSchemaNode;

class StubIndexSchemaElement implements IndexSchemaElement {

	protected final StubIndexSchemaNode.Builder builder;
	private final IndexSchemaNestingContext context;

	StubIndexSchemaElement(StubIndexSchemaNode.Builder builder, IndexSchemaNestingContext context) {
		this.builder = builder;
		this.context = context;
	}

	@Override
	public IndexSchemaFieldContext field(String relativeName) {
		return new IndexSchemaFieldContext() {
			@Override
			public <T> IndexSchemaFieldTypedContext<T> as(Class<T> inputType) {
				StubIndexSchemaNode.Builder childBuilder = StubIndexSchemaNode.field( inputType );
				return context.nest(
						relativeName,
						// If the field is included, make sure to link it to the parent
						prefixedName -> {
							builder.child( prefixedName, childBuilder );
							return new StubIndexSchemaFieldTypedContext<>( prefixedName, childBuilder );
						},
						// Otherwise, just make sure the code will work, but ignore any input from the client
						prefixedName -> new StubIndexSchemaFieldTypedContext<>( prefixedName, childBuilder )
				);
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
		};
	}

	@Override
	public IndexSchemaObjectField objectField(String relativeName, ObjectFieldStorage storage) {
		StubIndexSchemaNode.Builder childBuilder = StubIndexSchemaNode.objectField( storage );
		return context.nest(
				relativeName,
				// If the field is included, make sure to link it to the parent
				(prefixedName, nestingContext) -> {
					builder.child( prefixedName, childBuilder );
					return new StubIndexSchemaObjectField( prefixedName, childBuilder, nestingContext );
				},
				// Otherwise, just make sure the code will work, but ignore any input from the client
				(prefixedName, nestingContext) ->
						new StubIndexSchemaObjectField( prefixedName, childBuilder, nestingContext )
		);
	}

}
