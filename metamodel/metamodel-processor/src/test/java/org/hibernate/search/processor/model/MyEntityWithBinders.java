package org.hibernate.search.processor.model;

import org.hibernate.search.mapper.pojo.bridge.mapping.annotation.PropertyBinderRef;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.PropertyBinding;

@Indexed
public class MyEntityWithBinders {
	@DocumentId
	private String id;
	@PropertyBinding(binder = @PropertyBinderRef(type = SomeRandomTypeBinder.class))
	private SomeRandomType someRandomType;
}
