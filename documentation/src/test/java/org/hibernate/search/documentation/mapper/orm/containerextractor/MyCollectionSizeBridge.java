package org.hibernate.search.documentation.mapper.orm.containerextractor;

import java.util.List;

import org.hibernate.search.mapper.pojo.bridge.ValueBridge;
import org.hibernate.search.mapper.pojo.bridge.runtime.ValueBridgeToIndexedValueContext;

@SuppressWarnings("rawtypes")
public class MyCollectionSizeBridge implements ValueBridge<List, Integer> {
	@Override
	public Integer toIndexedValue(List value, ValueBridgeToIndexedValueContext context) {
		return value == null ? null : value.size();
	}
}
