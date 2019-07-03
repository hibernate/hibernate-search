/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.orm;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import org.hibernate.BaseSessionEventListener;
import org.hibernate.Session;
import org.hibernate.SessionEventListener;
import org.hibernate.SessionFactory;
import org.hibernate.resource.jdbc.spi.StatementInspector;

import org.assertj.core.api.AbstractIntegerAssert;
import org.assertj.core.api.AutoCloseableSoftAssertions;

public class OrmSoftAssertions extends AutoCloseableSoftAssertions {

	public static void withinSession(SessionFactory sessionFactory,
			BiConsumer<Session, OrmSoftAssertions> action) {
		try ( OrmSoftAssertions softAssertions = new OrmSoftAssertions() ) {
			try ( Session session = sessionFactory.withOptions()
					.eventListeners( softAssertions.sessionEventListener )
					.statementInspector( softAssertions.statementInspector )
					.openSession() ) {
				action.accept( session, softAssertions );
			}
		}
	}

	private final StatementInspector statementInspector;
	private final SessionEventListener sessionEventListener;

	private int statementExecutionCount = 0;
	private final List<String> statements = new ArrayList<>();

	private OrmSoftAssertions() {
		sessionEventListener = new BaseSessionEventListener() {
			@Override
			public void jdbcPrepareStatementStart() {
				++statementExecutionCount;
			}
		};
		statementInspector = this::inspectSql;
	}

	public void resetListenerData() {
		statementExecutionCount = 0;
		statements.clear();
	}

	public AbstractIntegerAssert<?> assertStatementExecutionCount() {
		// Don't use statement.size(), just in case a statement is executed twice... not sure it can happen, though.
		return assertThat( statementExecutionCount )
				.as( "Statement execution count for statements [\n"
						+ statements.stream().collect( Collectors.joining( "\n" ) )
						+ "\n]" );
	}

	private String inspectSql(String sql) {
		statements.add( sql );
		return sql;
	}

}
