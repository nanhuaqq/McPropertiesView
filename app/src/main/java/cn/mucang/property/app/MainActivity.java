package cn.mucang.property.app;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import cn.mucang.android.core.config.MucangActivity;
import cn.mucang.property.McPropertiesView;
import cn.mucang.property.R;
import cn.mucang.property.data.GetCarPropertiesResultEntity;
import cn.mucang.property.utils.AssetsUtil;

public class MainActivity extends AppCompatActivity {

    private McPropertiesView propertiesView;
    private McPropertiesTestAdapter testAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        propertiesView = (McPropertiesView) findViewById(R.id.propertiesView);
        testAdapter = new McPropertiesTestAdapter(this,getSourceData());
        propertiesView.setAdapter(testAdapter);
    }

    private GetCarPropertiesResultEntity getSourceData(){
        return (GetCarPropertiesResultEntity) AssetsUtil.readEntityFromAssets(this,"properties.txt",GetCarPropertiesResultEntity.class);
    }

}
