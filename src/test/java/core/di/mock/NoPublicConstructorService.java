package core.di.mock;

import io.github.renatompf.ember.annotations.service.Service;

@Service
public class NoPublicConstructorService {
    private final String name;

    private NoPublicConstructorService(String name) {
        this.name = name;
    }
}