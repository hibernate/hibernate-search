package org.hibernate.search.query.dsl.v2.impl;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.Token;
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
import org.hibernate.search.query.dsl.v2.TermContext;

/**
 * @author Emmanuel Bernard
 */
public class ConnectedTermContext implements TermContext {
	private final SearchFactory factory;
	private final Analyzer queryAnalyzer;

	public ConnectedTermContext(Analyzer queryAnalyzer, SearchFactory factory) {
		this.factory = factory;
		this.queryAnalyzer = queryAnalyzer;
	}

	public TermMatchingContext on(String field) {
		return new ConnectedTermMatchingContext(field, queryAnalyzer, factory);
	}

	public static class ConnectedTermMatchingContext implements TermMatchingContext {
		private final SearchFactory factory;
		private final String field;
		private final Analyzer queryAnalyzer;

		public ConnectedTermMatchingContext(String field, Analyzer queryAnalyzer, SearchFactory factory) {
			this.factory = factory;
			this.field = field;
			this.queryAnalyzer = queryAnalyzer;
		}

		public TermCustomization matches(String text) {
			return new ConnectedTermCustomization(text, field, queryAnalyzer, factory);
		}
	}

	public static class ConnectedTermCustomization implements TermCustomization, TermCustomization.TermFuzzy {
		private final SearchFactory factory;
		private final String field;
		private final String text;
		private final Analyzer queryAnalyzer;

		private boolean ignoreAnalyzer;
		private Approximation approximation = Approximation.EXACT;
		private float threshold = .5f;
		private int prefixLength = 0;

		public ConnectedTermCustomization(String text, String field, Analyzer queryAnalyzer, SearchFactory factory) {
			this.factory = factory;
			this.field = field;
			this.text = text;
			this.queryAnalyzer = queryAnalyzer;
		}

		public TermCustomization ignoreAnalyzer() {
			ignoreAnalyzer = true;
			return this;
		}

		public TermFuzzy fuzzy() {
			if (approximation != Approximation.EXACT) {
				throw new IllegalStateException( "Cannot call fuzzy() and wildcard() on the same term query" );
			}
			approximation = Approximation.FUZZY;
			return this;
		}

		public TermCustomization wildcard() {
			if (approximation != Approximation.EXACT) {
				throw new IllegalStateException( "Cannot call fuzzy() and wildcard() on the same term query" );
			}
			approximation = Approximation.WILDCARD;
			return this;
		}

		public Query createQuery() {
			if ( ignoreAnalyzer ) {
				return createTermQuery( text );	
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
					return createTermQuery( terms.get( 0 ) );
				}
				else {
					BooleanQuery finalQuery = new BooleanQuery();
					for (String term : terms) {
						Query termQuery = createTermQuery(term);
						//termQuery.setBoost( boost );
						finalQuery.add( termQuery, BooleanClause.Occur.SHOULD );
					}
					return finalQuery;
				}
			}
		}

		private Query createTermQuery(String term) {
			Query query;
			switch ( approximation ) {
				case EXACT:
					query = new TermQuery( new Term(field, term) );
					break;
				case WILDCARD:
					query = new WildcardQuery( new Term(field, term) );
					break;
				case FUZZY:
					query = new FuzzyQuery( new Term(field, term), threshold, prefixLength );
					break;
				default:
					throw new AssertionFailure( "Unknown approximation: " + approximation);

			}
			return query;
		}

		private List<String> getAllTermsFromText(String fieldName, String text, Analyzer analyzer) throws IOException {
			Reader reader = new StringReader(text);
			TokenStream stream = analyzer.reusableTokenStream( fieldName, reader);
			TermAttribute attribute = (TermAttribute) stream.addAttribute( TermAttribute.class );
			stream.reset();
			List<String> terms = new ArrayList<String>();
			while ( stream.incrementToken() ) {
				if ( attribute.termLength() > 0 ) {
					String term = attribute.term();
					terms.add( term );
				}
			}
			stream.end();
    		stream.close();
			return terms;
		}

		public TermFuzzy threshold(float threshold) {
			this.threshold = threshold;
			return this;
		}

		public TermFuzzy prefixLength(int prefixLength) {
			this.prefixLength = prefixLength;
			return this;
		}

		private static enum Approximation {
			EXACT,
			WILDCARD,
			FUZZY
		}
	}
}
