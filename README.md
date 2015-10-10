# PullToRefreshSwipeListView
Android implementing SwipeListView with Pull to Refresh

由于最近项目需要跟IOS风格(下拉刷新上拉加载、item滑动更多功能)同步，基于PullToRefresh和SwipeListView刚好能满足于我们的需求，但是找了很久的资料没有得到好的参考。因此笔者打算自己看看源码，然后尝试亲自合并一下。

## 准备
下拉刷新、上拉加载以及侧划动都是跟Touch事件密切相关的，如果你不知道这一块，那你确实需要补一下这方面的知识。

## PullToRefresh
用过PullToRefresh开源库的同学应该非常熟悉内部机制，对于内部加载的真正View是采用泛型来适配的。因此，这里必须关注最外层的事件拦截。首先来看看继承关系（用PullToRefresh来举例）：

```
PullToRefreshListView --> PullToRefreshAdapterViewBase<ListView> --> PullToRefreshBase --> LinearLayout
```

1、再来看看泛型ListView，需要理解这个ListView是什么时候添加到LinearLayout中的。

2、找泛型建议大家从最顶层找，在PullToRefreshBase<T extends View>中有一个T的变量（T mRefreshableView），从字面都能看出，这就是我们想要的正在加载实际内容的View。

3、如何被添加呢？
	统一在PullToRefreshBase中内看到一个FrameLayout类型的mRefreshableViewWrapper变量。这就是包装mRefreshableView的外层View，添加代码如下：
```
private void addRefreshableView(Context context, T refreshableView) {
		mRefreshableViewWrapper = new FrameLayout(context);
		mRefreshableViewWrapper.addView(refreshableView, ViewGroup.LayoutParams.MATCH_PARENT,
				ViewGroup.LayoutParams.MATCH_PARENT);

		addViewInternal(mRefreshableViewWrapper, new LayoutParams(LayoutParams.MATCH_PARENT,
				LayoutParams.MATCH_PARENT));
	}
	
protected final void addViewInternal(View child, ViewGroup.LayoutParams params) {
		super.addView(child, -1, params);
	}
```
因此，这就完成了添加工作。

4、继续看一下addRefreshableView何时被调起的情况：
```
private void init(Context context, AttributeSet attrs) {
	// ...省略... 
	// Refreshable View
	// By passing the attrs, we can add ListView/GridView params via XML
	mRefreshableView = createRefreshableView(context, attrs);
	addRefreshableView(context, mRefreshableView);
	// ...省略... 
}
```
很明显，通过xml中定义的属性，创建对应的View，并把对应的attrs传入。<strong>这个地方很重要，下面再继续</strong>

5、事件拦截
熟悉事件拦截机制的同学应该明白，最重要的两个方法就是：
```
// 事件发起时的拦截，一般在发起的地方只是需要做准备工作。另外，如果自己正在消费，那么它将不会向子view中透传事件，这一点也很重要。
public boolean onInterceptTouchEvent(MotionEvent event)

// 事件在子View中未被消费后的事件返回。如果有，那么自身拦截再决定是否自己消费。
public boolean onTouchEvent(MotionEvent event) 
```

## SwipeListView替换ListView
从中我们知道SwipeListView本身就是一个ListView，侧划功能就是
```
public boolean onInterceptTouchEvent(MotionEvent ev)
```
这样看来可以直接替换PullToRefreshListView的泛型为SwipeListView
```
PullToRefreshSwipeListView extends PullToRefreshAdapterViewBase<SwipeListView>
```

SwipeListView创建过程也需要改造：
```
protected SwipeListView createListView(Context context, AttributeSet attrs) {
        final SwipeListView lv;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
            lv = new InternalListViewSDK9(context, attrs);
        } else {
            lv = new InternalListView(context, attrs);
        }
        return lv;
    }

    @Override
    protected SwipeListView createRefreshableView(Context context, AttributeSet attrs) {
        SwipeListView lv = createListView(context, attrs);

        // Set it to this so it can be used in ListActivity/ListFragment
        lv.setId(android.R.id.list);
        return lv;
    }
```
将InternalListView改造成继承SwipeListView就OK
```
protected class InternalListView extends SwipeListView implements EmptyViewMethodAccessor
```
这时就要考虑SwipeListView属性如何添加了呢？
不知道你还是否记得上面粗体的那段文字：
PullToRefreshBase中定义的AttributeSet将会传递到子类的mRefreshableView中。
因此，我们只需要在定义xml的时候将SwipeListView自定的属性放入到PullToRefreshSwipeListView属性下就ok啦！

## 使用方式
```
<com.frodo.pulltorefreshswipelistview.library.PullToRefreshSwipeListView
        android:id="@+id/pull_refresh_list"
        android:layout_width="fill_parent"
        android:layout_height="fill_parent"
        android:cacheColorHint="#00000000"
        android:divider="#19000000"
        android:dividerHeight="4dp"
        android:fadingEdge="none"
        android:fastScrollEnabled="false"
        android:footerDividersEnabled="false"
        android:headerDividersEnabled="false"
        android:smoothScrollbar="true"

        android:listSelector="#00000000"
        swipe:swipeFrontView="@+id/front"
        swipe:swipeBackView="@+id/back"
        swipe:swipeDrawableChecked="@drawable/choice_selected"
        swipe:swipeDrawableUnchecked="@drawable/choice_unselected"
        swipe:swipeCloseAllItemsWhenMoveList="true"
        swipe:swipeMode="both"
        swipe:swipeAnimationTime="5"
        swipe:swipeOffsetLeft="20dp"
        swipe:swipeOffsetRight="20dp"
        />
```

## 效果图 
![下拉刷新效果图](http://frodoking.github.io/img/github-readme/PullToRefreshSwipeListView_01.png)

![item滑动效果图](http://frodoking.github.io/img/github-readme/PullToRefreshSwipeListView_02.png)

## 注意 
本例已经将PullToRefresh和SwipeListView进行了整合。不过相对于原来的PullToRefresh，只是加入了PullToRefreshListView功能。如果你需要其他GridView的额外功能请自行添加。DEMO工程是本工程的sample工程。

## 参考

 1. [Android-PullToRefresh](https://github.com/chrisbanes/Android-PullToRefresh)
 2. [android-swipelistview](https://github.com/47deg/android-swipelistview)
 3. [android-swipelistview-sample](https://github.com/47deg/android-swipelistview-sample)
