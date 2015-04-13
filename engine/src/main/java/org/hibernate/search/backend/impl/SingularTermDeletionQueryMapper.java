/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.impl;

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
import org.hibernate.search.backend.DeletionQuery;
import org.hibernate.search.backend.SingularTermDeletionQuery;
import org.hibernate.search.backend.SingularTermDeletionQuery.Type;
import org.hibernate.search.exception.AssertionFailure;
import org.hibernate.search.util.impl.ScopedAnalyzer;


/**
 * @author Martin Braun
 */
public class SingularTermDeletionQueryMapper implements DeletionQueryMapper {

	@Override
	public Query toLuceneQuery(DeletionQuery deletionQuery, ScopedAnalyzer analyzerForEntity) {
		SingularTermDeletionQuery query = (SingularTermDeletionQuery) deletionQuery;
		if ( query.getType() == Type.STRING ) {
			try {
				TokenStream tokenStream = analyzerForEntity.tokenStream( query.getFieldName(), (String) query.getValue() );
				tokenStream.reset();
				try {
					BooleanQuery booleanQuery = new BooleanQuery();
					while ( tokenStream.incrementToken() ) {
						String value = tokenStream.getAttribute( CharTermAttribute.class ).toString();
						booleanQuery.add( new TermQuery( new Term( query.getFieldName(), value ) ), Occur.MUST );
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
			Type type = query.getType();
			BytesRef valueAsBytes;
			switch ( type ) {
				case INT:
				case FLOAT: {
					int value;
					if ( type == Type.FLOAT ) {
						value = NumericUtils.floatToSortableInt( (Float) query.getValue() );
					}
					else {
						value = (Integer) query.getValue();
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
						value = NumericUtils.doubleToSortableLong( (Double) query.getValue() );
					}
					else {
						value = (Long) query.getValue();
					}
					BytesRefBuilder builder = new BytesRefBuilder();
					NumericUtils.longToPrefixCoded( value, 0, builder );
					valueAsBytes = builder.get();
					break;
				}
				default:
					throw new AssertionFailure( "has to be a Numeric Type at this point!" );
			}
			return new TermQuery( new Term( query.getFieldName(), valueAsBytes ) );
		}
	}

	@Override
	public String[] toString(DeletionQuery deletionQuery) {
		SingularTermDeletionQuery query = (SingularTermDeletionQuery) deletionQuery;
		return new String[] { query.getType().toString(), query.getFieldName(), String.valueOf( query.getValue() ) };
	}

	@Override
	public DeletionQuery fromString(String[] string) {
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

}
