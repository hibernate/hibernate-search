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
package org.hibernate.search.indexes.serialization.spi;

import java.io.Reader;
import java.io.Serializable;

import org.apache.lucene.document.Field;

import org.hibernate.annotations.common.AssertionFailure;
import org.hibernate.search.SearchException;
import org.hibernate.search.indexes.serialization.impl.CopyTokenStream;
import org.hibernate.search.indexes.serialization.impl.SerializationHelper;

/**
 * @author Emmanuel Bernard <emmanuel@hibernate.org>
 */
public class LuceneFieldContext {
	private Field field;

	public LuceneFieldContext(Field field) {
		this.field = field;
	}

	public String getName() {
		return field.name();
	}

	public SerializableStore getStore() {
		return field.isStored() ? SerializableStore.YES : SerializableStore.NO;
	}

	public SerializableIndex getIndex() {
		Field.Index index = Field.Index.toIndex( field.isIndexed(), field.isTokenized(), field.getOmitNorms() );
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
		Field.TermVector vector = Field.TermVector.toTermVector( field.isTermVectorStored(), field.isStoreOffsetWithTermVector(), field.isStorePositionWithTermVector() );
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

	public float getBoost() {
		return field.getBoost();
	}

	public boolean isOmitNorms() {
		return field.getOmitNorms();
	}

	public boolean isOmitTermFreqAndPositions() {
		return field.getOmitTermFreqAndPositions();
	}

	public String getStringValue() {
		return field.stringValue();
	}

	public byte[] getReaderValue() {
		Reader reader = field.readerValue();
		if (reader instanceof Serializable) {
			return SerializationHelper.toByteArray( (Serializable) reader );
		}
		else {
			throw new AssertionFailure( "Should not call getReaderValue for a non Serializable Reader" );
		}
	}

	public SerializableTokenStream getTokenStream() {
		return CopyTokenStream.buildSerializabletokenStream( field.tokenStreamValue() );
	}

	public byte[] getBinaryValue() {
		return field.getBinaryValue();
	}

	public int getBinaryOffset() {
		return field.getBinaryOffset();
	}

	public int getBinaryLength() {
		return field.getBinaryLength();
	}
}
