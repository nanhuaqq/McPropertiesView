package cn.mucang.property.data;

import java.io.Serializable;
import java.util.List;

/**
 * 参配一行数据
 * Created by YangJin on 2014-08-29.
 */
public class CanPeiRow implements Serializable {
    private static final long serialVersionUID = 1L;

    private int id;
    private String name;
    private List<CanPeiRowCell> values;
    private boolean different = false;
    private boolean showCompeteWord;
    private int bold;

    private CanPeiParamBoldPolicy boldPolicy;

    public boolean isShowCompeteWord() {
        return showCompeteWord;
    }

    public void setShowCompeteWord(boolean showCompeteWord) {
        this.showCompeteWord = showCompeteWord;
    }

    public int getBold() {
        return bold;
    }

    public void setBold(int bold) {
        this.bold = bold;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<CanPeiRowCell> getValues() {
        return values;
    }

    public void setValues(List<CanPeiRowCell> values) {
        this.values = values;
    }

    public boolean isDifferent() {
        return different;
    }

    public void setDifferent(boolean different) {
        this.different = different;
    }

    public CanPeiParamBoldPolicy getBoldPolicy() {
        return CanPeiParamBoldPolicy.parse(bold);
    }

    public void setBoldPolicy(CanPeiParamBoldPolicy boldPolicy) {
        this.boldPolicy = boldPolicy;
    }

}
