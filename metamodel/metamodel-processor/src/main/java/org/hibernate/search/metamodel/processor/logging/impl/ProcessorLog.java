/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.metamodel.processor.logging.impl;


import org.hibernate.search.util.common.logging.impl.MessageConstants;

import org.jboss.logging.annotations.MessageLogger;
import org.jboss.logging.annotations.ValidIdRange;

@MessageLogger(projectCode = MessageConstants.PROJECT_CODE)
@ValidIdRange(min = MessageConstants.PROCESSOR_ID_RANGE_MIN, max = MessageConstants.PROCESSOR_ID_RANGE_MAX)
public interface ProcessorLog extends MappingLog {
	int ID_OFFSET = MessageConstants.PROCESSOR_ID_RANGE_MIN;
}
