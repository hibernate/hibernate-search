/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.backend.work.execution.spi;

import org.hibernate.search.engine.backend.document.DocumentElement;

public interface DocumentContributor {

	void contribute(DocumentElement state);

}
