package coo.lxt.island.server.icloud.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import coo.lxt.island.common.icloud.exception.ICloudAuthenticationException;
import coo.lxt.island.common.icloud.vo.ICloudAuthVO;
import coo.lxt.island.common.icloud.vo.ICloudSessionVO;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
public class ICloudService {

    public static final MediaType MEDIA_TYPE_JSON = MediaType.get("application/json; charset=utf-8");

    @Autowired
    private OkHttpClient okHttpClient;

    @Autowired
    private ObjectMapper jsonMapper;

    @Value("${icloud.url.setup:https://setup.icloud.com/setup/ws/1}")
    private String setupUrl;

    @Value("${icloud.url.auth:https://idmsa.apple.com/appleauth/auth}")
    private String authUrl;

    @Value("${icloud.url.home:https://www.icloud.com}")
    private String homeUrl;

    public ICloudSessionVO login(ICloudAuthVO authVO) throws IOException {
        String clientId = UUID.randomUUID().toString().toLowerCase();
        return sendSignInRequest(authVO, clientId);
    }

    private ICloudSessionVO sendSignInRequest(ICloudAuthVO authVO, String clientId) throws IOException {
        String authClientId = String.format("auth-%s", clientId);

        Headers headers = new Headers.Builder()
                .add("Origin", homeUrl)
                .add("Referer", homeUrl)
                .add("Accept", "application/json;text/json")
                .add("Content-Type", "application/json")
                .add("X-Apple-OAuth-Client-Id", "d39ba9916b7251055b22c7f910e2ea796ee65e98b2ddecea8f5dde8d9d1a815d")
                .add("X-Apple-OAuth-Client-Type", "firstPartyAuth")
                .add("X-Apple-OAuth-Redirect-URI", "https://www.icloud.com")
                .add("X-Apple-OAuth-Require-Grant-Code", "true")
                .add("X-Apple-OAuth-Response-Mode", "web_message")
                .add("X-Apple-OAuth-Response-Type", "code")
                .add("X-Apple-Widget-Key", "d39ba9916b7251055b22c7f910e2ea796ee65e98b2ddecea8f5dde8d9d1a815d")
                .add("X-Apple-OAuth-State", authClientId)
                .build();

        HttpUrl loginUrl = HttpUrl.get(authUrl).newBuilder()
                .addPathSegment("signin")
                .addQueryParameter("isRememberMeEnabled", "true")
                .build();

        RequestBody requestBody = RequestBody.create(MEDIA_TYPE_JSON, jsonMapper.writeValueAsString(authVO));

        Request request = new Request.Builder()
                .headers(headers)
                .url(loginUrl)
                .post(requestBody)
                .build();

        try (Response response = okHttpClient.newCall(request).execute()) {
            String body = Optional.ofNullable(response.body())
                    .orElseGet(() -> ResponseBody.create(null, "")).string();
            if (!response.isSuccessful()) {
                log.warn("Failed signed in ICloud. Account: {}, Response: {}", authVO.getAccountName(), body);
                throw new ICloudAuthenticationException("Failed signed in ICloud.", authVO.getAccountName(), body);
            }
            log.info("Successfully signed in ICloud. Account: {}, Response: {}", authVO.getAccountName(), body);
            ICloudSessionVO session = new ICloudSessionVO();
            session.setAccountCountry(response.header("X-Apple-ID-Account-Country"));
            session.setSessionId(response.header("X-Apple-ID-Session-Id"));
            session.setSessionToken(response.header("X-Apple-Session-Token"));
            session.setTrustToken(response.header("X-Apple-TwoSV-Trust-Token"));
            session.setScnt(response.header("scnt"));
            session.setCookie(response.headers("Set-Cookie"));
            return session;
        }
    }
}
