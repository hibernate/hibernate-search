/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.impl;

import java.util.Set;
import java.util.regex.Pattern;

import org.hibernate.search.bridge.FieldBridge;
import org.hibernate.search.bridge.builtin.NumericFieldBridge;
import org.hibernate.search.bridge.builtin.impl.NullEncodingTwoWayFieldBridge;
import org.hibernate.search.bridge.spi.FieldType;
import org.hibernate.search.engine.metadata.impl.BridgeDefinedField;
import org.hibernate.search.engine.metadata.impl.DocumentFieldMetadata;
import org.hibernate.search.engine.metadata.impl.EmbeddedTypeMetadata;
import org.hibernate.search.engine.metadata.impl.PropertyMetadata;
import org.hibernate.search.engine.metadata.impl.TypeMetadata;
import org.hibernate.search.engine.spi.EntityIndexBinding;
import org.hibernate.search.metadata.NumericFieldSettingsDescriptor.NumericEncodingType;

/**
 * Helps with getting the property types for given fields.
 * <p>
 * Very hack-ish solution which is required atm. as we don't have access to the actual property types when dealing with
 * document fields in the work visitor. All this code should not be needed ideally.
 * <p>
 *
 * @author Gunnar Morling
 */
class FieldHelper {

	private static final Pattern DOT = Pattern.compile( "\\." );

	private FieldHelper() {
	}

	// TODO make it work with fields embedded types
	static NumericEncodingType getNumericEncodingType(EntityIndexBinding indexBinding, DocumentFieldMetadata field) {
		NumericEncodingType numericEncodingType = field.getNumericEncodingType();

		if ( numericEncodingType == NumericEncodingType.UNKNOWN ) {
			PropertyMetadata hostingProperty = getPropertyMetadata( indexBinding, field.getName() );
			if ( hostingProperty != null ) {
				BridgeDefinedField bridgeDefinedField = hostingProperty.getBridgeDefinedFields().get( field.getName() );
				if ( bridgeDefinedField != null ) {
					numericEncodingType = getNumericEncodingType( bridgeDefinedField.getType() );
				}
			}
		}

		return numericEncodingType;
	}

	private static NumericEncodingType getNumericEncodingType(FieldType fieldType) {
		switch ( fieldType ) {
			case FLOAT:
				return NumericEncodingType.FLOAT;
			case DOUBLE:
				return NumericEncodingType.DOUBLE;
			case INTEGER:
				return NumericEncodingType.INTEGER;
			case LONG:
				return NumericEncodingType.LONG;
			default:
				return NumericEncodingType.UNKNOWN;
		}
	}

	static boolean isBoolean(EntityIndexBinding indexBinding, String fieldName) {
		String propertyTypeName = getPropertyTypeName( indexBinding, fieldName );
		return "boolean".equals( propertyTypeName ) || "java.lang.Boolean".equals( propertyTypeName );
	}

	static boolean isDate(EntityIndexBinding indexBinding, String fieldName) {
		String propertyTypeName = getPropertyTypeName( indexBinding, fieldName );
		return "java.util.Date".equals( propertyTypeName );
	}

	static boolean isNumeric(DocumentFieldMetadata field) {
		if ( field.isNumeric() ) {
			return true;
		}

		FieldBridge fieldBridge = field.getFieldBridge();

		if ( fieldBridge instanceof NullEncodingTwoWayFieldBridge ) {
			return ( (NullEncodingTwoWayFieldBridge) fieldBridge ).unwrap() instanceof NumericFieldBridge;
		}

		return false;
	}

	static String[] getFieldNameParts(String fieldName) {
		boolean isEmbeddedField = fieldName.contains( "." );
		return isEmbeddedField ? DOT.split( fieldName ) : new String[]{ fieldName };
	}

	private static String getPropertyTypeName(EntityIndexBinding indexBinding, String fieldName) {
		PropertyMetadata propertyMetadata = getPropertyMetadata( indexBinding, fieldName );
		return propertyMetadata != null ? propertyMetadata.getPropertyAccessor().getType().getName() : null;
	}

	private static PropertyMetadata getPropertyMetadata(EntityIndexBinding indexBinding, String fieldName) {
		TypeMetadata typeMetadata;

		boolean isEmbeddedField = fieldName.contains( "." );
		String[] fieldNameParts = isEmbeddedField ? DOT.split( fieldName ) : new String[]{ fieldName };

		if ( isEmbeddedField ) {
			typeMetadata = getLeafTypeMetadata( indexBinding, fieldNameParts );
		}
		else {
			typeMetadata = indexBinding.getDocumentBuilder().getMetadata();
		}

		PropertyMetadata property = getPropertyMetadata( typeMetadata, fieldName, fieldNameParts );
		if ( property != null ) {
			return property;
		}

		return null;
	}

	static DocumentFieldMetadata getFieldMetadata(EntityIndexBinding indexBinding, String fieldName) {
		if ( indexBinding.getDocumentBuilder().getIdentifierName().equals( fieldName ) ) {
			return indexBinding.getDocumentBuilder()
					.getTypeMetadata()
					.getIdPropertyMetadata()
					.getFieldMetadata( fieldName );
		}

		PropertyMetadata property = FieldHelper.getPropertyMetadata( indexBinding, fieldName );

		if ( property != null ) {
			return property.getFieldMetadata( fieldName );
		}
		else {
			Set<DocumentFieldMetadata> classBridgeMetadata = indexBinding.getDocumentBuilder().getMetadata().getClassBridgeMetadata();
			for ( DocumentFieldMetadata documentFieldMetadata : classBridgeMetadata ) {
				if ( documentFieldMetadata.getFieldName().equals( fieldName ) ) {
					return documentFieldMetadata;
				}
			}
		}

		return null;
	}

	private static TypeMetadata getLeafTypeMetadata(EntityIndexBinding indexBinding, String[] fieldNameParts) {
		TypeMetadata parentMetadata = indexBinding.getDocumentBuilder().getMetadata();

		for ( int i = 0; i < fieldNameParts.length - 1; i++ ) {
			for ( EmbeddedTypeMetadata embeddedTypeMetadata : parentMetadata.getEmbeddedTypeMetadata() ) {
				if ( embeddedTypeMetadata.getEmbeddedFieldName().equals( fieldNameParts[i] ) ) {
					parentMetadata = embeddedTypeMetadata;
					break;
				}
			}
		}

		return parentMetadata;
	}

	private static PropertyMetadata getPropertyMetadata(TypeMetadata type, String fieldName, String[] fieldNameParts) {
		String lastParticle = fieldNameParts[fieldNameParts.length - 1];

		for ( PropertyMetadata property : type.getAllPropertyMetadata() ) {
			for ( DocumentFieldMetadata field : property.getFieldMetadata() ) {
				if ( field.getName().equals( fieldName ) ) {
					return property;
				}
			}
		}

		for ( EmbeddedTypeMetadata embeddedType : type.getEmbeddedTypeMetadata() ) {
			if ( !lastParticle.startsWith( embeddedType.getEmbeddedFieldName() ) ) {
				continue;
			}

			for ( PropertyMetadata property : embeddedType.getAllPropertyMetadata() ) {
				for ( DocumentFieldMetadata field : embeddedType.getAllDocumentFieldMetadata() ) {
					if ( field.getName().equals( fieldName ) ) {
						return property;
					}
				}
			}
		}

		return null;
	}
}
