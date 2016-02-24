package io.vokal.mockutil;

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

import static java.lang.String.CASE_INSENSITIVE_ORDER;

public final class MockServer {

    public static final String BASE_URL = "https://mock.vokal.io";
    private static final String BASE_RGX = "https://mock\\.vokal\\.io";

    private Context mContext;
    private String mMockDir;

    private SimpleArrayMap<String, String> mRouteMap = new SimpleArrayMap<String, String>();

    public MockServer(Context aContext, String aDirectory) {
        mContext = aContext.getApplicationContext();
        if (mContext == null) mContext = aContext;

        mMockDir = aDirectory;
    }

    public String getFileName(String url, String method, String body, String contentType) {
        String path = url.replaceFirst(BASE_RGX, "");
        String[] parts = path.split("\\?");
        path = parts[0].replaceAll("[:/]", "-");
        if (parts.length > 1) {
            path = path + "?" + alphabetizeEncodeQuery(parts[1]);
        }

        url = String.format("%s|%s", method, path);
        if (body != null && contentType != null) { 
            try {
                TreeMap map = convertBody(contentType, body);

                ByteArrayOutputStream out = new ByteArrayOutputStream();
                BencodingOutputStream ben = new BencodingOutputStream(out);
                ben.writeObject(map);

                body = new String(out.toByteArray(), "UTF-8");
                body = body.replaceAll("[:/]", "-");
            } catch(NullPointerException|IOException e) {
                e.printStackTrace();
            }
        }

        if (body != null) {
            url = String.format("%s|%s", url, body);
        }

        return url;
    }

    private String findFile(String filename) {
        String[] list;
        try {
            list = mContext.getAssets().list(mMockDir);
        } catch (IOException e) {
            list = new String[0];
        }
        String regex = String.format("%s\\.\\w+", filename).replaceAll("\\|", "\\\\|");
        for (String path : list) {
            if (path.matches(regex)) {
                return path;
            }
        }

        return null;
    }

    public String determineFilePath(String filename) {
        String fullPath = getFullPath(filename);

        if (fullPath == null) {
            fullPath = getFullPath(encryptBody(filename));
        }
        
        if (fullPath == null) {
            fullPath = getFullPath(encryptPathAndBody(filename));
        }
        
        if (fullPath == null) {
            fullPath = getFullPath(encrypt(filename));
        }

        if (fullPath == null) {
            Log.d("Mocktrofit", "Missing File: " + filename);
            Log.d("Mocktrofit", "    File: " + encryptBody(filename));
            Log.d("Mocktrofit", "    File: " + encryptPathAndBody(filename));
            Log.d("Mocktrofit", "    Key: " +  encrypt(filename));
            Log.d("Mocktrofit", "    Are you sure you set up the mocktrofit gradle plugin?");
        }

        return fullPath;
    }

    public MockFile serve(String filename) throws IOException {
        InputStream is = mContext.getAssets().open(mMockDir + "/" + filename);
        MockFile file  = new MockFile();
        file.name      = filename;
        file.headers   = new ArrayList<String>();

        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        String def = reader.readLine();
        String line = reader.readLine();

        while(line != null && !line.trim().isEmpty()) {
            String[] header = line.split(":", 2);
            file.headers.add(line);
            if (header[0].toLowerCase(Locale.getDefault()).equals("content-type")) {
                file.contentType = header[1].trim();
            }

            line = reader.readLine();
        }

        // Read the rest of the file
        char[] arr = new char[8 * 1024];
        StringBuilder buffer = new StringBuilder();
        int numCharsRead;
        while ((numCharsRead = reader.read(arr, 0, arr.length)) != -1) {
            buffer.append(arr, 0, numCharsRead);
        }

        file.body = buffer.toString();

        String[] parts = def.split("\\s+", 3);
        file.code = 500;
        file.reason = "Internal Mocking Error";

        if (parts.length == 3) {
            try {
                file.code = Integer.parseInt(parts[1]);
                file.reason = parts[2];
            } catch (NumberFormatException e) {
            }
        }

        return file;
    }

    private String getFullPath(String filename) {
        String fullPath;
        String key = encrypt(filename);

        if (mRouteMap.containsKey(key)) {
            fullPath = mRouteMap.get(key);
        } else {
            fullPath = findFile(key);
        }
        return fullPath;
    }

    public static TreeMap convertBody(String contentType, String body) {
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

    public static TreeMap<String, String> alphabetizeQuery(String params) {
        TreeMap<String, String> treemap = new TreeMap<String, String>(CASE_INSENSITIVE_ORDER);
        String[] pairs = params.split("&");
        for (String pair : pairs) {
            String[] vals = pair.split("=");
            treemap.put(vals[0], vals[1]);
        }

        return treemap;
    }


    public static String alphabetizeEncodeQuery(String params) {
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

    public static String encode(String value) {
        try {
            return URLEncoder.encode(value, "UTF-8");
        } catch(UnsupportedEncodingException e) {
            return value;
        }
    }


    public static String encrypt(String name) {
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


    public static String encryptBody(String filename) {
        String[] parts = filename.split("\\|");
        if (parts.length == 3) {
            parts[2] = encrypt(parts[2]);
            return String.format("%s|%s|%s", parts[0], parts[1], parts[2]);
        }
        return filename;
    }

    public static String encryptPathAndBody(String filename) {
        String[] parts = filename.split("\\|");
        parts[1] = encrypt(parts[1]);
        if (parts.length == 3) {
            parts[2] = encrypt(parts[2]);
            return String.format("%s|%s|%s", parts[0], parts[1], parts[2]);
        }
        return String.format("%s|%s", parts[0], parts[1]);
    }
}
