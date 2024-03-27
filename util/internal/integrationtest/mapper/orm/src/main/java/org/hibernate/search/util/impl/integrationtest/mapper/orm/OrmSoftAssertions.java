/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.util.impl.integrationtest.mapper.orm;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import org.hibernate.BaseSessionEventListener;
import org.hibernate.Session;
import org.hibernate.SessionEventListener;
import org.hibernate.SessionFactory;
import org.hibernate.resource.jdbc.spi.StatementInspector;
import org.hibernate.stat.Statistics;

import org.assertj.core.api.AbstractIntegerAssert;
import org.assertj.core.api.AbstractLongAssert;
import org.assertj.core.api.AutoCloseableSoftAssertions;

public class OrmSoftAssertions extends AutoCloseableSoftAssertions {

	public static void assertWithinSession(SessionFactory sessionFactory,
			BiConsumer<Session, OrmSoftAssertions> action) {
		try ( OrmSoftAssertions softAssertions = new OrmSoftAssertions( sessionFactory ) ) {
			try ( Session session = sessionFactory.withOptions()
					.eventListeners( softAssertions.sessionEventListener )
					.statementInspector( softAssertions.statementInspector )
					.openSession() ) {
				action.accept( session, softAssertions );
			}
		}
	}

	private final Statistics statistics;
	private final StatementInspector statementInspector;
	private final SessionEventListener sessionEventListener;

	private int statementExecutionCount = 0;
	private final List<String> statements = new ArrayList<>();

	private OrmSoftAssertions(SessionFactory sessionFactory) {
		statistics = sessionFactory.getStatistics();
		statistics.setStatisticsEnabled( true );
		statistics.clear();
		sessionEventListener = new BaseSessionEventListener() {
			@Override
			public void jdbcPrepareStatementStart() {
				++statementExecutionCount;
			}
		};
		statementInspector = this::inspectSql;
	}

	public void resetListenerData() {
		statistics.clear();
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

	public AbstractLongAssert<?> assertEntityLoadCount() {
		return assertThat( statistics.getEntityLoadCount() )
				.as( "Entity load count" );
	}

	public AbstractLongAssert<?> assertSecondLevelCacheHitCount() {
		return assertThat( statistics.getSecondLevelCacheHitCount() )
				.as( "Second level cache hit count" );
	}

	private String inspectSql(String sql) {
		statements.add( sql );
		return sql;
	}

}
