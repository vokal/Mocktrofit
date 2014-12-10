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
    }

    private DogService mService;

    protected void setUp() throws Exception {
        RestAdapter restAdapter = new RestAdapter.Builder()
            .setEndpoint(MockClient.BASE_URL)
            .setClient(new MockClient(getContext(), "mocks"))
            .build();

        mService = restAdapter.create(DogService.class);       
    }

    public void testProperJSONParse() {
        assertEquals("dogfish", mService.getFavorites().favorite);
    }
}
