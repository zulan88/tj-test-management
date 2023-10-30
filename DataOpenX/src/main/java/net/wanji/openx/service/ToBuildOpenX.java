package net.wanji.openx.service;

import net.wanji.business.domain.bo.ParticipantTrajectoryBo;
import net.wanji.business.domain.vo.FragmentedScenesDetailVo;
import net.wanji.business.exception.BusinessException;
import net.wanji.business.service.TjFragmentedSceneDetailService;
import net.wanji.common.common.TrajectoryValueDto;
import net.wanji.openx.generated.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.locationtech.proj4j.*;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringReader;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import javax.xml.bind.Unmarshaller;
import java.util.List;

@Component
public class ToBuildOpenX {

    @Autowired
    private TjFragmentedSceneDetailService tjFragmentedSceneDetailService;

    private static final CoordinateTransformFactory ctFactory = new CoordinateTransformFactory();
    private static final CRSFactory crsFactory = new CRSFactory();

    public void scenetoOpenX(FragmentedScenesDetailVo fragmentedScenesDetailVo) {
        try {
            //入参
            String c1 = "同济大学_openDrive_v0.4_20230506_修复环路.xodr";
            String proj="+proj=tmerc +lon_0=121.20585769414902 +lat_0=31.290823210868965 +ellps=WGS84";


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
            for(ParticipantTrajectoryBo participantTrajectoryBo:fragmentedScenesDetailVo.getTrajectoryJson().getParticipantTrajectories()){
                ScenarioObject scenarioObject = new ScenarioObject();
                scenarioObject.setName(participantTrajectoryBo.getName());
                Vehicle vehicle = new Vehicle("default",participantTrajectoryBo.getType());
                scenarioObject.setVehicle(vehicle);
                scenarioObjectList.add(scenarioObject);
            }
            openScenario.setEntities(entities);
            Storyboard storyboard = new Storyboard();
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
            Story story =new Story();
            story.setName("mystore");
            for(ParticipantTrajectoryBo participantTrajectoryBo:fragmentedScenesDetailVo.getTrajectoryJson().getParticipantTrajectories()){
                Act act = new Act();
                act.setName("Act_"+participantTrajectoryBo.getName());
                ManeuverGroup maneuverGroup = new ManeuverGroup();
                maneuverGroup.setName("Squence_"+participantTrajectoryBo.getName());
                Actors actors = new Actors();
                actors.setSelectTriggeringEntities("false");
                EntityRef entityRef = new EntityRef();
                entityRef.setEntityRef(participantTrajectoryBo.getName());
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
                trajectory.setName("Trajectory_"+participantTrajectoryBo.getName());
                trajectory.setClosed("false");
                Shape shape =new Shape();
                Polyline polyline =new Polyline();
                List<TrajectoryValueDto> routelist = tjFragmentedSceneDetailService.getroutelist(fragmentedScenesDetailVo.getId(),participantTrajectoryBo.getId()).get(0);
                for(TrajectoryValueDto trajectoryValueDto:routelist){
                    Vertex vertex =new Vertex();
                    vertex.setTime(trajectoryValueDto.getTimestamp());
                    Position position = new Position();
                    WorldPosition worldPosition =totrans(trajectoryValueDto.getLongitude(), trajectoryValueDto.getLatitude(), proj, trajectoryValueDto.getCourseAngle());
                    position.setWorldPosition(worldPosition);
                    vertex.setPosition(position);
                    polyline.getVertex().add(vertex);
                }
                shape.setPolyline(polyline);
                trajectory.setShape(shape);
                followTrajectoryAction.setTrajectory(trajectory);
                routingAction.setFollowTrajectoryAction(followTrajectoryAction);
                privateAction.setRoutingAction(routingAction);
                action.setPrivateAction(privateAction);
                event.getAction().add(action);
                maneuver.getEvent().add(event);
                maneuverGroup.setActors(actors);
                maneuverGroup.getManeuver().add(maneuver);
                act.getManeuverGroup().add(maneuverGroup);
                story.getAct().add(act);
            }
            storyboard.setInit(init);
            storyboard.getStory().add(story);
            openScenario.setStoryboard(storyboard);

            JAXBContext jaxbContext = JAXBContext.newInstance(OpenScenario.class);
            Marshaller marshaller = jaxbContext.createMarshaller();

            marshaller.marshal(openScenario, System.out);

            java.io.File file = new java.io.File("output.xml");
            OutputStream outputStream = Files.newOutputStream(file.toPath());
            marshaller.marshal(openScenario, outputStream);
        } catch (JAXBException e) {
            e.printStackTrace();
        } catch (BusinessException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    private CoordinateReferenceSystem createCRS(String crsSpec) {
        CoordinateReferenceSystem crs = null;
        if (crsSpec.indexOf("+") >= 0 || crsSpec.indexOf("=") >= 0) {
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
