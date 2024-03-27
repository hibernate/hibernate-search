/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */

package org.hibernate.search.test.util.logging;

import org.jboss.logging.BasicLogger;
import org.jboss.logging.annotations.MessageLogger;

@MessageLogger(projectCode = "HSEARCH")
public interface Log extends BasicLogger {

}
