package coo.lxt.island.server.icloud.dao;

import coo.lxt.island.common.dao.AbstractManager;
import coo.lxt.island.server.icloud.constant.ExecutorConstant;
import coo.lxt.island.server.icloud.jpa.IICloudSessionRepository;
import coo.lxt.island.server.icloud.jpa.repo.ICloudSessionRepository;
import coo.lxt.island.server.icloud.table.ICloudSessionDO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.Optional;
import java.util.concurrent.Future;

@Service
public class ICloudSessionManager extends AbstractManager implements IICloudSessionRepository {

    @Autowired
    private ICloudSessionRepository iCloudSessionRepository;

    @Resource(name = ExecutorConstant.ICLOUD_SESSION_MANAGER)
    private AsyncTaskExecutor executor;

    @Override
    public Optional<ICloudSessionDO> findByAccountName(String accountName) {
        return read(getKey(accountName), () -> iCloudSessionRepository.findByAccountName(accountName));
    }

    @Override
    @Transactional
    public int saveOrUpdate(String accountName, String clientId, String sessionToken, String trustToken, String scnt, String sessionId, String accountCountry, String cookie) {
        return writeCacheAside(getKey(accountName), () -> iCloudSessionRepository.saveOrUpdate(accountName, clientId, sessionToken, trustToken, scnt, sessionId, accountCountry, cookie));
    }

    public int saveOrUpdate(ICloudSessionDO sessionDO) {
        return saveOrUpdate(sessionDO.getAccountName(), sessionDO.getClientId(), sessionDO.getSessionToken(),
                sessionDO.getTrustToken(), sessionDO.getScnt(), sessionDO.getSessionId(),
                sessionDO.getAccountCountry(), sessionDO.getCookie());
    }

    public Future<Integer> saveOrUpdateAsync(ICloudSessionDO sessionDO) {
        return executor.submit(() -> saveOrUpdate(sessionDO));
    }

    private String getKey(String accountName) {
        return this.getClass().getName() + accountName;
    }
}
