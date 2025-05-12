package core.di.mock;

import io.github.renatompf.ember.annotations.service.Service;

@Service
public class DependentService {
    private final SimpleService simpleService;

    public DependentService(SimpleService simpleService) {
        this.simpleService = simpleService;
    }

    public SimpleService getSimpleService() {
        return simpleService;
    }
}
