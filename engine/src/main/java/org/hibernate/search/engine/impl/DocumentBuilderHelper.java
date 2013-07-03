/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat, Inc. and/or its affiliates or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat, Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */

package org.hibernate.search.engine.impl;

import java.io.Serializable;
import java.util.Arrays;
import java.util.zip.DataFormatException;

import org.apache.lucene.document.CompressionTools;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Fieldable;
import org.apache.lucene.document.NumericField;
import org.hibernate.search.SearchException;
import org.hibernate.search.annotations.Store;
import org.hibernate.search.bridge.FieldBridge;
import org.hibernate.search.bridge.TwoWayFieldBridge;
import org.hibernate.search.bridge.spi.ConversionContext;
import org.hibernate.search.engine.metadata.impl.DocumentFieldMetadata;
import org.hibernate.search.engine.metadata.impl.EmbeddedTypeMetadata;
import org.hibernate.search.engine.metadata.impl.PropertyMetadata;
import org.hibernate.search.engine.metadata.impl.TypeMetadata;
import org.hibernate.search.engine.spi.DocumentBuilderIndexedEntity;
import org.hibernate.search.engine.spi.EntityIndexBinding;
import org.hibernate.search.engine.spi.SearchFactoryImplementor;
import org.hibernate.search.util.impl.ClassLoaderHelper;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;

/**
 * @author Hardy Ferentschik
 * @author Sanne Grinovero
 * @author Ales Justin
 */
public final class DocumentBuilderHelper {
	private static final Log log = LoggerFactory.make();
	private static final Object NOT_SET = new Object();

	private DocumentBuilderHelper() {
	}

	public static Class getDocumentClass(String className) {
		try {
			// Use the same class loader used to load this class ...
			return ClassLoaderHelper.classForName( className, DocumentBuilderHelper.class.getClassLoader() );
		}
		catch (ClassNotFoundException e) {
			throw new SearchException( "Unable to load indexed class: " + className, e );
		}
	}

	public static Serializable getDocumentId(SearchFactoryImplementor searchFactoryImplementor, Class<?> clazz, Document document, ConversionContext conversionContext) {
		final DocumentBuilderIndexedEntity<?> builderIndexedEntity = getDocumentBuilder(
				searchFactoryImplementor,
				clazz
		);
		final TwoWayFieldBridge fieldBridge = builderIndexedEntity.getIdBridge();
		final String fieldName = builderIndexedEntity.getIdKeywordName();
		try {
			return (Serializable) conversionContext
					.setClass( clazz )
					.pushIdentifierProperty()
					.twoWayConversionContext( fieldBridge )
					.get( fieldName, document );
		}
		finally {
			conversionContext.popProperty();
		}
	}

	public static String getDocumentIdName(SearchFactoryImplementor searchFactoryImplementor, Class<?> clazz) {
		DocumentBuilderIndexedEntity<?> documentBuilder = getDocumentBuilder( searchFactoryImplementor, clazz );
		return documentBuilder.getIdentifierName();
	}

	public static Object[] getDocumentFields(SearchFactoryImplementor searchFactoryImplementor, Class<?> clazz, Document document, String[] fields, ConversionContext conversionContext) {
		DocumentBuilderIndexedEntity<?> builderIndexedEntity = getDocumentBuilder( searchFactoryImplementor, clazz );
		final int fieldNbr = fields.length;
		Object[] result = new Object[fieldNbr];
		Arrays.fill( result, NOT_SET );
		conversionContext.setClass( clazz );
		if ( builderIndexedEntity.getIdKeywordName() != null ) {
			final String fieldName = builderIndexedEntity.getIdKeywordName();
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

		final TypeMetadata metadata = builderIndexedEntity.getMetadata();
		processFieldsForProjection( metadata, fields, result, document, conversionContext );
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
				throw new SearchException( "Projecting an unstored field: " + fieldName );
			}
			else {
				throw new SearchException( "FieldBridge is not a TwoWayFieldBridge: " + fieldBridge.getClass() );
			}
		}
	}

	private static void processFieldsForProjection(TypeMetadata typeMetadata, String[] fields, Object[] result, Document document, ConversionContext contextualBridge) {
		//process base fields
		for ( PropertyMetadata propertyMetadata : typeMetadata.getAllPropertyMetadata() ) {
			for ( DocumentFieldMetadata fieldMetadata : propertyMetadata.getFieldMetadata() ) {
				final String fieldName = fieldMetadata.getName();
				int matchingPosition = getFieldPosition( fields, fieldName );
				if ( matchingPosition != -1 ) {
					contextualBridge.pushProperty( fieldName );
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
				contextualBridge.pushProperty( embeddedTypeMetadata.getEmbeddedFieldName() );
				try {
					processFieldsForProjection(
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
			int matchingPosition = getFieldPosition( fields, fieldMetadata.getName() );
			if ( matchingPosition != -1 ) {
				populateResult(
						fieldMetadata.getName(),
						fieldMetadata.getFieldBridge(),
						fieldMetadata.getStore(),
						result,
						document,
						contextualBridge,
						matchingPosition
				);
			}
		}

		//If we still didn't know the value using any bridge, return the raw value or string:
		for ( int index = 0; index < result.length; index++ ) {
			if ( result[index] == NOT_SET ) {
				result[index] = null; // make sure we never return NOT_SET
				if ( document != null ) {
					Fieldable field = document.getFieldable( fields[index] );
					if ( field != null ) {
						result[index] = extractObjectFromFieldable( field );
					}
				}
			}
		}
	}

	public static Object extractObjectFromFieldable(Fieldable field) {
		if ( field instanceof NumericField ) {
			return NumericField.class.cast( field ).getNumericValue();
		}
		else {
			return extractStringFromFieldable( field );
		}
	}

	public static String extractStringFromFieldable(Fieldable field) {
		if ( field.isBinary() ) {
			try {
				return CompressionTools.decompressString( field.getBinaryValue() );
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

	private static DocumentBuilderIndexedEntity<?> getDocumentBuilder(SearchFactoryImplementor searchFactoryImplementor, Class<?> clazz) {
		EntityIndexBinding entityIndexBinding = searchFactoryImplementor.getIndexBinding(
				clazz
		);
		if ( entityIndexBinding == null ) {
			throw new SearchException( "No Lucene configuration set up for: " + clazz );
		}
		return entityIndexBinding.getDocumentBuilder();
	}
}


