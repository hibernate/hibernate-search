/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.metadata.impl;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Field;
import org.hibernate.search.exception.SearchException;
import org.hibernate.search.annotations.Analyze;
import org.hibernate.search.annotations.Index;
import org.hibernate.search.annotations.Norms;
import org.hibernate.search.annotations.Store;
import org.hibernate.search.annotations.TermVector;
import org.hibernate.search.bridge.FieldBridge;
import org.hibernate.search.engine.metadata.impl.DocumentFieldMetadata;
import org.hibernate.search.metadata.FieldDescriptor;
import org.hibernate.search.metadata.FieldSettingsDescriptor;
import org.hibernate.search.metadata.NumericFieldSettingsDescriptor;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;

/**
 * @author Hardy Ferentschik
 */
public class FieldDescriptorImpl implements FieldDescriptor {
	private static final Log log = LoggerFactory.make();

	private final String name;
	private final Index index;
	private final Analyze analyze;
	private final Store store;
	private final TermVector termVector;
	private final Norms norms;
	private final float boost;
	private final String indexNullAs;
	private final Analyzer analyzer;
	private final FieldBridge fieldBridge;
	private final Type fieldType;

	public FieldDescriptorImpl(DocumentFieldMetadata documentFieldMetadata) {
		this.name = documentFieldMetadata.getName();
		this.index = determineIndexType( documentFieldMetadata.getIndex() );
		this.analyze = determineAnalyzeType( documentFieldMetadata.getIndex() );
		this.store = documentFieldMetadata.getStore();
		this.termVector = determineTermVectorType( documentFieldMetadata.getTermVector() );
		this.norms = determineNormsType( documentFieldMetadata.getIndex() );
		this.boost = documentFieldMetadata.getBoost();
		this.indexNullAs = documentFieldMetadata.indexNullAs();
		this.analyzer = documentFieldMetadata.getAnalyzer();
		this.fieldBridge = documentFieldMetadata.getFieldBridge();
		this.fieldType = determineFieldType( documentFieldMetadata );
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public Index getIndex() {
		return index;
	}

	@Override
	public Analyze getAnalyze() {
		return analyze;
	}

	@Override
	public Store getStore() {
		return store;
	}

	@Override
	public TermVector getTermVector() {
		return termVector;
	}

	@Override
	public Norms getNorms() {
		return norms;
	}

	@Override
	public float getBoost() {
		return boost;
	}

	@Override
	public Type getType() {
		return fieldType;
	}

	@Override
	public <T extends FieldSettingsDescriptor> T as(Class<T> type) {
		if ( fieldType == Type.NUMERIC && type == NumericFieldSettingsDescriptor.class ) {
			return type.cast( this );
		}

		throw log.getUnableToNarrowFieldDescriptorException(
				this.getClass().getName(),
				fieldType.toString(),
				type == null ? "null" : type.getName()
		);
	}

	@Override
	public String indexNullAs() {
		return indexNullAs;
	}

	@Override
	public boolean indexNull() {
		return indexNullAs != null;
	}

	@Override
	public FieldBridge getFieldBridge() {
		return fieldBridge;
	}

	@Override
	public Analyzer getAnalyzer() {
		return analyzer;
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder( "FieldDescriptorImpl{" );
		sb.append( "name='" ).append( name ).append( '\'' );
		sb.append( ", index=" ).append( index );
		sb.append( ", analyze=" ).append( analyze );
		sb.append( ", store=" ).append( store );
		sb.append( ", termVector=" ).append( termVector );
		sb.append( ", norms=" ).append( norms );
		sb.append( ", boost=" ).append( boost );
		sb.append( ", indexNullAs='" ).append( indexNullAs ).append( '\'' );
		sb.append( ", analyzer=" ).append( analyzer );
		sb.append( ", fieldBridge=" ).append( fieldBridge );
		sb.append( ", fieldType=" ).append( fieldType );
		sb.append( '}' );
		return sb.toString();
	}

	private Type determineFieldType(DocumentFieldMetadata documentFieldMetadata) {
		if ( documentFieldMetadata.isNumeric() ) {
			return Type.NUMERIC;
		}
		else if ( documentFieldMetadata.isSpatial() ) {
			return Type.SPATIAL;
		}
		else {
			return Type.BASIC;
		}
	}

	private Index determineIndexType(Field.Index index) {
		if ( Field.Index.NO.equals( index ) ) {
			return Index.NO;
		}
		else {
			return Index.YES;
		}
	}

	private Analyze determineAnalyzeType(Field.Index index) {
		if ( Field.Index.ANALYZED.equals( index ) || Field.Index.ANALYZED_NO_NORMS.equals( index ) ) {
			return Analyze.YES;
		}
		else {
			return Analyze.NO;
		}
	}

	private Norms determineNormsType(Field.Index index) {
		if ( Field.Index.ANALYZED.equals( index ) || Field.Index.NOT_ANALYZED.equals( index ) ) {
			return Norms.YES;
		}
		else {
			return Norms.NO;
		}
	}

	private TermVector determineTermVectorType(Field.TermVector termVector) {
		switch ( termVector ) {
			case NO: {
				return TermVector.NO;
			}
			case YES: {
				return TermVector.YES;
			}
			case WITH_POSITIONS: {
				return TermVector.WITH_POSITIONS;
			}
			case WITH_OFFSETS: {
				return TermVector.WITH_OFFSETS;
			}
			case WITH_POSITIONS_OFFSETS: {
				return TermVector.WITH_POSITION_OFFSETS;
			}
			default: {
				throw new SearchException( "Unknown term vector type" );
			}
		}
	}
}


