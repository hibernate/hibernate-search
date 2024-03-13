/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.types.dsl.impl;

import java.time.YearMonth;
import java.time.format.DateTimeFormatter;

import org.hibernate.search.backend.elasticsearch.types.codec.impl.ElasticsearchFieldCodec;
import org.hibernate.search.backend.elasticsearch.types.codec.impl.ElasticsearchYearMonthFieldCodec;
import org.hibernate.search.engine.backend.types.converter.spi.DefaultParseConverters;

class ElasticsearchYearMonthIndexFieldTypeOptionsStep
		extends
		AbstractElasticsearchTemporalIndexFieldTypeOptionsStep<ElasticsearchYearMonthIndexFieldTypeOptionsStep, YearMonth> {

	ElasticsearchYearMonthIndexFieldTypeOptionsStep(ElasticsearchIndexFieldTypeBuildContext buildContext) {
		super( buildContext, YearMonth.class, DefaultParseConverters.YEAR_MONTH );
	}

	@Override
	protected ElasticsearchFieldCodec<YearMonth> createCodec(DateTimeFormatter formatter) {
		return new ElasticsearchYearMonthFieldCodec( formatter );
	}

	@Override
	protected ElasticsearchYearMonthIndexFieldTypeOptionsStep thisAsS() {
		return this;
	}
}
