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
package org.hibernate.search.indexes.serialization.avro.impl;


import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.avro.Protocol;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericDatumWriter;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.io.BinaryEncoder;
import org.apache.avro.io.EncoderFactory;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.CharTermAttributeImpl;
import org.apache.lucene.analysis.tokenattributes.FlagsAttribute;
import org.apache.lucene.analysis.tokenattributes.KeywordAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.apache.lucene.analysis.tokenattributes.PayloadAttribute;
import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;
import org.apache.lucene.analysis.tokenattributes.TypeAttribute;
import org.apache.lucene.document.Fieldable;
import org.apache.lucene.util.AttributeImpl;
import org.apache.solr.handler.AnalysisRequestHandlerBase;

import org.hibernate.search.backend.LuceneWork;
import org.hibernate.search.indexes.serialization.spi.LuceneFieldContext;
import org.hibernate.search.indexes.serialization.spi.LuceneNumericFieldContext;
import org.hibernate.search.indexes.serialization.spi.Serializer;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;

import static org.hibernate.search.indexes.serialization.impl.SerializationHelper.toByteArray;

/**
 * @author Emmanuel Bernard <emmanuel@hibernate.org>
 */
public class AvroSerializer implements Serializer {
	private static final Log log = LoggerFactory.make();

	private GenericRecord idRecord;
	private List<GenericRecord> fieldables;
	private List<GenericRecord> operations;
	private List<String> classReferences;
	private GenericRecord document;
	private final Protocol protocol;

	public AvroSerializer(Protocol protocol) {
		this.protocol = protocol;
		this.classReferences = new ArrayList<String>();
	}

	@Override
	public void luceneWorks(List<LuceneWork> works) {
		operations = new ArrayList<GenericRecord>( works.size() );
	}

	@Override
	public void addOptimizeAll() {
		operations.add( new GenericData.Record( protocol.getType( "OptimizeAll" ) ) );
	}

	@Override
	public void addPurgeAll(String entityClassName) {
		int classRef = getClassReference( entityClassName );
		GenericRecord purgeAll = new GenericData.Record( protocol.getType( "PurgeAll" ) );
		purgeAll.put( "class", classRef );
		operations.add( purgeAll );
	}

	private int getClassReference(String entityClassName) {
		int classRef = classReferences.indexOf( entityClassName );
		if ( classRef == -1 ) {
			classReferences.add( entityClassName );
			classRef = classReferences.size() - 1;
		}
		return classRef;
	}

	@Override
	public void addIdSerializedInJava(byte[] id) {
		this.idRecord = new GenericData.Record( protocol.getType( "Id" ) );
		idRecord.put( "value", ByteBuffer.wrap( id ) );
	}

	@Override
	public void addIdAsInteger(int id) {
		this.idRecord = new GenericData.Record( protocol.getType( "Id" ) );
		idRecord.put( "value", id );
	}

	@Override
	public void addIdAsLong(long id) {
		this.idRecord = new GenericData.Record( protocol.getType( "Id" ) );
		idRecord.put( "value", id );
	}

	@Override
	public void addIdAsFloat(float id) {
		this.idRecord = new GenericData.Record( protocol.getType( "Id" ) );
		idRecord.put( "value", id );
	}

	@Override
	public void addIdAsDouble(double id) {
		this.idRecord = new GenericData.Record( protocol.getType( "Id" ) );
		idRecord.put( "value", id );
	}

	@Override
	public void addIdAsString(String id) {
		this.idRecord = new GenericData.Record( protocol.getType( "Id" ) );
		idRecord.put( "value", id );
	}

	@Override
	public void addDelete(String entityClassName) {
		int classRef = getClassReference( entityClassName );
		GenericRecord delete = new GenericData.Record( protocol.getType( "Delete" ) );
		delete.put( "class", classRef );
		delete.put( "id", idRecord );
		operations.add( delete );
		idRecord = null;
	}

	@Override
	public void addAdd(String entityClassName, Map<String, String> fieldToAnalyzerMap) {
		int classRef = getClassReference( entityClassName );
		GenericRecord add = new GenericData.Record( protocol.getType( "Add" ) );
		add.put( "class", classRef );
		add.put( "id", idRecord );
		add.put( "document", document );
		add.put( "fieldToAnalyzerMap", fieldToAnalyzerMap );
		operations.add( add );
		idRecord = null;
		clearDocument();
	}

