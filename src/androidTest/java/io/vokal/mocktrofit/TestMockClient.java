package io.vokal.mocktrofit;

import android.test.AndroidTestCase;

import retrofit.*;
import retrofit.http.*;

import com.google.gson.annotations.SerializedName;

public class TestMockClient extends AndroidTestCase {
    public static class Dog {
        @SerializedName("favorite_dog_breed")
        public String favorite;
    }

    public interface DogService {
        @GET("/v1/test/json")
        Dog getFavorites();

        @GET("/v1/test/params")
        Dog getFavorites(@Query("something") String thing, @Query("test") String test);
    }

    private DogService mService;

    protected void setUp() throws Exception {
        RestAdapter restAdapter = new RestAdapter.Builder()
            .setEndpoint(MockClient.BASE_URL)
            .setClient(new MockClient(getContext(), "mocks"))
            .build();

        mService = restAdapter.create(DogService.class);       
    }

    public void testAlphabetizeAndEncode() {
        assertEquals("something%3Dworked%26test%3Dtrue", 
            MockClient.encode(MockClient.alphabetize("test=true&something=worked")));
    }

    public void testProperJSONParse() {
        assertEquals("dogfish", mService.getFavorites().favorite);
    }

    public void testQueryParams() {
        assertEquals("nickdawg", mService.getFavorites("worked", "true").favorite);
    }
}
