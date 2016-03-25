package io.vokal.okmock;

import android.content.Context;

import java.io.IOException;

import io.vokal.mockutil.MockFile;
import io.vokal.mockutil.MockServer;
import okhttp3.*;
import okio.Buffer;

public class OkMockInterceptor implements Interceptor {

    public MockServer server;

    /**
     * Construct a mock interceptor using the root of the assets folder for mock files.
     *
     * @param ctx
     */
    public OkMockInterceptor(Context ctx) {
        this(ctx, "");
    }

    /**
     * Construct a mock interceptor using the specified asset folder path for mock files.
     *
     * @param ctx - any valid context
     * @param dir - folder path of mock asset files
     */
    public OkMockInterceptor(Context ctx, String dir) {
        server = new MockServer(ctx, dir);
    }

    private String getFileName(Request request) throws IOException {
        String body = null;
        String contentType = null;

        final Request copy = request.newBuilder().build(); 
        if (copy.body() != null && copy.body().contentLength() > 0) { 
            try {
                final Buffer buffer = new Buffer();
                copy.body().writeTo(buffer);
                body = buffer.readUtf8();

                contentType = copy.body().contentType().toString();
            } catch(NullPointerException|IOException e) {
                e.printStackTrace();
            }
        }

        return server.getFileName(request.url().toString(), request.method(), body, contentType);
    }

    private Response serve(Request request, String filename) throws IOException {
        MockFile file = server.serve(filename);
        Headers.Builder headers = new Headers.Builder();
        for (String line : file.headers) {
            headers.add(line);
        }

        return new Response.Builder()
                .request(request)
                .protocol(Protocol.HTTP_2)
                .code(file.code)
                .message(file.reason)
                .headers(headers.build())
                .body(ResponseBody.create(MediaType.parse(file.contentType), file.body))
                .build();
    }

    public Response intercept(Chain chain) throws IOException {
        String filename = getFileName(chain.request());
        String fullPath = server.determineFilePath(filename);

        Response output = null;       
        if (fullPath != null) {
            output = serve(chain.request(), fullPath);
        } else {
            output = chain.proceed(chain.request());
        }
        return output;

    }
}

