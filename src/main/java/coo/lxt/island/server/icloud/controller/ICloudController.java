package coo.lxt.island.server.icloud.controller;

import coo.lxt.island.common.icloud.api.IICloudApi;
import coo.lxt.island.common.icloud.vo.ICloudAuthWithTokenVO;
import coo.lxt.island.common.icloud.vo.ICloudHSA2VO;
import coo.lxt.island.common.icloud.vo.ICloudLoginVO;
import coo.lxt.island.common.icloud.vo.ICloudTrustSessionVO;
import coo.lxt.island.server.icloud.service.ICloudService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/icloud")
public class ICloudController implements IICloudApi {

    @Autowired
    private ICloudService iCloudService;

    @Override
    public IICloudApi.LoginStatus login(ICloudLoginVO authVO) throws Exception {
        return iCloudService.login(authVO);
    }

    @Override
    public boolean verifyHSA2(ICloudHSA2VO hsa2VO) throws Exception {
        return iCloudService.checkVerifyCode(hsa2VO);
    }

    @Override
    public boolean authWithToken(ICloudAuthWithTokenVO authWithTokenVO) throws Exception {
        return iCloudService.authWithToken(authWithTokenVO);
    }

    @Override
    public boolean trustSession(ICloudTrustSessionVO trustSessionVO) throws Exception {
        return iCloudService.trustSession(trustSessionVO);
    }

}
