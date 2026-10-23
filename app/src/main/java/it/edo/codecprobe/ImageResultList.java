package it.edo.codecprobe;

import android.view.*;
import android.widget.*;
import java.util.*;

/** Bounded report viewport. ListView creates/recycles only rows inside this viewport. */
final class ImageResultList {
    static final class Item {
        final int index;final String name,phase,text;final ImageRunState.Result result;
        final boolean metadataOnly,information;final Runnable open,details;
        Item(int index,String name,String phase,String text,ImageRunState.Result result,boolean metadataOnly,Runnable open,Runnable details,boolean information){this.index=index;this.name=name;this.phase=phase;this.text=text;this.result=result;this.metadataOnly=metadataOnly;this.open=open;this.details=details;this.information=information;}
    }
    final ListView list;
    final Button follow;
    private final AppUi ui;
    private final Adapter adapter=new Adapter();
    private List<Item> items=Collections.emptyList();
    private boolean following=true;
    private int current=-1,followed=-1;
    ImageResultList(AppUi ui,LinearLayout parent){
        this.ui=ui;
        ui.text(parent,"Scorri i risultati in questo riquadro. «Segui corrente» riporta alla foto in elaborazione.",13);
        follow=ui.button(parent,"Segui corrente · attivo");follow.setOnClickListener(v->{following=true;followed=-1;followCurrent();});
        list=new ListView(ui.activity){
            private float touchY;
            @Override public boolean onInterceptTouchEvent(MotionEvent event){
                if(event.getActionMasked()==MotionEvent.ACTION_DOWN){touchY=event.getY();following=false;UiText.set(follow,"Segui corrente");if(getParent()!=null)getParent().requestDisallowInterceptTouchEvent(true);}
                return super.onInterceptTouchEvent(event);
            }
            @Override public boolean onTouchEvent(MotionEvent event){
                int action=event.getActionMasked();
                if(action==MotionEvent.ACTION_MOVE){float delta=event.getY()-touchY;touchY=event.getY();if(getParent()!=null&&Math.abs(delta)>0)getParent().requestDisallowInterceptTouchEvent(canScrollVertically(delta>0?-1:1));}
                if((action==MotionEvent.ACTION_UP||action==MotionEvent.ACTION_CANCEL)&&getParent()!=null)getParent().requestDisallowInterceptTouchEvent(false);
                return super.onTouchEvent(event);
            }
            @Override public boolean performClick(){return super.performClick();}
        };
        list.setDividerHeight(ui.dp(1));list.setNestedScrollingEnabled(true);list.setItemsCanFocus(true);list.setAdapter(adapter);
        list.addOnLayoutChangeListener((v,l,t,r,b,ol,ot,or,ob)->{if(b-t!=ob-ot&&following){followed=-1;followCurrent();}});
        int height=Math.max(240,Math.min(420,ui.activity.getResources().getConfiguration().screenHeightDp/2));
        parent.addView(list,new LinearLayout.LayoutParams(-1,ui.dp(height)));
    }
    Item item(int absolute){if(items.isEmpty())return null;int offset=absolute-items.get(0).index;return offset>=0&&offset<items.size()?items.get(offset):null;}
    int size(){return items.size();}
    void submit(List<Item> next,int current){
        boolean changed=items.size()!=next.size();if(!changed)for(int i=0;i<next.size();i++)if(items.get(i)!=next.get(i)){changed=true;break;}
        int first=list.getFirstVisiblePosition(),anchor=first<items.size()?items.get(first).index:-1;
        int top=list.getChildCount()>0?list.getChildAt(0).getTop():0;
        int oldStart=items.isEmpty()?-1:items.get(0).index;
        this.current=current;
        if(changed){items=new ArrayList<>(next);adapter.notifyDataSetChanged();
            if(!following&&!items.isEmpty()&&oldStart!=items.get(0).index)list.setSelectionFromTop(Math.max(0,Math.min(items.size()-1,anchor-items.get(0).index)),top);
        }
        follow.setEnabled(!items.isEmpty());followCurrent();
    }
    private void followCurrent(){
        UiText.set(follow,following?"Segui corrente · attivo":"Segui corrente");
        if(!following||items.isEmpty()||current==followed)return;followed=current;
        int position=Math.max(0,Math.min(items.size()-1,current-items.get(0).index));
        list.setSelectionFromTop(position,Math.max(0,list.getHeight()/2-ui.dp(36)));
    }
    private final class Holder {
        final ResultReportRow.Reusable view;Item bound;
        Holder(){LinearLayout temporary=new LinearLayout(ui.activity);view=new ResultReportRow.Reusable(ui,temporary);temporary.removeView(view.row);view.row.setLayoutParams(new AbsListView.LayoutParams(-1,-2));view.row.setTag(this);}
    }
    private final class Adapter extends BaseAdapter {
        public int getCount(){return items.size();}
        public Item getItem(int position){return items.get(position);}
        public long getItemId(int position){return getItem(position).index;}
        public boolean hasStableIds(){return true;}
        public boolean areAllItemsEnabled(){return true;}
        public boolean isEnabled(int position){return true;}
        public View getView(int position,View recycled,ViewGroup parent){
            Holder holder=recycled==null?new Holder():(Holder)recycled.getTag();Item item=getItem(position);
            if(holder.bound!=item){holder.view.bind(item.text,item.open,item.details,item.information);holder.bound=item;}
            return holder.view.row;
        }
    }
}
