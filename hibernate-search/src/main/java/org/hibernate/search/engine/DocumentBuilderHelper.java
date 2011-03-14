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

package org.hibernate.search.engine;

import java.io.Serializable;

import org.apache.lucene.document.Document;
import org.slf4j.Logger;

import org.hibernate.annotations.common.reflection.XMember;
import org.hibernate.annotations.common.util.ReflectHelper;
import org.hibernate.search.SearchException;
import org.hibernate.search.annotations.Store;
import org.hibernate.search.bridge.FieldBridge;
import org.hibernate.search.bridge.TwoWayFieldBridge;
import org.hibernate.search.bridge.util.ContextualException2WayBridge;
import org.hibernate.search.util.LoggerFactory;

/**
 * @author Hardy Ferentschik
 */
public final class DocumentBuilderHelper {
	private static final Logger log = LoggerFactory.make();

	private DocumentBuilderHelper() {
	}

	public static Class getDocumentClass(String className) {
		try {
			return ReflectHelper.classForName( className );
		}
		catch ( ClassNotFoundException e ) {
			throw new SearchException( "Unable to load indexed class: " + className, e );
		}
	}

	public static Serializable getDocumentId(SearchFactoryImplementor searchFactoryImplementor, Class<?> clazz, Document document) {
		DocumentBuilderIndexedEntity<?> builderIndexedEntity = searchFactoryImplementor.getDocumentBuilderIndexedEntity(
				clazz
		);
		if ( builderIndexedEntity == null ) {
			throw new SearchException( "No Lucene configuration set up for: " + clazz );
		}


		final TwoWayFieldBridge fieldBridge = builderIndexedEntity.getIdBridge();
		final String fieldName = builderIndexedEntity.getIdKeywordName();
		ContextualException2WayBridge contextualBridge = new ContextualException2WayBridge();
		contextualBridge
				.setClass( clazz )
				.setFieldName( fieldName )
				.setFieldBridge( fieldBridge )
				.pushIdentifierMethod();
		return (Serializable) contextualBridge.get( fieldName, document );
	}

	public static String getDocumentIdName(SearchFactoryImplementor searchFactoryImplementor, Class<?> clazz) {
		DocumentBuilderIndexedEntity<?> builderIndexedEntity = searchFactoryImplementor.getDocumentBuilderIndexedEntity(
				clazz
		);
		if ( builderIndexedEntity == null ) {
			throw new SearchException( "No Lucene configuration set up for: " + clazz );
		}
		return builderIndexedEntity.getIdentifierName();
	}

	public static Object[] getDocumentFields(SearchFactoryImplementor searchFactoryImplementor, Class<?> clazz, Document document, String[] fields) {
		DocumentBuilderIndexedEntity<?> builderIndexedEntity = searchFactoryImplementor.getDocumentBuilderIndexedEntity(
				clazz
		);
		if ( builderIndexedEntity == null ) {
			throw new SearchException( "No Lucene configuration set up for: " + clazz );
		}
		final int fieldNbr = fields.length;
		Object[] result = new Object[fieldNbr];
		ContextualException2WayBridge contextualBridge = new ContextualException2WayBridge();
		contextualBridge.setClass( clazz );
		if ( builderIndexedEntity.getIdKeywordName() != null ) {
			final XMember member = builderIndexedEntity.getIdGetter();
			if ( member != null ) {
				contextualBridge.pushMethod( member );
			}
			populateResult(
					builderIndexedEntity.getIdKeywordName(),
					builderIndexedEntity.getIdBridge(),
					Store.YES,
					fields,
					result,
					document,
					contextualBridge
			);
			if ( member != null ) {
				contextualBridge.popMethod();
			}
		}

		final AbstractDocumentBuilder.PropertiesMetadata metadata = builderIndexedEntity.getMetadata();
		processFieldsForProjection( metadata, fields, result, document, contextualBridge );
		return result;
	}

	public static void populateResult(String fieldName, FieldBridge fieldBridge, Store store,
									  String[] fields, Object[] result, Document document, ContextualException2WayBridge contextualBridge) {
		int matchingPosition = getFieldPosition( fields, fieldName );
		if ( matchingPosition != -1 ) {
			//TODO make use of an isTwoWay() method
			if ( store != Store.NO && TwoWayFieldBridge.class.isAssignableFrom( fieldBridge.getClass() ) ) {
				contextualBridge.setFieldName( fieldName ).setFieldBridge( (TwoWayFieldBridge) fieldBridge );
				result[matchingPosition] = contextualBridge.get( fieldName, document );
				if ( log.isTraceEnabled() ) {
					log.trace( "Field {} projected as {}", fieldName, result[matchingPosition] );
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
	}

	public static void processFieldsForProjection(AbstractDocumentBuilder.PropertiesMetadata metadata, String[] fields, Object[] result, Document document, ContextualException2WayBridge contextualBridge) {
		//process base fields
		final int nbrFoEntityFields = metadata.fieldNames.size();
		for ( int index = 0; index < nbrFoEntityFields; index++ ) {
			final String fieldName = metadata.fieldNames.get( index );
			contextualBridge.pushMethod( metadata.fieldGetters.get( index ) );
			populateResult(
					fieldName,
					metadata.fieldBridges.get( index ),
					metadata.fieldStore.get( index ),
					fields,
					result,
					document,
					contextualBridge
			);
			contextualBridge.popMethod();
		}

		//process fields of embedded
		final int nbrOfEmbeddedObjects = metadata.embeddedPropertiesMetadata.size();
		for ( int index = 0; index < nbrOfEmbeddedObjects; index++ ) {
			//there is nothing we can do for collections
			if ( metadata.embeddedContainers
					.get( index ) == AbstractDocumentBuilder.PropertiesMetadata.Container.OBJECT ) {
				contextualBridge.pushMethod( metadata.embeddedGetters.get( index ) );
				processFieldsForProjection(
						metadata.embeddedPropertiesMetadata.get( index ), fields, result, document, contextualBridge
				);
				contextualBridge.popMethod();
			}
		}

		//process class bridges
		final int nbrOfClassBridges = metadata.classBridges.size();
		for ( int index = 0; index < nbrOfClassBridges; index++ ) {
			populateResult(
					metadata.classNames.get( index ),
					metadata.classBridges.get( index ),
					metadata.classStores.get( index ),
					fields,
					result,
					document,
					contextualBridge
			);
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


