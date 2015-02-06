/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.util.AttributeImpl;
import org.apache.lucene.util.BytesRef;

import org.hibernate.search.backend.LuceneWork;
import org.hibernate.search.indexes.serialization.spi.LuceneFieldContext;
import org.hibernate.search.indexes.serialization.spi.LuceneNumericFieldContext;
import org.hibernate.search.indexes.serialization.spi.Serializer;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;

import static org.hibernate.search.indexes.serialization.impl.SerializationHelper.toByteArray;

/**
 * @author Emmanuel Bernard &lt;emmanuel@hibernate.org&gt;
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
		this.classReferences = new ArrayList<>();
	}

	@Override
	public void luceneWorks(List<LuceneWork> works) {
		operations = new ArrayList<>( works.size() );
	}

	@Override
	public void addOptimizeAll() {
		operations.add( new GenericData.Record( protocol.getType( "OptimizeAll" ) ) );
	}

	@Override
	public void addFlush() {
		operations.add( new GenericData.Record( protocol.getType( "Flush" ) ) );
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
		out.write( KnownProtocols.MAJOR_VERSION);
		out.write( KnownProtocols.LATEST_MINOR_VERSION );
		Schema msgSchema = protocol.getType( "Message" );
		GenericDatumWriter<GenericRecord> writer = new GenericDatumWriter<>( msgSchema );
		BinaryEncoder encoder = EncoderFactory.get().directBinaryEncoder( out, null );
		GenericRecord message = new GenericData.Record( msgSchema );
		message.put( "classReferences", classReferences );
		message.put( "operations", operations );
		operations = null;
		try {
			writer.write( message, encoder );
			encoder.flush();
		}
		catch (IOException e) {
			throw log.unableToSerializeInAvro( e );
		}
		return out.toByteArray();
	}

	@Override
	public void fields(List<IndexableField> fields) {
		fieldables = new ArrayList<>( fields.size() );
	}

	@Override
	public void addIntNumericField(int value, LuceneNumericFieldContext context) {
		GenericRecord numericField = createNumericField( "NumericIntField", context );
		numericField.put( "value", value );
		fieldables.add( numericField );
	}

	private GenericRecord createNumericField(String schemaName, LuceneNumericFieldContext context) {
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
		GenericRecord numericField = createNumericField( "NumericLongField", context );
		numericField.put( "value", value );
		fieldables.add( numericField );
	}

	@Override
	public void addFloatNumericField(float value, LuceneNumericFieldContext context) {
		GenericRecord numericField = createNumericField( "NumericFloatField", context );
		numericField.put( "value", value );
		fieldables.add( numericField );
	}

	@Override
	public void addDoubleNumericField(double value, LuceneNumericFieldContext context) {
		GenericRecord numericField = createNumericField( "NumericDoubleField", context );
		numericField.put( "value", value );
		fieldables.add( numericField );
	}

	@Override
	public void addFieldWithBinaryData(LuceneFieldContext context) {
		GenericRecord field = createNormalField( "BinaryField", context );
		BytesRef binaryValue = context.getBinaryValue();
		field.put( "value", ByteBuffer.wrap( binaryValue.bytes, binaryValue.offset, binaryValue.length ) );
		//Following two attributes are meant for serialization format backwards compatibility:
		field.put( "offset", 0 );
		field.put( "length", binaryValue.length );
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
		List<List<Object>> value = new ArrayList<>( stream.size() );
		for ( List<AttributeImpl> attrs : stream ) {
			List<Object> elements = new ArrayList<>( attrs.size() );
			for ( AttributeImpl attr : attrs ) {
				elements.add( buildAttributeImpl( attr ) );
			}
			value.add( elements );
		}
		field.put( "value", value );
		field.put( "termVector", context.getTermVector() );
		fieldables.add( field );
	}

	private Object buildAttributeImpl(final AttributeImpl attr) {
		if ( attr instanceof CharTermAttributeImpl ) {
			GenericRecord record = new GenericData.Record( protocol.getType( "CharTermAttribute" ) );
			CharTermAttribute charAttr = (CharTermAttribute) attr;
			record.put( "sequence", charAttr.toString() );
			return record;
		}
		else if ( attr instanceof PayloadAttribute ) {
			GenericRecord record = new GenericData.Record( protocol.getType( "PayloadAttribute" ) );
			PayloadAttribute payloadAttr = (PayloadAttribute) attr;
			BytesRef payload = payloadAttr.getPayload();
			record.put( "payload", ByteBuffer.wrap( payload.bytes, payload.offset, payload.length ) );
			return record;
		}
		else if ( attr instanceof KeywordAttribute ) {
			GenericRecord record = new GenericData.Record( protocol.getType( "KeywordAttribute" ) );
			KeywordAttribute narrowedAttr = (KeywordAttribute) attr;
			record.put( "isKeyword", narrowedAttr.isKeyword() );
			return record;
		}
		else if ( attr instanceof PositionIncrementAttribute ) {
			GenericRecord record = new GenericData.Record( protocol.getType( "PositionIncrementAttribute" ) );
			PositionIncrementAttribute narrowedAttr = (PositionIncrementAttribute) attr;
			record.put( "positionIncrement", narrowedAttr.getPositionIncrement() );
			return record;
		}
		else if ( attr instanceof FlagsAttribute ) {
			GenericRecord record = new GenericData.Record( protocol.getType( "FlagsAttribute" ) );
			FlagsAttribute narrowedAttr = (FlagsAttribute) attr;
			record.put( "flags", narrowedAttr.getFlags() );
			return record;
		}
		else if ( attr instanceof TypeAttribute ) {
			GenericRecord record = new GenericData.Record( protocol.getType( "TypeAttribute" ) );
			TypeAttribute narrowedAttr = (TypeAttribute) attr;
			record.put( "type", narrowedAttr.type() );
			return record;
		}
		else if ( attr instanceof OffsetAttribute ) {
			GenericRecord record = new GenericData.Record( protocol.getType( "OffsetAttribute" ) );
			OffsetAttribute narrowedAttr = (OffsetAttribute) attr;
			record.put( "startOffset", narrowedAttr.startOffset() );
			record.put( "endOffset", narrowedAttr.endOffset() );
			return record;
		}
		else if ( attr instanceof Serializable ) {
			return ByteBuffer.wrap( toByteArray( (Serializable) attr ) );
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
	public void addDocValuesFieldWithBinaryValue(LuceneFieldContext context) {
		GenericRecord record = new GenericData.Record( protocol.getType( "BinaryDocValuesField" ) );
		record.put( "name", context.getName() );
		record.put( "type", context.getDocValuesType() );

		BytesRef binaryValue = context.getBinaryValue();
		record.put( "value", ByteBuffer.wrap( binaryValue.bytes, binaryValue.offset, binaryValue.length ) );
		record.put( "offset", 0 );
		record.put( "length", binaryValue.length );
		fieldables.add( record );
	}

	@Override
	public void addDocValuesFieldWithNumericValue(long value, LuceneFieldContext context) {
		GenericRecord record = new GenericData.Record( protocol.getType( "NumericDocValuesField" ) );
		record.put( "name", context.getName() );
		record.put( "type", context.getDocValuesType() );
		record.put( "value", value );
		fieldables.add( record );
	}

	@Override
	public void addDocument() {
		document = new GenericData.Record( protocol.getType( "Document" ) );
		//backwards compatibility: we used to have a boost here in Lucene 3 / Hibernate Search 4.x
		//With Lucene 3 there was a notion of "Document level boost" which was then dropped.
		//Using the constant 1f doesn't hurt as it would be multiplied by the field boost,
		//which in the new design incorporates the factor.
		document.put( "boost", 1f );
		document.put( "fieldables", fieldables );
	}

	private void clearDocument() {
		document = null;
		fieldables = null;
	}
}
