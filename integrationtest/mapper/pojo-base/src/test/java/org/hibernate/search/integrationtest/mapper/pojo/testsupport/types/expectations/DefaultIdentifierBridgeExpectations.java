/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.pojo.testsupport.types.expectations;

import java.util.List;

public interface DefaultIdentifierBridgeExpectations<I> {
	String TYPE_WITH_IDENTIFIER_BRIDGE_1_NAME = "TypeWithIdentifierBridge1Name";
	String TYPE_WITH_IDENTIFIER_BRIDGE_2_NAME = "TypeWithIdentifierBridge2Name";

	/**
	 * @return The type returned by projections. For primitive types, this is the corresponding boxed type.
	 * For other types, this is the type itself.
	 */
	Class<I> getProjectionType();

	List<I> getEntityIdentifierValues();

	List<String> getDocumentIdentifierValues();

	Class<?> getTypeWithIdentifierBridge1();

	Object instantiateTypeWithIdentifierBridge1(I identifier);

	Class<?> getTypeWithIdentifierBridge2();
}
