/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.indexes.serialization.impl;

import java.io.Serializable;
import java.util.List;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.FieldType.NumericType;
import org.apache.lucene.index.IndexableField;
import org.hibernate.search.exception.SearchException;
import org.hibernate.search.backend.AddLuceneWork;
import org.hibernate.search.backend.DeleteLuceneWork;
import org.hibernate.search.backend.LuceneWork;
import org.hibernate.search.backend.FlushLuceneWork;
import org.hibernate.search.backend.OptimizeLuceneWork;
import org.hibernate.search.backend.PurgeAllLuceneWork;
import org.hibernate.search.backend.UpdateLuceneWork;
import org.hibernate.search.engine.integration.impl.ExtendedSearchIntegrator;
import org.hibernate.search.indexes.serialization.spi.Deserializer;
import org.hibernate.search.indexes.serialization.spi.LuceneFieldContext;
import org.hibernate.search.indexes.serialization.spi.LuceneNumericFieldContext;
import org.hibernate.search.indexes.serialization.spi.LuceneWorkSerializer;
import org.hibernate.search.indexes.serialization.spi.SerializationProvider;
import org.hibernate.search.indexes.serialization.spi.Serializer;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;

import static org.hibernate.search.indexes.serialization.impl.SerializationHelper.toByteArray;

/**
 * Serializes {@code List<LuceneWork>} instances back and forth using a pluggable {@code SerializerProvider}.
 *
 * This class controls the overall traversal process and delegates true serialization work to the {@code SerializerProvider}.
 *
 * @author Emmanuel Bernard <emmanuel@hibernate.org>
 */
public class LuceneWorkSerializerImpl implements LuceneWorkSerializer {
	private static Log log = LoggerFactory.make();

	private final ExtendedSearchIntegrator searchIntegrator;
	private final SerializationProvider provider;

	public LuceneWorkSerializerImpl(SerializationProvider provider, ExtendedSearchIntegrator searchIntegrator) {
		this.provider = provider;
		this.searchIntegrator = searchIntegrator;
		if ( provider == null ) {
			throw log.parametersShouldNotBeNull( "provider" );
		}
		if ( searchIntegrator == null ) {
			throw log.parametersShouldNotBeNull( "searchIntegrator" );
		}
	}

	/**
	 * Convert a List of LuceneWork into a byte[]
	 */
	@Override
	public byte[] toSerializedModel(List<LuceneWork> works) {
		try {
			Serializer serializer = provider.getSerializer();
			serializer.luceneWorks( works );

			for ( LuceneWork work : works ) {
				if ( work instanceof OptimizeLuceneWork ) {
					serializer.addOptimizeAll();
				}
				else if (work instanceof PurgeAllLuceneWork) {
					serializer.addPurgeAll( work.getEntityClass().getName() );
				}
				else if (work instanceof FlushLuceneWork) {
					serializer.addFlush();
				}
				else if (work instanceof DeleteLuceneWork) {
					processId( work, serializer );
					serializer.addDelete( work.getEntityClass().getName() );
				}
				else if (work instanceof AddLuceneWork ) {
					buildDocument( work.getDocument(), serializer );
					processId( work, serializer );
					serializer.addAdd( work.getEntityClass().getName(), work.getFieldToAnalyzerMap() );
				}
				else if (work instanceof UpdateLuceneWork ) {
					buildDocument( work.getDocument(), serializer );
					processId( work, serializer );
					serializer.addUpdate( work.getEntityClass().getName(), work.getFieldToAnalyzerMap() );
				}
			}
			return serializer.serialize();
		}
		catch (RuntimeException e) {
			if ( e instanceof SearchException ) {
				throw e;
			}
			else {
				throw log.unableToSerializeLuceneWorks( e );
			}
		}
	}

	private void processId(LuceneWork work, Serializer serializer) {
		Serializable id = work.getId();
		if ( id instanceof Integer ) {
			serializer.addIdAsInteger( (Integer) id );
		}
		else if ( id instanceof Long ) {
			serializer.addIdAsLong( (Long) id );
		}
		else if ( id instanceof Float ) {
			serializer.addIdAsFloat( (Float) id );
		}
		else if ( id instanceof Double ) {
			serializer.addIdAsDouble( (Double) id );
		}
		else if ( id instanceof String ) {
			serializer.addIdAsString( id.toString() );
		}
		else {
			serializer.addIdSerializedInJava( toByteArray( id ) );
		}
	}

	/**
	 * Convert a byte[] to a List of LuceneWork (assuming the same SerializationProvider is used of course)
	 */
	@Override
	public List<LuceneWork> toLuceneWorks(byte[] data) {
		try {
			Deserializer deserializer = provider.getDeserializer();
			LuceneWorkHydrator hydrator = new LuceneWorkHydrator( searchIntegrator );
			deserializer.deserialize( data, hydrator );
			return hydrator.getLuceneWorks();
		}
		catch (RuntimeException e) {
			if ( e instanceof SearchException ) {
				throw e;
			}
			else {
				throw log.unableToReadSerializedLuceneWorks( e );
			}
		}
	}

	private void buildDocument(Document document, Serializer serializer) {
		final List<IndexableField> docFields = document.getFields();
		serializer.fields( docFields );
		for ( IndexableField fieldable : docFields ) {
			final FieldType fieldType = (FieldType) fieldable.fieldType();
			final NumericType numericType = fieldType.numericType();
			if ( numericType != null ) {
				LuceneNumericFieldContext context = new LuceneNumericFieldContext( fieldType, fieldable.name(), fieldable.boost() );
				switch ( numericType ) {
					case INT:
						serializer.addIntNumericField( fieldable.numericValue().intValue(), context );
						break;
					case LONG:
						serializer.addLongNumericField( fieldable.numericValue().longValue(), context );
						break;
					case FLOAT:
						serializer.addFloatNumericField( fieldable.numericValue().floatValue(), context );
						break;
					case DOUBLE:
						serializer.addDoubleNumericField( fieldable.numericValue().doubleValue(), context );
						break;
					default:
						String dataType = numericType.toString();
						throw log.unknownNumericFieldType( dataType );
				}
			}
			else if (fieldable instanceof Field) {
				Field safeField = (Field) fieldable;
				//FIXME it seems like in new Field implementation it's possible to have multiple data types at the same time. Investigate?
				//The following sequence of else/ifs would not be appropriate.
				if ( safeField.binaryValue() != null ) {
					serializer.addFieldWithBinaryData( new LuceneFieldContext( safeField ) );
				}
				else if ( safeField.stringValue() != null ) {
					serializer.addFieldWithStringData( new LuceneFieldContext( safeField ) );
				}
				else if ( safeField.readerValue() != null && safeField.readerValue() instanceof Serializable ) {
					serializer.addFieldWithSerializableReaderData( new LuceneFieldContext( safeField ) );
				}
				else if ( safeField.readerValue() != null ) {
					throw log.conversionFromReaderToStringNotYetImplemented();
				}
				else if ( safeField.tokenStreamValue() != null ) {
					serializer.addFieldWithTokenStreamData( new LuceneFieldContext( safeField ) );
				}
				else {
					throw log.unknownFieldType( safeField.getClass() );
				}
			}
			else {
				throw log.cannotSerializeCustomField( fieldable.getClass() );
			}
		}
		serializer.addDocument();
	}

	@Override
	public String describeSerializer() {
		return provider.toString();
	}
}
