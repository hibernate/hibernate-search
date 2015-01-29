/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.indexes.serialization.javaserialization.impl;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.CharTermAttributeImpl;
import org.apache.lucene.analysis.tokenattributes.FlagsAttribute;
import org.apache.lucene.analysis.tokenattributes.FlagsAttributeImpl;
import org.apache.lucene.analysis.tokenattributes.KeywordAttribute;
import org.apache.lucene.analysis.tokenattributes.KeywordAttributeImpl;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttributeImpl;
import org.apache.lucene.analysis.tokenattributes.PayloadAttribute;
import org.apache.lucene.analysis.tokenattributes.PayloadAttributeImpl;
import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;
import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttributeImpl;
import org.apache.lucene.analysis.tokenattributes.TypeAttribute;
import org.apache.lucene.analysis.tokenattributes.TypeAttributeImpl;
import org.apache.lucene.util.AttributeImpl;
import org.apache.lucene.util.BytesRef;

import org.hibernate.search.indexes.serialization.spi.LuceneFieldContext;
import org.hibernate.search.indexes.serialization.spi.SerializableTermVector;
import org.hibernate.search.indexes.serialization.spi.SerializableTokenStream;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;

/**
 * @author Emmanuel Bernard <emmanuel@hibernate.org>
 * @author Hardy Ferentschik
 */
public class SerializableTokenStreamField extends SerializableField {
	private static final Log log = LoggerFactory.make();

	List<List<SerializableAttribute>> serializableAttributes;
	private SerializableTermVector termVector;

	public SerializableTokenStreamField(LuceneFieldContext context) {
		super( context );
		SerializableTokenStream tokenStream = context.getTokenStream();

		this.serializableAttributes = new ArrayList<>( tokenStream.getStream().size() );
		for ( List<AttributeImpl> nativeAttributeList : tokenStream.getStream() ) {
			List<SerializableAttribute> serializableAttributeList = new ArrayList<>( nativeAttributeList.size() );
			for ( AttributeImpl nativeAttribute : nativeAttributeList ) {
				serializableAttributeList.add( buildSerializableAttribute( nativeAttribute ) );
			}
			serializableAttributes.add( serializableAttributeList );
		}
		this.termVector = context.getTermVector();
	}

	public SerializableTokenStream getValue() {
		List<List<AttributeImpl>> luceneAttributeList = new ArrayList<>( serializableAttributes.size() );
		for ( List<SerializableAttribute> serializableAttributesList : serializableAttributes ) {
			List<AttributeImpl> attributeList = new ArrayList<>( serializableAttributesList.size() );
			for ( SerializableAttribute serializableAttribute : serializableAttributesList ) {
				attributeList.add( buildLuceneAttribute( serializableAttribute ) );
			}
			luceneAttributeList.add( attributeList );
		}

		return new SerializableTokenStream( luceneAttributeList );
	}

	public SerializableTermVector getTermVector() {
		return termVector;
	}

	private AttributeImpl buildLuceneAttribute(SerializableAttribute serializableAttribute) {
		if ( serializableAttribute instanceof SerializableCharTermAttribute ) {
			SerializableCharTermAttribute serializableCharTermAttribute = (SerializableCharTermAttribute) serializableAttribute;

			CharTermAttributeImpl charTermAttribute = new CharTermAttributeImpl();
			charTermAttribute.copyBuffer(
					serializableCharTermAttribute.getCharSequence(), 0,
					serializableCharTermAttribute.getCharSequence().length
			);
			return charTermAttribute;
		}
		else if ( serializableAttribute instanceof SerializablePayloadAttribute ) {
			SerializablePayloadAttribute serializablePayloadAttribute = (SerializablePayloadAttribute) serializableAttribute;

			BytesRef bytesRef = new BytesRef(
					serializablePayloadAttribute.getBytes(), serializablePayloadAttribute.getOffset(),
					serializablePayloadAttribute.getLength()
			);
			return new PayloadAttributeImpl( bytesRef );
		}
		else if ( serializableAttribute instanceof SerializableKeywordAttribute ) {
			SerializableKeywordAttribute serializableKeywordAttribute = (SerializableKeywordAttribute) serializableAttribute;

			KeywordAttributeImpl keywordAttribute = new KeywordAttributeImpl();
			keywordAttribute.setKeyword( serializableKeywordAttribute.isKeyword() );
			return keywordAttribute;
		}
		else if ( serializableAttribute instanceof SerializablePositionIncrementAttribute ) {
			SerializablePositionIncrementAttribute serializablePositionIncrementAttribute = (SerializablePositionIncrementAttribute) serializableAttribute;

			PositionIncrementAttributeImpl positionIncrementAttribute = new PositionIncrementAttributeImpl();
			positionIncrementAttribute.setPositionIncrement(
					serializablePositionIncrementAttribute.getPositionIncrement()
			);
			return positionIncrementAttribute;
		}
		else if ( serializableAttribute instanceof SerializableFlagsAttribute ) {
			SerializableFlagsAttribute serializableFlagsAttribute = (SerializableFlagsAttribute) serializableAttribute;

			FlagsAttributeImpl flagsAttribute = new FlagsAttributeImpl();
			flagsAttribute.setFlags( serializableFlagsAttribute.getFlag() );
			return flagsAttribute;
		}
		else if ( serializableAttribute instanceof SerializableTypeAttribute ) {
			SerializableTypeAttribute serializableTypeAttribute = (SerializableTypeAttribute) serializableAttribute;

			TypeAttributeImpl typeAttribute = new TypeAttributeImpl();
			typeAttribute.setType( serializableTypeAttribute.getType() );
			return typeAttribute;
		}
		else if ( serializableAttribute instanceof SerializableOffsetAttribute ) {
			SerializableOffsetAttribute serializableOffsetAttribute = (SerializableOffsetAttribute) serializableAttribute;

			OffsetAttributeImpl offsetAttribute = new OffsetAttributeImpl();
			offsetAttribute.setOffset(
					serializableOffsetAttribute.getStartOffset(), serializableOffsetAttribute.getEndOffset()
			);
			return offsetAttribute;
		}
		else {
			// TODO HSEARCH-809 Right exception!?
			throw log.attributeNotRecognizedNorSerializable( serializableAttribute.getClass() );
		}
	}

