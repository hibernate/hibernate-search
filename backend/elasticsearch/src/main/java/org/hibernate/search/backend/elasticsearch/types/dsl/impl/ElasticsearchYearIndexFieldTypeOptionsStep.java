/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.types.dsl.impl;

import java.time.Year;
import java.time.format.DateTimeFormatter;

import org.hibernate.search.backend.elasticsearch.types.codec.impl.ElasticsearchFieldCodec;
import org.hibernate.search.backend.elasticsearch.types.codec.impl.ElasticsearchYearFieldCodec;
import org.hibernate.search.engine.backend.types.converter.spi.DefaultParseConverters;

class ElasticsearchYearIndexFieldTypeOptionsStep
		extends
		AbstractElasticsearchTemporalIndexFieldTypeOptionsStep<ElasticsearchYearIndexFieldTypeOptionsStep, Year> {

	ElasticsearchYearIndexFieldTypeOptionsStep(ElasticsearchIndexFieldTypeBuildContext buildContext) {
		super( buildContext, Year.class, DefaultParseConverters.YEAR );
	}

	@Override
	protected ElasticsearchFieldCodec<Year> createCodec(DateTimeFormatter formatter) {
		return new ElasticsearchYearFieldCodec( formatter );
	}

	@Override
	protected ElasticsearchYearIndexFieldTypeOptionsStep thisAsS() {
		return this;
	}
}
