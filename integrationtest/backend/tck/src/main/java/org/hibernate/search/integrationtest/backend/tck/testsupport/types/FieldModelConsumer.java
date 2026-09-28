package org.hibernate.search.integrationtest.backend.tck.testsupport.types;

public interface FieldModelConsumer<E, M> {

	void accept(FieldTypeDescriptor<?, ?> typeDescriptor, E expectations, M fieldModel);

}
