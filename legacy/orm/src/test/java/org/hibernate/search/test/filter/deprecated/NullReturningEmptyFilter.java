/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.search.test.filter.deprecated;

import java.io.IOException;
import java.io.Serializable;

import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.search.DocIdSet;
import org.apache.lucene.search.Filter;
import org.apache.lucene.util.Bits;

/**
 * Apparently it's legal for Lucene filters to return null
 * on {@link Filter#getDocIdSet} : make sure we can deal with it as well.
 *
 * @author Sanne Grinovero (C) 2011 Red Hat Inc.
 */
public class NullReturningEmptyFilter extends Filter implements Serializable {

	@Override
	public DocIdSet getDocIdSet(LeafReaderContext context, Bits acceptDocs) throws IOException {
		return null;
	}

	@Override
	public String toString(String field) {
		return "";
	}
}
