/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.pojo.mapping;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.invoke.MethodHandles;
import java.util.Collection;
import java.util.regex.Pattern;

import org.hibernate.search.mapper.pojo.mapping.definition.annotation.AssociationInverseSide;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.ObjectPath;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.PropertyValue;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendMock;
import org.hibernate.search.util.impl.integrationtest.mapper.pojo.standalone.StandalonePojoMappingSetupHelper;

import org.junit.Rule;
import org.junit.Test;

public class IncorrectPropertyNameObjectPathIT {
	private static final String BROKEN_PATH_WITH_DOTS = "broken.path.with.dots";
	@Rule
	public BackendMock defaultBackendMock = new BackendMock();

	@Rule
	public StandalonePojoMappingSetupHelper setupHelper = StandalonePojoMappingSetupHelper.withBackendMock(
			MethodHandles.lookup(),
			defaultBackendMock
	);

	@Test
	public void brokenPathWithDotsThrowsException() {
		assertThatThrownBy( () -> {
			setupHelper.start()
					.withAnnotatedEntityType( Person.class, Person.ENTITY_NAME )
					.withAnnotatedEntityType( PhoneNumber.class, PhoneNumber.ENTITY_NAME )
					.setup();
		} ).isInstanceOf( SearchException.class )
				.hasMessageFindingMatch( "propertyName=.?" + Pattern.quote( BROKEN_PATH_WITH_DOTS ) + ".?" )
				.hasMessageContainingAll(
						ObjectPath.class.getName(),
						PropertyValue.class.getName(),
						"Invalid ObjectPath encountered",
						"Property name '" + BROKEN_PATH_WITH_DOTS + "' cannot contain dots."
				);
	}


	@Indexed(index = Person.INDEX_NAME)
	private static class Person {
		public static final String ENTITY_NAME = "PersonEntity";
		public static final String INDEX_NAME = "Person";

		@DocumentId
		private Integer id;
		@GenericField
		private String name;

		private Collection<PhoneNumber> phoneNumbers;

		public Person() {
		}

		public Integer getId() {
			return id;
		}

		public String getName() {
			return name;
		}
	}

	private static class PhoneNumber {
		public static final String ENTITY_NAME = "PhoneNumber";

		private Integer id;
		@GenericField
		private String number;
		@AssociationInverseSide(inversePath = @ObjectPath(@PropertyValue(propertyName = BROKEN_PATH_WITH_DOTS)))
		private Person owner;

		public PhoneNumber() {
		}

		public Integer getId() {
			return id;
		}

		public String getNumber() {
			return number;
		}

		public Person getOwner() {
			return owner;
		}
	}
}
