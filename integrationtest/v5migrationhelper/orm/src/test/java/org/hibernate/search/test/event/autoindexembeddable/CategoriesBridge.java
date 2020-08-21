/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.search.test.event.autoindexembeddable;

import java.util.Map;

import org.apache.lucene.document.Document;
import org.hibernate.search.bridge.FieldBridge;
import org.hibernate.search.bridge.LuceneOptions;

/**
 * @author Hardy Ferentschik
 */
public class CategoriesBridge implements FieldBridge {
	@Override
	public void set(String name, Object value, Document document, LuceneOptions luceneOptions) {
		@SuppressWarnings("unchecked")
		Map<Long, String> categoriesValue = (Map<Long, String>) value;
		for ( String s : categoriesValue.values() ) {
			luceneOptions.addFieldToDocument( name, s, document );
		}
	}
}


