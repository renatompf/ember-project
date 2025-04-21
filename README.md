# Ember Framework

## Overview
Ember Framework is a lightweight Java web framework designed for building RESTful APIs with simplicity and flexibility. It provides features like dependency injection, middleware support, request validation, and annotation-based development to streamline the creation of modern web applications.

## Features
- **Routing**: Flexible routing with support for path and query parameters.
- **Dependency Injection**: Built-in DI container for managing services and controllers.
- **Middleware**: Global and route-specific middleware for request handling.
- **Annotations**: Simplified development with custom annotations (`@Controller`, `@Service`, `@PathParameter`, etc.).

## Architecture

```markdown
                  +----------------------------+
                  |      EmberApplication      | <-- Entry point
                  +----------------------------+
                             |
                             | builds routes, middleware, DI container
                             v
                +----------------------------------+
                |         DIContainer              |
                |  - Registers services/controllers|
                |  - Handles constructor injection |
                +----------------------------------+
                             |
                             v
                  +------------------------+
                  |        Router          | <-- Path + method to handler
                  +------------------------+
                             |
                             v
+----------------+     +----------------------+      +-------------------+
|  Middleware[]  | --> |     MiddlewareChain   | -->  |  Route Handler(s) |
+----------------+     +----------------------+      +-------------------+
                             ^
                             |
                      +--------------+
                      |   Server     | <-- Starts and handles HttpServer
                      +--------------+
                             |
                             v
                     [ Incoming Request ]
                             |
                             v
                 +--------------------------+
                 |        Context           | <-- Holds request state, response, etc.
                 +--------------------------+
                             |
                             v
                 +--------------------------+
                 |     Final Response       |
                 +--------------------------+

```


## Installation
To use Ember Framework in your project, add the following Maven dependency to your `pom.xml`:

```xml
<dependency>
    <groupId>io.ember</groupId>
    <artifactId>ember-framework</artifactId>
    <version>1.0.0</version>
</dependency>
```

## Getting Started

### 1. Define a Controller
Create a controller to handle HTTP requests:

```java
package io.ember.examples;

import io.ember.annotations.controller.Controller;
import io.ember.annotations.http.Get;
import io.ember.core.ContextHolder;

@Controller("/example")
public class ExampleController {

    @Get("/hello")
    public void sayHello() {
        ContextHolder.context().response().ok("Hello, World!");
    }
}
```

### 2. Run the Application
Start the server and access the endpoint:

```java
package io.ember.examples;

import io.ember.core.EmberApplication;

public class Application {
    public static void main(String[] args) {
        EmberApplication app = new EmberApplication();
        app.start(8080);
    }
}
```

Access the endpoint at `http://localhost:8080/example/hello`.

## Documentation

### Annotations
- `@Controller`: Marks a class as a controller.
- `@Service`: Marks a class as a service.
- `@Get`, `@Post`, `@Put`, ...: Maps HTTP methods to controller methods.
- `@PathParameter`: Binds a path parameter to a method argument.
- `@QueryParameter`: Binds a query parameter to a method argument.
- `@RequestBody`: Binds the request body to a method argument.

### Middleware
Use `@WithMiddleware` to apply middleware globally or to specific routes.


## Contributing
Contributions are welcome! Please follow these steps:
1. Fork the repository.
2. Create a new branch for your feature or bug fix.
3. Submit a pull request with a detailed description of your changes.

## License
This project is licensed under the Apache 2.0 License. See the `LICENSE` file for details.
```
