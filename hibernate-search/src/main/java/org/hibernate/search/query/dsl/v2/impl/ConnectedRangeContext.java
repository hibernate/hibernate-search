package org.hibernate.search.query.dsl.v2.impl;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.search.Filter;

import org.hibernate.search.SearchFactory;
import org.hibernate.search.query.dsl.v2.RangeContext;
import org.hibernate.search.query.dsl.v2.RangeMatchingContext;

/**
 * @author Emmanuel Bernard
 */
class ConnectedRangeContext implements RangeContext {
	private final SearchFactory factory;
	private final Analyzer queryAnalyzer;
	private final QueryCustomizer queryCustomizer;

	public ConnectedRangeContext(Analyzer queryAnalyzer, SearchFactory factory) {
		this.factory = factory;
		this.queryAnalyzer = queryAnalyzer;
		this.queryCustomizer = new QueryCustomizer();
	}

	public RangeMatchingContext onField(String fieldName) {
		return new ConnectedRangeMatchingContext(fieldName, queryCustomizer, queryAnalyzer, factory);
	}

	public RangeContext boostedTo(float boost) {
		queryCustomizer.boostedTo( boost );
		return this;
	}

	public RangeContext constantScore() {
		queryCustomizer.constantScore();
		return this;
	}

	public RangeContext filter(Filter filter) {
		queryCustomizer.filter(filter);
		return this;
	}


//
//	public <T> FromRangeContext<T> from(T from) {
//		context.setFrom( from );
//		return new ConnectedFromRangeContext<T>(this);
//	}
//
//
//
//	SearchFactory getFactory() {
//		return factory;
//	}
//
//	Analyzer getQueryAnalyzer() {
//		return queryAnalyzer;
//	}
//
//	QueryCustomizer getQueryCustomizer() {
//		return queryCustomizer;
//	}
//
//	static class ConnectedFromRangeContext<T> implements FromRangeContext<T> {
//		private ConnectedRangeContext mother;
//
//		public ConnectedFromRangeContext(ConnectedRangeContext mother) {
//			this.mother = mother;
//		}
//
//		public ToRangeContext to(Object to) {
//			mother.getContext().setTo( to );
//			return new ConnectedToRangeContext(mother);
//		}
//
//		public FromRangeContext<T> exclude() {
//			mother.getContext().setExcludeFrom( true );
//			return this;
//		}
//
//		public FromRangeContext<T> boostedTo(float boost) {
//			mother.boostedTo( boost );
//			return this;
//		}
//
//		public FromRangeContext<T> constantScore() {
//			mother.constantScore();
//			return this;
//		}
//
//		public FromRangeContext<T> filter(Filter filter) {
//			mother.filter( filter );
//			return this;
//		}
//	}
//
//	static class ConnectedToRangeContext implements ToRangeContext {
//		private ConnectedRangeContext mother;
//
//		public ConnectedToRangeContext(ConnectedRangeContext mother) {
//			this.mother = mother;
//		}
//
//		public TermMatchingContext onField(String field) {
//			return new ConnectedTermMatchingContext(
//					mother.getContext(),
//					field,
//					mother.getQueryCustomizer(),
//					mother.getQueryAnalyzer(),
//					mother.getFactory()
//			);
//		}
//
//		public ToRangeContext exclude() {
//			mother.getContext().setExcludeTo( true );
//			return this;
//		}
//
//		public ToRangeContext boostedTo(float boost) {
//			mother.boostedTo( boost );
//			return this;
//		}
//
//		public ToRangeContext constantScore() {
//			mother.constantScore();
//			return this;
//		}
//
//		public ToRangeContext filter(Filter filter) {
//			mother.filter( filter );
//			return this;
//		}
//	}


}
