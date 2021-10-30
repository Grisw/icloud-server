package coo.lxt.island.server.icloud.dao;

import coo.lxt.island.common.dao.AbstractManager;
import coo.lxt.island.server.icloud.constant.ExecutorConstant;
import coo.lxt.island.server.icloud.jpa.IICloudWebServiceRepository;
import coo.lxt.island.server.icloud.jpa.repo.ICloudWebServiceRepository;
import coo.lxt.island.server.icloud.table.ICloudWebServiceDO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

@Service
public class ICloudWebServiceManager extends AbstractManager implements IICloudWebServiceRepository {

    @Autowired
    private ICloudWebServiceRepository iCloudWebServiceRepository;

    @Resource(name = ExecutorConstant.ICLOUD_WEB_SERVICE_MANAGER)
    private AsyncTaskExecutor executor;

    @Override
    public Optional<ICloudWebServiceDO> findByServiceName(String serviceName) {
        return read(getKey(), serviceName, () -> iCloudWebServiceRepository.findAll(), ICloudWebServiceDO::getServiceName);
    }

    @Override
    public List<ICloudWebServiceDO> findAll() {
        return readAll(getKey(), () -> iCloudWebServiceRepository.findAll(), ICloudWebServiceDO::getServiceName);
    }

    @Override
    public int saveOrUpdate(String serviceName, String url, String status) {
        return writeCacheAside(getKey(), serviceName, () -> iCloudWebServiceRepository.saveOrUpdate(serviceName, url, status));
    }

    public int saveOrUpdate(ICloudWebServiceDO webServiceDO) {
        return saveOrUpdate(webServiceDO.getServiceName(), webServiceDO.getUrl(), webServiceDO.getStatus());
    }

    @Transactional(noRollbackFor = Exception.class)
    public int saveOrUpdate(List<ICloudWebServiceDO> webServiceDOs) {
        int total = 0;
        for (ICloudWebServiceDO webServiceDO : webServiceDOs) {
            total += saveOrUpdate(webServiceDO);
        }
        return total;
    }

    public Map<String, ICloudWebServiceDO> findAllMap() {
        return findAll().stream().collect(Collectors.toMap(ICloudWebServiceDO::getServiceName, (o) -> o));
    }

    public Future<Integer> saveOrUpdateAsync(ICloudWebServiceDO sessionDO) {
        return executor.submit(() -> saveOrUpdate(sessionDO));
    }

    public Future<Integer> saveOrUpdateAsync(List<ICloudWebServiceDO> webServiceDOs) {
        return executor.submit(() -> saveOrUpdate(webServiceDOs));
    }

    private String getKey() {
        return this.getClass().getName();
    }

}
