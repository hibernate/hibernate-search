package org.hibernate.search.query.dsl.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.FuzzyQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.WildcardQuery;

import org.hibernate.annotations.common.AssertionFailure;
import org.hibernate.search.SearchException;
import org.hibernate.search.engine.DocumentBuilderIndexedEntity;
import org.hibernate.search.query.dsl.TermTermination;

/**
* @author Emmanuel Bernard
*/
public class ConnectedMultiFieldsTermQueryBuilder implements TermTermination {
	private final Object value;
	private final QueryCustomizer queryCustomizer;
	private final TermQueryContext termContext;
	private final List<FieldContext> fieldContexts;
	private final QueryBuildingContext queryContext;

	public ConnectedMultiFieldsTermQueryBuilder(TermQueryContext termContext,
												Object value,
												List<FieldContext> fieldContexts,
												QueryCustomizer queryCustomizer,
												QueryBuildingContext queryContext) {
		this.termContext = termContext;
		this.value = value;
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
		final DocumentBuilderIndexedEntity<?> documentBuilder = Helper.getDocumentBuilder( queryContext );
		String text = fieldContext.isIgnoreFieldBridge() ? 
					value.toString() :
					documentBuilder.objectToString( fieldContext.getField(), value );
		if ( fieldContext.isIgnoreAnalyzer() ) {
			perFieldQuery = createTermQuery( fieldContext, text );
		}
		else {
			List<String> terms;
			try {
				terms = getAllTermsFromText( fieldContext.getField(), text, queryContext.getQueryAnalyzer() );
			}
			catch ( IOException e ) {
				throw new AssertionFailure("IO exception while reading String stream??", e);
			}
			if ( terms.size() == 0 ) {
				throw new SearchException( "try to search with an empty string: " + fieldContext.getField() );
			}
			else if (terms.size() == 1 ) {
				perFieldQuery = createTermQuery( fieldContext, terms.get( 0 ) );
			}
			else {
				BooleanQuery booleanQuery = new BooleanQuery();
				for (String localTerm : terms) {
					Query termQuery = createTermQuery(fieldContext, localTerm);
					booleanQuery.add( termQuery, BooleanClause.Occur.SHOULD );
				}
				perFieldQuery = booleanQuery;
			}
		}
		return fieldContext.getFieldCustomizer().setWrappedQuery( perFieldQuery ).createQuery();
	}

	private Query createTermQuery(FieldContext fieldContext, String term) {
		Query query;
		final String fieldName = fieldContext.getField();
		switch ( termContext.getApproximation() ) {
			case EXACT:
				query = new TermQuery( new Term( fieldName, term ) );
				break;
			case WILDCARD:
				query = new WildcardQuery( new Term( fieldName, term ) );
				break;
			case FUZZY:
				query = new FuzzyQuery(
						new Term( fieldName, term ),
						termContext.getThreshold(),
						termContext.getPrefixLength() );
				break;
			default:
				throw new AssertionFailure( "Unknown approximation: " + termContext.getApproximation() );
		}
		return query;
	}

	private List<String> getAllTermsFromText(String fieldName, String localText, Analyzer analyzer) throws IOException {
		//it's better not to apply the analyzer with wildcard as * and ? can be mistakenly removed
		List<String> terms = new ArrayList<String>();
		if ( termContext.getApproximation() == TermQueryContext.Approximation.WILDCARD ) {
			terms.add( localText );
		}
		else {
			terms = Helper.getAllTermsFromText( fieldName, localText, analyzer );
		}
		return terms;
	}

}
