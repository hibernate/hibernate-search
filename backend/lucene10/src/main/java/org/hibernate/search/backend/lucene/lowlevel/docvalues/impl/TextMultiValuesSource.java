/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
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
