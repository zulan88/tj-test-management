package net.wanji.business.common;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @Auther: guanyuduo
 * @Date: 2023/6/28 10:32
 * @Descriptoin:
 */

public interface Constants {

    class MapKey {
        public static final String VEHICLE_KEY = "vehicle";
        public static final String PEDESTRIAN_KEY = "pedestrian";
        public static final String OBSTACLE_KEY = "obstacle";
    }

    /**
     * 默认标签
     */
    class DefaultLabel {
        public static final String straightway = "直道";
        public static final String goStraight = "直行";
        public static final String vehicles = "机动车";

        public static List<String> getDefaultLabel() {
            return Arrays.asList(straightway, goStraight, vehicles);
        }
    }

    /**
     * 内容模板
     */
    class ContentTemplate {
        public static final String EXPORT_NAME_TEMPLATE = "{}_{}_{}";
        public static final String CASE_NUMBER_TEMPLATE = "CASE{}{}";
        public static final String SCENE_NAME_TEMPLATE = "{}_{}";
        public static final String COPY_SCENE_NAME_TEMPLATE = "{}_COPY{}";
    }

    class TreeStructure {
        public static final int DEFAULT_PARENT = 0;
        public static final int DEFAULT_LEVEL = 1;
    }

    /**
     * 使用状态
     */
    class UsingStatus {
        public static final int ENABLE = 0;
        public static final int DISABLE = 1;
    }

    /**
     * 是否
     */
    class YN {
        public static final String Y = "Y";
        public static final String N = "N";
        public static final int Y_INT = 1;
        public static final int N_INT = 0;
    }

    /**
     * 公共列名
     */
    class ColumnName {
        public static final String ID_COLUMN = "id";
        public static final String STATUS_COLUMN = "status";
        public static final String PARENT_ID_COLUMN = "parent_id";
        public static final String IS_FOLDER_COLUMN = "is_folder";
        public static final String COMPLETED_COLUMN = "completed";
        public static final String NAME_COLUMN = "name";
        public static final String SCENE_ID_COLUMN = "fragmented_scene_id";
        public static final String SCENE_DETAIL_ID_COLUMN = "id";
        public static final String RESOURCES_ID_COLUMN = "resources_id";
        public static final String TYPE_COLUMN = "type";
        public static final String CASE_ID_COLUMN = "case_id";
        public static final String TEST_TYPE_COLUMN = "test_type";
    }

    class ResourceType {
        /**
         * 集成性大地图
         */
        public static final String INTEGRATION_MAP = "integrationMap";
        public static final String ATOM_MAP = "atomMap";
        public static final String BG_TRAFFIC_FLOW = "bgTrafficFlow";
        public static final String MAIN = "main";
        public static final String FACILITIES = "facilities";
    }

    /**
     * 系统字典
     */
    class SysType {
        /**
         * 场景树类型
         */
        public static final String SCENE_TREE_TYPE = "scene_tree_type";
        /**
         * 资源类型
         */
        public static final String RESOURCE_TYPE = "resource_type";
        /**
         * 标签类型
         */
        public static final String LABEL_TYPE = "label_type";
        /**
         * 车道朝向类型
         */
        public static final String ROAD_WAY_TYPE = "road_way_type";
        /**
         * 场景类型
         */
        public static final String SCENE_TYPE = "scene_type";
        /**
         * 场景复杂度
         */
        public static final String SCENE_COMPLEXITY = "scene_complexity";
        /**
         * 交通流状态
         */
        public static final String TRAFFIC_FLOW_STATUS = "traffic_flow_status";
        /**
         * 道路类型
         */
        public static final String ROAD_TYPE = "road_type";
        /**
         * 天气
         */
        public static final String WEATHER = "weather";
        /**
         * 路面状况
         */
        public static final String ROAD_CONDITION = "road_condition";
        /**
         * 参与者角色
         */
        public static final String PART_ROLE = "part_role";
        /**
         * 参与者类型
         */
        public static final String PART_TYPE = "part_type";
    }

