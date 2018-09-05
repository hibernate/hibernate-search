/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.test.bridge;

import java.util.HashMap;
import java.util.Map;

import org.hibernate.search.annotations.Analyze;
import org.hibernate.search.annotations.DocumentId;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.FieldBridge;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.Norms;
import org.hibernate.search.annotations.Parameter;
import org.hibernate.search.annotations.Store;

/**
 * @author Davide D'Alto
 */
@Indexed(index = ElasticsearchDynamicIndexedValueHolder.INDEX_NAME)
public class ElasticsearchDynamicIndexedValueHolder {

	public static final String INDEX_NAME = "elasticsearchDynamicIndex";

	@DocumentId
	public final String id;

	@Field(norms = Norms.NO, store = Store.YES, analyze = Analyze.YES, name = "dynamicField")
	@FieldBridge(
			impl = MapAsInnerObjectFieldBridge.class,
			params = @Parameter(name = MapAsInnerObjectFieldBridge.DYNAMIC, value = "true")
	)
	private Map<String, String> dynamicFields = new HashMap<String, String>();

	@Field(norms = Norms.NO, store = Store.YES, analyze = Analyze.YES, name = "strictField")
	@FieldBridge(impl = MapAsInnerObjectFieldBridge.class)
	private Map<String, String> strictFields = new HashMap<String, String>();

	public ElasticsearchDynamicIndexedValueHolder(String id) {
		this.id = id;
	}

	public ElasticsearchDynamicIndexedValueHolder dynamicProperty(String key, String value) {
		dynamicFields.put( key, value );
		return this;
	}

	public Map<String, String> getDynamicFields() {
		return dynamicFields;
	}

	/*
	 * Add fields to a property that does not enable dynamic mapping: it will cause an exception at some point
	 */
	public ElasticsearchDynamicIndexedValueHolder strictProperty(String key, String value) {
		strictFields.put( key, value );
		return this;
	}
}