	private SerializableAttribute buildSerializableAttribute(AttributeImpl attribute) {
		if ( attribute instanceof CharTermAttributeImpl ) {
			CharTermAttribute charTermAttribute = (CharTermAttribute) attribute;

			CharTermAttributeImpl charTermAttributeImpl = new CharTermAttributeImpl();
			charTermAttributeImpl.copyBuffer( charTermAttribute.buffer(), 0, charTermAttribute.buffer().length );
			return new SerializableCharTermAttribute( charTermAttribute.buffer() );

		}
		else if ( attribute instanceof PayloadAttribute ) {
			PayloadAttribute payloadAttribute = (PayloadAttribute) attribute;
			BytesRef payload = payloadAttribute.getPayload();

			return new SerializablePayloadAttribute( payload.bytes, payload.offset, payload.length );
		}
		else if ( attribute instanceof KeywordAttribute ) {
			KeywordAttribute keywordAttribute = (KeywordAttribute) attribute;

			return new SerializableKeywordAttribute( keywordAttribute.isKeyword() );
		}
		else if ( attribute instanceof PositionIncrementAttribute ) {
			PositionIncrementAttribute positionIncrementAttribute = (PositionIncrementAttribute) attribute;

			return new SerializablePositionIncrementAttribute( positionIncrementAttribute.getPositionIncrement() );
		}
		else if ( attribute instanceof FlagsAttribute ) {
			FlagsAttribute flagsAttribute = (FlagsAttribute) attribute;

			return new SerializableFlagsAttribute( flagsAttribute.getFlags() );
		}
		else if ( attribute instanceof TypeAttribute ) {
			TypeAttribute typeAttribute = (TypeAttribute) attribute;

			return new SerializableTypeAttribute( typeAttribute.type() );
		}
		else if ( attribute instanceof OffsetAttribute ) {
			OffsetAttribute offsetAttribute = (OffsetAttribute) attribute;

			return new SerializableOffsetAttribute( offsetAttribute.startOffset(), offsetAttribute.endOffset() );
		}
		else {
			throw log.attributeNotRecognizedNorSerializable( attribute.getClass() );
		}
	}

	public static class SerializableAttribute implements Serializable {
	}

	public static class SerializableCharTermAttribute extends SerializableAttribute {
		private final char[] charSequence;

		public SerializableCharTermAttribute(char[] charSequence) {
			this.charSequence = charSequence;
		}

		public char[] getCharSequence() {
			return charSequence;
		}
	}

	private static class SerializablePayloadAttribute extends SerializableAttribute {
		private final byte[] bytes;
		private final int offset;
		private final int length;

		public SerializablePayloadAttribute(byte[] bytes, int offset, int length) {
			this.bytes = bytes;
			this.offset = offset;
			this.length = length;
		}

		public byte[] getBytes() {
			return bytes;
		}

		public int getOffset() {
			return offset;
		}

		public int getLength() {
			return length;
		}
	}

	private static class SerializableKeywordAttribute extends SerializableAttribute {
		private final boolean keyword;

		public SerializableKeywordAttribute(boolean keyword) {
			this.keyword = keyword;
		}

		public boolean isKeyword() {
			return keyword;
		}
	}

	private static class SerializablePositionIncrementAttribute extends SerializableAttribute {
		private final int positionIncrement;

		public SerializablePositionIncrementAttribute(int positionIncrement) {
			this.positionIncrement = positionIncrement;
		}

		public int getPositionIncrement() {
			return positionIncrement;
		}
	}

	private static class SerializableFlagsAttribute extends SerializableAttribute {
		private final int flag;

		public SerializableFlagsAttribute(int flag) {
			this.flag = flag;
		}

		public int getFlag() {
			return flag;
		}
	}

	private static class SerializableTypeAttribute extends SerializableAttribute {
		private final String type;

		public SerializableTypeAttribute(String type) {
			this.type = type;
		}

		public String getType() {
			return type;
		}
	}

	private static class SerializableOffsetAttribute extends SerializableAttribute {
		private final int startOffset;
		private final int endOffset;

		public SerializableOffsetAttribute(int startOffset, int endOffset) {
			this.startOffset = startOffset;
			this.endOffset = endOffset;
		}

		public int getStartOffset() {
			return startOffset;
		}

		public int getEndOffset() {
			return endOffset;
		}
	}
}
