/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.query.dsl;

import org.apache.lucene.document.Document;
import org.hibernate.search.bridge.FieldBridge;
import org.hibernate.search.bridge.LuceneOptions;

/**
 * Adds a custom field to be queried via explicitly passed field bridge.
 *
 * @author Gunnar Morling
 */
public class MonthClassBridge implements FieldBridge {

	public static final String FIELD_NAME_1 = "monthValueAsRomanNumberFromClassBridge1";
	public static final String FIELD_NAME_2 = "monthValueAsRomanNumberFromClassBridge2";

	@Override
	public void set(String name, Object value, Document document, LuceneOptions luceneOptions) {
		Month month = (Month) value;

		luceneOptions.addFieldToDocument(
				FIELD_NAME_1,
				new RomanNumberFieldBridge().objectToString( month.getMonthValue() ),
				document
		);

		luceneOptions.addFieldToDocument(
				FIELD_NAME_2,
				new RomanNumberFieldBridge().objectToString( month.getMonthValue() ),
				document
		);
	}
}
