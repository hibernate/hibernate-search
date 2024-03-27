/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.mapper.orm.smoke.bridge;

import java.time.LocalDate;

import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.engine.backend.document.IndexObjectFieldReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaObjectField;
import org.hibernate.search.mapper.pojo.bridge.PropertyBridge;
import org.hibernate.search.mapper.pojo.bridge.binding.PropertyBindingContext;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.PropertyBinder;
import org.hibernate.search.mapper.pojo.bridge.runtime.PropertyBridgeWriteContext;
import org.hibernate.search.mapper.pojo.model.PojoElementAccessor;
import org.hibernate.search.mapper.pojo.model.PojoModelProperty;

public final class CustomPropertyBridge implements PropertyBridge<Object> {

	private static final String TEXT_PROPERTY_NAME = "text";
	private static final String LOCAL_DATE_PROPERTY_NAME = "localDate";
	private static final String TEXT_FIELD_NAME = "text";
	private static final String LOCAL_DATE_FIELD_NAME = "date";

	private final PojoElementAccessor<String> textPropertyAccessor;
	private final PojoElementAccessor<LocalDate> localDatePropertyAccessor;
	private final IndexObjectFieldReference objectFieldReference;
	private final IndexFieldReference<String> textFieldReference;
	private final IndexFieldReference<LocalDate> localDateFieldReference;

	private CustomPropertyBridge(PojoElementAccessor<String> textPropertyAccessor,
			PojoElementAccessor<LocalDate> localDatePropertyAccessor,
			IndexSchemaElement indexSchemaElement, String objectName) {
		this.textPropertyAccessor = textPropertyAccessor;
		this.localDatePropertyAccessor = localDatePropertyAccessor;

		IndexSchemaObjectField objectField = indexSchemaElement.objectField( objectName );
		this.objectFieldReference = objectField.toReference();
		this.textFieldReference = objectField.field( TEXT_FIELD_NAME, f -> f.asString() ).toReference();
		this.localDateFieldReference = objectField.field( LOCAL_DATE_FIELD_NAME, f -> f.asLocalDate() )
				.toReference();
	}

	@Override
	public void write(DocumentElement target, Object bridgedElement, PropertyBridgeWriteContext context) {
		String textSourceValue = textPropertyAccessor.read( bridgedElement );
		LocalDate localDateSourceValue = localDatePropertyAccessor.read( bridgedElement );
		if ( textSourceValue != null || localDateSourceValue != null ) {
			DocumentElement object = target.addObject( objectFieldReference );
			object.addValue( textFieldReference, textSourceValue );
			object.addValue( localDateFieldReference, localDateSourceValue );
		}
	}

	@Override
	public void close() {
		// Nothing to do
	}

	public static final class Binder implements PropertyBinder {

		private String objectName;

		public Binder objectName(String value) {
			this.objectName = value;
			return this;
		}

		@Override
		public void bind(PropertyBindingContext context) {
			PojoModelProperty bridgedElement = context.bridgedElement();
			PojoElementAccessor<String> textPropertyAccessor =
					bridgedElement.property( TEXT_PROPERTY_NAME ).createAccessor( String.class );
			PojoElementAccessor<LocalDate> localDatePropertyAccessor =
					bridgedElement.property( LOCAL_DATE_PROPERTY_NAME ).createAccessor( LocalDate.class );
			context.bridge(
					new CustomPropertyBridge(
							textPropertyAccessor, localDatePropertyAccessor,
							context.indexSchemaElement(), objectName
					)
			);
		}
	}
}
