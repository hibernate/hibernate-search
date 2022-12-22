/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.document.model.dsl.impl;

import org.hibernate.search.engine.backend.document.model.dsl.spi.ImplicitFieldCollector;
import org.hibernate.search.engine.backend.document.model.dsl.spi.ImplicitFieldContributor;
import org.hibernate.search.engine.backend.types.Aggregable;
import org.hibernate.search.engine.backend.types.Projectable;
import org.hibernate.search.engine.backend.types.Searchable;
import org.hibernate.search.engine.backend.types.Sortable;

public class ElasticsearchStringImplicitFieldContributor implements ImplicitFieldContributor {

	private final String name;
	private final Searchable searchable;
	private final Sortable sortable;
	private final Projectable projectable;
	private final Aggregable aggregable;

	public ElasticsearchStringImplicitFieldContributor(String name) {
		this( name, Searchable.YES, Sortable.YES, Projectable.YES, Aggregable.YES );
	}

	public ElasticsearchStringImplicitFieldContributor(String name, Searchable searchable, Sortable sortable,
			Projectable projectable, Aggregable aggregable) {
		this.name = name;
		this.searchable = searchable;
		this.sortable = sortable;
		this.projectable = projectable;
		this.aggregable = aggregable;
	}

	public void contribute(ImplicitFieldCollector collector) {
		collector.addImplicitField(
				name,
				collector.indexFieldTypeFactory().asString()
						.searchable( searchable )
						.sortable( sortable )
						.projectable( projectable )
						.aggregable( aggregable )
						.toIndexFieldType()
		);
	}
}
