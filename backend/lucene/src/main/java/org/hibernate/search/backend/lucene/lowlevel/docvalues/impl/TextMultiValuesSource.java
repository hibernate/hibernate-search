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
 * A source of {@link TextMultiValues}.
 */
public abstract class TextMultiValuesSource {

	/**
	 * @return a {@link TextMultiValues} instance for the passed-in LeafReaderContext.
	 */
	public abstract TextMultiValues getValues(LeafReaderContext ctx) throws IOException;

}
