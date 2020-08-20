/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.search.query.dsl.impl;

import java.util.HashMap;
import java.util.List;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.simple.SimpleQueryParser;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.FuzzyQuery;
import org.apache.lucene.search.PrefixQuery;
import org.apache.lucene.search.Query;

/**
 * A FieldsContext aware implementation of the SimpleQueryParser of Lucene.
 *
 * @author Guillaume Smet
 */
public class ConnectedSimpleQueryParser extends SimpleQueryParser {

	/** Fields context as defined by the DSL */
	protected final List<FieldsContext> fieldsContexts;

	/** Creates a new parser searching over multiple fields with different weights. */
	public ConnectedSimpleQueryParser(Analyzer analyzer, List<FieldsContext> fieldsContexts) {
		this( analyzer, fieldsContexts, -1 );
	}

	/** Creates a new parser with custom flags used to enable/disable certain features. */
	public ConnectedSimpleQueryParser(Analyzer analyzer, List<FieldsContext> fieldsContexts, int flags) {
		super( analyzer, new HashMap<String, Float>( 0 ), flags );
		this.fieldsContexts = fieldsContexts;
	}

	/**
	 * Factory method to generate a standard query (no phrase or prefix operators).
	 */
	@Override
	protected Query newDefaultQuery(String text) {
		BooleanQuery.Builder bqb = new BooleanQuery.Builder().setDisableCoord( true );
		for ( FieldsContext fieldsContext : fieldsContexts ) {
			for ( FieldContext fieldContext : fieldsContext ) {
				Query q = createBooleanQuery( fieldContext.getField(), text, getDefaultOperator() );
				if ( q != null ) {
					bqb.add( fieldContext.getFieldCustomizer().setWrappedQuery( q ).createQuery(), BooleanClause.Occur.SHOULD );
				}
			}
		}
		return simplify( bqb.build() );
	}

	/**
	 * Factory method to generate a fuzzy query.
	 */
	@Override
	protected Query newFuzzyQuery(String text, int fuzziness) {
		BooleanQuery.Builder bqb = new BooleanQuery.Builder().setDisableCoord( true );
		for ( FieldsContext fieldsContext : fieldsContexts ) {
			for ( FieldContext fieldContext : fieldsContext ) {
				Query q = new FuzzyQuery( new Term( fieldContext.getField(), text ), fuzziness );
				if ( q != null ) {
					bqb.add( fieldContext.getFieldCustomizer().setWrappedQuery( q ).createQuery(), BooleanClause.Occur.SHOULD );
				}
			}
		}
		return simplify( bqb.build() );
	}

	/**
	 * Factory method to generate a phrase query with slop.
	 */
	@Override
	protected Query newPhraseQuery(String text, int slop) {
		BooleanQuery.Builder bqb = new BooleanQuery.Builder().setDisableCoord( true );
		for ( FieldsContext fieldsContext : fieldsContexts ) {
			for ( FieldContext fieldContext : fieldsContext ) {
				Query q = createPhraseQuery( fieldContext.getField(), text, slop );
				if ( q != null ) {
					bqb.add( fieldContext.getFieldCustomizer().setWrappedQuery( q ).createQuery(), BooleanClause.Occur.SHOULD );
				}
			}
		}
		return simplify( bqb.build() );
	}

	/**
	 * Factory method to generate a prefix query.
	 */
	@Override
	protected Query newPrefixQuery(String text) {
		BooleanQuery.Builder bqb = new BooleanQuery.Builder().setDisableCoord( true );
		for ( FieldsContext fieldsContext : fieldsContexts ) {
			for ( FieldContext fieldContext : fieldsContext ) {
				PrefixQuery prefix = new PrefixQuery( new Term( fieldContext.getField(), text ) );
				bqb.add( fieldContext.getFieldCustomizer().setWrappedQuery( prefix ).createQuery(), BooleanClause.Occur.SHOULD );
			}
		}
		return simplify( bqb.build() );
	}

}
