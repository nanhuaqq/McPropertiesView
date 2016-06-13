package cn.mucang.property.data;

/**
 * 参配值加粗策略
 * Created by YangJin on 2015-03-17.
 */
public enum CanPeiParamBoldPolicy {
    NONE,//不加粗
    MAX,//值高加粗
    MIN;//值低加粗

    public static CanPeiParamBoldPolicy parse(int ordinal) {
        for(CanPeiParamBoldPolicy policy : CanPeiParamBoldPolicy.values()) {
            if(policy.ordinal() == ordinal) {
                return policy;
            }
        }
        return NONE;
    }
}
