/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.query.engine.impl;

import org.apache.lucene.search.Query;
import org.hibernate.search.engine.service.spi.Service;
import org.hibernate.search.query.engine.spi.QueryDescriptor;
import org.hibernate.search.spi.IndexedTypeSet;

/**
 * Implementations translate Lucene queries into other backend-specific representations.
 * <p>
 * Not a public contract.
 *
 * @author Gunnar Morling
 */
public interface LuceneQueryTranslator extends Service {

	QueryDescriptor convertLuceneQuery(Query query);

	boolean conversionRequired(IndexedTypeSet entities);
}
