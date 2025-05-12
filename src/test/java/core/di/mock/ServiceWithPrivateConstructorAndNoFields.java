package core.di.mock;

import io.github.renatompf.ember.annotations.service.Service;

@Service
public class ServiceWithPrivateConstructorAndNoFields {

    private ServiceWithPrivateConstructorAndNoFields() {
    }

}
