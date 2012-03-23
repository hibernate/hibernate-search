/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat, Inc. and/or its affiliates or third-party contributors as
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

package org.hibernate.search.query.dsl.impl;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;
import org.apache.lucene.analysis.tokenattributes.TermAttribute;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.MultiPhraseQuery;
import org.apache.lucene.search.PhraseQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;

import org.hibernate.annotations.common.AssertionFailure;
import org.hibernate.search.query.dsl.PhraseTermination;

/**
 * @author Emmanuel Bernard
 */
public class ConnectedMultiFieldsPhraseQueryBuilder implements PhraseTermination {
	private final PhraseQueryContext phraseContext;
	private final QueryBuildingContext queryContext;
	private final QueryCustomizer queryCustomizer;
	private final List<FieldContext> fieldContexts;

	public ConnectedMultiFieldsPhraseQueryBuilder(PhraseQueryContext phraseContext, QueryCustomizer queryCustomizer,
												  List<FieldContext> fieldContexts, QueryBuildingContext queryContext) {
		this.phraseContext = phraseContext;
		this.queryContext = queryContext;
		this.queryCustomizer = queryCustomizer;
		this.fieldContexts = fieldContexts;
	}

	public Query createQuery() {
		final int size = fieldContexts.size();
		if ( size == 1 ) {
			return queryCustomizer.setWrappedQuery( createQuery( fieldContexts.get( 0 ) ) ).createQuery();
		}
		else {
			BooleanQuery aggregatedFieldsQuery = new BooleanQuery( );
			for ( FieldContext fieldContext : fieldContexts ) {
				aggregatedFieldsQuery.add( createQuery( fieldContext ), BooleanClause.Occur.SHOULD );
			}
			return  queryCustomizer.setWrappedQuery( aggregatedFieldsQuery ).createQuery();
		}
	}

	public Query createQuery(FieldContext fieldContext) {
		final Query perFieldQuery;
		final String fieldName = fieldContext.getField();

		/*
		 * Store terms per position and detect if for a given position more than one term is present
		 */
		TokenStream stream = null;
		boolean isMultiPhrase = false;
		Map<Integer, List<Term>> termsPerPosition = new HashMap<Integer, List<Term>>();
		final String sentence = phraseContext.getSentence();
		try {
			Reader reader = new StringReader( sentence );
			stream = queryContext.getQueryAnalyzer().reusableTokenStream( fieldName, reader);

			TermAttribute termAttribute = stream.addAttribute( TermAttribute.class );
			PositionIncrementAttribute positionAttribute = stream.addAttribute( PositionIncrementAttribute.class );

			stream.reset();
			int position = -1; //start at -1 since we apply at least one increment
			List<Term> termsAtSamePosition = null;
			while ( stream.incrementToken() ) {
				int positionIncrement = 1;
				if ( positionAttribute != null ) {
					positionIncrement = positionAttribute.getPositionIncrement();
				}

				if ( positionIncrement > 0 ) {
					position+=positionIncrement;
					termsAtSamePosition = termsPerPosition.get(position);
				}

				if (termsAtSamePosition == null) {
					termsAtSamePosition = new ArrayList<Term>();
					termsPerPosition.put( position, termsAtSamePosition  );
				}

				termsAtSamePosition.add( new Term( fieldName, termAttribute.term() ) );
				if ( termsAtSamePosition.size() > 1 ) {
					isMultiPhrase = true;
				}
			}
		}
		catch ( IOException e ) {
			throw new AssertionFailure( "IOException while reading a string. Doh!", e);
		}
		finally {
			if ( stream != null ) {
				try {
					stream.end();
					stream.close();
				}
				catch ( IOException e ) {
					throw new AssertionFailure( "IOException while reading a string. Doh!", e);
				}
			}
		}

		/*
		 * Create the appropriate query depending on the conditions
		 * note that a MultiPhraseQuery is needed if several terms share the same position
		 * as it will do a OR and not a AND like PhraseQuery
		 */
		final int size = termsPerPosition.size();
		if ( size == 0 ) {
			perFieldQuery = new BooleanQuery( );
		} else if ( size <= 1 ) {
			final List<Term> terms = termsPerPosition.values().iterator().next();
			if ( terms.size() == 1 ) {
				perFieldQuery = new TermQuery( terms.get( 0 ) );
			}
			else {
				BooleanQuery query = new BooleanQuery( );
				for ( Term term : terms ) {
					query.add( new TermQuery(term), BooleanClause.Occur.SHOULD );
				}
				perFieldQuery = query;
			}
		}
		else {
			if (isMultiPhrase) {
				MultiPhraseQuery query = new MultiPhraseQuery();
				query.setSlop( phraseContext.getSlop() );
				for ( Map.Entry<Integer,List<Term>> entry : termsPerPosition.entrySet() ) {
					final List<Term> value = entry.getValue();
					query.add( value.toArray( new Term[value.size()] ), entry.getKey() );
				}
				perFieldQuery = query;
			}
			else {
				PhraseQuery query = new PhraseQuery();
				query.setSlop(  phraseContext.getSlop() );
				for ( Map.Entry<Integer,List<Term>> entry : termsPerPosition.entrySet() ) {
					final List<Term> value = entry.getValue();
					query.add( value.get(0), entry.getKey() );
				}
				perFieldQuery = query;
			}
		}
		return fieldContext.getFieldCustomizer().setWrappedQuery( perFieldQuery ).createQuery();
	}
}