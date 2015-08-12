/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.bridge;

import java.util.HashMap;
import java.util.Map;

import org.hibernate.search.annotations.Analyze;
import org.hibernate.search.annotations.DocumentId;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.FieldBridge;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.Norms;
import org.hibernate.search.annotations.Store;

/**
 * @author Sanne Grinovero (C) 2013 Red Hat Inc.
 */
@Indexed(index = "all")
public class DynamicIndexedValueHolder {

	@DocumentId
	public final String id;

	@Field(norms = Norms.NO, store = Store.YES, analyze = Analyze.YES, name = "value")
	@FieldBridge(impl = MultiFieldMapBridge.class)
	private Map<String, String> values = new HashMap<String, String>();

	public DynamicIndexedValueHolder(String id) {
		this.id = id;
	}

	public DynamicIndexedValueHolder property(String key, String value) {
		values.put( key, value );
		return this;
	}

}
