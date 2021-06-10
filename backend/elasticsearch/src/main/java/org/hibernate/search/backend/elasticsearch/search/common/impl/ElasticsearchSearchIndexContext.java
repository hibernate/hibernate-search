/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.common.impl;

import org.hibernate.search.backend.elasticsearch.index.layout.impl.IndexNames;
import org.hibernate.search.engine.backend.types.converter.spi.DocumentIdentifierValueConverter;

/**
 * Information about an index targeted by search,
 * be it in a projection, a predicate, a sort, ...
 */
public interface ElasticsearchSearchIndexContext {

	IndexNames names();

	String mappedTypeName();

	DocumentIdentifierValueConverter<?> idDslConverter();

	int maxResultWindow();

}
