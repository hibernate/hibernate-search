/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.types.mapping.impl;

import org.hibernate.search.backend.elasticsearch.search.predicate.impl.ElasticsearchKnnPredicate;
import org.hibernate.search.backend.elasticsearch.types.impl.ElasticsearchIndexValueFieldType;
import org.hibernate.search.engine.search.predicate.spi.PredicateTypeKeys;

public class Elasticsearch812VectorFieldTypeMappingContributor extends Elasticsearch8VectorFieldTypeMappingContributor {
	@Override
	public <F> void contribute(ElasticsearchIndexValueFieldType.Builder<F> builder, Context context) {
		builder.queryElementFactory( PredicateTypeKeys.KNN,
				new ElasticsearchKnnPredicate.Elasticsearch812Factory<>( builder.codec() ) );
	}
}
