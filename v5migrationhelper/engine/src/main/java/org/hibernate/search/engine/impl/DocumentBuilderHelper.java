/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.search.engine.impl;

import java.io.Serializable;
import java.util.Arrays;
import java.util.zip.DataFormatException;

import org.apache.lucene.document.CompressionTools;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.util.BytesRef;

import org.hibernate.search.exception.SearchException;
import org.hibernate.search.spi.IndexedTypeIdentifier;
import org.hibernate.search.annotations.Store;
import org.hibernate.search.bridge.FieldBridge;
import org.hibernate.search.bridge.TwoWayFieldBridge;
import org.hibernate.search.bridge.spi.ConversionContext;
import org.hibernate.search.engine.integration.impl.ExtendedSearchIntegrator;
import org.hibernate.search.engine.metadata.impl.DocumentFieldMetadata;
import org.hibernate.search.engine.metadata.impl.EmbeddedTypeMetadata;
import org.hibernate.search.engine.metadata.impl.PropertyMetadata;
import org.hibernate.search.engine.metadata.impl.TypeMetadata;
import org.hibernate.search.engine.spi.DocumentBuilderIndexedEntity;
import org.hibernate.search.engine.spi.EntityIndexBinding;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;
import java.lang.invoke.MethodHandles;

/**
 * @author Hardy Ferentschik
 * @author Sanne Grinovero
 * @author Ales Justin
 */
public final class DocumentBuilderHelper {
	private static final Log log = LoggerFactory.make( MethodHandles.lookup() );
	private static final Object NOT_SET = new Object();

	private DocumentBuilderHelper() {
	}

	public static DocumentBuilderIndexedEntity getDocumentBuilder(String className, ExtendedSearchIntegrator searchIntegrator) {
		IndexedTypeIdentifier keyFromName = searchIntegrator.getIndexBindings().keyFromName( className );
		EntityIndexBinding entityIndexBinding = searchIntegrator.getIndexBinding( keyFromName );
		if ( entityIndexBinding == null ) {
			throw new SearchException( "No Lucene configuration set up for: " + keyFromName.getName() );
		}
		return entityIndexBinding.getDocumentBuilder();
	}

	public static Serializable getDocumentId(DocumentBuilderIndexedEntity builderIndexedEntity, Document document, ConversionContext conversionContext) {
		final TwoWayFieldBridge fieldBridge = builderIndexedEntity.getIdBridge();
		final String fieldName = builderIndexedEntity.getIdFieldName();
		try {
			return (Serializable) conversionContext
					.setConvertedTypeId( builderIndexedEntity.getTypeIdentifier() )
					.pushIdentifierProperty()
					.twoWayConversionContext( fieldBridge )
					.get( fieldName, document );
		}
		finally {
			conversionContext.popProperty();
		}
	}

	public static Object[] getDocumentFields(DocumentBuilderIndexedEntity builderIndexedEntity, Document document, String[] fields, ConversionContext conversionContext) {
		final int fieldNbr = fields.length;
		Object[] result = new Object[fieldNbr];
		Arrays.fill( result, NOT_SET );
		conversionContext.setConvertedTypeId( builderIndexedEntity.getTypeIdentifier() );
		if ( builderIndexedEntity.getIdFieldName() != null ) {
			final String fieldName = builderIndexedEntity.getIdFieldName();
			int matchingPosition = getFieldPosition( fields, fieldName );
			if ( matchingPosition != -1 ) {
				conversionContext.pushProperty( fieldName );
				try {
					populateResult(
							fieldName,
							builderIndexedEntity.getIdBridge(),
							Store.YES,
							result,
							document,
							conversionContext,
							matchingPosition
					);
				}
				finally {
					conversionContext.popProperty();
				}
			}
		}

		processFieldsForProjection( builderIndexedEntity, fields, result, document, conversionContext );
		return result;
	}

	public static void populateResult(String fieldName,
			FieldBridge fieldBridge,
			Store store,
			Object[] result,
			Document document,
			ConversionContext conversionContext,
			int matchingPosition) {
		//TODO make use of an isTwoWay() method
		if ( store != Store.NO && TwoWayFieldBridge.class.isAssignableFrom( fieldBridge.getClass() ) ) {
			result[matchingPosition] = conversionContext
					.twoWayConversionContext( (TwoWayFieldBridge) fieldBridge )
					.get( fieldName, document );
			if ( log.isTraceEnabled() ) {
				log.tracef( "Field %s projected as %s", fieldName, result[matchingPosition] );
			}
		}
		else {
			if ( store == Store.NO ) {
				throw log.projectingNonStoredField( fieldName );
			}
			else {
				throw log.projectingFieldWithoutTwoWayFieldBridge( fieldName, fieldBridge.getClass() );
			}
		}
	}