    /**
     * 测试类型
     */
    class TestType {
        /**
         * 虚实融合测试
         */
        public static final String VRFT = "virtualRealFusion";
        /**
         * 虚实对比测试
         */
        public static final String VRCT = "virtualRealContrast";
        /**
         * 人在环路测试
         */
        public static final String MILT = "mainInLoop";
        /**
         * 平行推演测试
         */
        public static final String PDT = "parallelDeduction";
        /**
         * 三项映射测试
         */
        public static final String TTMT = "threeTermMapping";
    }

    class CaseVerifyStatus {
        /**
         * 待核验
         */
        public static final String NEW = "NEW";
        /**
         * 系统核验中
         */
        public static final String SYS_VERIFYING = "SYS_VERIFYING";
        /**
         * 系统核验完成
         */
        public static final String SYS_VERIFY_FINISHED = "SYS_VERIFY_FINISHED";
        /**
         * 人工核验完成
         */
        public static final String FINISHED = "FINISHED";
    }

    class Extension {
        public static final String TXT = "txt";
    }

    class PlaybackAction {
        public static final int START = 1;
        public static final int SUSPEND = 2;
        public static final int CONTINUE = 3;
        public static final int STOP = 4;
    }


    /**
     * 场景车道方向
     */
    enum LanePointEnum {
        ONE_WAY("oneWay", "单向"),
        TWO_WAY("twoWay", "双向");

        private String value;

        private String label;

        LanePointEnum(String value, String label) {
            this.value = value;
            this.label = label;
        }

        public String getValue() {
            return value;
        }

        public String getLabel() {
            return label;
        }

        public static List<Map<String, String>> buildSelect() {
            return Arrays.stream(values()).map(item -> {
                Map<String, String> map = new HashMap<>(2);
                map.put("dictValue", item.value);
                map.put("dictLabel", item.label);
                return map;
            }).collect(Collectors.toList());
        }
    }

    /**
     * 用例状态枚举
     */
    enum CaseStatusEnum {
        TO_BE_CONFIG("0", "待配置"),
        TO_BE_SIMULATED("1", "待仿真"),
        SIMULATION_VERIFICATION("2", "仿真验证"),
        REAL_VERIFICATION("3", "实车验证"),
        TO_BE_IN_BASE("4", "待入库");

        private String code;

        private String desc;

        CaseStatusEnum(String code, String desc) {
            this.code = code;
            this.desc = desc;
        }

        public String getCode() {
            return code;
        }

        public String getDesc() {
            return desc;
        }

        public static String getDescByCode(String code) {
            for (CaseStatusEnum caseStatusEnum : values()) {
                if (code.equals(caseStatusEnum.getCode())) {
                    return caseStatusEnum.getDesc();
                }
            }
            return "";
        }
    }

    enum FileTypeEnum {
        OpenDrive("xodr", "OpenDrive"),
        OpenSCENARIO("xosc", "OpenSCENARIO");

        private String extension;

        private String type;

        FileTypeEnum(String extension, String type) {
            this.extension = extension;
            this.type = type;
        }

        public String getType() {
            return type;
        }

        public String getExtension() {
            return extension;
        }

        public static String getTypeByExt(String extension) {
            for (FileTypeEnum fileTypeEnum : values()) {
                if (extension.equals(fileTypeEnum.getExtension())) {
                    return fileTypeEnum.getType();
                }
            }
            return "";
        }
    }

    /**
     * 添加校验分组
     */
    interface InsertGroup {}
    /**
     * 修改校验分组
     */
    interface UpdateGroup {}
    /**
     * 查询校验分组
     */
    interface QueryGroup {}
    /**
     * 删除校验分组
     */
    interface DeleteGroup {}
    /**
     * 批量校验分组
     */
    interface BatchGroup {}
}
