/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.document.model.impl.esnative;

/**
 * An enum for Elasticsearch "fielddata" attribute values.
 * <p>
 * See https://www.elastic.co/guide/en/elasticsearch/reference/current/fielddata.html
 *
 * @author Yoann Rodiere
 */
public enum FieldDataType {

	TRUE,
	FALSE
	;
}
