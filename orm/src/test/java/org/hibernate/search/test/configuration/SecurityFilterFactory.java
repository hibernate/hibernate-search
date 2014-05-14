/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.configuration;

import java.io.IOException;

import org.apache.lucene.index.AtomicReader;
import org.apache.lucene.index.AtomicReaderContext;
import org.apache.lucene.index.DocsEnum;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.CachingWrapperFilter;
import org.apache.lucene.search.DocIdSet;
import org.apache.lucene.search.Filter;
import org.apache.lucene.util.Bits;
import org.apache.lucene.util.OpenBitSet;
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
		public DocIdSet getDocIdSet(AtomicReaderContext context, Bits acceptDocs) throws IOException {
			final AtomicReader reader = context.reader();
			OpenBitSet bitSet = new OpenBitSet( reader.maxDoc() );
			DocsEnum termDocsEnum = reader.termDocsEnum( new Term( "owner", ownerName ) );
			if ( termDocsEnum == null ) {
				return bitSet;//All bits already correctly set
			}
			while ( termDocsEnum.nextDoc() != DocsEnum.NO_MORE_DOCS ) {
				final int docID = termDocsEnum.docID();
				if ( acceptDocs == null || acceptDocs.get( docID ) ) {
					bitSet.set( docID );
				}
			}
			return bitSet;
		}

	}

}
