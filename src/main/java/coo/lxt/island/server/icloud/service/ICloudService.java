package coo.lxt.island.server.icloud.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import coo.lxt.island.common.icloud.api.IICloudApi;
import coo.lxt.island.common.icloud.exception.ICloudSessionInvalidException;
import coo.lxt.island.common.icloud.vo.ICloudAuthWithTokenVO;
import coo.lxt.island.common.icloud.vo.ICloudHSA2VO;
import coo.lxt.island.common.icloud.vo.ICloudLoginVO;
import coo.lxt.island.common.icloud.vo.ICloudTrustSessionVO;
import coo.lxt.island.server.icloud.dao.ICloudSessionManager;
import coo.lxt.island.server.icloud.dao.ICloudWebServiceManager;
import coo.lxt.island.server.icloud.table.ICloudSessionDO;
import coo.lxt.island.server.icloud.table.ICloudWebServiceDO;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.Future;

@Service
@Slf4j
public class ICloudService {

    public static final MediaType MEDIA_TYPE_JSON = MediaType.get("application/json; charset=utf-8");

    @Autowired
    private OkHttpClient okHttpClient;

    @Autowired
    private ObjectMapper jsonMapper;

    @Autowired
    private ICloudSessionManager iCloudSessionManager;

    @Autowired
    private ICloudWebServiceManager iCloudWebServiceManager;

    @Value("${icloud.url.setup:https://setup.icloud.com/setup/ws/1}")
    private String SETUP_URL;

    @Value("${icloud.url.auth:https://idmsa.apple.com/appleauth/auth}")
    private String AUTH_URL;

    @Value("${icloud.url.home:https://www.icloud.com}")
    private String HOME_URL;

    public IICloudApi.LoginStatus login(ICloudLoginVO authVO) throws Exception {
        ICloudSessionDO sessionDO = iCloudSessionManager.findByAccountName(authVO.getAccountName()).orElseGet(() -> {
            ICloudSessionDO session = new ICloudSessionDO();
            session.setAccountName(authVO.getAccountName());
            session.setClientId(UUID.randomUUID().toString().toLowerCase());
            return session;
        });

        List<String> trustTokens = new ArrayList<>();
        if (StringUtils.isNotEmpty(sessionDO.getTrustToken())) {
            trustTokens.add(sessionDO.getTrustToken());
        }
        authVO.setTrustTokens(trustTokens);

        try {
            return signIn(authVO, sessionDO);
        } finally {
            saveOrUpdateSessionAsync(sessionDO);
        }
    }

    public boolean authWithToken(ICloudAuthWithTokenVO vo) throws Exception {
        Optional<ICloudSessionDO> sessionDO = iCloudSessionManager.findByAccountName(vo.getAccountName());
        if (!sessionDO.isPresent()) {
            log.info("ICloud session is not found for {}.", vo.getAccountName());
            throw new ICloudSessionInvalidException("ICloud session is not found.", vo.getAccountName());
        }
        try {
            return authWithToken(sessionDO.get());
        } finally {
            saveOrUpdateSessionAsync(sessionDO.get());
        }
    }

    public boolean trustSession(ICloudTrustSessionVO vo) throws Exception {
        Optional<ICloudSessionDO> sessionDO = iCloudSessionManager.findByAccountName(vo.getAccountName());
        if (!sessionDO.isPresent()) {
            log.info("ICloud session is not found for {}.", vo.getAccountName());
            throw new ICloudSessionInvalidException("ICloud session is not found.", vo.getAccountName());
        }
        try {
            return trustSession(sessionDO.get());
        } finally {
            saveOrUpdateSessionAsync(sessionDO.get());
        }
    }

    public boolean checkVerifyCode(ICloudHSA2VO verifyCodeVO) throws Exception {
        Optional<ICloudSessionDO> sessionDO = iCloudSessionManager.findByAccountName(verifyCodeVO.getAccountName());
        if (!sessionDO.isPresent()) {
            log.info("ICloud session is not found for {}.", verifyCodeVO.getAccountName());
            throw new ICloudSessionInvalidException("ICloud session is not found.", verifyCodeVO.getAccountName());
        }
        try {
            return checkVerifyCode(verifyCodeVO, sessionDO.get());
        } finally {
            saveOrUpdateSessionAsync(sessionDO.get());
        }
    }

