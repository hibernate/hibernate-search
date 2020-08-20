/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.search.query.dsl.impl;

import org.apache.lucene.search.Query;
import org.hibernate.search.query.dsl.TermTermination;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;
import java.lang.invoke.MethodHandles;

/**
 * @author Emmanuel Bernard
 */
public class ConnectedMultiFieldsTermQueryBuilder implements TermTermination {

	private static final Log log = LoggerFactory.make( MethodHandles.lookup() );

	private final Object value;
	private final QueryCustomizer queryCustomizer;
	private final TermQueryContext termContext;
	private final FieldsContext fieldsContext;
	private final QueryBuildingContext queryContext;

	public ConnectedMultiFieldsTermQueryBuilder(TermQueryContext termContext,
												Object value,
												FieldsContext fieldsContext,
												QueryCustomizer queryCustomizer,
												QueryBuildingContext queryContext) {
		this.termContext = termContext;
		this.value = value;
		this.queryContext = queryContext;
		this.queryCustomizer = queryCustomizer;
		this.fieldsContext = fieldsContext;
	}

	@Override
	public Query createQuery() {
		throw new UnsupportedOperationException( "To be implemented through the Search 6 DSL" );
	}

}
