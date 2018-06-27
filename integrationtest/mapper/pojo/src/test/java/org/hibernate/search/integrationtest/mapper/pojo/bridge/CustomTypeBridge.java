/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.pojo.bridge;

import java.time.LocalDate;

import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.document.IndexFieldAccessor;
import org.hibernate.search.engine.backend.document.IndexObjectFieldAccessor;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaObjectField;
import org.hibernate.search.engine.mapper.model.SearchModel;
import org.hibernate.search.mapper.pojo.bridge.TypeBridge;
import org.hibernate.search.mapper.pojo.bridge.mapping.AnnotationBridgeBuilder;
import org.hibernate.search.mapper.pojo.bridge.mapping.BridgeBuildContext;
import org.hibernate.search.mapper.pojo.model.PojoElement;
import org.hibernate.search.mapper.pojo.model.PojoModelElementAccessor;
import org.hibernate.search.mapper.pojo.model.PojoModelType;
import org.hibernate.search.integrationtest.mapper.pojo.bridge.annotation.CustomTypeBridgeAnnotation;

public final class CustomTypeBridge implements TypeBridge {

	private static final String TEXT_PROPERTY_NAME = "text";
	private static final String LOCAL_DATE_PROPERTY_NAME = "localDate";
	private static final String TEXT_FIELD_NAME = "text";
	private static final String LOCAL_DATE_FIELD_NAME = "date";

	public static final class Builder implements AnnotationBridgeBuilder<TypeBridge, CustomTypeBridgeAnnotation> {

		private String objectName;

		@Override
		public void initialize(CustomTypeBridgeAnnotation annotation) {
			objectName( annotation.objectName() );
		}

		public Builder objectName(String value) {
			this.objectName = value;
			return this;
		}

		@Override
		public TypeBridge build(BridgeBuildContext buildContext) {
			return new CustomTypeBridge( objectName );
		}
	}

	private final String objectName;

	private PojoModelElementAccessor<String> textPropertyAccessor;
	private PojoModelElementAccessor<LocalDate> localDatePropertyAccessor;
	private IndexObjectFieldAccessor objectFieldAccessor;
	private IndexFieldAccessor<String> textFieldAccessor;
	private IndexFieldAccessor<LocalDate> localDateFieldAccessor;

	private CustomTypeBridge(String objectName) {
		this.objectName = objectName;
	}

	@Override
	public void bind(IndexSchemaElement indexSchemaElement, PojoModelType bridgedPojoModelType,
			SearchModel searchModel) {
		textPropertyAccessor = bridgedPojoModelType.property( TEXT_PROPERTY_NAME ).createAccessor( String.class );
		localDatePropertyAccessor = bridgedPojoModelType.property( LOCAL_DATE_PROPERTY_NAME ).createAccessor( LocalDate.class );

		IndexSchemaObjectField objectField = indexSchemaElement.objectField( objectName );
		objectFieldAccessor = objectField.createAccessor();
		textFieldAccessor = objectField.field( TEXT_FIELD_NAME ).asString().createAccessor();
		localDateFieldAccessor = objectField.field( LOCAL_DATE_FIELD_NAME ).asLocalDate().createAccessor();
	}

	@Override
	public void write(DocumentElement target, PojoElement source) {
		String textSourceValue = textPropertyAccessor.read( source );
		LocalDate localDateSourceValue = localDatePropertyAccessor.read( source );
		if ( textSourceValue != null || localDateSourceValue != null ) {
			DocumentElement object = objectFieldAccessor.add( target );
			textFieldAccessor.write( object, textSourceValue );
			localDateFieldAccessor.write( object, localDateSourceValue );
		}
	}

	@Override
	public void close() {
		// Nothing to do
	}
}
