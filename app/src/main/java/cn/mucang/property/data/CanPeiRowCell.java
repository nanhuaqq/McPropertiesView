package cn.mucang.property.data;

import java.io.Serializable;

/**
 * 参配一个单元格数据
 * Created by YangJin on 2014-08-29.
 */
public class CanPeiRowCell implements Serializable {
    private static final long serialVersionUID = 1L;

    private int carId;
    private boolean display;
    private int id;
    private String key;
    private String value;
    private boolean bold;
    private boolean showCompeteWord;

    public boolean isShowCompeteWord() {
        return showCompeteWord;
    }

    public void setShowCompeteWord(boolean showCompeteWord) {
        this.showCompeteWord = showCompeteWord;
    }

    public int getCarId() {
        return carId;
    }

    public void setCarId(int carId) {
        this.carId = carId;
    }

    public boolean isDisplay() {
        return display;
    }

    public void setDisplay(boolean display) {
        this.display = display;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value.trim();
    }

    public boolean isBold() {
        return bold;
    }

    public void setBold(boolean bold) {
        this.bold = bold;
    }
}
