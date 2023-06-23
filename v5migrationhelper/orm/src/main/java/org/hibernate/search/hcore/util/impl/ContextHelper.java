/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.hcore.util.impl;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.search.impl.V5MigrationOrmSearchIntegratorAdapter;
import org.hibernate.search.mapper.orm.Search;

/**
 * Static helper class to retrieve the instance of the current {@code SearchIntegrator} / {@code ExtendedSearchIntegrator}.
 *
 * <p>
 * <b>Note</b>:<br>
 * The use of this class is discouraged. If possible should {@link org.hibernate.search.FullTextSession#getSearchFactory()}
 * be used. However, this is not always possible, for example in FullTextSessionImpl itself.
 * </p>
 *
 * @author Emmanuel Bernard
 */
public class ContextHelper {

	private ContextHelper() {
	}

	public static V5MigrationOrmSearchIntegratorAdapter getSearchIntegrator(Session session) {
		return getSearchIntegratorBySessionImplementor( (SessionImplementor) session );
	}

	public static V5MigrationOrmSearchIntegratorAdapter getSearchIntegratorBySessionImplementor(SessionImplementor session) {
		return getSearchIntegratorBySFI( session.getFactory() );
	}

	public static V5MigrationOrmSearchIntegratorAdapter getSearchIntegratorBySF(SessionFactory factory) {
		return getSearchIntegratorBySFI( (SessionFactoryImplementor) factory );
	}

	public static V5MigrationOrmSearchIntegratorAdapter getSearchIntegratorBySFI(SessionFactoryImplementor sfi) {
		return new V5MigrationOrmSearchIntegratorAdapter( Search.mapping( sfi ) );
	}

}
