package cn.mucang.property.data;

import java.io.Serializable;

/**
 * Created by xiamingwei on 16/1/20.
 */
public class CanPeiCarEntity implements Serializable {
    private String brandId;
    private int carId;
    private int serialId;
    private String carName;

    public String getBrandId() {
        return brandId;
    }

    public void setBrandId(String brandId) {
        this.brandId = brandId;
    }

    public int getCarId() {
        return carId;
    }

    public void setCarId(int carId) {
        this.carId = carId;
    }

    public int getSerialId() {
        return serialId;
    }

    public void setSerialId(int serialId) {
        this.serialId = serialId;
    }

    public String getCarName() {
        return carName;
    }

    public void setCarName(String carName) {
        this.carName = carName;
    }
}
