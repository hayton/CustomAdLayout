# customAdLayout

Resize PublisherAdView according to screen width.


```xml
<com.hypebeast.adview.CustomAdLayout
            android:id="@+id/customAdLayout"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_gravity="center_horizontal"
            app:adUnit="@string/dfp_list_unit_id"
            app:supportedAdSize="970x250,300x250"
            app:enableAnimation="true"
            app:animationDuration="300" />
```
Custom attribute
-----------------------------------------------------------------------------------------------------------------------------

| Attribute | Type | Mandatory | Purpose |
|-----------|------|-----------|---------|
| adUnit | String | Yes | The adUnit of the AdView|
| supportedAdSize | String | Yes | The AdSize supported by this AdView, can be multiple|
| enableAnimation | Boolean | No | If ```true```, animation will be played during resize. Default is false |
| animationDuration | Integer | No | The duration of the resize animation. This value will be ignored if ```enableAnimation``` is not set |
