package core.di.mock;

import io.github.renatompf.ember.annotations.service.Service;

@Service
public class ServiceWithPrivateConstructorAndFinalFields {

    private final UnannotatedService service;

    private ServiceWithPrivateConstructorAndFinalFields(UnannotatedService service) {
        this.service = service;
    }

}
