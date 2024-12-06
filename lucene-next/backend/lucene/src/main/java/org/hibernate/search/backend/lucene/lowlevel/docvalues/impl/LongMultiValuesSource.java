/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
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
