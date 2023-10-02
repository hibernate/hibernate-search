/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.identity.spi;

import org.hibernate.search.mapper.pojo.bridge.runtime.spi.BridgeSessionContext;

public interface IdentifierMapping {

	Object fromDocumentIdentifier(String documentId, BridgeSessionContext sessionContext);

}
