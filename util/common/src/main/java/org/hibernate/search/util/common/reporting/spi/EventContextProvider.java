/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.util.common.reporting.spi;

import org.hibernate.search.util.common.reporting.EventContext;

public interface EventContextProvider {

	EventContext eventContext();

}
