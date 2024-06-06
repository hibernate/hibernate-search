/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.types.mapping.impl;

public class Elasticsearch814VectorFieldTypeMappingContributor extends Elasticsearch812VectorFieldTypeMappingContributor {

	@Override
	protected boolean indexOptionAddCondition(Context context) {
		// we want to always add index options and in particular include `hnsw` type.
		return context.searchable();
	}
}
