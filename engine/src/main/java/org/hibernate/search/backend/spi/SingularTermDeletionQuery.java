/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.spi;

import java.io.IOException;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.hibernate.search.analyzer.spi.AnalyzerReference;
import org.hibernate.search.analyzer.impl.LuceneAnalyzerReference;
import org.hibernate.search.analyzer.impl.RemoteAnalyzerReference;
import org.hibernate.search.analyzer.impl.ScopedLuceneAnalyzer;
import org.hibernate.search.bridge.FieldBridge;
import org.hibernate.search.bridge.util.impl.ContextualExceptionBridgeHelper;
import org.hibernate.search.bridge.util.impl.NumericFieldUtils;
import org.hibernate.search.engine.spi.DocumentBuilderIndexedEntity;
import org.hibernate.search.exception.AssertionFailure;

/**
 * DeleteByQuery equivalent to {@link org.apache.lucene.search.TermQuery}
 *
 * @hsearch.experimental
 *
 * @author Martin Braun
 * @author Guillaume Smet
 */
public final class SingularTermDeletionQuery implements DeletionQuery {

	public static final int QUERY_KEY = 0;

	private final String fieldName;
	private final Object value;
	private final Type type;

	public SingularTermDeletionQuery(String fieldName, String value) {
		this( fieldName, value, Type.STRING );
	}

	public SingularTermDeletionQuery(String fieldName, int value) {
		this( fieldName, value, Type.INT );
	}

	public SingularTermDeletionQuery(String fieldName, long value) {
		this( fieldName, value, Type.LONG );
	}

	public SingularTermDeletionQuery(String fieldName, float value) {
		this( fieldName, value, Type.FLOAT );
	}

	public SingularTermDeletionQuery(String fieldName, double value) {
		this( fieldName, value, Type.DOUBLE );
	}

	public SingularTermDeletionQuery(String fieldName, Object value, Type type) {
		this.fieldName = fieldName;
		this.value = value;
		this.type = type;
	}

	public String getFieldName() {
		return fieldName;
	}

	public Object getValue() {
		return value;
	}

	public Type getType() {
		return this.type;
	}

	@Override
	public int getQueryKey() {
		return QUERY_KEY;
	}

	@Override
	public String toString() {
		return "SingularTermQuery: +" + fieldName + ":" + value;
	}

	@Override
	public Query toLuceneQuery(DocumentBuilderIndexedEntity documentBuilder) {
		AnalyzerReference analyzerReferenceForEntity = documentBuilder.getAnalyzerReference();
		String stringValue = documentBuilder.objectToString( fieldName, this.getValue(), new ContextualExceptionBridgeHelper() );

		if ( this.getType() == Type.STRING ) {
			try {
				if ( analyzerReferenceForEntity.is( RemoteAnalyzerReference.class ) ) {
					// no need to take into account the analyzer here as it will be dealt with remotely
					return new TermQuery( new Term( this.getFieldName(), stringValue ) );
				}

				ScopedLuceneAnalyzer analyzerForEntity = (ScopedLuceneAnalyzer) analyzerReferenceForEntity.unwrap( LuceneAnalyzerReference.class ).getAnalyzer();
				TokenStream tokenStream = analyzerForEntity.tokenStream( this.getFieldName(), stringValue );
				tokenStream.reset();
				try {
					BooleanQuery.Builder booleanQueryBuilder = new BooleanQuery.Builder();
					while ( tokenStream.incrementToken() ) {
						String term = tokenStream.getAttribute( CharTermAttribute.class ).toString();
						booleanQueryBuilder.add(
								new TermQuery( new Term( this.getFieldName(), term ) ),
								Occur.FILTER
						);
					}
					return booleanQueryBuilder.build();
				}
				finally {
					tokenStream.close();
				}
			}
			catch (IOException e) {
				throw new AssertionFailure( "No IOException can occur while using a TokenStream that is generated via String" );
			}
		}
		else {
			FieldBridge fieldBridge = documentBuilder.getBridge( fieldName );
			if ( NumericFieldUtils.isNumericFieldBridge( fieldBridge ) ) {
				return NumericFieldUtils.createExactMatchQuery( fieldName, this.getValue() );
			}
			else {
				return new TermQuery( new Term( this.getFieldName(), stringValue ) );
			}
		}
	}

	@Override
	public String[] serialize() {
		return new String[] { this.getType().toString(), this.getFieldName(), String.valueOf( this.getValue() ) };
	}

	public static SingularTermDeletionQuery fromString(String[] string) {
		if ( string.length != 3 ) {
			throw new IllegalArgumentException( "To instantiate a SingularTermDeletionQuery, an array of size 3 is required"
					+ " (type, fieldName, value). Got an array of size " + string.length );
		}
		Type type = Type.valueOf( string[0] );
		switch ( type ) {
			case STRING:
				return new SingularTermDeletionQuery( string[1], string[2] );
			case INT:
				return new SingularTermDeletionQuery( string[1], Integer.parseInt( string[2] ) );
			case FLOAT:
				return new SingularTermDeletionQuery( string[1], Float.parseFloat( string[2] ) );
			case LONG:
				return new SingularTermDeletionQuery( string[1], Long.parseLong( string[2] ) );
			case DOUBLE:
				return new SingularTermDeletionQuery( string[1], Double.parseDouble( string[2] ) );
			default:
				throw new AssertionFailure( "Unsupported value type: " + type );
		}
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ( ( fieldName == null ) ? 0 : fieldName.hashCode() );
		result = prime * result + ( ( type == null ) ? 0 : type.hashCode() );
		result = prime * result + ( ( value == null ) ? 0 : value.hashCode() );
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if ( this == obj ) {
			return true;
		}
		if ( obj == null ) {
			return false;
		}
		if ( getClass() != obj.getClass() ) {
			return false;
		}
		SingularTermDeletionQuery other = (SingularTermDeletionQuery) obj;
		if ( fieldName == null ) {
			if ( other.fieldName != null ) {
				return false;
			}
		}
		else if ( !fieldName.equals( other.fieldName ) ) {
			return false;
		}
		if ( type != other.type ) {
			return false;
		}
		if ( value == null ) {
			if ( other.value != null ) {
				return false;
			}
		}
		else if ( !value.equals( other.value ) ) {
			return false;
		}
		return true;
	}

	public enum Type {
		STRING, INT, LONG, FLOAT, DOUBLE
	}

}
