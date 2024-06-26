package net.wanji.common.excel;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;

/**
 * 基础数据类.这里的排序和excel里面的排序一致
 *
 * @author Jiaju Zhuang
 **/
@Getter
@Setter
public class DemoData {
    private String string;
    private Date date;
    private Double doubleData;
    private Double atet;
}
