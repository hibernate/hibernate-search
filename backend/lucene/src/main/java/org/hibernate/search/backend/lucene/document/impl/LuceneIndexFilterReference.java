/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.document.impl;

import org.hibernate.search.backend.lucene.document.model.impl.LuceneIndexSchemaFilterNode;
import org.hibernate.search.engine.backend.document.IndexFilterReference;
import org.hibernate.search.engine.search.predicate.factories.FilterFactory;

public class LuceneIndexFilterReference<F extends FilterFactory> implements IndexFilterReference<F> {

	private LuceneIndexSchemaFilterNode schemaNode;
	private final String name;
	private final F factory;

	public LuceneIndexFilterReference(String name, F factory) {
		this.name = name;
		this.factory = factory;
	}

	public void enable(LuceneIndexSchemaFilterNode schemaNode) {
		this.schemaNode = schemaNode;
	}

	boolean isEnabled() {
		return schemaNode != null;
	}

	LuceneIndexSchemaFilterNode getSchemaNode() {
		return schemaNode;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public F getFactory() {
		return factory;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "[" + name + "]";
	}
}
