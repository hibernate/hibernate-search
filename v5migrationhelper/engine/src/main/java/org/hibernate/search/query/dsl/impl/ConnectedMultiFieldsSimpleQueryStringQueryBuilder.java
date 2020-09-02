/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.search.query.dsl.impl;

import org.apache.lucene.search.Query;
import org.hibernate.search.query.dsl.SimpleQueryStringTermination;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;
import java.lang.invoke.MethodHandles;

/**
 * @author Guillaume Smet
 */
public class ConnectedMultiFieldsSimpleQueryStringQueryBuilder implements SimpleQueryStringTermination {

	private static final Log LOG = LoggerFactory.make( MethodHandles.lookup() );

	private final String simpleQueryString;
	private final QueryCustomizer queryCustomizer;
	private final FieldsContext fieldsContext;
	private final QueryBuildingContext queryContext;

	private final boolean withAndAsDefaultOperator;

	public ConnectedMultiFieldsSimpleQueryStringQueryBuilder(String simpleQueryString,
			FieldsContext fieldsContext,
			boolean withAndAsDefaultOperator,
			QueryCustomizer queryCustomizer,
			QueryBuildingContext queryContext) {
		this.simpleQueryString = simpleQueryString;
		this.queryContext = queryContext;
		this.queryCustomizer = queryCustomizer;
		this.fieldsContext = fieldsContext;

		this.withAndAsDefaultOperator = withAndAsDefaultOperator;
	}

	@Override
	public Query createQuery() {
		throw new UnsupportedOperationException( "To be implemented through the Search 6 DSL" );
	}

}
