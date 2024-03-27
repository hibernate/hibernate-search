/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.identity.impl;

import java.util.Map;

import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.IdentifierBinder;
import org.hibernate.search.mapper.pojo.model.path.impl.BoundPojoModelPathPropertyNode;

public interface PojoIdentityMappingCollector {

	<T> void identifierBridge(BoundPojoModelPathPropertyNode<?, T> modelPath,
			IdentifierBinder binder, Map<String, Object> params);
}
