/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.types.dsl.impl;

import org.hibernate.search.backend.elasticsearch.lowlevel.index.mapping.impl.DataTypes;
import org.hibernate.search.backend.elasticsearch.types.codec.impl.ElasticsearchDoubleFieldCodec;
import org.hibernate.search.backend.elasticsearch.types.codec.impl.ElasticsearchFieldCodec;
import org.hibernate.search.engine.backend.types.converter.spi.DefaultParseConverters;

class ElasticsearchDoubleIndexFieldTypeOptionsStep
		extends AbstractElasticsearchNumericFieldTypeOptionsStep<ElasticsearchDoubleIndexFieldTypeOptionsStep, Double> {

	ElasticsearchDoubleIndexFieldTypeOptionsStep(ElasticsearchIndexFieldTypeBuildContext buildContext) {
		super( buildContext, Double.class, DataTypes.DOUBLE, DefaultParseConverters.DOUBLE );
	}

	@Override
	protected ElasticsearchFieldCodec<Double> completeCodec() {
		return ElasticsearchDoubleFieldCodec.INSTANCE;
	}

	@Override
	protected ElasticsearchDoubleIndexFieldTypeOptionsStep thisAsS() {
		return this;
	}
}
