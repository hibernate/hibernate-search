package org.hibernate.search.test.configuration;

import java.io.IOException;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermDocs;
import org.apache.lucene.search.CachingWrapperFilter;
import org.apache.lucene.search.DocIdSet;
import org.apache.lucene.search.Filter;
import org.apache.lucene.search.QueryWrapperFilter;
import org.apache.lucene.util.OpenBitSet;
import org.hibernate.search.annotations.Factory;
import org.hibernate.search.annotations.Key;
import org.hibernate.search.filter.FilterKey;
import org.hibernate.search.filter.StandardFilterKey;

public class SecurityFilterFactory {

	private static final long serialVersionUID = -19238668272676998L;

	private String ownerName;
	
	public void setOwnerName(String ownerName) {
		this.ownerName = ownerName;
	}
	
	@Factory
	public Filter buildSecurityFilter() {
		SecurityFilter securityFilter = new SecurityFilter(ownerName);
		return new CachingWrapperFilter(securityFilter);
	}
	
	@Key
	public FilterKey getKey() {
		StandardFilterKey key = new StandardFilterKey();
		key.addParameter(ownerName);
		return key;
	}
	
	private final class SecurityFilter extends Filter {
		private static final long serialVersionUID = -5105989141875576599L;
		private final String ownerName;
		
		private SecurityFilter(final String ownerName) {
			this.ownerName = ownerName;
		}
		
		public DocIdSet getDocIdSet(IndexReader reader) throws IOException {
			OpenBitSet bitSet = new OpenBitSet( reader.maxDoc() );
			TermDocs termDocs = reader.termDocs( new Term( "owner", ownerName ) );
			while ( termDocs.next() ) {
				bitSet.set( termDocs.doc() );
			}
			return bitSet;
		}
		
	}

}
