package org.hibernate.search.engine.mapper.scope.spi;


public interface MappedIndexScopeBuilder<SR, R, E> {

	MappedIndexScope<SR, R, E> build();

}
