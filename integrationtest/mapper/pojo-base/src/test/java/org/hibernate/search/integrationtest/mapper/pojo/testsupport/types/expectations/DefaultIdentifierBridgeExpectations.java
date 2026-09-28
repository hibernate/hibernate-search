package org.hibernate.search.integrationtest.mapper.pojo.testsupport.types.expectations;

public interface DefaultIdentifierBridgeExpectations<I> {
	String TYPE_WITH_IDENTIFIER_BRIDGE_1_NAME = "TypeWithIdentifierBridge1Name";
	String TYPE_WITH_IDENTIFIER_BRIDGE_2_NAME = "TypeWithIdentifierBridge2Name";

	Class<?> getTypeWithIdentifierBridge1();

	Object instantiateTypeWithIdentifierBridge1(I identifier);

	Class<?> getTypeWithIdentifierBridge2();
}
