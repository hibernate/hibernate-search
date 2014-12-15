/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.indexes.serialization.avro.impl;

import org.apache.avro.Protocol;

/**
 * Helper to build an Avro Protocol version 1.1 from all our resource
 * schemas.
 */
class ProtocolBuilderV1_2 extends ProtocolBuilderV1_1 {

	private static String V1_2_PATH = "org/hibernate/search/remote/codex/avro/v1_2/";

	/**
	 * @return an Avro Protocol at version 1.2
	 */
	@Override
	Protocol build() {
		parseSchema( "attribute/TokenTrackingAttribute" );
		parseSchema( "attribute/CharTermAttribute" );
		parseSchema( "attribute/PayloadAttribute" );
		parseSchema( "attribute/KeywordAttribute" );
		parseSchema( "attribute/PositionIncrementAttribute" );
		parseSchema( "attribute/FlagsAttribute" );
		parseSchema( "attribute/TypeAttribute" );
		parseSchema( "attribute/OffsetAttribute" );
		parseSchema( "field/TermVector" );
		parseSchema( "field/Index" );
		parseSchema( "field/Store" );
		parseSchema( "field/TokenStreamField" );
		parseSchema( "field/ReaderField" );
		parseSchema( "field/StringField" );
		parseSchema( "field/BinaryField" );
		parseSchema( "field/NumericIntField" );
		parseSchema( "field/NumericLongField" );
		parseSchema( "field/NumericFloatField" );
		parseSchema( "field/NumericDoubleField" );
		parseSchema( "field/CustomFieldable" );
		parseSchema( "field/BinaryDocValuesField" );
		parseSchema( "field/NumericDocValuesField" );
		parseSchema( "field/DocValuesType" );
		parseSchema( "Document" );
		parseSchema( "operation/Id" );
		parseSchema( "operation/OptimizeAll" );
		parseSchema( "operation/Flush" );
		parseSchema( "operation/PurgeAll" );
		parseSchema( "operation/Delete" );
		parseSchema( "operation/Add" );
		parseSchema( "operation/Update" );
		parseSchema( "operation/DeleteByQuery" );
		parseSchema( "Message" );
		return parseProtocol( "Works" );
	}

	@Override
	protected String getResourceBasePath() {
		return V1_2_PATH;
	}

}
