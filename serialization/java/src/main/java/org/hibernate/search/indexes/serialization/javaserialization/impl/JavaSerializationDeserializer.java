/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2011 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
package org.hibernate.search.indexes.serialization.javaserialization.impl;

import java.io.ByteArrayInputStream;
import java.util.List;

import org.apache.lucene.util.AttributeImpl;

import org.hibernate.search.SearchException;
import org.hibernate.search.bridge.spi.ConversionContext;
import org.hibernate.search.bridge.util.impl.ContextualExceptionBridgeHelper;
import org.hibernate.search.indexes.serialization.avro.impl.AvroSerializationProvider;
import org.hibernate.search.indexes.serialization.impl.SerializationHelper;
import org.hibernate.search.indexes.serialization.spi.Deserializer;
import org.hibernate.search.indexes.serialization.spi.LuceneWorksBuilder;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;

/**
 * @author Emmanuel Bernard <emmanuel@hibernate.org>
 */
public class JavaSerializationDeserializer implements Deserializer {
	private static final Log log = LoggerFactory.make();

	@Override
	public void deserialize(byte[] data, LuceneWorksBuilder hydrator) {
		ByteArrayInputStream inputStream = new ByteArrayInputStream(data);
		int majorVersion = inputStream.read();
		int minorVersion = inputStream.read();
		if ( AvroSerializationProvider.getMajorVersion() != majorVersion ) {
			throw new SearchException(
					"Unable to parse message from protocol version "
							+ majorVersion + "." + minorVersion
							+ ". Current protocol version: "
							+ AvroSerializationProvider.getMajorVersion()
							+ "." + AvroSerializationProvider.getMinorVersion() );
		}
		if ( AvroSerializationProvider.getMinorVersion() < minorVersion ) {
			//TODO what to do about it? Log each time? Once?
			if ( log.isTraceEnabled() ) {
				log.tracef( "Parsing message from a future protocol version. Some feature might not be propagated. Message version: "
								+ majorVersion + "." + minorVersion
								+ ". Current protocol version: "
								+ AvroSerializationProvider.getMajorVersion()
								+ "." + AvroSerializationProvider.getMinorVersion()
				);
			}
		}
		byte[] newData = new byte[data.length - 2];
		System.arraycopy( data, 2, newData, 0, newData.length );
		Message message = SerializationHelper.toInstance( newData, Message.class );
		final ConversionContext conversionContext = new ContextualExceptionBridgeHelper();
		for ( Operation operation : message.getOperations() ) {
			if ( operation instanceof OptimizeAll ) {
				hydrator.addOptimizeAll();
			}
			else if ( operation instanceof PurgeAll ) {
				PurgeAll safeOperation = (PurgeAll) operation;
				hydrator.addPurgeAllLuceneWork( safeOperation.getClass().getName() );
			}
			else if ( operation instanceof Delete ) {
				Delete safeOperation = (Delete) operation;
				hydrator.addId( safeOperation.getId() );
				hydrator.addDeleteLuceneWork(
						safeOperation.getEntityClassName(),
						conversionContext
				);
			}
			else if ( operation instanceof Add ) {
				Add safeOperation = (Add) operation;
				buildLuceneDocument( safeOperation.getDocument(), hydrator );
				hydrator.addId( safeOperation.getId() );
				hydrator.addAddLuceneWork(
						safeOperation.getEntityClassName(),
						safeOperation.getFieldToAnalyzerMap(),
						conversionContext
				);
			}
			else if ( operation instanceof Update ) {
				Update safeOperation = (Update) operation;
				buildLuceneDocument( safeOperation.getDocument(), hydrator );
				hydrator.addId( safeOperation.getId() );
				hydrator.addUpdateLuceneWork(
						safeOperation.getEntityClassName(),
						safeOperation.getFieldToAnalyzerMap(),
						conversionContext
				);
			}
		}
	}

