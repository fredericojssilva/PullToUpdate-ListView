PullToUpdate
============

##Features


##Usage

###Layout
``` xml
 <pt.fjss.pulltoupdatelibrary.PullToUpdateListView
        android:id="@+id/list"
        android:layout_width="fill_parent"
        android:layout_height="match_parent"
        />
```       
###JAVA
``` java
PullToUpdateListView list = (PullToUpdateListView) this.findViewById(R.id.list);
...

list.setPullMode(PullToUpdateListView.MODE.UP_AND_DOWN);
list.setAutoLoad(true, 8);
list.setPullMessageColor(Color.BLUE);
list.setLoadingMessage("Loading Message");
list.setPullRotateImage(getResources().getDrawable(R.drawable.rotate_img));
		
list.setOnRefreshListener(new IonRefreshListener() {
  @Override
	public void onRefreshUp() {
    //your code
    list.onRefreshUpComplete();
    }

	@Override
	public void onRefeshDown() {
    //your code
    list.onRefreshDownComplete(null);
    }

		});
```
##DOCUMENTATION
