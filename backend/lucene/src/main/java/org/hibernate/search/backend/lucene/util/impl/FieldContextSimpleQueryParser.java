/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.search.backend.lucene.util.impl;

import java.util.Collections;
import java.util.Map;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.simple.SimpleQueryParser;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.FuzzyQuery;
import org.apache.lucene.search.PrefixQuery;
import org.apache.lucene.search.Query;

/**
 * An implementation of {@link SimpleQueryParser} that allows to wrap created queries
 * to add boosts, constant score, ...
 *
 */
public class FieldContextSimpleQueryParser extends SimpleQueryParser {

	public interface FieldContext {

		Query wrap(Query query);

	}

	private final Map<String, ? extends FieldContext> fieldContexts;

	public FieldContextSimpleQueryParser(Analyzer analyzer, Map<String, ? extends FieldContext> fieldContexts) {
		this( analyzer, fieldContexts, -1 );
	}

	public FieldContextSimpleQueryParser(Analyzer analyzer, Map<String, ? extends FieldContext> fieldContexts, int flags) {
		super( analyzer, Collections.emptyMap(), flags );
		this.fieldContexts = fieldContexts;
	}

	@Override
	protected Query newDefaultQuery(String text) {
		BooleanQuery.Builder bqb = new BooleanQuery.Builder();
		for ( Map.Entry<String, ? extends FieldContext> entry : fieldContexts.entrySet() ) {
			Query q = createBooleanQuery( entry.getKey(), text, getDefaultOperator() );
			if ( q != null ) {
				bqb.add( entry.getValue().wrap( q ), BooleanClause.Occur.SHOULD );
			}
		}
		return simplify( bqb.build() );
	}

	@Override
	protected Query newFuzzyQuery(String text, int fuzziness) {
		BooleanQuery.Builder bqb = new BooleanQuery.Builder();
		for ( Map.Entry<String, ? extends FieldContext> entry : fieldContexts.entrySet() ) {
			Query q = new FuzzyQuery( new Term( entry.getKey(), text ), fuzziness );
			bqb.add( entry.getValue().wrap( q ), BooleanClause.Occur.SHOULD );
		}
		return simplify( bqb.build() );
	}

	@Override
	protected Query newPhraseQuery(String text, int slop) {
		BooleanQuery.Builder bqb = new BooleanQuery.Builder();
		for ( Map.Entry<String, ? extends FieldContext> entry : fieldContexts.entrySet() ) {
			Query q = createPhraseQuery( entry.getKey(), text, slop );
			if ( q != null ) {
				bqb.add( entry.getValue().wrap( q ), BooleanClause.Occur.SHOULD );
			}
		}
		return simplify( bqb.build() );
	}

	@Override
	protected Query newPrefixQuery(String text) {
		BooleanQuery.Builder bqb = new BooleanQuery.Builder();
		for ( Map.Entry<String, ? extends FieldContext> entry : fieldContexts.entrySet() ) {
			PrefixQuery prefix = new PrefixQuery( new Term( entry.getKey(), text ) );
			bqb.add( entry.getValue().wrap( prefix ), BooleanClause.Occur.SHOULD );
		}
		return simplify( bqb.build() );
	}

}
