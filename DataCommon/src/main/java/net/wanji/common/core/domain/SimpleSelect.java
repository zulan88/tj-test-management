package net.wanji.common.core.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.wanji.common.annotation.Excel;
import net.wanji.common.core.domain.entity.SysDictData;

import java.io.Serializable;

/**
 * @Auther: guanyuduo
 * @Date: 2023/7/26 11:14
 * @Descriptoin:
 */
@AllArgsConstructor
@NoArgsConstructor
public class SimpleSelect implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private Long sort;

    private String label;

    private String value;

    private String cssClass;

    private Long dictCode;

    private String dictLabel;

    private String dictValue;

    public SimpleSelect(SysDictData dictData) {
        this.id = dictData.getDictCode();
        this.sort = dictData.getDictSort();
        this.label = dictData.getDictLabel();
        this.value = dictData.getDictValue();
        this.cssClass = dictData.getCssClass();
        this.dictCode = dictData.getDictCode();
        this.dictLabel = dictData.getDictLabel();
        this.dictValue = dictData.getDictValue();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getSort() {
        return sort;
    }

    public void setSort(Long sort) {
        this.sort = sort;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getCssClass() {
        return cssClass;
    }

    public void setCssClass(String cssClass) {
        this.cssClass = cssClass;
    }

    public Long getDictCode() {
        return dictCode;
    }

    public void setDictCode(Long dictCode) {
        this.dictCode = dictCode;
    }

    public String getDictLabel() {
        return dictLabel;
    }

    public void setDictLabel(String dictLabel) {
        this.dictLabel = dictLabel;
    }

    public String getDictValue() {
        return dictValue;
    }

    public void setDictValue(String dictValue) {
        this.dictValue = dictValue;
    }
}
