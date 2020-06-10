/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.types.dsl.impl;

import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;

import org.hibernate.search.backend.elasticsearch.lowlevel.index.mapping.impl.DataTypes;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.mapping.impl.PropertyMapping;
import org.hibernate.search.backend.elasticsearch.types.codec.impl.ElasticsearchFieldCodec;
import org.hibernate.search.backend.elasticsearch.types.format.impl.ElasticsearchDefaultFieldFormatProvider;
import org.hibernate.search.backend.elasticsearch.types.sort.impl.ElasticsearchStandardFieldSortBuilderFactory;
import org.hibernate.search.backend.elasticsearch.types.sort.impl.ElasticsearchTemporalFieldSortBuilderFactory;

abstract class AbstractElasticsearchTemporalIndexFieldTypeOptionsStep<
				S extends AbstractElasticsearchTemporalIndexFieldTypeOptionsStep<?, F>, F extends TemporalAccessor
		>
		extends AbstractElasticsearchScalarFieldTypeOptionsStep<S, F> {

	AbstractElasticsearchTemporalIndexFieldTypeOptionsStep(ElasticsearchIndexFieldTypeBuildContext buildContext,
			Class<F> fieldType) {
		super( buildContext, fieldType, DataTypes.DATE );
	}

	@Override
	protected ElasticsearchFieldCodec<F> complete(PropertyMapping mapping) {
		ElasticsearchDefaultFieldFormatProvider defaultFieldFormatProvider =
				getBuildContext().getDefaultFieldFormatProvider();

		// TODO HSEARCH-2354 add method to allow customization of the format and formatter
		mapping.setFormat( defaultFieldFormatProvider.getDefaultMappingFormat( getFieldType() ) );

		DateTimeFormatter formatter = defaultFieldFormatProvider.getDefaultDateTimeFormatter( getFieldType() );

		return createCodec( formatter );
	}

	@Override
	protected ElasticsearchStandardFieldSortBuilderFactory<F> createFieldSortBuilderFactory(boolean resolvedAggregable,
			ElasticsearchFieldCodec<F> codec) {
		return new ElasticsearchTemporalFieldSortBuilderFactory<>( resolvedAggregable, codec );
	}

	protected abstract ElasticsearchFieldCodec<F> createCodec(DateTimeFormatter formatter);

}
