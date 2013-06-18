/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2013, Red Hat, Inc. and/or its affiliates or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat, Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.search.engine.metadata.impl;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Field;
import org.hibernate.search.annotations.Store;
import org.hibernate.search.bridge.FieldBridge;

/**
 * Encapsulating the metadata for a single document field.
 *
 * @author Hardy Ferentschik
 */
public class DocumentFieldMetadata {
	private final String fieldName;
	private final Store store;
	private final Field.Index index;
	private final Field.TermVector termVector;
	private final FieldBridge fieldBridge;
	private final Float boost;
	private final Analyzer analyzer;

	private DocumentFieldMetadata(Builder builder) {
		this.fieldName = builder.fieldName;
		this.store = builder.store;
		this.index = builder.index;
		this.termVector = builder.termVector;
		this.fieldBridge = builder.fieldBridge;
		this.boost = builder.boost;
		this.analyzer = builder.analyzer;
	}

	public String getName() {
		return fieldName;
	}

	public Store getStore() {
		return store;
	}

	public Field.Index getIndex() {
		return index;
	}

	public Field.TermVector getTermVector() {
		return termVector;
	}

	public FieldBridge getFieldBridge() {
		return fieldBridge;
	}

	public Float getBoost() {
		return boost;
	}

	public Analyzer getAnalyzer() {
		return analyzer;
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder( "FieldMetadata{" );
		sb.append( "fieldName='" ).append( fieldName ).append( '\'' );
		sb.append( ", store=" ).append( store );
		sb.append( ", index=" ).append( index );
		sb.append( ", termVector=" ).append( termVector );
		sb.append( ", fieldBridge=" ).append( fieldBridge );
		sb.append( ", boost=" ).append( boost );
		sb.append( ", analyzer=" ).append( analyzer );
		sb.append( '}' );
		return sb.toString();
	}

	public static class Builder {
		// required parameters
		private final String fieldName;
		private final Store store;
		private final Field.Index index;
		private final Field.TermVector termVector;

		// optional parameters
		private FieldBridge fieldBridge;
		private Float boost;
		private Analyzer analyzer;

		public Builder(String fieldName,
			Store store,
			Field.Index index,
			Field.TermVector termVector) {

			this.fieldName = fieldName;
			this.store = store;
			this.index = index;
			this.termVector = termVector;
		}

		public Builder fieldBridge(FieldBridge fieldBridge) {
			this.fieldBridge = fieldBridge;
			return this;
		}

		public Builder boost(Float boost) {
			this.boost = boost;
			return this;
		}

		public Builder analyzer(Analyzer analyzer) {
			this.analyzer = analyzer;
			return this;
		}

		public DocumentFieldMetadata build() {
			return new DocumentFieldMetadata( this );
		}
	}
}


