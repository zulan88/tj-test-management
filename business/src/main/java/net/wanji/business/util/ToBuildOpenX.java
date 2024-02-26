package net.wanji.business.util;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import net.wanji.business.domain.bo.ParticipantTrajectoryBo;
import net.wanji.business.domain.vo.FragmentedScenesDetailVo;
import net.wanji.business.entity.TjResourcesDetail;
import net.wanji.business.entity.TjScenelib;
import net.wanji.business.entity.TjTaskCase;
import net.wanji.business.exception.BusinessException;
import net.wanji.business.mapper.TjTaskCaseMapper;
import net.wanji.business.service.*;
import net.wanji.common.common.ClientSimulationTrajectoryDto;
import net.wanji.common.common.TrajectoryValueDto;
import net.wanji.common.config.WanjiConfig;
import net.wanji.common.constant.Constants;
import net.wanji.common.utils.DateUtils;
import net.wanji.common.utils.StringUtils;
import net.wanji.common.utils.file.FileUploadUtils;
import net.wanji.openx.generated.*;
import net.wanji.openx.generated.File;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.locationtech.proj4j.*;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import java.io.*;
import java.nio.file.Files;
import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.*;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.util.Enumeration;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

@Component
public class ToBuildOpenX {

    @Autowired
    private TjFragmentedSceneDetailService tjFragmentedSceneDetailService;

    @Autowired
    ITjScenelibService scenelibService;

    @Autowired
    private TjResourcesDetailService tjResourcesDetailService;

    @Autowired
    private TjTaskCaseMapper taskCaseMapper;

    private static final CoordinateTransformFactory ctFactory = new CoordinateTransformFactory();
    private static final CRSFactory crsFactory = new CRSFactory();

