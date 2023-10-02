/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.orm.loading.spi;

import org.hibernate.graph.GraphSemantic;
import org.hibernate.graph.RootGraph;
import org.hibernate.search.util.common.impl.Contracts;

public final class EntityGraphHint<T> {

	public final RootGraph<T> graph;
	public final GraphSemantic semantic;

	public EntityGraphHint(RootGraph<T> graph, GraphSemantic semantic) {
		Contracts.assertNotNull( graph, "graph" );
		Contracts.assertNotNull( semantic, "semantic" );
		this.graph = graph;
		this.semantic = semantic;
	}
}
