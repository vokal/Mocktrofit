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

Notes
----
If you are unsure what to name your mocks, check the logs.  It will include both the expected name and the hash in case the filename is too long for your file system.


Limitations
-----

Mocktrofit utilizes the names of mock files to determine the path and parameters of the request.  
Due to limitations in the Android Asset system, we need a compile time processor to hash the names such that the name is always a consistent length.
Because of this, there are still some unimplemented features that [VOKMockUrlProtocol](https://github.com/vokal/VOKMockUrlProtocol) has.

Implemented

 * Basic Mocks ending with .http
 * Mocks in both App and Test applicatons will work properly
 * Double hashing for long names (Most operating systems have a 255 character limit)

Unimplemented

 * .json and .xml file names
 * Fallback hashing of body and path
