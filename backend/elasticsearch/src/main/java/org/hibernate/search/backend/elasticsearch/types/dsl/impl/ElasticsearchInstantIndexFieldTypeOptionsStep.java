/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.types.dsl.impl;

import java.time.Instant;
import java.time.format.DateTimeFormatter;

import org.hibernate.search.backend.elasticsearch.types.codec.impl.ElasticsearchFieldCodec;
import org.hibernate.search.backend.elasticsearch.types.codec.impl.ElasticsearchInstantFieldCodec;
import org.hibernate.search.engine.backend.types.converter.spi.DefaultParseConverters;

class ElasticsearchInstantIndexFieldTypeOptionsStep
		extends
		AbstractElasticsearchTemporalIndexFieldTypeOptionsStep<ElasticsearchInstantIndexFieldTypeOptionsStep, Instant> {

	ElasticsearchInstantIndexFieldTypeOptionsStep(ElasticsearchIndexFieldTypeBuildContext buildContext) {
		super( buildContext, Instant.class, DefaultParseConverters.INSTANT );
	}

	@Override
	protected ElasticsearchFieldCodec<Instant> createCodec(DateTimeFormatter formatter) {
		return new ElasticsearchInstantFieldCodec( formatter );
	}

	@Override
	protected ElasticsearchInstantIndexFieldTypeOptionsStep thisAsS() {
		return this;
	}
}
