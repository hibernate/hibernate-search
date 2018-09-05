/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.impl;

import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;

/**
 * A base class for Elasticsearch-specific document fields used to add more
 * information to a Document than it usually carries.
 *
 * <p>Implementations should implement a specific interface.
 *
 * @author Yoann Rodiere
 */
public abstract class AbstractMarkerField extends Field {

	public AbstractMarkerField() {
		this( "" );
	}

	public AbstractMarkerField(String fieldName) {
		super( fieldName, new FieldType() );
	}

}