	@Override
	public void addUpdate(String entityClassName, Map<String, String> fieldToAnalyzerMap) {
		int classRef = getClassReference( entityClassName );
		GenericRecord update = new GenericData.Record( protocol.getType( "Update" ) );
		update.put( "class", classRef );
		update.put( "id", idRecord );
		update.put( "document", document );
		update.put( "fieldToAnalyzerMap", fieldToAnalyzerMap );
		operations.add( update );
		idRecord = null;
		clearDocument();
	}


	@Override
	public byte[] serialize() {
		final ByteArrayOutputStream out = new ByteArrayOutputStream();
		out.write( AvroSerializationProvider.getMajorVersion() );
		out.write( AvroSerializationProvider.getMinorVersion() );
		Schema msgSchema = protocol.getType( "Message" );
		GenericDatumWriter<GenericRecord> writer = new GenericDatumWriter<GenericRecord>( msgSchema );
		BinaryEncoder encoder = EncoderFactory.get().directBinaryEncoder( out, null );
		GenericRecord message = new GenericData.Record( msgSchema );
		message.put( "classReferences", classReferences );
		message.put( "operations", operations );
		operations = null;
		try {
			writer.write( message, encoder );
			encoder.flush();
		}
		catch ( IOException e ) {
			throw log.unableToSerializeInAvro(e);
		}
		return out.toByteArray();
	}

	@Override
	public void fields(List<Fieldable> fields) {
		fieldables = new ArrayList<GenericRecord>( fields.size() );
	}

	@Override
	public void addIntNumericField(int value, LuceneNumericFieldContext context) {
		GenericRecord numericField = createNumericfield( "NumericIntField", context );
		numericField.put( "value", value );
		fieldables.add( numericField );
	}

	private GenericRecord createNumericfield(String schemaName, LuceneNumericFieldContext context) {
		GenericRecord numericField = new GenericData.Record( protocol.getType( schemaName ) );
		numericField.put( "name", context.getName() );
		numericField.put( "precisionStep", context.getPrecisionStep() );
		numericField.put( "store", context.getStore() );
		numericField.put( "indexed", context.isIndexed() );
		numericField.put( "boost", context.getBoost() );
		numericField.put( "omitNorms", context.getOmitNorms() );
		numericField.put( "omitTermFreqAndPositions", context.getOmitTermFreqAndPositions() );
		return numericField;
	}

	@Override
	public void addLongNumericField(long value, LuceneNumericFieldContext context) {
		GenericRecord numericField = createNumericfield( "NumericLongField", context );
		numericField.put( "value", value );
		fieldables.add( numericField );
	}

	@Override
	public void addFloatNumericField(float value, LuceneNumericFieldContext context) {
		GenericRecord numericField = createNumericfield( "NumericFloatField", context );
		numericField.put( "value", value );
		fieldables.add( numericField );
	}

	@Override
	public void addDoubleNumericField(double value, LuceneNumericFieldContext context) {
		GenericRecord numericField = createNumericfield( "NumericDoubleField", context );
		numericField.put( "value", value );
		fieldables.add( numericField );
	}

	@Override
	public void addFieldWithBinaryData(LuceneFieldContext context) {
		GenericRecord field = createNormalField( "BinaryField", context );
		field.put( "offset", context.getBinaryOffset() );
		field.put( "length", context.getBinaryLength() );
		field.put( "value", ByteBuffer.wrap( context.getBinaryValue() ) );
		fieldables.add( field );
	}

	private GenericRecord createNormalField(String schemaName, LuceneFieldContext context) {
		GenericRecord field = new GenericData.Record( protocol.getType( schemaName ) );
		field.put( "name", context.getName() );
		field.put( "boost", context.getBoost() );
		field.put( "omitNorms", context.isOmitNorms() );
		field.put( "omitTermFreqAndPositions", context.isOmitTermFreqAndPositions() );
		return field;
	}

	@Override
	public void addFieldWithStringData(LuceneFieldContext context) {
		GenericRecord field = createNormalField( "StringField", context );
		field.put( "value", context.getStringValue() );
		field.put( "store", context.getStore() );
		field.put( "index", context.getIndex() );
		field.put( "termVector", context.getTermVector() );
		fieldables.add( field );
	}

