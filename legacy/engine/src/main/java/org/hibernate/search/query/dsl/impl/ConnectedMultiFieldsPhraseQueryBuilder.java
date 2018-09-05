/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.search.query.dsl.impl;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.MultiPhraseQuery;
import org.apache.lucene.search.PhraseQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.hibernate.search.analyzer.impl.LuceneAnalyzerReference;
import org.hibernate.search.analyzer.impl.RemoteAnalyzerReference;
import org.hibernate.search.exception.AssertionFailure;
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

	@Override
	public Query createQuery() {
		final int size = fieldContexts.size();
		if ( size == 1 ) {
			return queryCustomizer.setWrappedQuery( createQuery( fieldContexts.get( 0 ) ) ).createQuery();
		}
		else {
			BooleanQuery.Builder aggregatedFieldsQueryBuilder = new BooleanQuery.Builder();
			for ( FieldContext fieldContext : fieldContexts ) {
				aggregatedFieldsQueryBuilder.add( createQuery( fieldContext ), BooleanClause.Occur.SHOULD );
			}
			BooleanQuery aggregatedFieldsQuery = aggregatedFieldsQueryBuilder.build();
			return queryCustomizer.setWrappedQuery( aggregatedFieldsQuery ).createQuery();
		}
	}

	public Query createQuery(FieldContext fieldContext) {
		final Query perFieldQuery;
		final String fieldName = fieldContext.getField();
		final String sentence = phraseContext.getSentence();

		// we need to build differentiated queries depending of if the search terms should be analyzed
		// locally or not
		if ( queryContext.getQueryAnalyzerReference().is( RemoteAnalyzerReference.class ) ) {
			perFieldQuery = new RemotePhraseQuery( fieldName, phraseContext.getSlop(), sentence,
					queryContext.getOriginalAnalyzerReference().unwrap( RemoteAnalyzerReference.class ),
					queryContext.getQueryAnalyzerReference().unwrap( RemoteAnalyzerReference.class ) );
		}
		else {
			Analyzer analyzer = queryContext.getQueryAnalyzerReference().unwrap( LuceneAnalyzerReference.class ).getAnalyzer();
			/*
			 * Store terms per position and detect if for a given position more than one term is present
			 */
			TokenStream stream = null;
			boolean isMultiPhrase = false;
			Map<Integer, List<Term>> termsPerPosition = new TreeMap<Integer, List<Term>>();
			try {
				Reader reader = new StringReader( sentence );
				stream = analyzer.tokenStream( fieldName, reader );

				CharTermAttribute termAttribute = stream.addAttribute( CharTermAttribute.class );
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
						position += positionIncrement;
						termsAtSamePosition = termsPerPosition.get( position );
					}

					if ( termsAtSamePosition == null ) {
						termsAtSamePosition = new ArrayList<Term>();
						termsPerPosition.put( position, termsAtSamePosition );
					}

					String termString = new String( termAttribute.buffer(), 0, termAttribute.length() );
					termsAtSamePosition.add( new Term( fieldName, termString ) );
					if ( termsAtSamePosition.size() > 1 ) {
						isMultiPhrase = true;
					}
				}
			}
			catch (IOException e) {
				throw new AssertionFailure( "IOException while reading a string. Doh!", e );
			}
			finally {
				if ( stream != null ) {
					try {
						stream.end();
						stream.close();
					}
					catch (IOException e) {
						throw new AssertionFailure( "IOException while reading a string. Doh!", e );
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
				perFieldQuery = new BooleanQuery.Builder().build();
			}
			else if ( size <= 1 ) {
				final List<Term> terms = termsPerPosition.values().iterator().next();
				if ( terms.size() == 1 ) {
					perFieldQuery = new TermQuery( terms.get( 0 ) );
				}
				else {
					BooleanQuery.Builder booleanQueryBuilder = new BooleanQuery.Builder();
					for ( Term term : terms ) {
						booleanQueryBuilder.add( new TermQuery( term ), BooleanClause.Occur.SHOULD );
					}
					perFieldQuery = booleanQueryBuilder.build();
				}
			}
			else {
				if ( isMultiPhrase ) {
					MultiPhraseQuery query = new MultiPhraseQuery();
					query.setSlop( phraseContext.getSlop() );
					for ( Map.Entry<Integer, List<Term>> entry : termsPerPosition.entrySet() ) {
						final List<Term> value = entry.getValue();
						query.add( value.toArray( new Term[value.size()] ), entry.getKey() );
					}
					perFieldQuery = query;
				}
				else {
					PhraseQuery.Builder phraseBuilder = new PhraseQuery.Builder();
					phraseBuilder.setSlop( phraseContext.getSlop() );
					for ( Map.Entry<Integer, List<Term>> entry : termsPerPosition.entrySet() ) {
						final List<Term> value = entry.getValue();
						phraseBuilder.add( value.get( 0 ), entry.getKey() );
					}
					perFieldQuery = phraseBuilder.build();
				}
			}
		}

		return fieldContext.getFieldCustomizer().setWrappedQuery( perFieldQuery ).createQuery();
	}
}
