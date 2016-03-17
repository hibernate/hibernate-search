What does (probably) no work?
=============================

- Using an id fieldname for other purposes in an Entity hierarchy

What is incompatible with the approach?
======================================

- Using Inheritance that uses a common JPA id and changing the @DocumentId somewhere the class hierarchy
  (using something different than the JPA id is a bad idea anyways)