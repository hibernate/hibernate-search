/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.orm.automaticindexing.spi;

import org.hibernate.Session;
import org.hibernate.SharedSessionContract;
import org.hibernate.search.engine.backend.common.spi.EntityReferenceFactory;

public interface AutomaticIndexingEventSendingSessionContext {

	EntityReferenceFactory entityReferenceFactory();

	@Deprecated(since = "9.0", forRemoval = true)
	Session session();

	SharedSessionContract sessionContract();

}
