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
package org.hibernate.search.remote.codex.impl;

import java.util.ArrayList;
import java.util.List;

import org.apache.lucene.document.Field;

import org.hibernate.search.SearchException;
import org.hibernate.search.backend.LuceneWork;
import org.hibernate.search.remote.codex.spi.Deserializer;
import org.hibernate.search.remote.codex.spi.LuceneHydrator;
import org.hibernate.search.remote.operations.impl.Add;
import org.hibernate.search.remote.operations.impl.Delete;
import org.hibernate.search.remote.operations.impl.Message;
import org.hibernate.search.remote.operations.impl.Operation;
import org.hibernate.search.remote.operations.impl.OptimizeAll;
import org.hibernate.search.remote.operations.impl.PurgeAll;
import org.hibernate.search.remote.operations.impl.SerializableBinaryField;
import org.hibernate.search.remote.operations.impl.SerializableCustomFieldable;
import org.hibernate.search.remote.operations.impl.SerializableDocument;
import org.hibernate.search.remote.operations.impl.SerializableDoubleField;
import org.hibernate.search.remote.operations.impl.SerializableField;
import org.hibernate.search.remote.operations.impl.SerializableFieldable;
import org.hibernate.search.remote.operations.impl.SerializableFloatField;
import org.hibernate.search.remote.operations.impl.SerializableIntField;
import org.hibernate.search.remote.operations.impl.SerializableLongField;
import org.hibernate.search.remote.operations.impl.SerializableNumericField;
import org.hibernate.search.remote.operations.impl.SerializableReaderField;
import org.hibernate.search.remote.operations.impl.SerializableStringField;
import org.hibernate.search.remote.operations.impl.SerializableTokenStreamField;
import org.hibernate.search.remote.operations.impl.Update;

/**
 * @author Emmanuel Bernard <emmanuel@hibernate.org>
 */
public class ModelDeserializer implements Deserializer {

	@Override
	public void deserialize(byte[] data, LuceneHydrator hydrator) {
		Message message = SerializationHelper.toInstance( data, Message.class );

		if ( message.getProtocolVersion() != 1 ) {
			throw new SearchException( "Serialization protocol not supported. Protocol version: " + message.getProtocolVersion() );
		}
		for ( Operation operation : message.getOperations() ) {
			if ( operation instanceof OptimizeAll ) {
				hydrator.addOptimizeAll();
			}
			else if ( operation instanceof PurgeAll ) {
				PurgeAll safeOperation = ( PurgeAll ) operation;
				hydrator.addPurgeAllLuceneWork( safeOperation.getClass().getName() );
			}
			else if ( operation instanceof Delete ) {
				Delete safeOperation = ( Delete ) operation;
				hydrator.addDeleteLuceneWork(
						safeOperation.getEntityClassName(),
						safeOperation.getId()
				);
			}
			else if ( operation instanceof Add ) {
				Add safeOperation = ( Add ) operation;
				buildLuceneDocument( safeOperation.getDocument(), hydrator );
				hydrator.addAddLuceneWork(
						safeOperation.getEntityClassName(),
						safeOperation.getId(),
						safeOperation.getFieldToAnalyzerMap()
				);
			}
			else if ( operation instanceof Update ) {
				Update safeOperation = ( Update ) operation;
				buildLuceneDocument( safeOperation.getDocument(), hydrator );
				hydrator.addUpdateLuceneWork(
						safeOperation.getEntityClassName(),
						safeOperation.getId(),
						safeOperation.getFieldToAnalyzerMap()
				);
			}
		}
	}

	private void buildLuceneDocument(SerializableDocument document, LuceneHydrator hydrator) {
		hydrator.defineDocument( document.getBoost() );
		for ( SerializableFieldable field : document.getFieldables() ) {
			if ( field instanceof SerializableCustomFieldable ) {
				SerializableCustomFieldable safeField = ( SerializableCustomFieldable ) field;
				hydrator.addFieldable( safeField.getInstance() );
			}
			else if ( field instanceof SerializableNumericField ) {
				SerializableNumericField safeField = ( SerializableNumericField ) field;
				if ( field instanceof SerializableIntField ) {
					hydrator.addIntNumericField(
							( ( SerializableIntField ) field ).getValue(),
							safeField.getName(),
							safeField.getPrecisionStep(),
							safeField.getStore(),
							safeField.isIndexed(),
							safeField.isOmitNorms(),
							safeField.isOmitTermFreqAndPositions()
					);
				}
				else if ( field instanceof SerializableLongField ) {
					hydrator.addLongNumericField(
							( ( SerializableLongField ) field ).getValue(),
							safeField.getName(),
							safeField.getPrecisionStep(),
							safeField.getStore(),
							safeField.isIndexed(),
							safeField.isOmitNorms(),
							safeField.isOmitTermFreqAndPositions()
					);
				}
				else if ( field instanceof SerializableFloatField ) {
					hydrator.addFloatNumericField(
							( ( SerializableFloatField ) field ).getValue(),
							safeField.getName(),
							safeField.getPrecisionStep(),
							safeField.getStore(),
							safeField.isIndexed(),
							safeField.isOmitNorms(),
							safeField.isOmitTermFreqAndPositions()
					);
				}
				else if ( field instanceof SerializableDoubleField ) {
					hydrator.addDoubleNumericField(
							( ( SerializableDoubleField ) field ).getValue(),
							safeField.getName(),
							safeField.getPrecisionStep(),
							safeField.getStore(),
							safeField.isIndexed(),
							safeField.isOmitNorms(),
							safeField.isOmitTermFreqAndPositions()
					);
				}
				else {
					throw new SearchException( "Unknown SerializableNumericField: " + field.getClass() );
				}
			}
			else if ( field instanceof SerializableField ) {
				SerializableField safeField = ( SerializableField ) field;
				Field luceneField;
				if ( field instanceof SerializableBinaryField ) {
					SerializableBinaryField reallySafeField = ( SerializableBinaryField ) field;
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
					SerializableStringField reallySafeField = ( SerializableStringField ) field;
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
					SerializableTokenStreamField reallySafeField = ( SerializableTokenStreamField ) field;
					hydrator.addFieldWithTokenStreamData(
							reallySafeField.getName(),
							reallySafeField.getValue().getStream(),
							reallySafeField.getTermVector(),
							safeField.getBoost(),
							safeField.isOmitNorms(),
							safeField.isOmitTermFreqAndPositions()
					);
				}
				else if ( field instanceof SerializableReaderField ) {
					SerializableReaderField reallySafeField = ( SerializableReaderField ) field;
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
