/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.query.impl;

import java.util.List;

import org.apache.lucene.document.Document;
import org.hibernate.search.bridge.TwoWayFieldBridge;
import org.hibernate.search.bridge.spi.ConversionContext;

import com.google.gson.JsonObject;

class TwoWayFieldBridgeProjection extends FieldProjection {

	private final String absoluteName;
	private final TwoWayFieldBridge bridge;
	private final PrimitiveProjection defaultFieldProjection;
	private final List<PrimitiveProjection> bridgeDefinedFieldsProjections;

	public TwoWayFieldBridgeProjection(String absoluteName,
			TwoWayFieldBridge bridge,
			PrimitiveProjection defaultFieldProjection,
			List<PrimitiveProjection> bridgeDefinedFieldsProjections) {
		super();
		this.absoluteName = absoluteName;
		this.bridge = bridge;
		this.defaultFieldProjection = defaultFieldProjection;
		this.bridgeDefinedFieldsProjections = bridgeDefinedFieldsProjections;
	}

	@Override
	public Object convertHit(JsonObject hit, ConversionContext conversionContext) {
		return convertFieldValue( hit, conversionContext );
	}

	private Object convertFieldValue(JsonObject hit, ConversionContext conversionContext) {
		Document tmp = new Document();

		defaultFieldProjection.addDocumentField( tmp, hit, conversionContext );

		// Add to the document the additional fields created when indexing the value
		for ( PrimitiveProjection subProjection : bridgeDefinedFieldsProjections ) {
			subProjection.addDocumentField( tmp, hit, conversionContext );
		}

		return conversionContext.twoWayConversionContext( bridge ).get( absoluteName, tmp );
	}
}