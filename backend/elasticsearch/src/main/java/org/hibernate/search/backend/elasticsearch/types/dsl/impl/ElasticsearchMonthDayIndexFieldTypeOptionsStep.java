/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.types.dsl.impl;

import java.time.MonthDay;
import java.time.format.DateTimeFormatter;

import org.hibernate.search.backend.elasticsearch.types.codec.impl.ElasticsearchFieldCodec;
import org.hibernate.search.backend.elasticsearch.types.codec.impl.ElasticsearchMonthDayFieldCodec;

class ElasticsearchMonthDayIndexFieldTypeOptionsStep
		extends
		AbstractElasticsearchTemporalIndexFieldTypeOptionsStep<ElasticsearchMonthDayIndexFieldTypeOptionsStep, MonthDay> {

	ElasticsearchMonthDayIndexFieldTypeOptionsStep(ElasticsearchIndexFieldTypeBuildContext buildContext) {
		super( buildContext, MonthDay.class );
	}

	@Override
	protected ElasticsearchFieldCodec<MonthDay> createCodec(DateTimeFormatter formatter) {
		return new ElasticsearchMonthDayFieldCodec( formatter );
	}

	@Override
	protected ElasticsearchMonthDayIndexFieldTypeOptionsStep thisAsS() {
		return this;
	}
}
