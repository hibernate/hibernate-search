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
