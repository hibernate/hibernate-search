package org.hibernate.search.mapper.pojo.model.spi;

public interface PojoTypeContext<T> {

	PojoRawTypeIdentifier<T> typeIdentifier();

}
