package com.example.myapplication;

import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.tencent.lbssearch.TencentSearch;
import com.tencent.lbssearch.httpresponse.BaseObject;
import com.tencent.lbssearch.httpresponse.HttpResponseListener;
import com.tencent.lbssearch.object.param.DrivingParam;
import com.tencent.lbssearch.object.param.SearchParam;
import com.tencent.lbssearch.object.param.SuggestionParam;
import com.tencent.lbssearch.object.result.DrivingResultObject;
import com.tencent.lbssearch.object.result.SearchResultObject;
import com.tencent.lbssearch.object.result.SuggestionResultObject;
import com.tencent.tencentmap.mapsdk.maps.CameraUpdate;
import com.tencent.tencentmap.mapsdk.maps.CameraUpdateFactory;
import com.tencent.tencentmap.mapsdk.maps.MapView;
import com.tencent.tencentmap.mapsdk.maps.TencentMap;
import com.tencent.tencentmap.mapsdk.maps.UiSettings;
import com.tencent.tencentmap.mapsdk.maps.model.AoiLayer;
import com.tencent.tencentmap.mapsdk.maps.model.BitmapDescriptorFactory;
import com.tencent.tencentmap.mapsdk.maps.model.CameraPosition;
import com.tencent.tencentmap.mapsdk.maps.model.LatLng;
import com.tencent.tencentmap.mapsdk.maps.model.LatLngBounds;
import com.tencent.tencentmap.mapsdk.maps.model.Marker;
import com.tencent.tencentmap.mapsdk.maps.model.MarkerOptions;
import com.tencent.tencentmap.mapsdk.maps.model.Polyline;
import com.tencent.tencentmap.mapsdk.maps.model.PolylineOptions;
import com.tencent.tencentmap.mapsdk.vector.utils.animation.MarkerTranslateAnimator;

import java.lang.ref.WeakReference;
import java.util.List;

public class MapViewActivity extends AppCompatActivity {

    private MapView mapView;
    protected TencentMap mTencentMap;

    private UiSettings uiSettings;
    private ImageView imageView;

    private Switch trafficControl;
    private Switch darkControl;
    private Switch threeDControl;
    private Switch aoiControl;

    private SearchParam.Region region;

    private Polyline polyline = null;
    private AoiLayer aoiLayer = null;

    private EditText etSearch;
    private Button btnSearch;
    private ListView lvSuggestion;
    private final int MSG_SUGGESTION = 10000;
    private final MyHandler handler = new MyHandler(this);
    private SuggestionAdapter suggestionAdapter;

    private static class MyHandler extends Handler {
        private final WeakReference<MapViewActivity> mActivity;

        public MyHandler(MapViewActivity activity) {
            // TODO Auto-generated constructor stub
            mActivity = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            // TODO Auto-generated method stub
            MapViewActivity activity = mActivity.get();
            if (activity != null) {
                activity.handleMessage(msg);
            }
        }
    }

    final TextWatcher textWatcher = new TextWatcher() {
        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            suggestion(s.toString());
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            //
        }

        @Override
        public void afterTextChanged(Editable s) {
            //
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map_view);

