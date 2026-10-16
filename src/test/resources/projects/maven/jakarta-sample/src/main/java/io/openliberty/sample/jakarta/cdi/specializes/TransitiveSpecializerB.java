package io.openliberty.sample.jakarta.cdi.specializes;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Specializes;

/** Valid: linear transitive chain — B specializes A, A specializes Base. Only one direct subclass at each level. */
@Specializes
@ApplicationScoped
public class TransitiveSpecializerB extends TransitiveSpecializerA {

    @Override
    public void execute() {
        System.out.println("Transitive specializer B execution");
    }
}
