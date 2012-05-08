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

import org.hibernate.search.SearchException;
import org.hibernate.search.annotations.Store;
import org.hibernate.search.bridge.FieldBridge;
import org.hibernate.search.bridge.TwoWayFieldBridge;
import org.hibernate.search.bridge.spi.ConversionContext;
import org.hibernate.search.engine.spi.AbstractDocumentBuilder;
import org.hibernate.search.engine.spi.DocumentBuilderIndexedEntity;
import org.hibernate.search.engine.spi.EntityIndexBinder;
import org.hibernate.search.engine.spi.SearchFactoryImplementor;
import org.hibernate.search.util.impl.ClassLoaderHelper;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;

/**
 * @author Hardy Ferentschik
 * @author Sanne Grinovero
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
		catch ( ClassNotFoundException e ) {
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

		final AbstractDocumentBuilder.PropertiesMetadata metadata = builderIndexedEntity.getMetadata();
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

	private static void processFieldsForProjection(AbstractDocumentBuilder.PropertiesMetadata metadata, String[] fields, Object[] result, Document document, ConversionContext contextualBridge) {
		//process base fields
		final int nbrFoEntityFields = metadata.fieldNames.size();
		for ( int index = 0; index < nbrFoEntityFields; index++ ) {
			final String fieldName = metadata.fieldNames.get( index );
			int matchingPosition = getFieldPosition( fields, fieldName );
			if ( matchingPosition != -1 ) {
				contextualBridge.pushProperty( fieldName );
				try {
					populateResult(
							fieldName,
							metadata.fieldBridges.get( index ),
							metadata.fieldStore.get( index ),
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

		//process fields of embedded
		final int nbrOfEmbeddedObjects = metadata.embeddedPropertiesMetadata.size();
		for ( int index = 0; index < nbrOfEmbeddedObjects; index++ ) {
			//there is nothing we can do for collections
			if ( metadata.embeddedContainers.get( index ) == AbstractDocumentBuilder.PropertiesMetadata.Container.OBJECT ) {
				contextualBridge.pushProperty( metadata.embeddedFieldNames.get( index ) );
				try {
					processFieldsForProjection(
							metadata.embeddedPropertiesMetadata.get( index ), fields, result, document, contextualBridge
					);
				}
				finally {
					contextualBridge.popProperty();
				}
			}
		}

		//process class bridges
		final int nbrOfClassBridges = metadata.classBridges.size();
		for ( int index = 0; index < nbrOfClassBridges; index++ ) {
			final String fieldName = metadata.classNames.get( index );
			int matchingPosition = getFieldPosition( fields, fieldName );
			if ( matchingPosition != -1 ) {
				populateResult(
						fieldName,
						metadata.classBridges.get( index ),
						metadata.classStores.get( index ),
						result,
						document,
						contextualBridge,
						matchingPosition
				);
			}
		}

		//If we still didn't know the value using any bridge, return the raw string:
		for ( int index = 0; index < result.length; index++ ) {
			if ( result[index] == NOT_SET ) {
				result[index] = null; // make sure we never return NOT_SET
				if ( document != null ) {
					Fieldable field = document.getFieldable( fields[index] );
					if ( field != null ) {
						result[index] = extractStringFromFieldable( field );
					}
				}
			}
		}
	}

	public static String extractStringFromFieldable(Fieldable field) {
		if ( field.isBinary() ) {
			try {
				return CompressionTools.decompressString( field.getBinaryValue() );
			}
			catch ( DataFormatException e ) {
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
		EntityIndexBinder entityIndexBinding = searchFactoryImplementor.getIndexBindingForEntity(
				clazz
		);
		if ( entityIndexBinding == null ) {
			throw new SearchException( "No Lucene configuration set up for: " + clazz );
		}
		return entityIndexBinding.getDocumentBuilder();
	}
}


