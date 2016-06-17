package cn.mucang.property.app;

import android.support.v4.widget.ScrollerCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ListView;

import cn.mucang.android.core.config.MucangActivity;
import cn.mucang.property.McPropertiesView;
import cn.mucang.property.R;
import cn.mucang.property.data.GetCarPropertiesResultEntity;
import cn.mucang.property.utils.AssetsUtil;

public class NormalActivity extends AppCompatActivity {

    private McPropertiesView propertiesView;
    private SerialSpecMcPropertiesAdapter testAdapter;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_normal);
        propertiesView = (McPropertiesView) findViewById(R.id.propertiesView);
        testAdapter = new SerialSpecMcPropertiesAdapter(this,getSourceData());
        propertiesView.setAdapter(testAdapter);

        findViewById(R.id.btnSmoothScroll).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                propertiesView.smoothScrollToColumn(propertiesView.getFirstColumn() + 1);
            }
        });
    }

    private GetCarPropertiesResultEntity getSourceData(){
        return (GetCarPropertiesResultEntity) AssetsUtil.readEntityFromAssets(this,"properties.txt",GetCarPropertiesResultEntity.class);
    }

}