    @Async
    public void scenetoOpenX(FragmentedScenesDetailVo fragmentedScenesDetailVo, Long id, Integer type) throws RuntimeException {
        try {
            if (type == null) {
                type = 0;
            }
            //入参
            String c1 = "tjtest.xodr";
            String proj = "+proj=tmerc +lon_0=121.20585769414902 +lat_0=31.290823210868965 +ellps=WGS84";

            String outputFolder = WanjiConfig.getScenelibPath() + java.io.File.separator + DateUtils.datePath();
            ;
            java.io.File folder = new java.io.File(outputFolder);
            if (!folder.exists()) {
                folder.mkdirs();
            }

            java.io.File xodrfile = new java.io.File(WanjiConfig.getScenelibPath(), c1);

            if (fragmentedScenesDetailVo.getResourcesDetailId() != null) {
                TjResourcesDetail tjResourcesDetail = tjResourcesDetailService.getById(fragmentedScenesDetailVo.getResourcesDetailId());
                if (tjResourcesDetail.getAttribute5().isEmpty()) {
                    String localPath = WanjiConfig.getProfile();
                    String downloadPath = localPath + StringUtils.substringAfter(tjResourcesDetail.getFilePath(), Constants.RESOURCE_PREFIX);
                    java.io.File file = new java.io.File(downloadPath);
                    ZipFile zipFile = new ZipFile(file);
                    Enumeration<? extends ZipEntry> entries = zipFile.entries();
                    while (entries.hasMoreElements()) {
                        ZipEntry entry = entries.nextElement();
                        String entryName = (int) (System.currentTimeMillis() % 1000) + "_" + entry.getName();
                        java.io.File entryFile = new java.io.File(outputFolder, entryName);

                        if (entry.isDirectory()) {
                            entryFile.mkdirs();
                        } else {
                            InputStream inputStream = zipFile.getInputStream(entry);
                            FileOutputStream outputStream = new FileOutputStream(entryFile);

                            byte[] buffer = new byte[1024];
                            int length;
                            while ((length = inputStream.read(buffer)) > 0) {
                                outputStream.write(buffer, 0, length);
                            }

                            inputStream.close();
                            outputStream.close();
                        }
                        String filepath = entryFile.getAbsolutePath();
                        if (filepath.endsWith("xodr")) {
                            tjResourcesDetail.setAttribute5(filepath);
                            xodrfile = new java.io.File(filepath);
                            c1 = entryName;
                            BufferedReader reader = new BufferedReader(new FileReader(filepath));
                            StringBuilder fileContent = new StringBuilder();
                            String line;
                            while ((line = reader.readLine()) != null) {
                                fileContent.append(line);
                            }
                            reader.close();
                            // 使用正则表达式提取proj参数值
                            String regex = "\\+proj=[^\\s]+.*?\\]";
                            Pattern pattern = Pattern.compile(regex);
                            Matcher matcher = pattern.matcher(fileContent.toString());
                            if (matcher.find()) {
                                String projValue = matcher.group();
                                proj = projValue.substring(0, projValue.length() - 1);
                            } else {
                                //异常处理
                                System.out.println("未提取到proj参数");
                            }
                            break;
                        }
                    }
                    tjResourcesDetailService.updateById(tjResourcesDetail);
                } else {
                    String filepath = tjResourcesDetail.getAttribute5();
                    xodrfile = new java.io.File(filepath);
                    c1 = StringUtils.substringAfterLast(filepath, java.io.File.separator);
                    BufferedReader reader = new BufferedReader(new FileReader(filepath));
                    StringBuilder fileContent = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        fileContent.append(line);
                    }
                    reader.close();
                    // 使用正则表达式提取proj参数值
                    String regex = "\\+proj=[^\\s]+.*?\\]";
                    Pattern pattern = Pattern.compile(regex);
                    Matcher matcher = pattern.matcher(fileContent.toString());
                    if (matcher.find()) {
                        String projValue = matcher.group();
                        proj = projValue.substring(0, projValue.length() - 1);
                    } else {
                        //异常处理
                        System.out.println("未提取到proj参数");
                    }
                }
            }

            OpenScenario openScenario = new OpenScenario();
            FileHeader fileHeader = new FileHeader();
            fileHeader.setRevMajor("1");
            fileHeader.setRevMinor("0");
            LocalDateTime currentDateTime = LocalDateTime.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
            String formattedDateTime = currentDateTime.format(formatter);
            fileHeader.setDate(formattedDateTime);
            fileHeader.setDescription("scenario_NDS");
            fileHeader.setAuthor("OnStte_wanji");
            openScenario.setFileHeader(fileHeader);
            RoadNetwork roadNetwork = new RoadNetwork();
            File opendrive = new File();
            opendrive.setFilepath(c1);
            roadNetwork.setLogicFile(opendrive);
            openScenario.setRoadNetwork(roadNetwork);
            Entities entities = new Entities();
            List<ScenarioObject> scenarioObjectList = entities.getScenarioObject();
            String xmlInit = "<Init>\n" +
                    "    <Actions>\n" +
                    "        <GlobalAction>\n" +
                    "             <EnvironmentAction>\n" +
                    "                <Environment name=\"Default_Environment\">\n" +
                    "                    <TimeOfDay animation=\"false\" dateTime=\"2021-12-13T17:00:00\" />\n" +
                    "                    <Weather cloudState=\"free\">\n" +
                    "                        <Sun intensity=\"1.0\" azimuth=\"0.0\" elevation=\"1.571\" />\n" +
                    "                        <Fog visualRange=\"100000.0\" />\n" +
                    "                        <Precipitation precipitationType=\"dry\" intensity=\"0.0\" />\n" +
                    "                    </Weather>\n" +
                    "                    <RoadCondition frictionScaleFactor=\"1.0\" />\n" +
                    "                </Environment>\n" +
                    "            </EnvironmentAction>\n" +
                    "        </GlobalAction>\n" +
                    "    </Actions>\n" +
                    "</Init>";
            JAXBContext context = JAXBContext.newInstance(Init.class);
            Unmarshaller unmarshaller = context.createUnmarshaller();
            Init init = (Init) unmarshaller.unmarshal(new StringReader(xmlInit));
            init.getActions().getGlobalAction().get(0).getEnvironmentAction().getEnvironment().getTimeOfDay().setDateTime(formattedDateTime);
            for (ParticipantTrajectoryBo participantTrajectoryBo : fragmentedScenesDetailVo.getTrajectoryJson().getParticipantTrajectories()) {
                ScenarioObject scenarioObject = new ScenarioObject();
                scenarioObject.setName(participantTrajectoryBo.getId());
                Vehicle vehicle = new Vehicle("default", participantTrajectoryBo.getType());
                scenarioObject.setVehicle(vehicle);
                scenarioObjectList.add(scenarioObject);
                Private privateone = new Private();
                privateone.setEntityRef(scenarioObject.getName());
                init.getActions().getPrivate().add(privateone);
            }
            openScenario.setEntities(entities);
            Storyboard storyboard = new Storyboard();
            Story story = new Story();
            story.setName("mystore");
            Double maxTime = 0D;
            DecimalFormat df = new DecimalFormat("0.00");
            for (ParticipantTrajectoryBo participantTrajectoryBo : fragmentedScenesDetailVo.getTrajectoryJson().getParticipantTrajectories()) {
                Act act = new Act();
                act.setName("Act_" + participantTrajectoryBo.getId());
                ManeuverGroup maneuverGroup = new ManeuverGroup();
                maneuverGroup.setName("Squence_" + participantTrajectoryBo.getId());
                Actors actors = new Actors();
                actors.setSelectTriggeringEntities("false");
                EntityRef entityRef = new EntityRef();
                entityRef.setEntityRef(participantTrajectoryBo.getId());
                actors.getEntityRef().add(entityRef);
                Maneuver maneuver = new Maneuver();
                maneuver.setName("Maneuver1");
                Event event = new Event();
                event.setName("Event1");
                event.setPriority("overwrite");
                Action action = new Action();
                action.setName("Action1");
                PrivateAction privateAction = new PrivateAction();
                RoutingAction routingAction = new RoutingAction();
                FollowTrajectoryAction followTrajectoryAction = new FollowTrajectoryAction();
                Trajectory trajectory = new Trajectory();
                trajectory.setName("Trajectory_" + participantTrajectoryBo.getId());
                trajectory.setClosed("false");
                Shape shape = new Shape();
                Polyline polyline = new Polyline();
                Double base = null;
                List<List<TrajectoryValueDto>> routelist = tjFragmentedSceneDetailService.getroutelist(fragmentedScenesDetailVo.getId(), participantTrajectoryBo.getId(), type);
                for (List<TrajectoryValueDto> trajectoryValueDtos : routelist) {
                    if (trajectoryValueDtos.size() > 0) {
                        TrajectoryValueDto trajectoryValueDto = trajectoryValueDtos.get(0);
                        if (base == null) {
                            base = Double.valueOf(trajectoryValueDto.getGlobalTimeStamp());
                        }
                        Vertex vertex = new Vertex();
                        Double time = Double.valueOf(trajectoryValueDto.getGlobalTimeStamp());
                        vertex.setTime(df.format(time - base));
                        if ((time - base) > maxTime) {
                            maxTime = (time - base);
                        }
                        Position position = new Position();
                        position.setWorldPosition(totrans(trajectoryValueDto.getLongitude(), trajectoryValueDto.getLatitude(), proj, trajectoryValueDto.getCourseAngle()));
                        vertex.setPosition(position);
                        polyline.getVertex().add(vertex);
                    }
                }
                shape.setPolyline(polyline);
                trajectory.setShape(shape);
                followTrajectoryAction.setTrajectory(trajectory);
                TimeReference timeReference = new TimeReference();
                Timing timing = new Timing();
                timing.setDomainAbsoluteRelative("absolute");
                timing.setScale("1.0");
                timing.setOffset("0.0");
                timeReference.setTiming(timing);
                followTrajectoryAction.setTimeReference(timeReference);
                TrajectoryFollowingMode trajectoryFollowingMode = new TrajectoryFollowingMode();
                trajectoryFollowingMode.setFollowingMode("follow");
                followTrajectoryAction.setTrajectoryFollowingMode(trajectoryFollowingMode);
                routingAction.setFollowTrajectoryAction(followTrajectoryAction);
                privateAction.setRoutingAction(routingAction);
                action.setPrivateAction(privateAction);
                Trigger startTrigger = new Trigger();
                ConditionGroup conditionGroup = new ConditionGroup();
                Condition condition = new Condition("none", "0.03");
                conditionGroup.getCondition().add(condition);
                startTrigger.getConditionGroup().add(conditionGroup);
                event.getAction().add(action);
                event.setStartTrigger(startTrigger);
                maneuver.getEvent().add(event);
                maneuverGroup.setActors(actors);
                maneuverGroup.getManeuver().add(maneuver);
                act.getManeuverGroup().add(maneuverGroup);
                Trigger actstartTrigger = new Trigger();
                ConditionGroup actconditionGroup = new ConditionGroup();
                Condition actcondition = new Condition("rising", "0");
                actconditionGroup.getCondition().add(actcondition);
                actstartTrigger.getConditionGroup().add(actconditionGroup);
                act.setStartTrigger(actstartTrigger);
                story.getAct().add(act);
            }
            storyboard.setInit(init);
            storyboard.getStory().add(story);
            Trigger endTrigger = new Trigger();
            ConditionGroup endconditionGroup = new ConditionGroup();
            Condition endcondition = new Condition("rising", df.format(maxTime + 0.08));
            endconditionGroup.getCondition().add(endcondition);
            endTrigger.getConditionGroup().add(endconditionGroup);
            storyboard.setStopTrigger(endTrigger);
            openScenario.setStoryboard(storyboard);

            JAXBContext jaxbContext = JAXBContext.newInstance(OpenScenario.class);
            Marshaller marshaller = jaxbContext.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);

//            marshaller.marshal(openScenario, System.out);
            java.io.File file = new java.io.File(outputFolder, fragmentedScenesDetailVo.getNumber() + (int) (System.currentTimeMillis() % 1000) + ".xosc");
            OutputStream outputStream = Files.newOutputStream(file.toPath());

