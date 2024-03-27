/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.mapper.orm.search.loading.model.singletype;

import java.util.List;

final class FetchSubSelectModel extends SingleTypeLoadingModel<FetchSubSelectIndexedEntity> {

	@Override
	protected String describe() {
		return "fetch-subselect";
	}

	@Override
	public String getIndexName() {
		return FetchSubSelectIndexedEntity.NAME;
	}

	@Override
	public Class<FetchSubSelectIndexedEntity> getIndexedClass() {
		return FetchSubSelectIndexedEntity.class;
	}

	@Override
	public Class<?> getContainedClass() {
		return FetchSubSelectContainedEntity.class;
	}

	@Override
	public String getIndexedEntityName() {
		return FetchSubSelectIndexedEntity.NAME;
	}

	@Override
	public String getEagerGraphName() {
		return FetchSubSelectIndexedEntity.GRAPH_EAGER;
	}

	@Override
	public String getLazyGraphName() {
		return FetchSubSelectIndexedEntity.GRAPH_LAZY;
	}

	@Override
	public FetchSubSelectIndexedEntity newIndexed(int id, SingleTypeLoadingMapping mapping) {
		return new FetchSubSelectIndexedEntity( id, mapping.generateUniquePropertyForEntityId( id ) );
	}

	@Override
	public FetchSubSelectIndexedEntity newIndexedWithContained(int id, SingleTypeLoadingMapping mapping) {
		FetchSubSelectIndexedEntity entity = newIndexed( id, mapping );
		FetchSubSelectContainedEntity containedEager = new FetchSubSelectContainedEntity( id * 10000 );
		entity.setContainedEager( containedEager );
		containedEager.setContainingEager( entity );
		FetchSubSelectContainedEntity containedLazy = new FetchSubSelectContainedEntity( id * 10000 + 1 );
		entity.getContainedLazy().add( containedLazy );
		containedLazy.setContainingLazy( entity );
		return entity;
	}

	@Override
	public Object getContainedEager(FetchSubSelectIndexedEntity entity) {
		return entity.getContainedEager();
	}

	@Override
	public void clearContainedEager(FetchSubSelectIndexedEntity entity) {
		entity.setContainedEager( null );
	}

	@Override
	public List<?> getContainedLazy(FetchSubSelectIndexedEntity entity) {
		return entity.getContainedLazy();
	}
}