        initView();
        initAOI();
        initSearch();
        initScreenShot();
    }

    /* 初始化地图显示相关 */
    private void initView() {
        // 获取地图
        mapView = findViewById(R.id.mapview);
        mTencentMap = mapView.getMap();

        // 交通实况
        trafficControl = findViewById(R.id.switch_traffic);
        trafficControl.setVisibility(View.VISIBLE);
        mTencentMap.setTrafficEnabled(false);
        trafficControl.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                if (isChecked) {
                    mTencentMap.setTrafficEnabled(true);
                } else {
                    mTencentMap.setTrafficEnabled(false);
                }
            }
        });

        // 暗色模式
        darkControl = findViewById(R.id.switch_dark);
        darkControl.setVisibility(View.VISIBLE);
        mTencentMap.setMapType(TencentMap.MAP_TYPE_NORMAL);
        darkControl.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                if (isChecked) {
                    mTencentMap.setMapType(TencentMap.MAP_TYPE_DARK);
                } else {
                    mTencentMap.setMapType(TencentMap.MAP_TYPE_NORMAL);
                }
            }
        });

        // 3D建筑物
        threeDControl = findViewById(R.id.switch_3D);
        threeDControl.setVisibility(View.VISIBLE);
        mTencentMap.setBuildingEnable(false);
        threeDControl.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                if (isChecked) {
                    mTencentMap.setBuildingEnable(true);
                } else {
                    mTencentMap.setBuildingEnable(false);
                }
            }
        });

        // 调整logo大小
        uiSettings = mTencentMap.getUiSettings();
        uiSettings.setLogoScale((float) 0.7);
    }

    /* 故宫AOI面 */
    private void initAOI() {
        final String poiId = "5866905815035848227";
        aoiControl = findViewById(R.id.switch_aoi);
        aoiControl.setVisibility(View.VISIBLE);
        aoiControl.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                if (isChecked) {
                        aoiLayer = mTencentMap.addAoiLayer(poiId, null, new AoiLayer.OnAoiLayerLoadListener() {
                            @Override
                            public void onAoiLayerLoaded(boolean b, AoiLayer aoiLayer) {
                                // 炫酷地移动到AOI
                                CameraUpdate cameraAoi = CameraUpdateFactory.newCameraPosition(
                                        new CameraPosition(new LatLng(39.917671, 116.39631),
                                                15,
                                                40f,
                                                0)
                                );
                                mTencentMap.animateCamera(cameraAoi);
                            }
                        });
                } else {
                    if (aoiLayer != null) {
                        aoiLayer.remove();
                    }
                }
            }
        });
    }

    /* 初始化搜索相关 */
    private void initSearch() {
        region = new SearchParam.Region("北京");

        etSearch = (EditText) findViewById(R.id.et_search_poi);
        btnSearch = (Button) findViewById(R.id.btn_search_poi);
        lvSuggestion = (ListView) findViewById(R.id.lv_suggestions);
        etSearch.setVisibility(View.VISIBLE);
        etSearch.setBackgroundColor(Color.WHITE);
        btnSearch.setVisibility(View.VISIBLE);

        etSearch.addTextChangedListener(textWatcher);
        etSearch.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                // 没东西，不显示列表
                if (!etSearch.hasFocus()) {
                    lvSuggestion.setVisibility(View.GONE);
                }
            }
        });

        // 当点击建议列表中的一项时，把它获取到输入框中
        // 因为这时改变了输入框内容，为了避免再一次触发suggestion，把watcher暂时关闭
        lvSuggestion.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view,
                                    int position, long id) {
                etSearch.removeTextChangedListener(textWatcher);
                CharSequence cs =
                        ((TextView)view.findViewById((R.id.label))).getText();
                etSearch.setText(cs);
                lvSuggestion.setVisibility(View.GONE);
                etSearch.addTextChangedListener(textWatcher);
            }
        });

        // 点击按钮，搜索poi
        btnSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                searchPoi();
            }
        });
    }

    /* 初始化：长按截图 */
    private void initScreenShot() {
        imageView = findViewById(R.id.imgview);
        mTencentMap.setOnMapLongClickListener(new TencentMap.OnMapLongClickListener() {
            @Override
            public void onMapLongClick(LatLng latLng) {
                mTencentMap.snapshot(new TencentMap.SnapshotReadyCallback() {
                    @Override
                    public void onSnapshotReady(Bitmap bitmap) {
                        imageView.setImageBitmap(bitmap);
                        // 截图显示3秒后消失
                        imageView.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                imageView.setVisibility(View.GONE);
                            }
                        }, 3000);
                    }
                });
            }
        });
    }

    /* 搜索POI */
    private void searchPoi() {
        TencentSearch tencentSearch = new TencentSearch(this);
        String keyWord = etSearch.getText().toString().trim();
        SearchParam searchParam = new SearchParam(keyWord, region);

        tencentSearch.search(searchParam, new HttpResponseListener<BaseObject>() {

            @Override
            public void onFailure(int arg0, String arg2,
                                  Throwable arg3) {
                Toast.makeText(getApplicationContext(), arg2, Toast.LENGTH_LONG).show();
            }

            @Override
            public void onSuccess(int arg0, BaseObject arg1) {
                if (arg1 == null) {
                    return;
                }
                SearchResultObject obj = (SearchResultObject) arg1;
                if(obj.data == null){
                    return;
                }
                mTencentMap.moveCamera(CameraUpdateFactory.newLatLngZoom(
                        obj.data.get(0).latLng, 15f)
                );
                for (SearchResultObject.SearchResultData data : obj.data) {
                    mTencentMap.addMarker(new MarkerOptions()
                            .position(data.latLng)
                            .title(data.title)
                            .snippet((data.address)));
                }
                // 点击marker，显示从西格玛大厦到该地点的驾驶路线
                mTencentMap.setOnMarkerClickListener(new TencentMap.OnMarkerClickListener() {
                    @Override
                    public boolean onMarkerClick(Marker marker) {
                        LatLng fromPoint = new LatLng(39.984066, 116.307548);
                        LatLng toPoint = marker.getPosition();
                        DrawDrivingRoute(fromPoint, toPoint);
                        return false;
                    }
                });
            }
        });
    }

    /* 关键词提示 */
    private void suggestion(String keyword) {
        if (keyword.trim().length() == 0) {
            lvSuggestion.setVisibility(View.GONE);
            return;
        }

        TencentSearch tencentSearch = new TencentSearch(this);
        SuggestionParam suggestionParam = new SuggestionParam(keyword, "北京");
        tencentSearch.suggestion(suggestionParam, new HttpResponseListener<BaseObject>() {

            @Override
            public void onSuccess(int arg0, BaseObject arg1) {
                if (arg1 == null ||
                        etSearch.getText().toString().trim().length() == 0) {
                    lvSuggestion.setVisibility(View.GONE);
                    return;
                }
                // 把信息发给自己？
                Message msg = new Message();
                msg.what = MSG_SUGGESTION;
                msg.obj = arg1;
                handler.sendMessage(msg);
            }

            @Override
            public void onFailure(int arg0, String arg1, Throwable arg2) {
                //
            }
        });
    }

    // 处理自己给自己发的信息
    public void handleMessage(Message msg) {
        switch (msg.what) {
            case MSG_SUGGESTION:
                showAutoComplete((SuggestionResultObject)msg.obj);
                break;

            default:
                break;
        }
    }

    // 在列表中显示suggestion
    protected void showAutoComplete(SuggestionResultObject obj) {
        if (obj.data.size() == 0) {
            lvSuggestion.setVisibility(View.GONE);
            return;
        }
        if (suggestionAdapter == null) {
            suggestionAdapter = new SuggestionAdapter(obj.data);
            lvSuggestion.setAdapter(suggestionAdapter);
        } else {
            suggestionAdapter.setDatas(obj.data);
            suggestionAdapter.notifyDataSetChanged();
        }
        lvSuggestion.setVisibility(View.VISIBLE);
    }

    /* 驾驶路线 */
    private void DrawDrivingRoute(LatLng fromPoint, LatLng toPoint) {
        DrivingParam drivingParam = new DrivingParam(fromPoint, toPoint);
        drivingParam.policy(DrivingParam.Policy.TRIP,
                DrivingParam.Preference.REAL_TRAFFIC);

        TencentSearch tencentSearch = new TencentSearch(this);
        tencentSearch.getRoutePlan(drivingParam, new HttpResponseListener<DrivingResultObject>() {
            @Override
            public void onSuccess(int i, DrivingResultObject drivingResultObject) {
                if (drivingResultObject == null) {
                    return;
                }
                DrivingResultObject.Route route = drivingResultObject.result.routes.get(0);
                    // 画轨迹
                    List<LatLng> lines = route.polyline;
                    if (polyline != null)
                        polyline.remove();
                    polyline = mTencentMap.addPolyline(new PolylineOptions().addAll(lines));

                    // 小车平移
                    LatLng catLatLng = lines.get(0);
                    Marker mCarMarker = mTencentMap.addMarker(
                            new MarkerOptions(catLatLng)
                                .anchor(0.5f, 0.5f)
                                .icon(BitmapDescriptorFactory.fromResource(R.mipmap.taxi))
                                .flat(true)
                                .clockwise(false));
                    MarkerTranslateAnimator mAnimator = new MarkerTranslateAnimator(mCarMarker, 5000, (LatLng[]) lines.toArray(new LatLng[0]), true);
                    mTencentMap.animateCamera(CameraUpdateFactory.newLatLngBounds(
                            LatLngBounds.builder().include(lines).build(), 50
                    ));
                    mAnimator.startAnimation();

                }

            @Override
            public void onFailure(int i, String s, Throwable throwable) {
                //
            }
        });

    }

    @Override
    protected void onStart() {
        super.onStart();
        mapView.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mapView.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        mapView.onRestart();
    }

    /* listview需要adapter来显示数据 */
    class SuggestionAdapter extends BaseAdapter {

        List<SuggestionResultObject.SuggestionData> mSuggestionDatas;

        public SuggestionAdapter(List<SuggestionResultObject.SuggestionData> suggestionDatas) {
            // TODO Auto-generated constructor stub
            setDatas(suggestionDatas);
        }

        public void setDatas(List<SuggestionResultObject.SuggestionData> suggestionDatas) {
            mSuggestionDatas = suggestionDatas;
        }

        @Override
        public int getCount() {
            // TODO Auto-generated method stub
            return mSuggestionDatas.size();
        }

        @Override
        public Object getItem(int position) {
            // TODO Auto-generated method stub
            return mSuggestionDatas.get(position);
        }

        @Override
        public long getItemId(int position) {
            // TODO Auto-generated method stub
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            // 解释：https://blog.csdn.net/chenyantc02/article/details/103939451?
            // utm_medium=distribute.pc_relevant_t0.none-task-blog-
            // BlogCommendFromMachineLearnPai2-1.channel_param&depth_1-utm_source
            // =distribute.pc_relevant_t0.none-task-blog-
            // BlogCommendFromMachineLearnPai2-1.channel_param
            ViewHolder viewHolder;
            if (convertView == null) { //初始
                convertView = View.inflate(MapViewActivity.this,
                        R.layout.suggestion_list_item, null);
                viewHolder = new ViewHolder();
                viewHolder.tvTitle = (TextView) convertView.findViewById(R.id.label);
                viewHolder.tvAddress = (TextView) convertView.findViewById(R.id.desc);
                convertView.setTag(viewHolder);
            } else { // 加载新的
                viewHolder = (ViewHolder) convertView.getTag();
            }
            viewHolder.tvTitle.setText(mSuggestionDatas.get(position).title);
            viewHolder.tvAddress.setText(mSuggestionDatas.get(position).address);
            return convertView;
        }

        private class ViewHolder{
            TextView tvTitle;
            TextView tvAddress;
        }
    }
}