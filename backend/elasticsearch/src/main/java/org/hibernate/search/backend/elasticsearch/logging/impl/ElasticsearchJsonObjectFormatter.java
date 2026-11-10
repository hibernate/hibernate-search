package org.hibernate.search.backend.elasticsearch.logging.impl;

import org.hibernate.search.backend.elasticsearch.client.common.json.internal.spi.JsonLogHelper;

import tools.jackson.databind.node.ObjectNode;

/**
 * Used with JBoss Logging's {@link org.jboss.logging.annotations.FormatWith}
 * to display {@link ObjectNode}s in log messages.
 */
public final class ElasticsearchJsonObjectFormatter {

	private final Object object;

	public ElasticsearchJsonObjectFormatter(Object object) {
		this.object = object;
	}

	@Override
	public String toString() {
		return object instanceof ObjectNode node ? JsonLogHelper.get().toString( node ) : String.valueOf( object );
	}
}
