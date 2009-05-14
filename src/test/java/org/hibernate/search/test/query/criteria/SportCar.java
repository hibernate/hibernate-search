//$Id$
package org.hibernate.search.test.query.criteria;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

import org.hibernate.search.annotations.Indexed;

@Entity
@DiscriminatorValue(value = "Sport")
@Indexed
public class SportCar extends AbstractCar {
}
