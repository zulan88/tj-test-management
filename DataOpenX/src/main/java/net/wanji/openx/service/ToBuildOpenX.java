package net.wanji.openx.service;

import net.wanji.business.domain.bo.ParticipantTrajectoryBo;
import net.wanji.business.domain.vo.FragmentedScenesDetailVo;
import net.wanji.business.entity.TjScenelib;
import net.wanji.business.exception.BusinessException;
import net.wanji.business.service.ITjScenelibService;
import net.wanji.business.service.TjFragmentedSceneDetailService;
import net.wanji.common.common.TrajectoryValueDto;
import net.wanji.common.config.WanjiConfig;
import net.wanji.common.utils.DateUtils;
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
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Component
public class ToBuildOpenX {

    @Autowired
    private TjFragmentedSceneDetailService tjFragmentedSceneDetailService;

    @Autowired
    ITjScenelibService scenelibService;

    private static final CoordinateTransformFactory ctFactory = new CoordinateTransformFactory();
    private static final CRSFactory crsFactory = new CRSFactory();

    @Async
    public void scenetoOpenX(FragmentedScenesDetailVo fragmentedScenesDetailVo, Long id) throws RuntimeException {
        try {
            //入参
            String c1 = "tjtest.xodr";
            String proj="+proj=tmerc +lon_0=121.20585769414902 +lat_0=31.290823210868965 +ellps=WGS84";

            String outputFolder = WanjiConfig.getScenelibPath() + java.io.File.separator + DateUtils.datePath();;
            java.io.File folder = new java.io.File(outputFolder);
            if (!folder.exists()) {
                folder.mkdirs();
            }

            java.io.File xodrfile = new java.io.File(WanjiConfig.getScenelibPath(),c1);

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
            List<ScenarioObject> scenarioObjectList =entities.getScenarioObject();
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
            for(ParticipantTrajectoryBo participantTrajectoryBo:fragmentedScenesDetailVo.getTrajectoryJson().getParticipantTrajectories()){
                ScenarioObject scenarioObject = new ScenarioObject();
                scenarioObject.setName(participantTrajectoryBo.getId());
                Vehicle vehicle = new Vehicle("default",participantTrajectoryBo.getType());
                scenarioObject.setVehicle(vehicle);
                scenarioObjectList.add(scenarioObject);
                Private privateone = new Private();
                privateone.setEntityRef(scenarioObject.getName());
                init.getActions().getPrivate().add(privateone);
            }
            openScenario.setEntities(entities);
            Storyboard storyboard = new Storyboard();
            Story story =new Story();
            story.setName("mystore");
            Double maxTime = 0D;
            DecimalFormat df = new DecimalFormat("0.00");
            for(ParticipantTrajectoryBo participantTrajectoryBo:fragmentedScenesDetailVo.getTrajectoryJson().getParticipantTrajectories()){
                Act act = new Act();
                act.setName("Act_"+participantTrajectoryBo.getId());
                ManeuverGroup maneuverGroup = new ManeuverGroup();
                maneuverGroup.setName("Squence_"+participantTrajectoryBo.getId());
                Actors actors = new Actors();
                actors.setSelectTriggeringEntities("false");
                EntityRef entityRef = new EntityRef();
                entityRef.setEntityRef(participantTrajectoryBo.getId());
                actors.getEntityRef().add(entityRef);
                Maneuver maneuver = new Maneuver();
                maneuver.setName("Maneuver1");
                Event event =new Event();
                event.setName("Event1");
                event.setPriority("overwrite");
                Action action = new Action();
                action.setName("Action1");
                PrivateAction privateAction = new PrivateAction();
                RoutingAction routingAction =new RoutingAction();
                FollowTrajectoryAction followTrajectoryAction =new FollowTrajectoryAction();
                Trajectory trajectory =new Trajectory();
                trajectory.setName("Trajectory_"+participantTrajectoryBo.getId());
                trajectory.setClosed("false");
                Shape shape =new Shape();
                Polyline polyline =new Polyline();
                Double base = null;
                List<List<TrajectoryValueDto>>routelist = tjFragmentedSceneDetailService.getroutelist(fragmentedScenesDetailVo.getId(),participantTrajectoryBo.getId());
                for(List<TrajectoryValueDto> trajectoryValueDtos:routelist){
                    if(trajectoryValueDtos.size()>0) {
                        TrajectoryValueDto trajectoryValueDto = trajectoryValueDtos.get(0);
                        if (base == null) {
                            base = Double.valueOf(trajectoryValueDto.getGlobalTimeStamp());
                        }
                        Vertex vertex = new Vertex();
                        Double time = Double.valueOf(trajectoryValueDto.getGlobalTimeStamp());
                        vertex.setTime(df.format(time - base));
                        if((time-base)>maxTime){
                            maxTime = (time-base);
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
                TimeReference timeReference =new TimeReference();
                Timing timing =new Timing();
                timing.setDomainAbsoluteRelative("absolute");
                timing.setScale("1.0");
                timing.setOffset("0.0");
                timeReference.setTiming(timing);
                followTrajectoryAction.setTimeReference(timeReference);
                TrajectoryFollowingMode trajectoryFollowingMode =new TrajectoryFollowingMode();
                trajectoryFollowingMode.setFollowingMode("follow");
                followTrajectoryAction.setTrajectoryFollowingMode(trajectoryFollowingMode);
                routingAction.setFollowTrajectoryAction(followTrajectoryAction);
                privateAction.setRoutingAction(routingAction);
                action.setPrivateAction(privateAction);
                Trigger startTrigger =new Trigger();
                ConditionGroup conditionGroup =new ConditionGroup();
                Condition condition =new Condition("none","0.03");
                conditionGroup.getCondition().add(condition);
                startTrigger.getConditionGroup().add(conditionGroup);
                event.getAction().add(action);
                event.setStartTrigger(startTrigger);
                maneuver.getEvent().add(event);
                maneuverGroup.setActors(actors);
                maneuverGroup.getManeuver().add(maneuver);
                act.getManeuverGroup().add(maneuverGroup);
                Trigger actstartTrigger =new Trigger();
                ConditionGroup actconditionGroup =new ConditionGroup();
                Condition actcondition =new Condition("rising","0");
                actconditionGroup.getCondition().add(actcondition);
                actstartTrigger.getConditionGroup().add(actconditionGroup);
                act.setStartTrigger(actstartTrigger);
                story.getAct().add(act);
            }
            storyboard.setInit(init);
            storyboard.getStory().add(story);
            Trigger endTrigger =new Trigger();
            ConditionGroup endconditionGroup =new ConditionGroup();
            Condition endcondition =new Condition("rising",df.format(maxTime+0.08));
            endconditionGroup.getCondition().add(endcondition);
            endTrigger.getConditionGroup().add(endconditionGroup);
            storyboard.setStopTrigger(endTrigger);
            openScenario.setStoryboard(storyboard);

            JAXBContext jaxbContext = JAXBContext.newInstance(OpenScenario.class);
            Marshaller marshaller = jaxbContext.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);

//            marshaller.marshal(openScenario, System.out);
            java.io.File file = new java.io.File(outputFolder,fragmentedScenesDetailVo.getNumber()+(int) (System.currentTimeMillis() % 1000)+".xosc");
            OutputStream outputStream = Files.newOutputStream(file.toPath());

            StringWriter stringWriter = new StringWriter();

            marshaller.marshal(openScenario, stringWriter);
            Transformer transformer = TransformerFactory.newInstance().newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");

            transformer.transform(new StreamSource(new StringReader(stringWriter.toString()))
                    ,new StreamResult(outputStream));



            TjScenelib tjScenelib = new TjScenelib();
            tjScenelib.setId(id);
            tjScenelib.setXodrPath(xodrfile.getPath());
            tjScenelib.setXoscPath(file.getPath());

            java.io.File zipfile = new java.io.File(outputFolder,fragmentedScenesDetailVo.getNumber()+(int) (System.currentTimeMillis() % 1000)+".zip");

            FileOutputStream fos = new FileOutputStream(zipfile);
            ZipOutputStream zos = new ZipOutputStream(fos);
            zipfile(xodrfile,zos);
            zipfile(file,zos);
            zos.close();
            fos.close();

            tjScenelib.setZipPath(FileUploadUtils.getPathFileName(outputFolder,zipfile.getName()));
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

    private WorldPosition totrans(Double lon, Double lat, String tgtCRS, Double angle){
        String WGS84_PARAM = "+proj=longlat +datum=WGS84 +no_defs ";
        CoordinateTransform trans = ctFactory
                .createTransform(createCRS(WGS84_PARAM), createCRS(tgtCRS));
        ProjCoordinate pout = new ProjCoordinate();
        ProjCoordinate p = new ProjCoordinate(lon, lat);
        trans.transform(p, pout);
        return new WorldPosition(String.format("%.16e", pout.x),String.format("%.16e", pout.y),String.format("%.16e", angle));
    }


}
