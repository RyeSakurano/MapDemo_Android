一、 总体功能介绍
1. 显示基础地图，可选择显示暗色模式、3D建筑物、实时路况
2. 显示故宫AOI面
3. 输入关键字查询显示对应POI（包括关键词输入提示）
4. 点击POI，查询从西格玛大厦出发到该地点的驾驶路线规划，显示小车平移动画
5. 长按地图，进行截图

二、 程序结构
1. MainActivity
程序入口，点击按钮进入地图界面（MapViewActivity）
【一开始为了熟悉插入按钮、点击按钮、从一个activity跳转到另一个activity的流程编写的，没什么重要功能】
2. MapViewAcitivity
（1） 重写OnCreate()，依次调用initView()、initAOI()、initSearch()、initScreenShot()初始化
（2） initView()：显示，使用到的接口包括：
	a. 获取地图
	b. 切换地图类型：交通实况、暗色模式
	c. 显示3D建筑物
	d. 地图控件设置（logo）
（3） initAOI()：故宫AOI面显示
（4） initSearch()：搜索功能
	a. 设置输入框的TextChangedListener，当内容发生变动时，调用suggestion()提示关键词
	b. 设置输入框的OnFocusChangeListener，当没有输入时，不显示建议列表
	c. 当点击列表中的一项时，将它输入到输入框中
	d. 点击按钮，调用searchPoi()进行搜索
（5） initScreenShot()
设置OnMapLongClickListener，手势事件发生后进行截图
（6） searchPoi()
	a. 进行地点搜索，绘制POI的点标记
	b. 设置OnMarkerClickListener()回调，当点击marker时，调用DrawDrivingRoute()
（7） suggestion()
关键词输入提示，获得结果后给自己发消息，收到消息后调用showAutoComplete()更新建议列表
（8） showAutoComplete()
用suggestionAdapter更新ListView
（9） DrawDrivingRoute()
	a. 驾驶路线规划
	b. 在地图上绘制线
	c. 使用地图组件的小车平移动画
（10）重写onStart()、onResume()、onPause()、onStop()、onDestroy()、onRestart()
