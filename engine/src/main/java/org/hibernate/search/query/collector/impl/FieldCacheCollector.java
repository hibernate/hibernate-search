/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.search.query.collector.impl;

import java.io.IOException;

import org.apache.lucene.search.Collector;
import org.apache.lucene.search.Scorer;

/**
 * Because Lucene's Collector is not an interface, we have to create extensions of it.
 * All our implementations need a {@link #getValue(int)}, so we need an
 * abstract superclass defining it.
 *
 * @author Sanne Grinovero <sanne@hibernate.org> (C) 2011 Red Hat Inc.
 */
public abstract class FieldCacheCollector extends Collector {
	protected final Collector delegate;
	private final boolean acceptsDocsOutOfOrder;

	public FieldCacheCollector(Collector delegate) {
		this.delegate = delegate;
		this.acceptsDocsOutOfOrder = delegate.acceptsDocsOutOfOrder();
	}

	@Override
	public final void setScorer(Scorer scorer) throws IOException {
		this.delegate.setScorer( scorer );
	}

	@Override
	public final boolean acceptsDocsOutOfOrder() {
		return acceptsDocsOutOfOrder;
	}

	@Override
	public abstract void collect(int doc) throws IOException;

	public abstract Object getValue(int docId);
}
