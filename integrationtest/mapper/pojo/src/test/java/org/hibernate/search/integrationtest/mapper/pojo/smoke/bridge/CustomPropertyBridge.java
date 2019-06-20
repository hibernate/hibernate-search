/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.pojo.smoke.bridge;

import java.time.LocalDate;

import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.engine.backend.document.IndexObjectFieldReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaObjectField;
import org.hibernate.search.engine.environment.bean.BeanHolder;
import org.hibernate.search.mapper.pojo.bridge.PropertyBridge;
import org.hibernate.search.mapper.pojo.bridge.binding.PropertyBridgeBindingContext;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.AnnotationBridgeBuilder;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.BridgeBuildContext;
import org.hibernate.search.mapper.pojo.bridge.runtime.PropertyBridgeWriteContext;
import org.hibernate.search.mapper.pojo.model.PojoElementAccessor;
import org.hibernate.search.mapper.pojo.model.PojoModelProperty;

public final class CustomPropertyBridge implements PropertyBridge {

	private static final String TEXT_PROPERTY_NAME = "text";
	private static final String LOCAL_DATE_PROPERTY_NAME = "localDate";
	private static final String TEXT_FIELD_NAME = "text";
	private static final String LOCAL_DATE_FIELD_NAME = "date";

	public static final class Builder
			implements AnnotationBridgeBuilder<PropertyBridge, CustomPropertyBridgeAnnotation> {

		private String objectName;

		@Override
		public void initialize(CustomPropertyBridgeAnnotation annotation) {
			objectName( annotation.objectName() );
		}

		public Builder objectName(String value) {
			this.objectName = value;
			return this;
		}

		@Override
		public BeanHolder<PropertyBridge> build(BridgeBuildContext buildContext) {
			return BeanHolder.of( new CustomPropertyBridge( objectName ) );
		}
	}

	private final String objectName;

	private PojoElementAccessor<String> textPropertyAccessor;
	private PojoElementAccessor<LocalDate> localDatePropertyAccessor;
	private IndexObjectFieldReference objectFieldReference;
	private IndexFieldReference<String> textFieldReference;
	private IndexFieldReference<LocalDate> localDateFieldReference;

	private CustomPropertyBridge(String objectName) {
		this.objectName = objectName;
	}

	@Override
	public void bind(PropertyBridgeBindingContext context) {
		PojoModelProperty bridgedElement = context.getBridgedElement();
		textPropertyAccessor = bridgedElement.property( TEXT_PROPERTY_NAME ).createAccessor( String.class );
		localDatePropertyAccessor = bridgedElement.property( LOCAL_DATE_PROPERTY_NAME ).createAccessor( LocalDate.class );

		IndexSchemaObjectField objectField = context.getIndexSchemaElement().objectField( objectName );
		objectFieldReference = objectField.toReference();
		textFieldReference = objectField.field( TEXT_FIELD_NAME, f -> f.asString() ).toReference();
		localDateFieldReference = objectField.field( LOCAL_DATE_FIELD_NAME, f -> f.asLocalDate() )
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
}
