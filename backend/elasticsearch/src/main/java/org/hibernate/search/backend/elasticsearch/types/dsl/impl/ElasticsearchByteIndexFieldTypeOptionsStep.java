/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.types.dsl.impl;

import org.hibernate.search.backend.elasticsearch.lowlevel.index.mapping.impl.DataTypes;
import org.hibernate.search.backend.elasticsearch.types.codec.impl.ElasticsearchByteFieldCodec;
import org.hibernate.search.backend.elasticsearch.types.codec.impl.ElasticsearchFieldCodec;
import org.hibernate.search.engine.backend.types.converter.spi.DefaultParseConverters;

class ElasticsearchByteIndexFieldTypeOptionsStep
		extends AbstractElasticsearchNumericFieldTypeOptionsStep<ElasticsearchByteIndexFieldTypeOptionsStep, Byte> {

	ElasticsearchByteIndexFieldTypeOptionsStep(ElasticsearchIndexFieldTypeBuildContext buildContext) {
		super( buildContext, Byte.class, DataTypes.BYTE, DefaultParseConverters.BYTE );
	}

	@Override
	protected ElasticsearchFieldCodec<Byte> completeCodec() {
		return ElasticsearchByteFieldCodec.INSTANCE;
	}

	@Override
	protected ElasticsearchByteIndexFieldTypeOptionsStep thisAsS() {
		return this;
	}
}
