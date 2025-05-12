package core.parameter.mock;

import io.github.renatompf.ember.annotations.content.Consumes;
import io.github.renatompf.ember.annotations.content.Produces;
import io.github.renatompf.ember.annotations.parameters.PathParameter;
import io.github.renatompf.ember.annotations.parameters.QueryParameter;
import io.github.renatompf.ember.annotations.parameters.RequestBody;
import io.github.renatompf.ember.core.server.Context;
import io.github.renatompf.ember.enums.MediaType;

// Test class with annotated methods for testing
public class TestController {
    @Consumes(MediaType.APPLICATION_JSON)
    public void consumesJson() {}

    @Produces(MediaType.APPLICATION_JSON)
    public void producesJson() {}

    @Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public void consumesMultiple() {}

    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public void producesMultiple() {}

    public void noAnnotations() {}

    public void contextMethod(Context context) {}

    public void pathParamMethod(@PathParameter("id") Integer id) {}

    public void queryParamMethod(@QueryParameter("name") String name) {}

    public void bodyMethod(@RequestBody TestData model) {}

    public void unsupportedMethod(String param) {}

    public void missingPathParamMethod(@PathParameter("nonexistent") Integer id) {}

    public void missingQueryParamMethod(@QueryParameter("nonexistent") String name) {}
}