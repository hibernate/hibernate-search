/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
