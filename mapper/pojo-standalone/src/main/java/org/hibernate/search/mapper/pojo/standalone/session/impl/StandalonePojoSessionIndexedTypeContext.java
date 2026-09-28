package org.hibernate.search.mapper.pojo.standalone.session.impl;

import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeIdentifier;

/**
 * @param <E> The entity type mapped to the index.
 */
public interface StandalonePojoSessionIndexedTypeContext<E> {

	PojoRawTypeIdentifier<E> typeIdentifier();

	String name();

}
