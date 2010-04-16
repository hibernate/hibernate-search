package org.hibernate.search.query.dsl.v2.impl;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.TermAttribute;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.FuzzyQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.WildcardQuery;

import org.hibernate.annotations.common.AssertionFailure;
import org.hibernate.search.SearchException;
import org.hibernate.search.SearchFactory;
import org.hibernate.search.query.dsl.v2.TermTermination;

/**
* @author Emmanuel Bernard
*/
public class ConnectedSingleTermQueryBuilder implements TermTermination {
	private final SearchFactory factory;
	private final String field;
	private final String text;
	private final Analyzer queryAnalyzer;
	private final QueryCustomizer queryCustomizer;
	private boolean ignoreAnalyzer;
	private final QueryContext context;

	public ConnectedSingleTermQueryBuilder(
			QueryContext context,
			boolean ignoreAnalyzer,
			String text,
			String field,
			QueryCustomizer queryCustomizer,
			Analyzer queryAnalyzer,
			SearchFactory factory) {
		this.context = context;
		this.factory = factory;
		this.field = field;
		this.text = text;
		this.queryAnalyzer = queryAnalyzer;
		this.queryCustomizer = queryCustomizer;
		this.ignoreAnalyzer = ignoreAnalyzer;
	}

	public Query createQuery() {
		final Query result;
		if ( ignoreAnalyzer ) {
			result = createTermQuery( text );
		}
		else {
			List<String> terms;
			try {
				terms = getAllTermsFromText( field, text, queryAnalyzer );
			}
			catch ( IOException e ) {
				throw new AssertionFailure("IO exception while reading String stream??", e);
			}
			if ( terms.size() == 0 ) {
				throw new SearchException("try to search with an empty string: " + field);
			}
			else if (terms.size() == 1 ) {
				result = createTermQuery( terms.get( 0 ) );
			}
			else {
				BooleanQuery booleanQuery = new BooleanQuery();
				for (String term : terms) {
					Query termQuery = createTermQuery(term);
					booleanQuery.add( termQuery, BooleanClause.Occur.SHOULD );
				}
				result = booleanQuery;
			}
		}
		return queryCustomizer.setWrappedQuery( result ).createQuery();
	}

	private Query createTermQuery(String term) {
		Query query;
		switch ( context.getApproximation() ) {
			case EXACT:
				query = new TermQuery( new Term(field, term) );
				break;
			case WILDCARD:
				query = new WildcardQuery( new Term(field, term) );
				break;
			case FUZZY:
				query = new FuzzyQuery( new Term(field, term), context.getThreshold(), context.getPrefixLength() );
				break;
			default:
				throw new AssertionFailure( "Unknown approximation: " + context.getApproximation());
		}
		return query;
	}

	private List<String> getAllTermsFromText(String fieldName, String text, Analyzer analyzer) throws IOException {
		//it's better not to apply the analyzer with windcards as * and ? can be mistakenly removed
		List<String> terms = new ArrayList<String>();
		if ( context.getApproximation() == QueryContext.Approximation.WILDCARD ) {
			terms.add( text );
		}
		else {
			Reader reader = new StringReader(text);
			TokenStream stream = analyzer.reusableTokenStream( fieldName, reader);
			TermAttribute attribute = (TermAttribute) stream.addAttribute( TermAttribute.class );
			stream.reset();

			while ( stream.incrementToken() ) {
				if ( attribute.termLength() > 0 ) {
					String term = attribute.term();
					terms.add( term );
				}
			}
			stream.end();
			stream.close();
		}
		return terms;
	}

}
