package net.wanji.business.service.impl;

import com.alibaba.fastjson.JSONObject;
import net.wanji.business.common.Constants.YN;
import net.wanji.business.domain.bo.CaseConfigBo;
import net.wanji.business.domain.bo.TaskCaseConfigBo;
import net.wanji.business.domain.dto.device.DeviceReadyStateDto;
import net.wanji.business.domain.dto.device.DeviceReadyStateParam;
import net.wanji.business.domain.param.CaseRuleControl;
import net.wanji.business.domain.param.TestStartParam;
import net.wanji.business.entity.TjDevice;
import net.wanji.business.service.RestService;
import net.wanji.common.core.redis.RedisCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * @Auther: guanyuduo
 * @Date: 2023/8/9 15:26
 * @Descriptoin:
 */
@Service
public class RestServiceImpl implements RestService {

    private static final Logger log = LoggerFactory.getLogger("business");

    @Autowired
    private RestTemplate restTemplate;

    @Value("${tess.start}")
    private String tessStartUrl;

    @Value("${masterControl.queryDeviceReadyState}")
    private String queryDeviceReadyStateUrl;

    @Value("${masterControl.sendRule}")
    private String sendRuleUrl;

    @Value("${imitate.client}")
    private String imitateClientUrl;

    @Autowired
    private RedisCache redisCache;

    @Override
    public boolean start(TestStartParam startParam) {
        try {
            String resultUrl = tessStartUrl;
            log.info("============================== tess start：{}", tessStartUrl);
            HttpHeaders httpHeaders = new HttpHeaders();
            httpHeaders.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<TestStartParam> resultHttpEntity = new HttpEntity<>(startParam, httpHeaders);
            log.info("============================== tess start param：{}", JSONObject.toJSONString(startParam));
            ResponseEntity<String> response =
                    restTemplate.exchange(resultUrl, HttpMethod.POST, resultHttpEntity, String.class);
            if (response.getStatusCodeValue() == 200) {
                JSONObject result = JSONObject.parseObject(response.getBody(), JSONObject.class);
                log.info("============================== tess start result:{}", JSONObject.toJSONString(result));
                if (Objects.isNull(result) || !"success".equals(result.get("status"))) {
                    log.error("远程服务调用失败:{}", result.get("msg"));
                    return false;
                }
                return true;
            }
        } catch (Exception e) {
            log.error("远程服务调用失败:{}", e);
        }
        return false;
    }

    @Override
    public Map<String, Object> searchDeviceInfo(String ip, HttpMethod method) {
        Map<String, Object> result = new HashMap<>();
        result.put("status", YN.Y_INT);
        result.put("longitude", 121.20166333688785);
        result.put("latitude", 31.291084438789756);
        result.put("courseAngle", 0.3);
        return result;
    }

    @Override
    public boolean selectDeviceReadyState(DeviceReadyStateParam deviceReadyStateParam) {
        String key = "READY_STATE_" + deviceReadyStateParam.getCaseId() + "_" + deviceReadyStateParam.getDeviceId();
        if (redisCache.hasKey(key)) {
            return false;
        }
        redisCache.setCacheObject(key, key, 5, TimeUnit.SECONDS);
        try {
            String resultUrl = queryDeviceReadyStateUrl;
            log.info("============================== queryDeviceReadyStateUrl：{}", queryDeviceReadyStateUrl);
            HttpHeaders httpHeaders = new HttpHeaders();
            httpHeaders.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<DeviceReadyStateDto> resultHttpEntity = new HttpEntity<>(deviceReadyStateParam, httpHeaders);
            log.info("============================== queryDeviceReadyStateUrl：{}", deviceReadyStateParam.getDeviceId());
            ResponseEntity<String> response =
                    restTemplate.exchange(resultUrl, HttpMethod.POST, resultHttpEntity, String.class);
            if (response.getStatusCodeValue() == 200) {
                if (!"success".equals(response.getBody())) {
                    log.error("远程服务调用失败:{}", response.getBody());
                    return false;
                }
                return true;
            }
        } catch (Exception e) {
            log.error("远程服务调用失败:{}", e);
        }
        return false;
    }

    @Override
    public boolean sendRuleUrl(CaseRuleControl caseRuleControl) {
        try {
            String resultUrl = sendRuleUrl;
            log.info("============================== connectMasterControl：{}", sendRuleUrl);
            HttpHeaders httpHeaders = new HttpHeaders();
            httpHeaders.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<CaseRuleControl> resultHttpEntity = new HttpEntity<>(caseRuleControl, httpHeaders);
            log.info("============================== connectMasterControl：{}", JSONObject.toJSONString(caseRuleControl));
            ResponseEntity<String> response =
                    restTemplate.exchange(resultUrl, HttpMethod.POST, resultHttpEntity, String.class);
            if (response.getStatusCodeValue() == 200) {
                if (!"success".equals(response.getBody())) {
                    log.error("远程服务调用失败");
                    return false;
                }
                return true;
            }
        } catch (Exception e) {
            log.error("远程服务调用失败:{}", e);
        }
        return false;
    }

    @Override
    public Object imitateClientUrl(List<CaseConfigBo> param) {
        try {
            String resultUrl = imitateClientUrl;
            log.info("============================== imitateClient：{}", imitateClientUrl);
            HttpHeaders httpHeaders = new HttpHeaders();
            httpHeaders.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<List<CaseConfigBo>> resultHttpEntity = new HttpEntity<>(param, httpHeaders);
            log.info("============================== imitateClient：{}", JSONObject.toJSONString(param));
            ResponseEntity<String> response =
                    restTemplate.exchange(resultUrl, HttpMethod.POST, resultHttpEntity, String.class);
            if (response.getStatusCodeValue() == 200) {
                JSONObject result = JSONObject.parseObject(response.getBody(), JSONObject.class);
                log.info("============================== imitateClient  result:{}", JSONObject.toJSONString(result));
                if (Objects.isNull(result) || !"success".equals(result.get("status"))) {
                    log.error("远程服务调用失败:{}", result.get("msg"));
                    return false;
                }
                return true;
            }
        } catch (Exception e) {
            log.error("远程服务调用失败:{}", e);
        }
        return false;
    }

    @Override
    public Object taskClientUrl(List<TaskCaseConfigBo> param) {
        try {
            String resultUrl = imitateClientUrl;
            log.info("============================== imitateClient：{}", imitateClientUrl);
            HttpHeaders httpHeaders = new HttpHeaders();
            httpHeaders.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<List<TaskCaseConfigBo>> resultHttpEntity = new HttpEntity<>(param, httpHeaders);
            log.info("============================== imitateClient：{}", JSONObject.toJSONString(param));
            ResponseEntity<String> response =
                    restTemplate.exchange(resultUrl, HttpMethod.POST, resultHttpEntity, String.class);
            if (response.getStatusCodeValue() == 200) {
                JSONObject result = JSONObject.parseObject(response.getBody(), JSONObject.class);
                log.info("============================== imitateClient  result:{}", JSONObject.toJSONString(result));
                if (Objects.isNull(result) || !"success".equals(result.get("status"))) {
                    log.error("远程服务调用失败:{}", result.get("msg"));
                    return false;
                }
                return true;
            }
        } catch (Exception e) {
            log.error("远程服务调用失败:{}", e);
        }
        return false;
    }
}
