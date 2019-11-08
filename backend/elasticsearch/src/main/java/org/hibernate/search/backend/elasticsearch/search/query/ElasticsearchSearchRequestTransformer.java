/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.query;

/**
 * A pluggable component that gets the chance to transform search requests (path, body, ...)
 * before they are sent to Elasticsearch.
 * <p>
 * <strong>WARNING:</strong> Direct changes to the request may conflict with Hibernate Search features
 * and be supported differently by different versions of Elasticsearch.
 * Thus they cannot be guaranteed to continue to work when upgrading Hibernate Search,
 * even for micro upgrades ({@code x.y.z} to {@code x.y.(z+1)}).
 * Use this at your own risk.
 *
 * @hsearch.experimental This type is under active development.
 *    Usual compatibility policies do not apply: incompatible changes may be introduced in any future release.
 */
public interface ElasticsearchSearchRequestTransformer {

	void transform(ElasticsearchSearchRequestTransformerContext context);

}
