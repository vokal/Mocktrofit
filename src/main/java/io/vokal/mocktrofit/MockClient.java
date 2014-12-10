package io.vokal.mocktrofit;

import android.content.Context;
import android.support.v4.util.SimpleArrayMap;

import java.io.*;
import java.net.URLEncoder;
import java.util.*;

import retrofit.client.*;
import retrofit.mime.TypedByteArray;

import static java.lang.String.CASE_INSENSITIVE_ORDER;

public class MockClient implements Client {

    public static final String BASE_URL = "https://mock.vokal.io";
    private static final String BASE_RGX = "https://mock\\.vokal\\.io";

    private Context mContext;
    private String mMockDir;

    private SimpleArrayMap<String, String> mRouteMap = new SimpleArrayMap<String, String>();

    public MockClient(Context aContext) {
        this(aContext, "");
    }

    public MockClient(Context aContext, String aDirectory) {
        mContext = aContext.getApplicationContext();
        if (mContext == null) mContext = aContext;

        mMockDir = aDirectory;
    }

    private String alphabetizeAndEncode(String params) {
        TreeMap<String, String> treemap = new TreeMap<String, String>(CASE_INSENSITIVE_ORDER);
        String[] pairs = params.split("&");
        for (String pair : pairs) {
            String[] vals = pair.split("=");
            treemap.put(vals[0], vals[1]);
        }

        StringBuilder output = new StringBuilder();
        for (String key : treemap.keySet()) {
            output.append(key).append("=").append(treemap.get(key));
            if (key.equals(treemap.lastKey())) {
                output.append("?");
            }
        }

        try {
            return URLEncoder.encode(output.toString(), "UTF-8");
        } catch(UnsupportedEncodingException e) {
            return output.toString();
        }
    }

    private String getFileName(Request request) {
        String path = request.getUrl().replaceFirst(BASE_RGX, "");

        String[] parts = path.split("?");
        path = parts[0];

        String url = String.format("%s|%s", request.getMethod(), path.replace("[/:]", "-"));

        if (parts.length > 1) {
            url = String.format("%s|%s", url, alphabetizeAndEncode(parts[1]));
        }

        return url;
    }

    private String findFile(String filename) throws IOException {
        String[] list = mContext.getAssets().list(mMockDir);
        for (String path : list) {
            String regex = String.format("%s\\..+");
            if (path.matches(regex)) {
                return path;
            }
        }

        return null;
    }

    private Response serve(String filename) throws IOException {
        InputStream is = mContext.getAssets().open(filename);
        ArrayList<Header> headers = new ArrayList<Header>();

        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        String def = reader.readLine();
        String line = reader.readLine();

        String contentType = null;

        while(!line.trim().isEmpty()) {
            String[] header = line.split(":", 2);
            headers.add(new Header(header[0], header[1].trim()));
            if (header[0].toLowerCase(Locale.US).equals("content-type")) {
                contentType = header[1].trim();
            }
        }

        // Read the rest of the file
        char[] arr = new char[8 * 1024];
        StringBuilder buffer = new StringBuilder();
        int numCharsRead;
        while ((numCharsRead = reader.read(arr, 0, arr.length)) != -1) {
            buffer.append(arr, 0, numCharsRead);
        }

        String[] parts = def.split("\\w", 3);
        int code = 500;
        String reason = "Internal Mocking Error";

        if (parts.length == 3) {
            code = Integer.getInteger(parts[1], 500);
            reason = parts[3];
        }

        // TODO: Parse response info
        return new Response(filename, code, reason, headers, 
            new TypedByteArray(contentType, buffer.toString().getBytes()));
    }

    @Override
    public Response execute(Request request) throws IOException {

        String filename = getFileName(request);
        String fullPath;
        if (mRouteMap.containsKey(filename)) {
            fullPath = mRouteMap.get(filename);
        } else {
            fullPath = findFile(filename);
        }

        Response output = new Response("", 404, "Not Found", Collections.EMPTY_LIST, null);       
        if (fullPath != null) {
            output = serve(fullPath);
        }
        return output;
    }
}
