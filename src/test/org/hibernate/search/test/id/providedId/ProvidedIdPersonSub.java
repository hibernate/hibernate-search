package org.hibernate.search.test.id.providedId;

import javax.persistence.Entity;

import org.hibernate.search.annotations.Indexed;

/**
 * @author Navin Surtani (<a href="mailto:nsurtani@redhat.com">nsurtani@redhat.com</a>)
 */
@Indexed
@Entity
public class ProvidedIdPersonSub extends ProvidedIdPerson {   
}
