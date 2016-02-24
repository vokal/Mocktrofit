package io.vokal.mocktrofit;

import android.test.AndroidTestCase;

import retrofit.*;
import retrofit.client.Response;
import retrofit.http.*;

import io.vokal.mockutil.*;

import com.google.gson.annotations.SerializedName;

public class TestMockClient extends AndroidTestCase {
    public static class Dog {
        @SerializedName("favorite_dog_breed")
        public String favorite;
    }

    public static class User {
        public int id;
        public String email;
        public String auth_token;
        public String phone_number;
        public String joined;
        public String role;
        public String display_name;
    }

    public static class UserCreds {
        public String email;
        public String password;
    }

    public interface DogService {
        @GET("/v1/test/json")
        Dog getFavorites();

        @GET("/v1/test/params")
        Dog getFavorites(@Query("something") String thing, @Query("test") String test);
    }

    public interface UserService {
        @POST("/v1/user/login")
        User login(@Body UserCreds creds);

        @DELETE("/v1/test/delete")
        Response delete();
    }

    private DogService mDogService;
    private UserService mUserService;

    protected void setUp() throws Exception {
        RestAdapter restAdapter = new RestAdapter.Builder()
            .setEndpoint(MockServer.BASE_URL)
            .setClient(new MockClient(getContext(), "mocks"))
            .build();

        mDogService = restAdapter.create(DogService.class);       
        mUserService = restAdapter.create(UserService.class);       
    }

    public void testAlphabetizeAndEncode() {
        assertEquals("something=worked&test=true", 
            MockServer.alphabetizeEncodeQuery("test=true&something=worked"));
    }

    public void testProperJSONParse() {
        assertEquals("dogfish", mDogService.getFavorites().favorite);
    }

    public void testQueryParams() {
        assertEquals("nickdawg", mDogService.getFavorites("worked", "true").favorite);
    }

    public void testPostWithBody() {
        UserCreds creds = new UserCreds();
        creds.email = "joe.customer@example.com";
        creds.password = "P4rkMe";
        User myuser = mUserService.login(creds);
        assertEquals(30, myuser.id);
    }

    public void testDelete() {
        Response response = mUserService.delete();
        assertEquals(204, response.getStatus());
        assertEquals("DELETED", response.getReason());
        assertEquals(0, response.getBody().length());
    }
}