	private static void processFieldsForProjection(DocumentBuilderIndexedEntity builderIndexedEntity, String[] fields, Object[] result, Document document, ConversionContext conversionContext) {
		final TypeMetadata metadata = builderIndexedEntity.getTypeMetadata();

		//First try setting each projected field considering mapping metadata to apply (inverse) field bridges:
		processMetadataRecursivelyForProjections( metadata, fields, result, document, conversionContext );

		//If we still didn't know the value using any bridge, return the raw value or string:
		//Important: make sure this happens as last step of projections! See also HSEARCH-1786
		for ( int index = 0; index < result.length; index++ ) {
			if ( result[index] == NOT_SET ) {
				result[index] = null; // make sure we never return NOT_SET
				if ( document != null ) {
					IndexableField field = document.getField( fields[index] );
					if ( field != null ) {
						result[index] = extractObjectFromFieldable( field );
					}
				}
			}
		}
	}

	private static void processMetadataRecursivelyForProjections(TypeMetadata typeMetadata, String[] fields, Object[] result, Document document, ConversionContext contextualBridge) {
		//process base fields
		for ( PropertyMetadata propertyMetadata : typeMetadata.getAllPropertyMetadata() ) {
			for ( DocumentFieldMetadata fieldMetadata : propertyMetadata.getFieldMetadataSet() ) {
				final String fieldName = fieldMetadata.getAbsoluteName();
				int matchingPosition = getFieldPosition( fields, fieldName );
				if ( matchingPosition != -1 && result[matchingPosition] == NOT_SET ) {
					contextualBridge.pushProperty( propertyMetadata.getPropertyAccessorName() );
					try {
						populateResult(
								fieldName,
								fieldMetadata.getFieldBridge(),
								fieldMetadata.getStore(),
								result,
								document,
								contextualBridge,
								matchingPosition
						);
					}
					finally {
						contextualBridge.popProperty();
					}
				}
			}
		}

		//process fields of embedded
		for ( EmbeddedTypeMetadata embeddedTypeMetadata : typeMetadata.getEmbeddedTypeMetadata() ) {
			//there is nothing we can do for collections
			if ( embeddedTypeMetadata.getEmbeddedContainer() == EmbeddedTypeMetadata.Container.OBJECT ) {
				contextualBridge.pushProperty( embeddedTypeMetadata.getEmbeddedPropertyName() );
				try {
					processMetadataRecursivelyForProjections(
							embeddedTypeMetadata, fields, result, document, contextualBridge
					);
				}
				finally {
					contextualBridge.popProperty();
				}
			}
		}

		//process class bridges
		for ( DocumentFieldMetadata fieldMetadata : typeMetadata.getClassBridgeMetadata() ) {
			int matchingPosition = getFieldPosition( fields, fieldMetadata.getAbsoluteName() );
			if ( matchingPosition != -1 && result[matchingPosition] == NOT_SET ) {
				populateResult(
						fieldMetadata.getAbsoluteName(),
						fieldMetadata.getFieldBridge(),
						fieldMetadata.getStore(),
						result,
						document,
						contextualBridge,
						matchingPosition
				);
			}
		}
	}

	/**
	 * @deprecated we should know the projection rules from the metadata rather than guess from the field properties
	 * @param field the field
	 * @return the object
	 */
	@Deprecated
	public static Object extractObjectFromFieldable(IndexableField field) {
		//TODO remove this guess work
		final Number numericValue = field.numericValue();
		if ( numericValue != null ) {
			return numericValue;
		}
		else {
			return extractStringFromFieldable( field );
		}
	}

	/**
	 * @deprecated we should know the projection rules from the metadata rather than guess from the field properties
	 * @param field the field to decompress
	 * @return the decompressed field
	 */
	@Deprecated
	public static String extractStringFromFieldable(IndexableField field) {
		final BytesRef binaryValue = field.binaryValue();
		//TODO remove this guess work
		if ( binaryValue != null ) {
			try {
				return CompressionTools.decompressString( binaryValue );
			}
			catch (DataFormatException e) {
				throw log.fieldLooksBinaryButDecompressionFailed( field.name() );
			}
		}
		else {
			return field.stringValue();
		}
	}

	public static int getFieldPosition(String[] fields, String fieldName) {
		int fieldNbr = fields.length;
		for ( int index = 0; index < fieldNbr; index++ ) {
			if ( fieldName.equals( fields[index] ) ) {
				return index;
			}
		}
		return -1;
	}

}

