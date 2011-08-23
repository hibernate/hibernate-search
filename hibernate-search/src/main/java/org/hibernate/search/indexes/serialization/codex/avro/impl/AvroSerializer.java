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
package org.hibernate.search.indexes.serialization.codex.avro.impl;


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
import org.apache.lucene.document.Fieldable;
import org.apache.lucene.util.AttributeImpl;
import org.apache.solr.handler.AnalysisRequestHandlerBase;

import org.hibernate.search.SearchException;
import org.hibernate.search.backend.LuceneWork;
import org.hibernate.search.indexes.serialization.codex.impl.SerializationHelper;
import org.hibernate.search.indexes.serialization.codex.spi.Serializer;
import org.hibernate.search.indexes.serialization.operations.impl.LuceneFieldContext;
import org.hibernate.search.indexes.serialization.operations.impl.LuceneNumericFieldContext;
import org.hibernate.search.indexes.serialization.operations.impl.SerializableTermVector;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;

import static org.hibernate.search.indexes.serialization.codex.impl.SerializationHelper.toByteArray;

/**
 * @author Emmanuel Bernard <emmanuel@hibernate.org>
 */
public class AvroSerializer implements Serializer {
	private static final Log log = LoggerFactory.make();

	private List<GenericRecord> fieldables;
	private List<GenericRecord> operations;
	private GenericRecord document;
	private final Protocol protocol;

	public AvroSerializer(Protocol protocol) {
		this.protocol = protocol;
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
		GenericRecord purgeAll = new GenericData.Record( protocol.getType( "PurgeAll" ) );
		purgeAll.put( "class", entityClassName );
		operations.add( purgeAll );
	}

	@Override
	public void addDelete(String entityClassName, byte[] id) {
		GenericRecord delete = new GenericData.Record( protocol.getType( "Delete" ) );
		delete.put( "class", entityClassName );
		delete.put( "id", ByteBuffer.wrap( id ) );
		operations.add( delete );
	}

	@Override
	public void addAdd(String entityClassName, byte[] id, Map<String, String> fieldToAnalyzerMap) {
		GenericRecord add = new GenericData.Record( protocol.getType( "Add" ) );
		add.put( "class", entityClassName );
		add.put( "id", ByteBuffer.wrap( id ) );
		add.put( "document", document );
		add.put( "fieldToAnalyzerMap", fieldToAnalyzerMap );
		operations.add( add );
		clearDocument();
	}

	@Override
	public void addUpdate(String entityClassName, byte[] id, Map<String, String> fieldToAnalyzerMap) {
		GenericRecord update = new GenericData.Record( protocol.getType( "Update" ) );
		update.put( "class", entityClassName );
		update.put( "id", ByteBuffer.wrap( id ) );
		update.put( "document", document );
		update.put( "fieldToAnalyzerMap", fieldToAnalyzerMap );
		operations.add( update );
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
		message.put( "operations", operations );
		operations = null;
		try {
			writer.write( message, encoder );
			encoder.flush();
		}
		catch ( IOException e ) {
			throw new SearchException( "Unable to serialize message with Avro", e );
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
