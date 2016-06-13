package cn.mucang.property.data;

import java.io.Serializable;
import java.util.List;

/**
 * Created by YangJin on 2014-09-24.
 */
public class CanPeiGroup implements Serializable {
    private static final long serialVersionUID = 1L;

    private int id;
    private String name;
    private List<CanPeiRow> items;

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

    public List<CanPeiRow> getItems() {
        return items;
    }

    public void setItems(List<CanPeiRow> items) {
        this.items = items;
    }
}
