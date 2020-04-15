/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.search.backend.lucene.lowlevel.docvalues.impl;

import java.io.IOException;

import org.apache.lucene.index.LeafReaderContext;

/**
 * A source of {@link LongMultiValues}.
 */
public abstract class LongMultiValuesSource {

	/**
	 * @return a {@link LongMultiValues} instance for the passed-in LeafReaderContext.
	 */
	public abstract LongMultiValues getValues(LeafReaderContext ctx) throws IOException;

}
