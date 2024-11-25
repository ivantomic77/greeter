package dev.tomic.greetingservice.greet;

import org.json.JSONObject;
import org.springframework.stereotype.Service;

import java.util.Base64;

@Service
public class GreetService {
    public String greet(String authHeader) {
        return "Hello " + extractNameFromBearer(authHeader) + ", this is Greeting Service";
    }

    private String extractNameFromBearer(String authHeader) {
        String bearer = authHeader.replace("Bearer ", "");
        String[] chunks = bearer.split("\\.");
        Base64.Decoder decoder = Base64.getUrlDecoder();
        String payload = new String(decoder.decode(chunks[1]));

        JSONObject jsonObject = new JSONObject(payload);

        if (!jsonObject.has("name")){
            // In case when First name or Last name property is not set for that user in Keycloak
            throw new IllegalArgumentException("This user doesn't have an assigned name. Check your Identity Provider user config.");
        }
        return jsonObject.getString("name");
    }

}
