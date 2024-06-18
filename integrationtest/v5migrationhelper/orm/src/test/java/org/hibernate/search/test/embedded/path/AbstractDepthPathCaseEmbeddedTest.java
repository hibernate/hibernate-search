/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */

package org.hibernate.search.test.embedded.path;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.search.test.SearchTestBase;

public abstract class AbstractDepthPathCaseEmbeddedTest extends SearchTestBase {

	protected final void deleteAll(Session s, Class<?>... classes) {
		Transaction tx = s.beginTransaction();
		s.createMutationQuery( "update EntityB e set e.a = null, e.indexed = null, e.skipped = null where 1=1 " )
				.executeUpdate();
		for ( Class<?> clazz : classes ) {
			s.createMutationQuery( "delete from " + clazz.getName() ).executeUpdate();
		}
		tx.commit();
	}

	protected final void persistEntity(Session s, Object... entities) {
		Transaction tx = s.beginTransaction();
		for ( Object entity : entities ) {
			s.persist( entity );
		}
		tx.commit();
	}
}