	private void buildLuceneDocument(SerializableDocument document, LuceneWorksBuilder hydrator) {
		hydrator.defineDocument( document.getBoost() );
		for ( SerializableFieldable field : document.getFieldables() ) {
			if ( field instanceof SerializableCustomFieldable ) {
				SerializableCustomFieldable safeField = (SerializableCustomFieldable) field;
				hydrator.addFieldable( safeField.getInstance() );
			}
			else if ( field instanceof SerializableNumericField ) {
				SerializableNumericField safeField = (SerializableNumericField) field;
				if ( field instanceof SerializableIntField ) {
					hydrator.addIntNumericField(
							( (SerializableIntField) field ).getValue(),
							safeField.getName(),
							safeField.getPrecisionStep(),
							safeField.getStore(),
							safeField.isIndexed(),
							safeField.getBoost(),
							safeField.isOmitNorms(),
							safeField.isOmitTermFreqAndPositions()
					);
				}
				else if ( field instanceof SerializableLongField ) {
					hydrator.addLongNumericField(
							( (SerializableLongField) field ).getValue(),
							safeField.getName(),
							safeField.getPrecisionStep(),
							safeField.getStore(),
							safeField.isIndexed(),
							safeField.getBoost(),
							safeField.isOmitNorms(),
							safeField.isOmitTermFreqAndPositions()
					);
				}
				else if ( field instanceof SerializableFloatField ) {
					hydrator.addFloatNumericField(
							( (SerializableFloatField) field ).getValue(),
							safeField.getName(),
							safeField.getPrecisionStep(),
							safeField.getStore(),
							safeField.isIndexed(),
							safeField.getBoost(),
							safeField.isOmitNorms(),
							safeField.isOmitTermFreqAndPositions()
					);
				}
				else if ( field instanceof SerializableDoubleField ) {
					hydrator.addDoubleNumericField(
							( (SerializableDoubleField) field ).getValue(),
							safeField.getName(),
							safeField.getPrecisionStep(),
							safeField.getStore(),
							safeField.isIndexed(),
							safeField.getBoost(),
							safeField.isOmitNorms(),
							safeField.isOmitTermFreqAndPositions()
					);
				}
				else {
					throw new SearchException( "Unknown SerializableNumericField: " + field.getClass() );
				}
			}
			else if ( field instanceof SerializableField ) {
				SerializableField safeField = (SerializableField) field;
				if ( field instanceof SerializableBinaryField ) {
					SerializableBinaryField reallySafeField = (SerializableBinaryField) field;
					hydrator.addFieldWithBinaryData(
							reallySafeField.getName(),
							reallySafeField.getValue(),
							reallySafeField.getOffset(),
							reallySafeField.getLength(),
							safeField.getBoost(),
							safeField.isOmitNorms(),
							safeField.isOmitTermFreqAndPositions()
					);
				}
				else if ( field instanceof SerializableStringField ) {
					SerializableStringField reallySafeField = (SerializableStringField) field;
					hydrator.addFieldWithStringData(
							reallySafeField.getName(),
							reallySafeField.getValue(),
							reallySafeField.getStore(),
							reallySafeField.getIndex(),
							reallySafeField.getTermVector(),
							safeField.getBoost(),
							safeField.isOmitNorms(),
							safeField.isOmitTermFreqAndPositions()
					);
				}
				else if ( field instanceof SerializableTokenStreamField ) {
					SerializableTokenStreamField reallySafeField = (SerializableTokenStreamField) field;
					List<List<AttributeImpl>> tokens = reallySafeField.getValue().getStream();
					for ( List<AttributeImpl> token : tokens ) {
						for ( AttributeImpl attribute : token ) {
							hydrator.addAttributeInstance( attribute );
						}
						hydrator.addToken();
					}
					hydrator.addFieldWithTokenStreamData(
							reallySafeField.getName(),
							reallySafeField.getTermVector(),
							safeField.getBoost(),
							safeField.isOmitNorms(),
							safeField.isOmitTermFreqAndPositions()
					);
				}
				else if ( field instanceof SerializableReaderField ) {
					SerializableReaderField reallySafeField = (SerializableReaderField) field;
					hydrator.addFieldWithSerializableReaderData(
							reallySafeField.getName(),
							reallySafeField.getValue(),
							reallySafeField.getTermVector(),
							safeField.getBoost(),
							safeField.isOmitNorms(),
							safeField.isOmitTermFreqAndPositions()
					);
				}
				else {
					throw new SearchException( "Unknown SerializableField: " + field.getClass() );
				}
			}
			else {
				throw new SearchException( "Unknown SerializableFieldable: " + field.getClass() );
			}
		}
	}
}