            StringWriter stringWriter = new StringWriter();

            marshaller.marshal(openScenario, stringWriter);
            Transformer transformer = TransformerFactory.newInstance().newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");

            transformer.transform(new StreamSource(new StringReader(stringWriter.toString()))
                    , new StreamResult(outputStream));


            TjScenelib tjScenelib = new TjScenelib();
            tjScenelib.setId(id);
            tjScenelib.setXodrPath(xodrfile.getPath());
            tjScenelib.setXoscPath(file.getPath());

            java.io.File zipfile = new java.io.File(outputFolder, fragmentedScenesDetailVo.getNumber() + (int) (System.currentTimeMillis() % 1000) + ".zip");

            FileOutputStream fos = new FileOutputStream(zipfile);
            ZipOutputStream zos = new ZipOutputStream(fos);
            zipfile(xodrfile, zos);
            zipfile(file, zos);
            zos.close();
            fos.close();

            tjScenelib.setZipPath(FileUploadUtils.getPathFileName(outputFolder, zipfile.getName()));
            tjScenelib.setImgPath(fragmentedScenesDetailVo.getImgUrl());
            scenelibService.updateTjScenelib(tjScenelib);


        } catch (JAXBException e) {
            e.printStackTrace();
        } catch (BusinessException | IOException e) {
            throw new RuntimeException(e);
        } catch (TransformerConfigurationException e) {
            throw new RuntimeException(e);
        } catch (TransformerException e) {
            throw new RuntimeException(e);
        }
    }

    @Async
    public void casetoOpenX(List<List<ClientSimulationTrajectoryDto>> trajectories, Integer taskId, Integer caseId, TjResourcesDetail tjResourcesDetail) {
        try {
            //入参
            String c1 = "tjtest.xodr";
            String proj = "+proj=tmerc +lon_0=121.20585769414902 +lat_0=31.290823210868965 +ellps=WGS84";

            String outputFolder = WanjiConfig.getScenelibPath() + java.io.File.separator + DateUtils.datePath();
            java.io.File folder = new java.io.File(outputFolder);
            if (!folder.exists()) {
                folder.mkdirs();
            }

            java.io.File xodrfile = new java.io.File(WanjiConfig.getScenelibPath(), c1);

            if(tjResourcesDetail!=null){
                if (tjResourcesDetail.getAttribute5().isEmpty()) {
                    String localPath = WanjiConfig.getProfile();
                    String downloadPath = localPath + StringUtils.substringAfter(tjResourcesDetail.getFilePath(), Constants.RESOURCE_PREFIX);
                    java.io.File file = new java.io.File(downloadPath);
                    ZipFile zipFile = new ZipFile(file);
                    Enumeration<? extends ZipEntry> entries = zipFile.entries();
                    while (entries.hasMoreElements()) {
                        ZipEntry entry = entries.nextElement();
                        String entryName = (int) (System.currentTimeMillis() % 1000) + "_" + entry.getName();
                        java.io.File entryFile = new java.io.File(outputFolder, entryName);

                        if (entry.isDirectory()) {
                            entryFile.mkdirs();
                        } else {
                            InputStream inputStream = zipFile.getInputStream(entry);
                            FileOutputStream outputStream = new FileOutputStream(entryFile);

                            byte[] buffer = new byte[1024];
                            int length;
                            while ((length = inputStream.read(buffer)) > 0) {
                                outputStream.write(buffer, 0, length);
                            }

                            inputStream.close();
                            outputStream.close();
                        }
                        String filepath = entryFile.getAbsolutePath();
                        if (filepath.endsWith("xodr")) {
                            tjResourcesDetail.setAttribute5(filepath);
                            xodrfile = new java.io.File(filepath);
                            c1 = entryName;
                            BufferedReader reader = new BufferedReader(new FileReader(filepath));
                            StringBuilder fileContent = new StringBuilder();
                            String line;
                            while ((line = reader.readLine()) != null) {
                                fileContent.append(line);
                            }
                            reader.close();
                            // 使用正则表达式提取proj参数值
                            String regex = "\\+proj=[^\\s]+.*?\\]";
                            Pattern pattern = Pattern.compile(regex);
                            Matcher matcher = pattern.matcher(fileContent.toString());
                            if (matcher.find()) {
                                String projValue = matcher.group();
                                proj = projValue.substring(0, projValue.length() - 1);
                            } else {
                                //异常处理
                                System.out.println("未提取到proj参数");
                            }
                            break;
                        }
                    }
                    tjResourcesDetailService.updateById(tjResourcesDetail);
                } else {
                    String filepath = tjResourcesDetail.getAttribute5();
                    xodrfile = new java.io.File(filepath);
                    c1 = StringUtils.substringAfterLast(filepath, java.io.File.separator);
                    BufferedReader reader = new BufferedReader(new FileReader(filepath));
                    StringBuilder fileContent = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        fileContent.append(line);
                    }
                    reader.close();
                    // 使用正则表达式提取proj参数值
                    String regex = "\\+proj=[^\\s]+.*?\\]";
                    Pattern pattern = Pattern.compile(regex);
                    Matcher matcher = pattern.matcher(fileContent.toString());
                    if (matcher.find()) {
                        String projValue = matcher.group();
                        proj = projValue.substring(0, projValue.length() - 1);
                    } else {
                        //异常处理
                        System.out.println("未提取到proj参数");
                    }
                }
            }


            OpenScenario openScenario = new OpenScenario();
            FileHeader fileHeader = new FileHeader();
            fileHeader.setRevMajor("1");
            fileHeader.setRevMinor("0");
            LocalDateTime currentDateTime = LocalDateTime.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
            String formattedDateTime = currentDateTime.format(formatter);
            fileHeader.setDate(formattedDateTime);
            fileHeader.setDescription("scenario_NDS");
            fileHeader.setAuthor("OnStte_wanji");
            openScenario.setFileHeader(fileHeader);
            RoadNetwork roadNetwork = new RoadNetwork();
            File opendrive = new File();
            opendrive.setFilepath(c1);
            roadNetwork.setLogicFile(opendrive);
            openScenario.setRoadNetwork(roadNetwork);
            Entities entities = new Entities();
            List<ScenarioObject> scenarioObjectList = entities.getScenarioObject();
            String xmlInit = "<Init>\n" +
                    "    <Actions>\n" +
                    "        <GlobalAction>\n" +
                    "             <EnvironmentAction>\n" +
                    "                <Environment name=\"Default_Environment\">\n" +
                    "                    <TimeOfDay animation=\"false\" dateTime=\"2021-12-13T17:00:00\" />\n" +
                    "                    <Weather cloudState=\"free\">\n" +
                    "                        <Sun intensity=\"1.0\" azimuth=\"0.0\" elevation=\"1.571\" />\n" +
                    "                        <Fog visualRange=\"100000.0\" />\n" +
                    "                        <Precipitation precipitationType=\"dry\" intensity=\"0.0\" />\n" +
                    "                    </Weather>\n" +
                    "                    <RoadCondition frictionScaleFactor=\"1.0\" />\n" +
                    "                </Environment>\n" +
                    "            </EnvironmentAction>\n" +
                    "        </GlobalAction>\n" +
                    "    </Actions>\n" +
                    "</Init>";
            JAXBContext context = JAXBContext.newInstance(Init.class);
            Unmarshaller unmarshaller = context.createUnmarshaller();
            Init init = (Init) unmarshaller.unmarshal(new StringReader(xmlInit));
            init.getActions().getGlobalAction().get(0).getEnvironmentAction().getEnvironment().getTimeOfDay().setDateTime(formattedDateTime);
            if(trajectories.size() == 0){
                return;
            }
            List<ClientSimulationTrajectoryDto> fristsimulationTrajectoryDtos = trajectories.get(0);
            char c = 'a';
            for(ClientSimulationTrajectoryDto clientSimulationTrajectoryDto : fristsimulationTrajectoryDtos) {
                if (clientSimulationTrajectoryDto.getValue().size() > 0) {
                    for (TrajectoryValueDto trajectoryValueDto : clientSimulationTrajectoryDto.getValue()) {
                        ScenarioObject scenarioObject = new ScenarioObject();
                        scenarioObject.setName(c + trajectoryValueDto.getId());
                        Vehicle vehicle = new Vehicle("default", "veh" + trajectoryValueDto.getId());
                        scenarioObject.setVehicle(vehicle);
                        scenarioObjectList.add(scenarioObject);
                        Private privateone = new Private();
                        privateone.setEntityRef(scenarioObject.getName());
                        init.getActions().getPrivate().add(privateone);
                    }
                }
                c++;
            }
            openScenario.setEntities(entities);
            Storyboard storyboard = new Storyboard();
            Story story = new Story();
            story.setName("mystore");
            Double maxTime = 0D;
            DecimalFormat df = new DecimalFormat("0.00");
            int index = 0;
            int bindex = 0;
            c = 'a';
            for (ClientSimulationTrajectoryDto clientSimulationTrajectoryDto : fristsimulationTrajectoryDtos){
                for (TrajectoryValueDto trajectoryValueDto : clientSimulationTrajectoryDto.getValue()){
                    Act act = new Act();
                    act.setName("Act_" + c + trajectoryValueDto.getId());
                    ManeuverGroup maneuverGroup = new ManeuverGroup();
                    maneuverGroup.setName("Squence_" + c + trajectoryValueDto.getId());
                    Actors actors = new Actors();
                    actors.setSelectTriggeringEntities("false");
                    EntityRef entityRef = new EntityRef();
                    entityRef.setEntityRef(c + trajectoryValueDto.getId());
                    actors.getEntityRef().add(entityRef);
                    Maneuver maneuver = new Maneuver();
                    maneuver.setName("Maneuver1");
                    Event event = new Event();
                    event.setName("Event1");
                    event.setPriority("overwrite");
                    Action action = new Action();
                    action.setName("Action1");
                    PrivateAction privateAction = new PrivateAction();
                    RoutingAction routingAction = new RoutingAction();
                    FollowTrajectoryAction followTrajectoryAction = new FollowTrajectoryAction();
                    Trajectory trajectory = new Trajectory();
                    trajectory.setName("Trajectory_" + c + trajectoryValueDto.getId());
                    trajectory.setClosed("false");
                    Shape shape = new Shape();
                    Polyline polyline = new Polyline();
                    Double base = null;
                    for (List<ClientSimulationTrajectoryDto> simulationTrajectoryDtos : trajectories) {
                        try {
                            trajectoryValueDto = simulationTrajectoryDtos.get(bindex).getValue().get(index);
                            if (base == null) {
                                base = Double.valueOf(trajectoryValueDto.getTimestamp());
                            }
                            Vertex vertex = new Vertex();
                            Double time = Double.valueOf(trajectoryValueDto.getTimestamp());
                            vertex.setTime(df.format((time - base)/1000));
                            if ((time - base) / 1000 > maxTime) {
                                maxTime = (time - base) / 1000;
                            }
                            Position position = new Position();
                            position.setWorldPosition(totrans(trajectoryValueDto.getLongitude(), trajectoryValueDto.getLatitude(), proj, trajectoryValueDto.getCourseAngle()));
                            vertex.setPosition(position);
                            polyline.getVertex().add(vertex);
                        }catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    shape.setPolyline(polyline);
                    trajectory.setShape(shape);
                    followTrajectoryAction.setTrajectory(trajectory);
                    TimeReference timeReference = new TimeReference();
                    Timing timing = new Timing();
                    timing.setDomainAbsoluteRelative("absolute");
                    timing.setScale("1.0");
                    timing.setOffset("0.0");
                    timeReference.setTiming(timing);
                    followTrajectoryAction.setTimeReference(timeReference);
                    TrajectoryFollowingMode trajectoryFollowingMode = new TrajectoryFollowingMode();
                    trajectoryFollowingMode.setFollowingMode("follow");
                    followTrajectoryAction.setTrajectoryFollowingMode(trajectoryFollowingMode);
                    routingAction.setFollowTrajectoryAction(followTrajectoryAction);
                    privateAction.setRoutingAction(routingAction);
                    action.setPrivateAction(privateAction);
                    Trigger startTrigger = new Trigger();
                    ConditionGroup conditionGroup = new ConditionGroup();
                    Condition condition = new Condition("none", "0.03");
                    conditionGroup.getCondition().add(condition);
                    startTrigger.getConditionGroup().add(conditionGroup);
                    event.getAction().add(action);
                    event.setStartTrigger(startTrigger);
                    maneuver.getEvent().add(event);
                    maneuverGroup.setActors(actors);
                    maneuverGroup.getManeuver().add(maneuver);
                    act.getManeuverGroup().add(maneuverGroup);
                    Trigger actstartTrigger = new Trigger();
                    ConditionGroup actconditionGroup = new ConditionGroup();
                    Condition actcondition = new Condition("rising", "0");
                    actconditionGroup.getCondition().add(actcondition);
                    actstartTrigger.getConditionGroup().add(actconditionGroup);
                    act.setStartTrigger(actstartTrigger);
                    story.getAct().add(act);
                    index ++;
                }
                bindex ++;
                index = 0;
                c ++;
            }
            storyboard.setInit(init);
            storyboard.getStory().add(story);
            Trigger endTrigger = new Trigger();
            ConditionGroup endconditionGroup = new ConditionGroup();
            Condition endcondition = new Condition("rising", df.format(maxTime + 0.08));
            endconditionGroup.getCondition().add(endcondition);
            endTrigger.getConditionGroup().add(endconditionGroup);
            storyboard.setStopTrigger(endTrigger);
            openScenario.setStoryboard(storyboard);

            JAXBContext jaxbContext = JAXBContext.newInstance(OpenScenario.class);
            Marshaller marshaller = jaxbContext.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);

//            marshaller.marshal(openScenario, System.out);
            java.io.File file = new java.io.File(outputFolder, "task_"+ taskId + "_case_"+ caseId + "_" + (int) (System.currentTimeMillis() % 1000) + ".xosc");
            OutputStream outputStream = Files.newOutputStream(file.toPath());

            StringWriter stringWriter = new StringWriter();

            marshaller.marshal(openScenario, stringWriter);
            Transformer transformer = TransformerFactory.newInstance().newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");

            transformer.transform(new StreamSource(new StringReader(stringWriter.toString()))
                    , new StreamResult(outputStream));


            QueryWrapper<TjTaskCase> tjTaskCase = new QueryWrapper<>();
            tjTaskCase.eq("task_id", taskId);
            tjTaskCase.eq("case_id", caseId);
            tjTaskCase.orderByDesc("create_time");

            TjTaskCase taskCase = taskCaseMapper.selectOne(tjTaskCase);
            taskCase.setXodrPath(xodrfile.getPath());
            taskCase.setXoscPath(file.getPath());
            System.out.println(taskCase.getId());

            java.io.File zipfile = new java.io.File(outputFolder, "task_"+ taskId + "_case_"+ caseId + "_" + (int) (System.currentTimeMillis() % 1000) + ".zip");
            System.out.println(zipfile.getPath());

            FileOutputStream fos = new FileOutputStream(zipfile);
            ZipOutputStream zos = new ZipOutputStream(fos);
            zipfile(xodrfile, zos);
            zipfile(file, zos);
            zos.close();
            fos.close();

            taskCase.setZipPath(FileUploadUtils.getPathFileName(outputFolder, zipfile.getName()));
            taskCaseMapper.updateById(taskCase);


        } catch (JAXBException e) {
            e.printStackTrace();
        } catch (IOException | TransformerException e) {
            throw new RuntimeException(e);
        }
    }

    private void zipfile(java.io.File file, ZipOutputStream zos) throws IOException {
        FileInputStream fis = new FileInputStream(file);
        ZipEntry zipEntry = new ZipEntry(file.getName());
        zos.putNextEntry(zipEntry);

        byte[] buffer = new byte[1024];
        int length;
        while ((length = fis.read(buffer)) > 0) {
            zos.write(buffer, 0, length);
        }

        fis.close();
    }


    private CoordinateReferenceSystem createCRS(String crsSpec) {
        CoordinateReferenceSystem crs = null;
        if (crsSpec.contains("+") || crsSpec.contains("=")) {
            crs = crsFactory.createFromParameters("Anon", crsSpec);
        } else {
            crs = crsFactory.createFromName(crsSpec);
        }

        return crs;
    }

    private WorldPosition totrans(Double lon, Double lat, String tgtCRS, Double angle) {
        String WGS84_PARAM = "+proj=longlat +datum=WGS84 +no_defs ";
        CoordinateTransform trans = ctFactory
                .createTransform(createCRS(WGS84_PARAM), createCRS(tgtCRS));
        ProjCoordinate pout = new ProjCoordinate();
        ProjCoordinate p = new ProjCoordinate(lon, lat);
        trans.transform(p, pout);
        double angleInRadians = Math.toRadians(angle);
//        angleInRadians = (angleInRadians + Math.PI) % (2 * Math.PI);
        angleInRadians = -angleInRadians;
        angleInRadians += Math.PI / 2;
        return new WorldPosition(String.format("%.16e", pout.x), String.format("%.16e", pout.y), String.format("%.16e", angleInRadians));
    }

//    public static void main(String[] args) {
//        String proj = "+proj=tmerc +lon_0=121.20585769414902 +lat_0=31.290823210868965 +ellps=WGS84";
//        String WGS84_PARAM = "+proj=longlat +datum=WGS84 +no_defs ";
//        CoordinateTransform trans = ctFactory
//                .createTransform(createCRS(proj), createCRS(WGS84_PARAM));
//        ProjCoordinate pout = new ProjCoordinate();
//        ProjCoordinate p = new ProjCoordinate(40.46, -3.9);
//        trans.transform(p, pout);
//        System.out.println(pout.x + "," + pout.y);
//    }


}