    private boolean checkVerifyCode(ICloudHSA2VO verifyCodeVO, ICloudSessionDO sessionDO) throws Exception {
        Headers.Builder headers = getCommonHeaders(sessionDO);
        if (StringUtils.isNotEmpty(sessionDO.getScnt())) {
            headers.add("scnt", sessionDO.getScnt());
        }
        if (StringUtils.isNotEmpty(sessionDO.getSessionId())) {
            headers.add("X-Apple-ID-Session-Id", sessionDO.getSessionId());
        }

        HttpUrl verifyUrl = HttpUrl.get(AUTH_URL).newBuilder()
                .addPathSegment("verify")
                .addPathSegment("trusteddevice")
                .addPathSegment("securitycode")
                .build();

        ObjectNode root = jsonMapper.createObjectNode();
        ObjectNode securityCodeNode = jsonMapper.createObjectNode();
        securityCodeNode.put("code", verifyCodeVO.getSecurityCode());
        root.set("securityCode", securityCodeNode);

        RequestBody requestBody = RequestBody.create(MEDIA_TYPE_JSON, jsonMapper.writeValueAsString(root));

        Request.Builder request = new Request.Builder()
                .headers(headers.build())
                .url(verifyUrl)
                .post(requestBody);

        try (Response response = sessionDO.sendRequest(okHttpClient, request)) {
            String body = Objects.requireNonNull(response.body()).string();
            if (response.isSuccessful()) {
                log.info("Code verification successful. Account: {}, Response: {}", sessionDO.getAccountName(), body);
                return true;
            } else if (response.code() == HttpStatus.UNAUTHORIZED.value()) {
                log.warn("Security code incorrect. Account: {}, Response: {}", sessionDO.getAccountName(), body);
                return false;
            } else {
                log.error("Failed validate security code. Account: {}, Response: {}, {}", sessionDO.getAccountName(), response.code(), body);
                throw new Exception(String.format("Failed validate security code. Response: %d, %s", response.code(), body));
            }
        }
    }

    private boolean trustSession(ICloudSessionDO sessionDO) throws Exception {
        Headers.Builder headers = getCommonHeaders(sessionDO);
        if (StringUtils.isNotEmpty(sessionDO.getScnt())) {
            headers.add("scnt", sessionDO.getScnt());
        }
        if (StringUtils.isNotEmpty(sessionDO.getSessionId())) {
            headers.add("X-Apple-ID-Session-Id", sessionDO.getSessionId());
        }

        HttpUrl verifyUrl = HttpUrl.get(AUTH_URL).newBuilder()
                .addPathSegment("2sv")
                .addPathSegment("trust")
                .build();

        Request.Builder request = new Request.Builder()
                .headers(headers.build())
                .url(verifyUrl)
                .get();

        try (Response response = sessionDO.sendRequest(okHttpClient, request)) {
            String body = Objects.requireNonNull(response.body()).string();
            if (response.isSuccessful()) {
                log.info("Trust session successful. Account: {}, Response: {}", sessionDO.getAccountName(), body);
                return true;
            } else {
                log.error("Failed to trust session. Account: {}, Response: {}, {}", sessionDO.getAccountName(), response.code(), body);
                throw new Exception(String.format("Failed to trust session. Response: %d, %s", response.code(), body));
            }
        }
    }

    private IICloudApi.LoginStatus signIn(ICloudLoginVO authVO, ICloudSessionDO session) throws Exception {
        Headers.Builder headers = getCommonHeaders(session);

        if(StringUtils.isNotEmpty(session.getScnt())) {
            headers.add("scnt", session.getScnt());
        }
        if(StringUtils.isNotEmpty(session.getSessionId())) {
            headers.add("X-Apple-ID-Session-Id", session.getScnt());
        }

        HttpUrl loginUrl = HttpUrl.get(AUTH_URL).newBuilder()
                .addPathSegment("signin")
                .addQueryParameter("isRememberMeEnabled", "true")
                .build();

        RequestBody requestBody = RequestBody.create(MEDIA_TYPE_JSON, jsonMapper.writeValueAsString(authVO));

        Request.Builder request = new Request.Builder()
                .headers(headers.build())
                .url(loginUrl)
                .post(requestBody);

        try (Response response = session.sendRequest(okHttpClient, request)) {
            String body = Objects.requireNonNull(response.body()).string();
            if (response.isSuccessful()) {
                log.info("Successfully signed in ICloud. Account: {}, Response: {}", authVO.getAccountName(), body);
                // No need of HSA2.
                return IICloudApi.LoginStatus.SUCCESS;
            } else if (response.code() == HttpStatus.CONFLICT.value()) {
                try {
                    JsonNode root = jsonMapper.readTree(body);
                    log.info("Require {} auth. Account: {}, Response: {}", root.get("authType"), authVO.getAccountName(), body);
                } catch (JsonProcessingException e) {
                    log.error("Require unknown auth. Account: {}, Response: {}", authVO.getAccountName(), body);
                }
                // Needs Hsa2.
                return IICloudApi.LoginStatus.NEED_HSA2;
            } else if (response.code() == HttpStatus.UNAUTHORIZED.value()) {
                log.warn("Credential incorrect. Account: {}, Response: {}", authVO.getAccountName(), body);
                return IICloudApi.LoginStatus.INCORRECT_CREDENTIALS;
            } else if (response.code() == HttpStatus.FORBIDDEN.value()) {
                log.warn("Account is locked: {}, Response: {}", authVO.getAccountName(), body);
                return IICloudApi.LoginStatus.ACCOUNT_LOCKED;
            } else {
                log.error("Failed signed into ICloud. Account: {}, Response: {}, {}", authVO.getAccountName(), response.code(), body);
                throw new Exception(String.format("Failed signed into ICloud. Response: %d, %s", response.code(), body));
            }
        }
    }

