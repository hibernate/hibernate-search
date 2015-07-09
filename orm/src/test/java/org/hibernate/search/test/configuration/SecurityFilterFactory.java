/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.configuration;

import java.io.IOException;

import org.apache.lucene.index.DocsEnum;
import org.apache.lucene.index.LeafReader;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.CachingWrapperFilter;
import org.apache.lucene.search.DocIdSet;
import org.apache.lucene.search.Filter;
import org.apache.lucene.util.BitDocIdSet;
import org.apache.lucene.util.Bits;
import org.apache.lucene.util.FixedBitSet;
import org.hibernate.search.annotations.Factory;
import org.hibernate.search.annotations.Key;
import org.hibernate.search.filter.FilterKey;
import org.hibernate.search.filter.StandardFilterKey;

public class SecurityFilterFactory {

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
		key.addParameter( ownerName );
		return key;
	}

	private static final class SecurityFilter extends Filter {

		private final String ownerName;

		private SecurityFilter(final String ownerName) {
			this.ownerName = ownerName;
		}

		@Override
		public DocIdSet getDocIdSet(LeafReaderContext context, Bits acceptDocs) throws IOException {
			final LeafReader reader = context.reader();
			FixedBitSet bits = new FixedBitSet( reader.maxDoc() );
			DocsEnum termDocsEnum = reader.termDocsEnum( new Term( "owner", ownerName ) );
			if ( termDocsEnum == null ) {
				return new BitDocIdSet( bits );//All bits already correctly set
			}
			while ( termDocsEnum.nextDoc() != DocsEnum.NO_MORE_DOCS ) {
				final int docID = termDocsEnum.docID();
				if ( acceptDocs == null || acceptDocs.get( docID ) ) {
					bits.set( docID );
				}
			}
			return new BitDocIdSet( bits );
		}

		@Override
		public String toString(String field) {
			return "";
		}
	}
}
