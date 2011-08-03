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
package org.hibernate.search.remote.operations.impl;

import java.io.Reader;
import java.io.Serializable;

import org.apache.lucene.document.Field;

import org.hibernate.annotations.common.AssertionFailure;
import org.hibernate.search.SearchException;
import org.hibernate.search.remote.codex.impl.CopyTokenStream;

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

	public Store getStore() {
		return field.isStored() ? Store.YES : Store.NO;
	}

	public Index getIndex() {
		Field.Index index = Field.Index.toIndex( field.isIndexed(), field.isTokenized(), field.getOmitNorms() );
		switch ( index ) {
			case ANALYZED:
				return Index.ANALYZED;
			case ANALYZED_NO_NORMS:
				return Index.ANALYZED_NO_NORMS;
			case NO:
				return Index.NO;
			case NOT_ANALYZED:
				return Index.NOT_ANALYZED;
			case NOT_ANALYZED_NO_NORMS:
				return Index.NOT_ANALYZED_NO_NORMS;
			default:
				throw new SearchException( "Unable to convert Field.Index value into serializable Index: " + index);
		}
	}

	public TermVector getTermVector() {
		Field.TermVector vector = Field.TermVector.toTermVector( field.isStored(), field.isStoreOffsetWithTermVector(), field.isStorePositionWithTermVector() );
		switch ( vector ) {
			case NO:
				return TermVector.NO;
			case WITH_OFFSETS:
				return TermVector.WITH_OFFSETS;
			case WITH_POSITIONS:
				return TermVector.WITH_POSITIONS;
			case WITH_POSITIONS_OFFSETS:
				return TermVector.WITH_POSITIONS_OFFSETS;
			case YES:
				return TermVector.YES;
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

	public Reader getReaderValue() {
		Reader reader = field.readerValue();
		if (reader instanceof Serializable) {
			return reader;
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
