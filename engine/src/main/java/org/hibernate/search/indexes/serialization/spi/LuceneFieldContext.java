/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.indexes.serialization.spi;

import java.io.Reader;
import java.io.Serializable;

import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.index.DocValuesType;
import org.apache.lucene.index.IndexOptions;
import org.apache.lucene.util.BytesRef;
import org.hibernate.search.exception.AssertionFailure;
import org.hibernate.search.exception.SearchException;
import org.hibernate.search.indexes.serialization.impl.CopyTokenStream;
import org.hibernate.search.indexes.serialization.impl.SerializationHelper;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;

/**
 * @author Emmanuel Bernard &lt;emmanuel@hibernate.org&gt;
 */
public final class LuceneFieldContext {
	private static Log log = LoggerFactory.make();

	private final Field field;
	private final FieldType fieldType;

	public LuceneFieldContext(Field field) {
		this.field = field;
		fieldType = field.fieldType();
	}

	public String getName() {
		return field.name();
	}

	public SerializableStore getStore() {
		return fieldType.stored() ? SerializableStore.YES : SerializableStore.NO;
	}

	public SerializableIndex getIndex() {
		Field.Index index = Field.Index.toIndex( fieldType.indexOptions() != IndexOptions.NONE , fieldType.tokenized(), fieldType.omitNorms() );
		switch ( index ) {
			case ANALYZED:
				return SerializableIndex.ANALYZED;
			case ANALYZED_NO_NORMS:
				return SerializableIndex.ANALYZED_NO_NORMS;
			case NO:
				return SerializableIndex.NO;
			case NOT_ANALYZED:
				return SerializableIndex.NOT_ANALYZED;
			case NOT_ANALYZED_NO_NORMS:
				return SerializableIndex.NOT_ANALYZED_NO_NORMS;
			default:
				throw new SearchException( "Unable to convert Field.Index value into serializable Index: " + index);
		}
	}

	public SerializableTermVector getTermVector() {
		Field.TermVector vector = Field.TermVector.toTermVector( fieldType.storeTermVectors(), fieldType.storeTermVectorOffsets(), fieldType.storeTermVectorPositions() );
		switch ( vector ) {
			case NO:
				return SerializableTermVector.NO;
			case WITH_OFFSETS:
				return SerializableTermVector.WITH_OFFSETS;
			case WITH_POSITIONS:
				return SerializableTermVector.WITH_POSITIONS;
			case WITH_POSITIONS_OFFSETS:
				return SerializableTermVector.WITH_POSITIONS_OFFSETS;
			case YES:
				return SerializableTermVector.YES;
			default:
				throw new SearchException( "Unable to convert Field.TermVector value into serializable TermVector: " + vector);
		}
	}

	public SerializableDocValuesType getDocValuesType() {
		DocValuesType docValuesType = field.fieldType().docValuesType();
		switch ( docValuesType ) {
			// data is a long value
			case NUMERIC: {
				return SerializableDocValuesType.NUMERIC;
			}
			case SORTED_NUMERIC: {
				return SerializableDocValuesType.SORTED_NUMERIC;
			}

			// data is ByteRef
			case BINARY: {
				return SerializableDocValuesType.BINARY;
			}
			case SORTED: {
				return SerializableDocValuesType.SORTED;
			}
			case SORTED_SET: {
				return SerializableDocValuesType.SORTED_SET;
			}
			default: {
				// in case Lucene is going to add more in coming releases
				throw log.unknownDocValuesTypeType( docValuesType.toString() );
			}
		}
	}

	public float getBoost() {
		return field.boost();
	}

	public boolean isOmitNorms() {
		return fieldType.omitNorms();
	}

	public boolean isOmitTermFreqAndPositions() {
		return fieldType.indexOptions() == IndexOptions.DOCS;
	}

	public String getStringValue() {
		return field.stringValue();
	}

	public byte[] getReaderValue() {
		Reader reader = field.readerValue();
		if ( reader instanceof Serializable ) {
			return SerializationHelper.toByteArray( (Serializable) reader );
		}
		else {
			throw new AssertionFailure( "Should not call getReaderValue for a non Serializable Reader" );
		}
	}

	public SerializableTokenStream getTokenStream() {
		return CopyTokenStream.buildSerializableTokenStream( field.tokenStreamValue() );
	}

	public BytesRef getBinaryValue() {
		return field.binaryValue();
	}

}
