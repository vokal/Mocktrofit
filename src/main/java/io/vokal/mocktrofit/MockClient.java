package io.vokal.mocktrofit;

import android.content.Context;
import android.support.v4.util.SimpleArrayMap;
import android.util.Log;

import java.io.*;
import java.net.URLEncoder;
import java.math.BigInteger;
import java.util.*;
import java.security.*;

import com.google.gson.*;

import org.ardverk.coding.BencodingOutputStream;

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

    static TreeMap<String, String> alphabetizeQuery(String params) {
        TreeMap<String, String> treemap = new TreeMap<String, String>(CASE_INSENSITIVE_ORDER);
        String[] pairs = params.split("&");
        for (String pair : pairs) {
            String[] vals = pair.split("=");
            treemap.put(vals[0], vals[1]);
        }

        return treemap;
    }


    static String alphabetizeEncodeQuery(String params) {
        TreeMap<String, String> treemap = alphabetizeQuery(params);

        StringBuilder output = new StringBuilder();
        for (String key : treemap.keySet()) {
            output.append(encode(key)).append("=").append(encode(treemap.get(key)));
            if (!key.equals(treemap.lastKey())) {
                output.append("&");
            }
        }

        return output.toString();
    }

    static String encode(String value) {
        try {
            return URLEncoder.encode(value, "UTF-8");
        } catch(UnsupportedEncodingException e) {
            return value;
        }
    }


    static String encrypt(String name) {
        String sha1 = name;
        try {
            MessageDigest crypt = MessageDigest.getInstance("SHA-1");
            crypt.reset();
            crypt.update(name.getBytes("UTF-8"));
            sha1 = new BigInteger(1, crypt.digest()).toString(16);
        } catch(NoSuchAlgorithmException|UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return sha1;
    }

    static TreeMap convertBody(String contentType, String body) {

        TreeMap<String, String> result = null;
        String[] ctypeParts = contentType.split(";\\ ");
        switch (ctypeParts[0].toLowerCase()) {
            case "application/json":
                Gson gson = new GsonBuilder().create();
                result = gson.fromJson(body, TreeMap.class);
                break;
            case "application/x-www-form-urlencoded":
                result = alphabetizeQuery(body);
                break;
        }
        return result;
    }

    private String getFileName(Request request) {
        String path = request.getUrl().replaceFirst(BASE_RGX, "");
        String[] parts = path.split("\\?");
        path = parts[0].replaceAll("[:/]", "-");
        if (parts.length > 1) {
            path = path + "?" + alphabetizeEncodeQuery(parts[1]);
        }

        String url = String.format("%s#%s", request.getMethod(), path);
        String body = null;

        if (request.getBody() != null && request.getBody().length() > 0) { 
            try {
                ByteArrayOutputStream out = new ByteArrayOutputStream((int) request.getBody().length());
                request.getBody().writeTo(out);
                body = new String(out.toByteArray(), "UTF-8");

                String contentType = request.getBody().mimeType();

                TreeMap map = convertBody(contentType, body);

                out = new ByteArrayOutputStream();
                BencodingOutputStream ben = new BencodingOutputStream(out);
                ben.writeObject(map);

                body = new String(out.toByteArray(), "UTF-8");
                body = body.replaceAll("[:/]", "-");
            } catch(NullPointerException|IOException e) {
                e.printStackTrace();
            }
        }

        if (body != null) {
            url = String.format("%s#%s", url, body);
        }

        return url;
    }

    private String findFile(String filename) throws IOException {
        String[] list = mContext.getAssets().list(mMockDir);
        String regex = String.format("%s.%s", filename, "http");
        for (String path : list) {
            if (path.equals(regex)) {
                return path;
            }
        }
        return null;
    }

    private Response serve(String filename) throws IOException {
        InputStream is = mContext.getAssets().open(mMockDir + "/" + filename);
        ArrayList<Header> headers = new ArrayList<Header>();

        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        String def = reader.readLine();
        String line = reader.readLine();

        String contentType = null;

        while(!line.trim().isEmpty()) {
            String[] header = line.split(":", 2);
            headers.add(new Header(header[0], header[1].trim()));
            if (header[0].toLowerCase(Locale.getDefault()).equals("content-type")) {
                contentType = header[1].trim();
            }
            line=reader.readLine();
        }

        // Read the rest of the file
        char[] arr = new char[8 * 1024];
        StringBuilder buffer = new StringBuilder();
        int numCharsRead;
        while ((numCharsRead = reader.read(arr, 0, arr.length)) != -1) {
            buffer.append(arr, 0, numCharsRead);
        }

        String[] parts = def.split("\\s+", 3);
        int code = 500;
        String reason = "Internal Mocking Error";

        if (parts.length == 3) {
            try {
                code = Integer.parseInt(parts[1]);
                reason = parts[2];
            } catch (NumberFormatException e) {
            }
        }

        return new Response(filename, code, reason, headers, 
            new TypedByteArray(contentType, buffer.toString().getBytes()));
    }

    @Override
    public Response execute(Request request) throws IOException {

        String filename = getFileName(request);
        String key = encrypt(filename);
        String fullPath;
        if (mRouteMap.containsKey(key)) {
            fullPath = mRouteMap.get(key);
        } else {
            fullPath = findFile(filename);
            if (fullPath != null) mRouteMap.put(key, fullPath);
        }

        Response output = new Response(filename, 404, "Not Found", Collections.EMPTY_LIST, null);       
        if (fullPath != null) {
            output = serve(fullPath);
        } else {
            Log.d("Mocktrofit", "Missing File: " + filename);
            Log.d("Mocktrofit", "    Key: " + key);
            Log.d("Mocktrofit", "    Are you sure you set up the mocktrofit gradle plugin?");
        }
        return output;
    }
}
