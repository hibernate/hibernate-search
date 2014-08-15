/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.indexes.serialization.javaserialization.impl;

import java.io.Serializable;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.lucene.index.IndexableField;
import org.hibernate.search.backend.LuceneWork;
import org.hibernate.search.indexes.serialization.impl.SerializationHelper;
import org.hibernate.search.indexes.serialization.spi.LuceneFieldContext;
import org.hibernate.search.indexes.serialization.spi.LuceneNumericFieldContext;
import org.hibernate.search.indexes.serialization.spi.Serializer;

/**
 * @author Emmanuel Bernard <emmanuel@hibernate.org>
 */
public class NativeJavaSerializer implements Serializer {
	private Serializable id;
	private Set<Operation> ops;
	private Set<SerializableFieldable> serialFields;
	private SerializableDocument currentDocument;

	@Override
	public void luceneWorks(List<LuceneWork> works) {
		ops = new HashSet<Operation>( works.size() );
	}

	@Override
	public void addOptimizeAll() {
		ops.add( new OptimizeAll() );
	}

	@Override
	public void addFlush() {
		ops.add( new Flush() );
	}

	@Override
	public void addPurgeAll(String entityClassName) {
		ops.add( new PurgeAll( entityClassName ) );
	}

	@Override
	public void addIdSerializedInJava(byte[] id) {
		this.id = id;
	}

	@Override
	public void addIdAsInteger(int id) {
		this.id = id;
	}

	@Override
	public void addIdAsLong(long id) {
		this.id = id;
	}

	@Override
	public void addIdAsFloat(float id) {
		this.id = id;
	}

	@Override
	public void addIdAsDouble(double id) {
		this.id = id;
	}

	@Override
	public void addIdAsString(String id) {
		this.id = id;
	}

	@Override
	public void addDelete(String entityClassName) {
		ops.add( new Delete( entityClassName, id ) );
	}

	@Override
	public void addAdd(String entityClassName, Map<String, String> fieldToAnalyzerMap) {
		ops.add( new Add( entityClassName, id, currentDocument, fieldToAnalyzerMap ) );
		clearDocument();
	}

	@Override
	public void addUpdate(String entityClassName, Map<String, String> fieldToAnalyzerMap) {
		ops.add( new Update( entityClassName, id, currentDocument, fieldToAnalyzerMap ) );
		clearDocument();
	}

	@Override
	public byte[] serialize() {
		Message message = new Message( ops );
		return SerializationHelper.toByteArray( message );
	}

	@Override
	public void fields(List<IndexableField> fields) {
		serialFields = new HashSet<SerializableFieldable>( fields.size() );
	}

	@Override
	public void addIntNumericField(int value, LuceneNumericFieldContext context) {
		serialFields.add( new SerializableIntField( value, context ) );
	}

	@Override
	public void addLongNumericField(long value, LuceneNumericFieldContext context) {
		serialFields.add( new SerializableLongField( value, context ) );
	}

	@Override
	public void addFloatNumericField(float value, LuceneNumericFieldContext context) {
		serialFields.add( new SerializableFloatField( value, context ) );
	}

	@Override
	public void addDoubleNumericField(double value, LuceneNumericFieldContext context) {
		serialFields.add( new SerializableDoubleField( value, context ) );
	}

	@Override
	public void addFieldWithBinaryData(LuceneFieldContext luceneFieldContext) {
		serialFields.add( new SerializableBinaryField( luceneFieldContext ) );
	}

	@Override
	public void addFieldWithStringData(LuceneFieldContext luceneFieldContext) {
		serialFields.add( new SerializableStringField( luceneFieldContext ) );
	}

	@Override
	public void addFieldWithTokenStreamData(LuceneFieldContext luceneFieldContext) {
		serialFields.add( new SerializableTokenStreamField( luceneFieldContext ) );
	}

	@Override
	public void addFieldWithSerializableReaderData(LuceneFieldContext luceneFieldContext) {
		serialFields.add( new SerializableReaderField( luceneFieldContext ) );
	}

	@Override
	public void addFieldWithSerializableFieldable(byte[] fieldable) {
		serialFields.add( new SerializableCustomFieldable( fieldable ) );
	}

	@Override
	public void addDocument() {
		currentDocument = new SerializableDocument( serialFields );
	}

	private void clearDocument() {
		currentDocument = null;
		serialFields = null;
	}

}
