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
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.BytesRefBuilder;
import org.apache.lucene.util.NumericUtils;
import org.hibernate.search.exception.AssertionFailure;
import org.hibernate.search.util.impl.ScopedAnalyzer;

/**
 * DeleteByQuery equivalent to {@link org.apache.lucene.search.TermQuery}
 *
 * @hsearch.experimental
 *
 * @author Martin Braun
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
	public Query toLuceneQuery(ScopedAnalyzer analyzerForEntity) {
		if ( this.getType() == Type.STRING ) {
			try {
				TokenStream tokenStream = analyzerForEntity.tokenStream( this.getFieldName(), (String) this.getValue() );
				tokenStream.reset();
				try {
					BooleanQuery booleanQuery = new BooleanQuery();
					while ( tokenStream.incrementToken() ) {
						String value = tokenStream.getAttribute( CharTermAttribute.class ).toString();
						booleanQuery.add( new TermQuery( new Term( this.getFieldName(), value ) ), Occur.FILTER );
					}
					return booleanQuery;
				}
				finally {
					tokenStream.close();
				}
			}
			catch (IOException e) {
				throw new AssertionFailure( "no IOException can occur while using a TokenStream that is generated via String" );
			}
		}
		else {
			Type type = this.getType();
			BytesRef valueAsBytes;
			switch ( type ) {
				case INT:
				case FLOAT: {
					int value;
					if ( type == Type.FLOAT ) {
						value = NumericUtils.floatToSortableInt( (Float) this.getValue() );
					}
					else {
						value = (Integer) this.getValue();
					}
					BytesRefBuilder builder = new BytesRefBuilder();
					NumericUtils.intToPrefixCoded( value, 0, builder );
					valueAsBytes = builder.get();
					break;
				}
				case LONG:
				case DOUBLE: {
					long value;
					if ( type == Type.DOUBLE ) {
						value = NumericUtils.doubleToSortableLong( (Double) this.getValue() );
					}
					else {
						value = (Long) this.getValue();
					}
					BytesRefBuilder builder = new BytesRefBuilder();
					NumericUtils.longToPrefixCoded( value, 0, builder );
					valueAsBytes = builder.get();
					break;
				}
				default:
					throw new AssertionFailure( "has to be a Numeric Type at this point!" );
			}
			return new TermQuery( new Term( this.getFieldName(), valueAsBytes ) );
		}
	}

	@Override
	public String[] serialize() {
		return new String[] { this.getType().toString(), this.getFieldName(), String.valueOf( this.getValue() ) };
	}

	public static SingularTermDeletionQuery fromString(String[] string) {
		if ( string.length != 3 ) {
			throw new IllegalArgumentException( "for a TermQuery to work there have to be " + "exactly 3 Arguments (type & fieldName & value" );
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
				throw new AssertionFailure( "wrong Type!" );
		}
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ( ( fieldName == null ) ? 0 : fieldName.hashCode() );
		result = prime * result + ( ( type == null ) ? 0 : type.hashCode() );
		result = prime * result + ( ( value == null ) ? 0 : value.hashCode() );
		return result;
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
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
