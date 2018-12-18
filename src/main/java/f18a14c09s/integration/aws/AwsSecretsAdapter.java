package f18a14c09s.integration.aws;

import com.amazonaws.services.secretsmanager.AWSSecretsManager;
import com.amazonaws.services.secretsmanager.AWSSecretsManagerClientBuilder;
import com.amazonaws.services.secretsmanager.model.GetSecretValueRequest;
import com.amazonaws.services.secretsmanager.model.GetSecretValueResult;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.*;

public class AwsSecretsAdapter {
    private ObjectMapper jsonMapper = new ObjectMapper();

    public Map<String, Object> getSecret(String name) throws IOException {
        AWSSecretsManager mgr = AWSSecretsManagerClientBuilder.defaultClient();
        GetSecretValueRequest request = new GetSecretValueRequest();
        request.setSecretId(name);
        GetSecretValueResult result = mgr.getSecretValue(request);
        return jsonMapper.readValue(result.getSecretString(), HashMap.class);
    }
}
