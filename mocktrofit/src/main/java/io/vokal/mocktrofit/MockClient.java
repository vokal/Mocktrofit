package io.vokal.mocktrofit;

import android.content.Context;
import android.util.Log;

import java.io.*;
import java.net.URLEncoder;
import java.math.BigInteger;
import java.util.*;
import java.security.*;

import retrofit.client.*;
import retrofit.mime.TypedByteArray;

import io.vokal.mockutil.*;

public class MockClient implements Client {

    public MockServer server;

    public MockClient(Context aContext) {
        this(aContext, "");
    }

    public MockClient(Context aContext, String aDirectory) {
        server = new MockServer(aContext, aDirectory);
    }

    private String getFileName(Request request) {
        String body = null;
        String contentType = null;

        if (request.getBody() != null && request.getBody().length() > 0) { 
            try {
                ByteArrayOutputStream out = new ByteArrayOutputStream((int) request.getBody().length());
                request.getBody().writeTo(out);
                body = new String(out.toByteArray(), "UTF-8");
                contentType = request.getBody().mimeType();
            } catch(NullPointerException|IOException e) {
                e.printStackTrace();
            }
        }

        return server.getFileName(request.getUrl(), request.getMethod(), body, contentType);
    }

    private Response serve(String filename) throws IOException {
        MockFile file = server.serve(filename);
        ArrayList<Header> headers = new ArrayList<Header>();
        for (String line : file.headers) {
            String[] header = line.split(":", 2);
            headers.add(new Header(header[0], header[1].trim()));
        }

        return new Response(file.name, file.code, file.reason, headers, 
            new TypedByteArray(file.contentType, file.body.getBytes()));
    }

    @Override
    public Response execute(Request request) throws IOException {
        String filename = getFileName(request);
        String fullPath = server.determineFilePath(filename);

        Response output = new Response(filename, 404, "Not Found", Collections.EMPTY_LIST, null);       
        if (fullPath != null) {
            output = serve(fullPath);
        }
        return output;
    }
}
