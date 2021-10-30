package coo.lxt.island.server.icloud.jpa;

import coo.lxt.island.server.icloud.table.ICloudSessionDO;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

public interface IICloudSessionRepository {
    Optional<ICloudSessionDO> findByAccountName(String accountName);

    @Modifying
    @Transactional(rollbackFor = Exception.class)
    @Query(value = "insert into icloud_session (account_name, client_id, session_token, trust_token, scnt, session_id, account_country, cookie) values " +
            "(:accountName, :clientId, :sessionToken, :trustToken, :scnt, :sessionId, :accountCountry, :cookie) on duplicate key update " +
            "account_name=:accountName, client_id=:clientId, session_token=:sessionToken, trust_token=:trustToken, scnt=:scnt, session_id=:sessionId, account_country=:accountCountry, cookie=:cookie", nativeQuery = true)
    int saveOrUpdate(String accountName, String clientId, String sessionToken, String trustToken, String scnt, String sessionId, String accountCountry, String cookie);
}