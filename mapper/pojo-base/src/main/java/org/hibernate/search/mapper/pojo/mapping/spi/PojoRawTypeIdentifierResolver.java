package org.hibernate.search.mapper.pojo.mapping.spi;

import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeIdentifier;
import org.hibernate.search.util.common.data.spi.KeyValueProvider;

public interface PojoRawTypeIdentifierResolver {

	KeyValueProvider<String, PojoRawTypeIdentifier<?>> typeIdentifierByEntityName();

	KeyValueProvider<String, PojoRawTypeIdentifier<?>> typeIdentifierBySecondaryEntityName();

}
