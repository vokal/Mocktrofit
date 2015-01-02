Mocktrofit
==========

Mocking Client for Retrofit

Usage
-----

Include the mocktrofit build time plugin

```
classpath 'io.vokal.gradle:mocktrofit-processor:0.1.0'
```

Apply the plugin

```
apply plugin: 'io.vokal.mocktrofit'
```

Include Mocktrofit

```
compile 'io.vokal:mocktrofit:0.2.1'
```

Use the Client

```java
  RestAdapter restAdapter = new RestAdapter.Builder()
            .setEndpoint(MockClient.BASE_URL)
            .setClient(new MockClient(getContext(), "mocks"))
            .build();
```
