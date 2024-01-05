package net.wanji.business.service.impl;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import net.wanji.business.common.Constants.YN;
import net.wanji.business.domain.bo.SaveCustomIndexWeightBo;
import net.wanji.business.domain.bo.SaveCustomScenarioWeightBo;
import net.wanji.business.domain.bo.SaveTaskSchemeBo;
import net.wanji.business.domain.dto.device.DeviceReadyStateDto;
import net.wanji.business.domain.dto.device.DeviceReadyStateParam;
import net.wanji.business.domain.dto.device.TaskSaveDto;
import net.wanji.business.domain.param.CaseRuleControl;
import net.wanji.business.domain.param.CaseTrajectoryParam;
import net.wanji.business.domain.param.TessParam;
import net.wanji.business.domain.param.TestStartParam;
import net.wanji.business.domain.vo.IndexCustomWeightVo;
import net.wanji.business.domain.vo.IndexWeightDetailsVo;
import net.wanji.business.domain.vo.SceneIndexSchemeVo;
import net.wanji.business.domain.vo.SceneWeightDetailsVo;
import net.wanji.business.service.RestService;
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
import org.springframework.util.CollectionUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

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

    @Value("${tess.routingPlan}")
    private String routingPlanUrl;

    @Value("${masterControl.queryDeviceReadyState}")
    private String queryDeviceReadyStateUrl;

    @Value("${masterControl.sendRule}")
    private String sendRuleUrl;

    @Value("${tess.server}")
    private String tessServerUrl;

    @Value("${tess.sceneIndexScheme}")
    private String sceneIndexSchemeUrl;

    @Value("${tess.weightDetails}")
    private String weightDetailsUrl;

    @Value("${tess.indexCustomWeight}")
    private String indexCustomWeightUrl;

    @Value("${tess.saveTaskScheme}")
    private String saveTaskSchemeUrl;

    @Value("${tess.saveCustomScenarioWeight}")
    private String saveCustomScenarioWeightUrl;

    @Value("${tess.saveCustomIndexWeight}")
    private String saveCustomIndexWeightUrl;

    @Value("${tess.downloadTestReport}")
    private String downloadTestReportUrl;

    @Value("${masterControl.sendCaseTrajectoryInfo}")
    private String sendCaseTrajectoryInfoUrl;

    @Value("${masterControl.manualTermination}")
    private String sendManualTerminationUrl;

    @Override
    public boolean startServer(String ip, Integer port, TessParam tessParam) {
        try {
            String resultUrl = ip + ":" + port + tessServerUrl;
            log.info("============================== tessServerUrl：{}", resultUrl);
            HttpHeaders httpHeaders = new HttpHeaders();
            httpHeaders.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<TessParam> resultHttpEntity = new HttpEntity<>(tessParam, httpHeaders);
            log.info("============================== tessServerUrl：{}", JSONObject.toJSONString(tessParam));
            ResponseEntity<String> response =
                    restTemplate.exchange(resultUrl, HttpMethod.POST, resultHttpEntity, String.class);
            if (response.getStatusCodeValue() == 200) {
                JSONObject result = JSONObject.parseObject(response.getBody(), JSONObject.class);
                log.info("============================== tess server start result:{}", JSONObject.toJSONString(result));
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
    public boolean start(String ip, Integer port, TestStartParam startParam) {
        try {
            String resultUrl = ip + ":" + port + tessStartUrl;
            log.info("============================== tess start：{}", resultUrl);
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
    public boolean startRoutingPlan(String ip, Integer port, Map<String, Object> params) {
        try {
            String resultUrl = ip + ":" + port + routingPlanUrl;
            log.info("============================== tess routing plan ：{}", resultUrl);
            HttpHeaders httpHeaders = new HttpHeaders();
            httpHeaders.setContentType(MediaType.APPLICATION_JSON);
            Map<String, Object> param = new HashMap<>();
            param.put("params", params.get("cases"));
            HttpEntity<Map<String, Object>> resultHttpEntity = new HttpEntity<>(param, httpHeaders);
            log.info("============================== tess routing plan param：{}", param.size());
            ResponseEntity<String> response =
                    restTemplate.exchange(resultUrl, HttpMethod.POST, resultHttpEntity, String.class);
            if (response.getStatusCodeValue() == 200) {
                JSONObject result = JSONObject.parseObject(response.getBody(), JSONObject.class);
                log.info("============================== tess routing plan result:{}", JSONObject.toJSONString(result));
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
    public boolean sendCaseTrajectoryInfo(CaseTrajectoryParam param) {
        try {
            String resultUrl = sendCaseTrajectoryInfoUrl;
            log.info("============================== sendCaseTrajectoryInfoUrl：{}", resultUrl);
            HttpHeaders httpHeaders = new HttpHeaders();
            httpHeaders.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<CaseTrajectoryParam> resultHttpEntity = new HttpEntity<>(param, httpHeaders);
            log.info("============================== sendCaseTrajectoryInfo param：{}", JSONObject.toJSONString(param));
            ResponseEntity<String> response =
                    restTemplate.exchange(resultUrl, HttpMethod.POST, resultHttpEntity, String.class);
            if (response.getStatusCodeValue() == 200) {
                if (!"true".equals(response.getBody())) {
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
    public boolean sendManualTermination(Integer taskId, Integer caseId) {
        try {
            String resultUrl = sendManualTerminationUrl;
            log.info("============================== sendManualTerminationUrl：{}", resultUrl);
            Map<String, Object> param = new HashMap<>();
            param.put("taskId", taskId);
            param.put("caseId", caseId);
            HttpHeaders httpHeaders = new HttpHeaders();
            httpHeaders.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> resultHttpEntity = new HttpEntity<>(param, httpHeaders);
            log.info("============================== sendManualTermination param：{}", JSONObject.toJSONString(param));
            ResponseEntity<String> response =
                    restTemplate.exchange(resultUrl, HttpMethod.POST, resultHttpEntity, String.class);
            if (response.getStatusCodeValue() == 200) {
                if (!"true".equals(response.getBody())) {
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
    public List<SceneIndexSchemeVo> getSceneIndexSchemeList(TaskSaveDto taskSaveDto) {
        List<SceneIndexSchemeVo> sceneIndexSchemeVos = new ArrayList<>();
        try {
            String resultUrl = sceneIndexSchemeUrl;
            // 使用 UriComponentsBuilder 构建带参数的 URL
            UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(resultUrl)
                    .queryParam("type", taskSaveDto.getType());

            // 构建最终的 URL
            String url = builder.toUriString();

            log.info("============================== sceneIndexSchemeUrl：{}", url);
            ResponseEntity<String> response =
                    restTemplate.exchange(url, HttpMethod.GET, null, String.class);
            if (response.getStatusCodeValue() == 200) {
                JSONObject result = JSONObject.parseObject(response.getBody(), JSONObject.class);
                log.info("============================== sceneIndexScheme  result:{}", JSONObject.toJSONString(result));
                if (Objects.isNull(result) || !"0".equals(result.get("status").toString())) {
                    log.error("远程服务调用失败:{}", result.get("msg"));
                    return sceneIndexSchemeVos;
                }

                if (result.get("data") != null){
                    sceneIndexSchemeVos = JSONObject.parseArray(result.get("data").toString(), SceneIndexSchemeVo.class);
                }
                return sceneIndexSchemeVos;
            }
        } catch (Exception e) {
            log.error("远程服务调用失败:{}", e);
        }
        return sceneIndexSchemeVos;
    }

    @Override
    public List<SceneWeightDetailsVo> getSceneWeightDetailsById(String id) {
        List<SceneWeightDetailsVo> sceneWeightDetailsVos = new ArrayList<>();
        try {
            String resultUrl = weightDetailsUrl;
            // 使用 UriComponentsBuilder 构建带参数的 URL
            UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(resultUrl)
                    .queryParam("id", id);

            // 构建最终的 URL
            String url = builder.toUriString();

            log.info("============================== weightDetailsUrl：{}", url);
            ResponseEntity<String> response =
                    restTemplate.exchange(url, HttpMethod.GET, null, String.class);
            if (response.getStatusCodeValue() == 200) {
                JSONObject result = JSONObject.parseObject(response.getBody(), JSONObject.class);
                log.info("============================== sceneWeightDetails  result:{}", JSONObject.toJSONString(result));
                if (Objects.isNull(result) || !"0".equals(result.get("status").toString())) {
                    log.error("远程服务调用失败:{}", result.get("msg"));
                    return sceneWeightDetailsVos;
                }
                if (result.get("data") != null){
                    JSONObject data = JSONObject.parseObject(result.get("data").toString());
                    sceneWeightDetailsVos = JSONObject.parseArray(data.get("list").toString(), SceneWeightDetailsVo.class);
                }
                return sceneWeightDetailsVos;
            }
        } catch (Exception e) {
            log.error("远程服务调用失败:{}", e);
        }
        return sceneWeightDetailsVos;
    }

    @Override
    public List<IndexWeightDetailsVo> getIndexWeightDetailsById(String id) {
        List<IndexWeightDetailsVo> indexWeightDetailsVos = new ArrayList<>();
        try {
            String resultUrl = weightDetailsUrl;
            // 使用 UriComponentsBuilder 构建带参数的 URL
            UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(resultUrl)
                    .queryParam("id", id);

            // 构建最终的 URL
            String url = builder.toUriString();

            log.info("============================== weightDetailsUrl：{}", url);
            ResponseEntity<String> response =
                    restTemplate.exchange(url, HttpMethod.GET, null, String.class);
            if (response.getStatusCodeValue() == 200) {
                JSONObject result = JSONObject.parseObject(response.getBody(), JSONObject.class);
                log.info("============================== weightDetails  result:{}", JSONObject.toJSONString(result));
                if (Objects.isNull(result) || !"0".equals(result.get("status").toString())) {
                    log.error("远程服务调用失败:{}", result.get("msg"));
                    return indexWeightDetailsVos;
                }
                if (result.get("data") != null){
                    JSONObject data = JSONObject.parseObject(result.get("data").toString());

                    JSONArray listTop = JSONObject.parseArray(data.get("list_top").toString());
                    IndexWeightDetailsVo indexWeightDetailsVo;
                    for(Object value : listTop){
                        JSONObject value1 = JSONObject.parseObject(value.toString());

                        indexWeightDetailsVo = new IndexWeightDetailsVo();

                        String code = value1.get("code").toString();
                        indexWeightDetailsVo.setCode(code);
                        indexWeightDetailsVo.setIndexName(value1.get("indexName").toString());
                        indexWeightDetailsVo.setWeight(Double.parseDouble(value1.get("weight").toString()));

                        List<IndexWeightDetailsVo.IndexWeightDetails> list = JSONObject.parseArray(data.get("list").toString(), IndexWeightDetailsVo.IndexWeightDetails.class);

                        // 使用 Stream API 过滤出年龄等于 0 的人
                        List<IndexWeightDetailsVo.IndexWeightDetails> collect = list.stream()
                                .filter(e -> e.getParentCode().equals(code))
                                .collect(Collectors.toList());

                        indexWeightDetailsVo.setList(collect);

                        indexWeightDetailsVos.add(indexWeightDetailsVo);
                    }
                }
                return indexWeightDetailsVos;
            }
        } catch (Exception e) {
            log.error("远程服务调用失败:{}", e);
        }
        return indexWeightDetailsVos;
    }

    @Override
    public List<IndexCustomWeightVo> getValuationIndexCustomWeight() {

        List<IndexCustomWeightVo> indexCustomWeightVos = new ArrayList<>();
        try {
            String resultUrl = indexCustomWeightUrl;
            // 使用 UriComponentsBuilder 构建带参数的 URL
            UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(resultUrl)
                    .queryParam("parentCode", "0");

            // 构建最终的 URL
            String url = builder.toUriString();

            log.info("============================== indexCustomWeightUrl：{}", url);
            ResponseEntity<String> response =
                    restTemplate.exchange(url, HttpMethod.GET, null, String.class);
            if (response.getStatusCodeValue() == 200) {
                JSONObject result = JSONObject.parseObject(response.getBody(), JSONObject.class);
                log.info("============================== IndexCustomWeight  result:{}", JSONObject.toJSONString(result));
                if (Objects.isNull(result) || !"0".equals(result.get("status").toString())) {
                    log.error("远程服务调用失败:{}", result.get("msg"));
                    return indexCustomWeightVos;
                }
                if (result.get("data") != null){
                    JSONArray data = JSONObject.parseArray(result.get("data").toString());

                    IndexCustomWeightVo indexCustomWeightVo;
                    for(Object value : data){
                        JSONObject value1 = JSONObject.parseObject(value.toString());

                        indexCustomWeightVo = new IndexCustomWeightVo();

                        String code = value1.get("code").toString();
                        indexCustomWeightVo.setCode(code);
                        indexCustomWeightVo.setName(value1.get("name").toString());
                        indexCustomWeightVo.setDefaultWeight(Double.parseDouble(value1.get("defaultWeight").toString()));

                        List<IndexCustomWeightVo.IndexWeightDetails> indexWeightDetails = getIndexWeightDetails(code);

                        indexCustomWeightVo.setList(indexWeightDetails);

                        indexCustomWeightVos.add(indexCustomWeightVo);
                    }
                }
                return indexCustomWeightVos;
            }
        } catch (Exception e) {
            log.error("远程服务调用失败:{}", e);
        }
        return indexCustomWeightVos;
    }

    public List<IndexCustomWeightVo.IndexWeightDetails> getIndexWeightDetails(String code) {
        List<IndexCustomWeightVo.IndexWeightDetails> indexWeightDetailsVos = new ArrayList<>();
        try {
            String resultUrl = indexCustomWeightUrl;
            // 使用 UriComponentsBuilder 构建带参数的 URL
            UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(resultUrl)
                    .queryParam("parentCode", code);

            // 构建最终的 URL
            String url = builder.toUriString();

            log.info("============================== indexCustomWeightUrl：{}", url);
            ResponseEntity<String> response =
                    restTemplate.exchange(url, HttpMethod.GET, null, String.class);
            if (response.getStatusCodeValue() == 200) {
                JSONObject result = JSONObject.parseObject(response.getBody(), JSONObject.class);
                log.info("============================== IndexWeightDetails  result:{}", JSONObject.toJSONString(result));
                if (Objects.isNull(result) || !"0".equals(result.get("status").toString())) {
                    log.error("远程服务调用失败:{}", result.get("msg"));
                    return indexWeightDetailsVos;
                }
                if (result.get("data") != null){
                    List<IndexCustomWeightVo.IndexWeightDetails> list = JSONObject.parseArray(result.get("data").toString(), IndexCustomWeightVo.IndexWeightDetails.class);

                    // 使用 Stream API 过滤出年龄等于 0 的人
                    indexWeightDetailsVos = list.stream()
                            .filter(e -> e.getParentCode().equals(code))
                            .collect(Collectors.toList());
                }
                return indexWeightDetailsVos;
            }
        } catch (Exception e) {
            log.error("远程服务调用失败:{}", e);
        }
        return indexWeightDetailsVos;
    }

    @Override
    public Map<String, String> saveTaskScheme(SaveTaskSchemeBo saveTaskSchemeBo) {
        Map<String, String> resultMap = new HashMap<>();
        resultMap.put("code", "200");
        resultMap.put("msg", "创建任务和方案关联成功!");
        try {
            String resultUrl = saveTaskSchemeUrl;
            log.info("============================== saveTaskSchemeUrl：{}", saveTaskSchemeUrl);
            HttpHeaders httpHeaders = new HttpHeaders();
            httpHeaders.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<SaveTaskSchemeBo> resultHttpEntity = new HttpEntity<>(saveTaskSchemeBo, httpHeaders);
            log.info("============================== saveTaskScheme：{}", JSONObject.toJSONString(saveTaskSchemeBo));
            ResponseEntity<String> response =
                    restTemplate.exchange(resultUrl, HttpMethod.POST, resultHttpEntity, String.class);
            if (response.getStatusCodeValue() == 200) {
                JSONObject result = JSONObject.parseObject(response.getBody(), JSONObject.class);
                log.info("============================== saveTaskScheme  result:{}", JSONObject.toJSONString(result));
                if (Objects.isNull(result) || !"0".equals(result.get("status").toString())) {
                    log.error("远程服务调用失败:{}", result.get("msg"));
                    resultMap.put("code", "500");
                    resultMap.put("msg", result.get("msg").toString());
                    return resultMap;
                }
                return resultMap;
            }
        } catch (Exception e) {
            log.error("远程服务调用失败:{}", e);
            resultMap.put("code", "500");
            resultMap.put("msg", "济达创建任务和方案关联接口异常!");
        }
        return resultMap;
    }

    @Override
    public Map<String, String> saveCustomScenarioWeight(SaveCustomScenarioWeightBo saveCustomScenarioWeightBo) {
        Map<String, String> resultMap = new HashMap<>();
        resultMap.put("code", "200");
        resultMap.put("msg", "自定义场景权重保存成功!");
        try {
            String resultUrl = saveCustomScenarioWeightUrl;
            log.info("============================== saveCustomScenarioWeightUrl：{}", saveCustomScenarioWeightUrl);
            HttpHeaders httpHeaders = new HttpHeaders();
            httpHeaders.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<SaveCustomScenarioWeightBo> resultHttpEntity = new HttpEntity<>(saveCustomScenarioWeightBo, httpHeaders);
            log.info("============================== saveCustomScenarioWeight：{}", JSONObject.toJSONString(saveCustomScenarioWeightBo));
            ResponseEntity<String> response =
                    restTemplate.exchange(resultUrl, HttpMethod.POST, resultHttpEntity, String.class);
            if (response.getStatusCodeValue() == 200) {
                JSONObject result = JSONObject.parseObject(response.getBody(), JSONObject.class);
                log.info("============================== saveCustomScenarioWeight  result:{}", JSONObject.toJSONString(result));
                if (Objects.isNull(result) || !"0".equals(result.get("status").toString())) {
                    log.error("远程服务调用失败:{}", result.get("msg"));
                    resultMap.put("code", "500");
                    resultMap.put("msg", result.get("msg").toString());
                    return resultMap;
                }
                return resultMap;
            }
        } catch (Exception e) {
            log.error("远程服务调用失败:{}", e);
            resultMap.put("code", "500");
            resultMap.put("msg", "济达自定义-场景权重创建接口异常!");
        }
        return resultMap;
    }

    @Override
    public Map<String, String> saveCustomIndexWeight(SaveCustomIndexWeightBo saveCustomIndexWeightBo) {
        Map<String, String> resultMap = new HashMap<>();
        resultMap.put("code", "200");
        resultMap.put("msg", "自定义指标权重保存成功!");
        try {
            String resultUrl = saveCustomIndexWeightUrl;
            log.info("============================== saveCustomIndexWeightUrl：{}", saveCustomIndexWeightUrl);
            HttpHeaders httpHeaders = new HttpHeaders();
            httpHeaders.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<SaveCustomIndexWeightBo> resultHttpEntity = new HttpEntity<>(saveCustomIndexWeightBo, httpHeaders);
            log.info("============================== saveCustomIndexWeight：{}", JSONObject.toJSONString(saveCustomIndexWeightBo));
            ResponseEntity<String> response =
                    restTemplate.exchange(resultUrl, HttpMethod.POST, resultHttpEntity, String.class);
            if (response.getStatusCodeValue() == 200) {
                JSONObject result = JSONObject.parseObject(response.getBody(), JSONObject.class);
                log.info("============================== saveCustomIndexWeight  result:{}", JSONObject.toJSONString(result));
                if (Objects.isNull(result) || !"0".equals(result.get("status").toString())) {
                    log.error("远程服务调用失败:{}", result.get("msg"));
                    resultMap.put("code", "500");
                    resultMap.put("msg", result.get("msg").toString());
                    return resultMap;
                }
                return resultMap;
            }
        } catch (Exception e) {
            log.error("远程服务调用失败:{}", e);
            resultMap.put("code", "500");
            resultMap.put("msg", "济达场景方案&指标方案创建接口异常!");
        }
        return resultMap;
    }

    @Override
    public void downloadTestReport(HttpServletResponse response, int taskId) {
        try {
            String url = downloadTestReportUrl + "/" + taskId;
            // 发送HTTP GET请求获取文件字节数组
            ResponseEntity<byte[]> responseEntity = restTemplate.exchange(url, HttpMethod.GET, null, byte[].class);
            if(CollectionUtils.isEmpty(responseEntity.getHeaders().get(HttpHeaders.CONTENT_DISPOSITION))){
               return;
            }

            MediaType contentType = responseEntity.getHeaders().getContentType();
            String contentTypeStr;
            if(contentType != null){
                contentTypeStr = contentType.toString();
            }else{
                contentTypeStr = "multipart/form-data";
            }
            // 设置响应头
            response.setContentType(contentTypeStr);
            response.setHeader(HttpHeaders.CONTENT_DISPOSITION, responseEntity.getHeaders().get(HttpHeaders.CONTENT_DISPOSITION).get(0));

            // 将文件字节数组写入响应流
            response.getOutputStream().write(Objects.requireNonNull(responseEntity.getBody()));
        }catch (Exception e){
            e.printStackTrace();
        }finally {
            if(response != null){
                try {
                    response.getOutputStream().flush();
                    response.getOutputStream().close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

}
