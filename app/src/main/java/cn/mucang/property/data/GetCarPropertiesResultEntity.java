package cn.mucang.property.data;

import java.io.Serializable;
import java.util.List;

/**
 * Created by YangJin on 2015-04-30.
 */
public class GetCarPropertiesResultEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    private boolean empty = true;
    private List<CanPeiGroup> items;
    private List<CanPeiCarEntity> cars;

    public boolean isEmpty() {
        return empty;
    }

    public void setEmpty(boolean empty) {
        this.empty = empty;
    }

    public List<CanPeiGroup> getItems() {
        return items;
    }

    public void setItems(List<CanPeiGroup> items) {
        this.items = items;
    }

    public List<CanPeiCarEntity> getCars() {
        return cars;
    }

    public void setCars(List<CanPeiCarEntity> cars) {
        this.cars = cars;
    }
}
