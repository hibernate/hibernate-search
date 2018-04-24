/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.common.stub.backend.document.model.impl;

import java.time.LocalDate;

import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaFieldContext;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaFieldTypedContext;
import org.hibernate.search.engine.backend.document.model.dsl.ObjectFieldStorage;
import org.hibernate.search.engine.backend.document.model.dsl.spi.IndexSchemaNestingContext;
import org.hibernate.search.engine.backend.document.model.dsl.spi.IndexSchemaObjectField;
import org.hibernate.search.engine.backend.spatial.GeoPoint;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.document.model.StubIndexSchemaNode;

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
				return context.nest(
						relativeName,
						// If the field is included, make sure to link it to the parent
						prefixedName -> {
							StubIndexSchemaNode.Builder childBuilder =
									StubIndexSchemaNode.field( builder, prefixedName, inputType );
							builder.child( childBuilder );
							return new StubIndexSchemaFieldTypedContext<>( childBuilder, true );
						},
						// Otherwise, just make sure the code will work, but ignore any input from the client
						prefixedName -> {
							StubIndexSchemaNode.Builder childBuilder =
									StubIndexSchemaNode.field( builder, prefixedName, inputType );
							return new StubIndexSchemaFieldTypedContext<>( childBuilder, false );
						}
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
		return context.nest(
				relativeName,
				// If the field is included, make sure to link it to the parent
				(prefixedName, nestingContext) -> {
					StubIndexSchemaNode.Builder childBuilder =
							StubIndexSchemaNode.objectField( builder, prefixedName, storage );
					builder.child( childBuilder );
					return new StubIndexSchemaObjectField( childBuilder, nestingContext, true );
				},
				// Otherwise, just make sure the code will work, but ignore any input from the client
				(prefixedName, nestingContext) -> {
					StubIndexSchemaNode.Builder childBuilder =
							StubIndexSchemaNode.objectField( builder, prefixedName, storage );
					return new StubIndexSchemaObjectField( childBuilder, nestingContext, false );
				}
		);
	}

}
