package net.wanji.common.core.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
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

    public SimpleSelect(SysDictData dictData) {
        this.id = dictData.getDictCode();
        this.sort = dictData.getDictSort();
        this.label = dictData.getDictLabel();
        this.value = dictData.getDictValue();
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
}
