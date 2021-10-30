package coo.lxt.island.server.icloud.table;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;

import javax.persistence.*;
import java.io.IOException;
import java.io.Serializable;
import java.util.List;
import java.util.StringJoiner;

@Entity
@Getter
@Setter
@ToString
@Table(name = "icloud_session")
public class ICloudSessionDO implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "account_name", nullable = false, unique = true, length = 255)
    private String accountName;

    @Column(name = "client_id", length = 36)
    private String clientId;

    @Column(name = "session_token", columnDefinition = "text")
    private String sessionToken;

    @Column(name = "trust_token", length = 255)
    private String trustToken;

    @Column(name = "scnt", length = 500)
    private String scnt;

    @Column(name = "session_id", length = 255)
    private String sessionId;

    @Column(name = "account_country", length = 32)
    private String accountCountry;

    @Column(name = "cookie", columnDefinition = "text")
    private String cookie;

    @JsonIgnore
    private transient Boolean updateFlag;

    public Response sendRequest(OkHttpClient okHttpClient, Request.Builder request) throws IOException {
        if (StringUtils.isNotEmpty(getCookie())) {
            request.addHeader("Cookie", getCookie());
        }
        request.addHeader("Origin", "https://www.icloud.com");
        request.addHeader("Referer", "https://www.icloud.com");

        Response response = okHttpClient.newCall(request.build()).execute();

        setUpdateFlag(false);

        String accountCountry = response.header("X-Apple-ID-Account-Country");
        String sessionId = response.header("X-Apple-ID-Session-Id");
        String sessionToken = response.header("X-Apple-Session-Token");
        String trustToken = response.header("X-Apple-TwoSV-Trust-Token");
        String scnt = response.header("scnt");
        String cookie = handleSetCookie(response.headers("Set-Cookie"));

        if ((StringUtils.isNotEmpty(accountCountry) && !StringUtils.equals(accountCountry, getAccountCountry()))){
            setAccountCountry(accountCountry);
            setUpdateFlag(true);
        }
        if ((StringUtils.isNotEmpty(sessionId) && !StringUtils.equals(sessionId, getSessionId()))){
            setSessionId(sessionId);
            setUpdateFlag(true);
        }
        if ((StringUtils.isNotEmpty(sessionToken) && !StringUtils.equals(sessionToken, getSessionToken()))){
            setSessionToken(sessionToken);
            setUpdateFlag(true);
        }
        if ((StringUtils.isNotEmpty(trustToken) && !StringUtils.equals(trustToken, getTrustToken()))){
            setTrustToken(trustToken);
            setUpdateFlag(true);
        }
        if ((StringUtils.isNotEmpty(scnt) && !StringUtils.equals(scnt, getScnt()))){
            setScnt(scnt);
            setUpdateFlag(true);
        }
        if ((StringUtils.isNotEmpty(cookie) && !StringUtils.equals(cookie, getCookie()))){
            setCookie(cookie);
            setUpdateFlag(true);
        }

        return response;
    }

    private String handleSetCookie(List<String> setCookies) {
        if (CollectionUtils.isEmpty(setCookies)) {
            return "";
        }
        StringJoiner joiner = new StringJoiner("; ");
        for (String setCookie : setCookies) {
            if (StringUtils.isEmpty(setCookie)) {
                continue;
            }
            String cookie = StringUtils.substringBefore(setCookie, ";");
            if (StringUtils.isEmpty(cookie)) {
                continue;
            }
            joiner.add(cookie);
        }
        return joiner.toString();
    }
}
