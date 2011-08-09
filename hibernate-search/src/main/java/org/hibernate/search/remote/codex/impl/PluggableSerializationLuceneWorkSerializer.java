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

import java.io.Serializable;
import java.util.List;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.Fieldable;
import org.apache.lucene.document.NumericField;

import org.hibernate.search.SearchException;
import org.hibernate.search.backend.AddLuceneWork;
import org.hibernate.search.backend.DeleteLuceneWork;
import org.hibernate.search.backend.LuceneWork;
import org.hibernate.search.backend.OptimizeLuceneWork;
import org.hibernate.search.backend.PurgeAllLuceneWork;
import org.hibernate.search.backend.UpdateLuceneWork;
import org.hibernate.search.engine.spi.SearchFactoryImplementor;
import org.hibernate.search.remote.codex.spi.Deserializer;
import org.hibernate.search.remote.codex.spi.LuceneWorkSerializer;
import org.hibernate.search.remote.codex.spi.Serializer;
import org.hibernate.search.remote.codex.spi.SerializationProvider;
import org.hibernate.search.remote.operations.impl.LuceneFieldContext;
import org.hibernate.search.remote.operations.impl.LuceneNumericFieldContext;

import static org.hibernate.search.remote.codex.impl.SerializationHelper.*;

/**
 * Serializes List<LuceneWork> back and forth using
 * a pluggable SerializerProvider.
 *
 * This class control the over all traversal process and delegates true serialization
 * work to the SerializerProvider.
 *
 * @author Emmanuel Bernard <emmanuel@hibernate.org>
 */
public class PluggableSerializationLuceneWorkSerializer implements LuceneWorkSerializer {

	private SearchFactoryImplementor searchFactory;
	private SerializationProvider provider;

	public PluggableSerializationLuceneWorkSerializer(SerializationProvider provider, SearchFactoryImplementor searchFactory) {
		this.provider = provider;
		this.searchFactory = searchFactory;
	}

	/**
	 * Convert a List of LuceneWork into a byte[]
	 */
	@Override
	public byte[] toSerializedModel(List<LuceneWork> works) {
		Serializer serializer = provider.getSerializer();
		serializer.luceneWorks( works );

		for (LuceneWork work : works) {
			if (work instanceof OptimizeLuceneWork) {
				serializer.addOptimizeAll();
			}
			else if (work instanceof PurgeAllLuceneWork) {
				serializer.addPurgeAll( work.getEntityClass().getName() );
			}
			else if (work instanceof DeleteLuceneWork) {
				serializer.addDelete( work.getEntityClass().getName(), toByteArray( work.getId() ) );
			}
			else if (work instanceof AddLuceneWork ) {
				buildDocument( work.getDocument(), serializer );
				serializer.addAdd( work.getEntityClass().getName(), toByteArray( work.getId() ), work.getFieldToAnalyzerMap() );
			}
			else if (work instanceof UpdateLuceneWork ) {
				buildDocument( work.getDocument(), serializer );
				serializer.addUpdate( work.getEntityClass().getName(), toByteArray( work.getId() ), work.getFieldToAnalyzerMap() );
			}
		}
		return serializer.serialize();
	}

	/**
	 * Convert a byte[] to a List of LuceneWork (assuming the same SerializationProvider is used of course)
	 */
	@Override
	public List<LuceneWork> toLuceneWorks(byte[] data) {
		Deserializer deserializer = provider.getDeserializer();
		LuceneWorkHydrator hydrator = new LuceneWorkHydrator( searchFactory );
		deserializer.deserialize( data, hydrator );
		return hydrator.getLuceneWorks();
	}


	private void buildDocument(Document document, Serializer serializer) {
		List<Fieldable> docFields = document.getFields();
		serializer.fields( docFields );
		for(Fieldable fieldable : docFields) {
			if (fieldable instanceof NumericField) {
				NumericField safeField = (NumericField) fieldable;
				LuceneNumericFieldContext context = new LuceneNumericFieldContext( (NumericField) fieldable );
				switch ( safeField.getDataType() ) {
					case INT:
						serializer.addIntNumericField(
								safeField.getNumericValue().intValue(),
								context
						);
						break;
					case LONG:
						serializer.addLongNumericField(
								safeField.getNumericValue().longValue(),
								context
						);
						break;
					case FLOAT:
						serializer.addFloatNumericField(
								safeField.getNumericValue().floatValue(),
								context
						);
						break;
					case DOUBLE:
						serializer.addDoubleNumericField(
								safeField.getNumericValue().doubleValue(),
								context
						);
						break;
					default:
					    throw new SearchException( "Unknown NumericField type: " + safeField.getDataType() );
				}
			}
			else if (fieldable instanceof Field) {
				Field safeField = (Field) fieldable;
				if ( safeField.isBinary() ) {
					serializer.addFieldWithBinaryData( new LuceneFieldContext( safeField ) );
				}
				else if ( safeField.stringValue() != null )  {
					serializer.addFieldWithStringData( new LuceneFieldContext( safeField ) );
				}
				else if ( safeField.readerValue() != null && safeField.readerValue() instanceof Serializable )  {
					serializer.addFieldWithSerializableReaderData( new LuceneFieldContext( safeField ) );
				}
				else if ( safeField.readerValue() != null )  {
					throw new SearchException( "Conversion from Reader to String not yet implemented" );
				}
				else if ( safeField.tokenStreamValue() != null )  {
					serializer.addFieldWithTokenStreamData( new LuceneFieldContext( safeField ) );
				}
				else {
					throw new SearchException( "Unknown value type for Field: " + safeField );
				}
			}
			else if (fieldable instanceof Serializable) { //Today Fieldable is Serializable but for how long?
				serializer.addFieldWithSerializableFieldable( toByteArray( ( Serializable ) fieldable ) );
			}
			else {
				throw new SearchException( "Cannot serialize custom field '" + fieldable.getClass() + "'. Must be NumericField, Field or a Serializable Fieldable implementation." );
			}
		}
		serializer.addDocument( document.getBoost() );
	}
}
