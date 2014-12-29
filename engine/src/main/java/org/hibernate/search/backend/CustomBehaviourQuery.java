/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend;

/**
 * @author Martin Braun
 */
public class CustomBehaviourQuery implements DeletionQuery {

	public static final int QUERY_KEY = 3;

	private final String behaviourClass;
	private final Object data;

	public CustomBehaviourQuery(Class<? extends CustomBehaviour> behaviourClass, Object data) {
		this( behaviourClass.getName(), data );
	}

	public CustomBehaviourQuery(String behaviourClass, Object data) {
		this.behaviourClass = behaviourClass;
		this.data = data;
	}

	/*
	 * (non-Javadoc)
	 * @see org.hibernate.search.backend.DeletionQuery#getQueryKey()
	 */
	@Override
	public int getQueryKey() {
		return QUERY_KEY;
	}

	public String getBehaviourClass() {
		return behaviourClass;
	}

	public Object getData() {
		return data;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ( ( behaviourClass == null ) ? 0 : behaviourClass.hashCode() );
		result = prime * result + ( ( data == null ) ? 0 : data.hashCode() );
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if ( this == obj ) {
			return true;
		}
		if ( obj == null ) {
			return false;
		}
		if ( getClass() != obj.getClass() ) {
			return false;
		}
		CustomBehaviourQuery other = (CustomBehaviourQuery) obj;
		if ( behaviourClass == null ) {
			if ( other.behaviourClass != null ) {
				return false;
			}
		}
		else if ( !behaviourClass.equals( other.behaviourClass ) ) {
			return false;
		}
		if ( data == null ) {
			if ( other.data != null ) {
				return false;
			}
		}
		else if ( !data.equals( other.data ) ) {
			return false;
		}
		return true;
	}

}
