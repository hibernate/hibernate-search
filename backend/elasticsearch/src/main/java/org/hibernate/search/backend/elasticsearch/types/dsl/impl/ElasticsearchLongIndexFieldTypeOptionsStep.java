/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.types.dsl.impl;

import org.hibernate.search.backend.elasticsearch.lowlevel.index.mapping.impl.DataTypes;
import org.hibernate.search.backend.elasticsearch.types.codec.impl.ElasticsearchFieldCodec;
import org.hibernate.search.backend.elasticsearch.types.codec.impl.ElasticsearchLongFieldCodec;
import org.hibernate.search.engine.backend.types.converter.spi.DefaultParseConverters;

class ElasticsearchLongIndexFieldTypeOptionsStep
		extends AbstractElasticsearchNumericFieldTypeOptionsStep<ElasticsearchLongIndexFieldTypeOptionsStep, Long> {

	ElasticsearchLongIndexFieldTypeOptionsStep(ElasticsearchIndexFieldTypeBuildContext buildContext) {
		super( buildContext, Long.class, DataTypes.LONG, DefaultParseConverters.LONG );
	}

	@Override
	protected ElasticsearchFieldCodec<Long> completeCodec() {
		return ElasticsearchLongFieldCodec.INSTANCE;
	}

	@Override
	protected ElasticsearchLongIndexFieldTypeOptionsStep thisAsS() {
		return this;
	}
}
