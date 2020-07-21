/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.types.dsl.impl;

import org.hibernate.search.backend.elasticsearch.lowlevel.index.mapping.impl.DataTypes;
import org.hibernate.search.backend.elasticsearch.search.aggregation.impl.AggregationTypeKeys;
import org.hibernate.search.backend.elasticsearch.search.aggregation.impl.ElasticsearchTermsAggregation;
import org.hibernate.search.backend.elasticsearch.search.projection.impl.ElasticsearchFieldProjection;
import org.hibernate.search.backend.elasticsearch.search.projection.impl.ProjectionTypeKeys;
import org.hibernate.search.backend.elasticsearch.types.codec.impl.ElasticsearchBooleanFieldCodec;
import org.hibernate.search.backend.elasticsearch.types.codec.impl.ElasticsearchFieldCodec;
import org.hibernate.search.backend.elasticsearch.types.predicate.impl.ElasticsearchStandardFieldPredicateBuilderFactory;
import org.hibernate.search.backend.elasticsearch.types.sort.impl.ElasticsearchStandardFieldSortBuilderFactory;

class ElasticsearchBooleanIndexFieldTypeOptionsStep
		extends AbstractElasticsearchSimpleStandardFieldTypeOptionsStep<ElasticsearchBooleanIndexFieldTypeOptionsStep, Boolean> {

	ElasticsearchBooleanIndexFieldTypeOptionsStep(ElasticsearchIndexFieldTypeBuildContext buildContext) {
		super( buildContext, Boolean.class, DataTypes.BOOLEAN );
	}

	@Override
	protected void complete() {
		ElasticsearchFieldCodec<Boolean> codec = ElasticsearchBooleanFieldCodec.INSTANCE;
		builder.codec( codec );

		builder.predicateBuilderFactory(
				new ElasticsearchStandardFieldPredicateBuilderFactory<>( resolvedSearchable, codec ) );
		builder.sortBuilderFactory(
				new ElasticsearchStandardFieldSortBuilderFactory<>( resolvedSortable, codec ) );

		if ( resolvedProjectable ) {
			builder.projectable( true );
			builder.queryElementFactory( ProjectionTypeKeys.FIELD, new ElasticsearchFieldProjection.Factory<>( codec ) );
		}

		if ( resolvedAggregable ) {
			builder.aggregable( true );
			builder.queryElementFactory( AggregationTypeKeys.TERMS, new ElasticsearchTermsAggregation.Factory<>( codec ) );
		}
	}

	@Override
	protected ElasticsearchBooleanIndexFieldTypeOptionsStep thisAsS() {
		return this;
	}

}