    private boolean authWithToken(ICloudSessionDO session) throws Exception {
        HttpUrl loginUrl = HttpUrl.get(SETUP_URL).newBuilder()
                .addPathSegment("accountLogin")
                .addQueryParameter("clientBuildNumber", "2021Project52")
                .addQueryParameter("clientMasteringNumber", "2021B29")
                .addQueryParameter("clientId", session.getClientId())
                .build();

        ObjectNode data = jsonMapper.createObjectNode();
        data.put("accountCountryCode", session.getAccountCountry());
        data.put("dsWebAuthToken", session.getSessionToken());
        data.put("extended_login", true);
        data.put("trustToken", StringUtils.defaultString(session.getTrustToken(), ""));
        data.put("accountCountryCode", session.getAccountCountry());

        RequestBody requestBody = RequestBody.create(MEDIA_TYPE_JSON, jsonMapper.writeValueAsString(data));

        Request.Builder request = new Request.Builder()
                .url(loginUrl)
                .post(requestBody);

        try (Response response = session.sendRequest(okHttpClient, request)) {
            String body = Objects.requireNonNull(response.body()).string();
            if (response.isSuccessful()) {
                log.info("ICloud token auth successful. Account: {}, Response: {}", session.getAccountName(), body);

                Map<String, ICloudWebServiceDO> originWebServiceDOs = iCloudWebServiceManager.findAllMap();
                List<ICloudWebServiceDO> needForUpdateDOs = new ArrayList<>();
                jsonMapper.readTree(body).get("webservices").fields().forEachRemaining((entry) -> {
                    JsonNode url = entry.getValue().get("url");
                    JsonNode status = entry.getValue().get("status");
                    if (url != null && status != null) {
                        ICloudWebServiceDO webServiceDO = new ICloudWebServiceDO();
                        webServiceDO.setServiceName(entry.getKey());
                        webServiceDO.setUrl(url.asText());
                        webServiceDO.setStatus(status.asText());

                        if (originWebServiceDOs.containsKey(webServiceDO.getServiceName())) {
                            ICloudWebServiceDO originWebServiceDO = originWebServiceDOs.get(webServiceDO.getServiceName());
                            if (!StringUtils.equals(originWebServiceDO.getUrl(), webServiceDO.getUrl()) ||
                                    !StringUtils.equals(originWebServiceDO.getStatus(), webServiceDO.getStatus())) {
                                needForUpdateDOs.add(webServiceDO);
                            }
                        } else {
                            needForUpdateDOs.add(webServiceDO);
                        }
                    }
                });

                iCloudWebServiceManager.saveOrUpdateAsync(needForUpdateDOs);
                return true;
            } else {
                log.warn("ICloud token auth failed. Account: {}, Response: {}, {}", session.getAccountName(), response.code(), body);
                throw new Exception(String.format("ICloud token auth failed. Account: %s, Response: %d, %s", session.getAccountName(), response.code(), body));
            }
        }
    }

    private Future<Integer> saveOrUpdateSessionAsync(ICloudSessionDO session) {
        if (session.getUpdateFlag()) {
            log.info("Session update for {}, Session: {}", session.getAccountName(), session);
            return iCloudSessionManager.saveOrUpdateAsync(session);
        }
        return null;
    }

    private Headers.Builder getCommonHeaders(ICloudSessionDO sessionDO) {
        String authClientId = String.format("auth-%s", sessionDO.getClientId());

        return new Headers.Builder()
                .add("Accept", "application/json;text/json")
                .add("Content-Type", "application/json")
                .add("X-Apple-OAuth-Client-Id", "d39ba9916b7251055b22c7f910e2ea796ee65e98b2ddecea8f5dde8d9d1a815d")
                .add("X-Apple-OAuth-Client-Type", "firstPartyAuth")
                .add("X-Apple-OAuth-Redirect-URI", "https://www.icloud.com")
                .add("X-Apple-OAuth-Require-Grant-Code", "true")
                .add("X-Apple-OAuth-Response-Mode", "web_message")
                .add("X-Apple-OAuth-Response-Type", "code")
                .add("X-Apple-Widget-Key", "d39ba9916b7251055b22c7f910e2ea796ee65e98b2ddecea8f5dde8d9d1a815d")
                .add("X-Apple-OAuth-State", authClientId);
    }
}