	@Override
	public void addFieldWithTokenStreamData(LuceneFieldContext context) {
		GenericRecord field = createNormalField( "TokenStreamField", context );
		List<List<AttributeImpl>> stream = context.getTokenStream().getStream();
		List<List<Object>> value = new ArrayList<List<Object>>( stream.size() );
		for( List<AttributeImpl> attrs : stream ) {
			List<Object> elements = new ArrayList<Object>( attrs.size() );
			for(AttributeImpl attr : attrs) {
				elements.add( buildAttributeImpl( attr ) );
			}
			value.add(elements);
		}
		field.put( "value", value );
		field.put( "termVector", context.getTermVector() );
		fieldables.add( field );
	}

	private Object buildAttributeImpl(AttributeImpl attr) {
		if ( attr instanceof AnalysisRequestHandlerBase.TokenTrackingAttributeImpl ) {
			GenericRecord record = new GenericData.Record( protocol.getType( "TokenTrackingAttribute" ) );
			int[] positions = ( (AnalysisRequestHandlerBase.TokenTrackingAttributeImpl) attr ).getPositions();
			List<Integer> fullPositions = new ArrayList<Integer>( positions.length );
			for (int position : positions) {
				fullPositions.add( position );
			}
			record.put( "positions", fullPositions );
			return record;
		}
		else if (attr instanceof CharTermAttributeImpl) {
			GenericRecord record = new GenericData.Record( protocol.getType( "CharTermAttribute" ) );
			CharTermAttribute charAttr = (CharTermAttribute) attr;
			record.put("sequence", charAttr.toString() );
			return record;
		}
		else if (attr instanceof PayloadAttribute) {
			GenericRecord record = new GenericData.Record( protocol.getType( "PayloadAttribute" ) );
			PayloadAttribute payloadAttr = (PayloadAttribute) attr;
			record.put("payload", ByteBuffer.wrap( payloadAttr.getPayload().toByteArray() ) );
			return record;
		}
		else if (attr instanceof KeywordAttribute) {
			GenericRecord record = new GenericData.Record( protocol.getType( "KeywordAttribute" ) );
			KeywordAttribute narrowedAttr = (KeywordAttribute) attr;
			record.put("isKeyword", narrowedAttr.isKeyword() );
			return record;
		}
		else if (attr instanceof PositionIncrementAttribute ) {
			GenericRecord record = new GenericData.Record( protocol.getType( "PositionIncrementAttribute" ) );
			PositionIncrementAttribute narrowedAttr = (PositionIncrementAttribute) attr;
			record.put("positionIncrement", narrowedAttr.getPositionIncrement() );
			return record;
		}
		else if (attr instanceof FlagsAttribute ) {
			GenericRecord record = new GenericData.Record( protocol.getType( "FlagsAttribute" ) );
			FlagsAttribute narrowedAttr = (FlagsAttribute) attr;
			record.put("flags", narrowedAttr.getFlags() );
			return record;
		}
		else if (attr instanceof TypeAttribute ) {
			GenericRecord record = new GenericData.Record( protocol.getType( "TypeAttribute" ) );
			TypeAttribute narrowedAttr = (TypeAttribute) attr;
			record.put("type", narrowedAttr.type() );
			return record;
		}
		else if (attr instanceof OffsetAttribute ) {
			GenericRecord record = new GenericData.Record( protocol.getType( "OffsetAttribute" ) );
			OffsetAttribute narrowedAttr = (OffsetAttribute) attr;
			record.put("startOffset", narrowedAttr.startOffset() );
			record.put("endOffset", narrowedAttr.endOffset() );
			return record;
		}
		else if (attr instanceof Serializable) {
			return ByteBuffer.wrap( toByteArray(attr) );
		}
		else {
			throw log.attributeNotRecognizedNorSerializable( attr.getClass() );
		}
	}

	@Override
	public void addFieldWithSerializableReaderData(LuceneFieldContext context) {
		GenericRecord field = createNormalField( "ReaderField", context );
		field.put( "value", ByteBuffer.wrap( context.getReaderValue() ) );
		field.put( "termVector", context.getTermVector() );
		fieldables.add( field );
	}

	@Override
	public void addFieldWithSerializableFieldable(byte[] fieldable) {
		GenericRecord customFieldable = new GenericData.Record( protocol.getType( "CustomFieldable" ) );
		customFieldable.put( "instance", ByteBuffer.wrap( fieldable ) );
		fieldables.add( customFieldable );
	}

	@Override
	public void addDocument(float boost) {
		document = new GenericData.Record( protocol.getType( "Document" ) );
		document.put( "boost", boost );
		document.put( "fieldables", fieldables );
	}

	private void clearDocument() {
		document = null;
		fieldables = null;
	}
}
