/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.document.impl;

import org.hibernate.search.backend.elasticsearch.document.model.impl.ElasticsearchIndexSchemaFilterNode;
import org.hibernate.search.engine.backend.document.IndexFilterReference;
import org.hibernate.search.engine.search.predicate.factories.FilterFactory;

public class ElasticsearchIndexFilterReference<F extends FilterFactory> implements IndexFilterReference<F> {

	private ElasticsearchIndexSchemaFilterNode<F> schemaNode;
	private final String name;
	private final F factory;

	public ElasticsearchIndexFilterReference(String name, F factory) {
		this.name = name;
		this.factory = factory;
	}

	public void enable(ElasticsearchIndexSchemaFilterNode<F> schemaNode) {
		this.schemaNode = schemaNode;
	}

	boolean isEnabled() {
		return schemaNode != null;
	}

	ElasticsearchIndexSchemaFilterNode<F> getSchemaNode() {
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
