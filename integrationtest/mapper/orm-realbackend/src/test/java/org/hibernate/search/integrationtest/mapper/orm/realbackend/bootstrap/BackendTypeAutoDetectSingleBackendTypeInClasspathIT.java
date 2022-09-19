/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.orm.realbackend.bootstrap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.search.util.impl.integrationtest.common.rule.BackendConfiguration.BACKEND_TYPE;
import static org.hibernate.search.util.impl.integrationtest.common.rule.BackendConfiguration.IS_IDE;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import org.hibernate.SessionFactory;
import org.hibernate.search.integrationtest.mapper.orm.realbackend.testsupport.BackendConfigurations;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmSetupHelper;

import org.junit.Rule;
import org.junit.Test;

/**
 * Checks that Hibernate Search will auto-detect the backend type when the property "hibernate.search.backend.type" is not set
 * and there is only one backend in the classpath.
 */
public class BackendTypeAutoDetectSingleBackendTypeInClasspathIT {

	@Rule
	public OrmSetupHelper ormSetupHelper = OrmSetupHelper.withSingleBackend( BackendConfigurations.simple() );

	@Test
	public void backendType_autoDetect() {
		if ( IS_IDE ) {
			throw new IllegalStateException( "Tests seem to be running from an IDE."
					+ " This test cannot run from the IDE"
					+ " as it requires a very specific classpath (with a single backend type)." );
		}

		SessionFactory sessionFactory = ormSetupHelper.start()
				.withBackendProperty( "type", null )
				.setup( IndexedEntity.class );

		assertThat( Search.mapping( sessionFactory ).backend().getClass().getName() )
				.containsIgnoringCase( BACKEND_TYPE );
	}

	@Entity(name = IndexedEntity.NAME)
	@Indexed(index = IndexedEntity.NAME)
	public static final class IndexedEntity {

		static final String NAME = "indexed";

		@Id
		private Integer id;

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}
	}
}
