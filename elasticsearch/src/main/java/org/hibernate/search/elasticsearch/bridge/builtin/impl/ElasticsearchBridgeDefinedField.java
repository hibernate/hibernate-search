/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.bridge.builtin.impl;

import org.hibernate.search.elasticsearch.cfg.DynamicType;

/**
 * Contains details of the field that only make sense with Elasticsearch.
 *
 * @author Davide D'Alto
 */
public class ElasticsearchBridgeDefinedField {

	private DynamicType dynamic;

	public DynamicType getDynamic() {
		return dynamic;
	}

	public void setDynamic(DynamicType dynamic) {
		this.dynamic = dynamic;
	}

}
