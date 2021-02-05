/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.orm.search.loading.model.singletype;

import java.util.List;

final class BasicModel extends SingleTypeLoadingModel<BasicIndexedEntity> {

	@Override
	protected String describe() {
		return "basic";
	}

	@Override
	public String getIndexName() {
		return BasicIndexedEntity.NAME;
	}

	@Override
	public Class<BasicIndexedEntity> getIndexedClass() {
		return BasicIndexedEntity.class;
	}

	@Override
	public Class<?> getContainedClass() {
		return BasicContainedEntity.class;
	}

	@Override
	public String getIndexedEntityName() {
		return BasicIndexedEntity.NAME;
	}

	@Override
	public String getEagerGraphName() {
		return BasicIndexedEntity.GRAPH_EAGER;
	}

	@Override
	public String getLazyGraphName() {
		return BasicIndexedEntity.GRAPH_LAZY;
	}

	@Override
	public BasicIndexedEntity newIndexed(int id, SingleTypeLoadingMapping mapping) {
		return new BasicIndexedEntity( id, mapping.generateUniquePropertyForEntityId( id ) );
	}

	@Override
	public BasicIndexedEntity newIndexedWithContained(int id, SingleTypeLoadingMapping mapping) {
		BasicIndexedEntity entity = newIndexed( id, mapping );
		BasicContainedEntity containedEager = new BasicContainedEntity( id * 10000 );
		entity.setContainedEager( containedEager );
		containedEager.setContainingEager( entity );
		BasicContainedEntity containedLazy = new BasicContainedEntity( id * 10000 + 1 );
		entity.getContainedLazy().add( containedLazy );
		containedLazy.setContainingLazy( entity );
		return entity;
	}

	@Override
	public Object getContainedEager(BasicIndexedEntity entity) {
		return entity.getContainedEager();
	}

	@Override
	public List<?> getContainedLazy(BasicIndexedEntity entity) {
		return entity.getContainedLazy();
	}
}
