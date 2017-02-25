package animalize.github.com.quantangshi;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Rect;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.sothree.slidinguppanel.SlidingUpPanelLayout;

import java.util.ArrayList;
import java.util.List;

import animalize.github.com.quantangshi.Data.Poem;
import animalize.github.com.quantangshi.Data.RecentInfo;
import animalize.github.com.quantangshi.Database.MyDatabaseHelper;


public class OnePoemActivity extends AppCompatActivity {
    final static int recentLimit = 50;

    private Poem currentPoem;

    private SlidingUpPanelLayout slider;
    private FrameLayout swichFrame;

    private OnePoemFragment poemFragment;

    private LinearLayout recentView;
    private LinearLayout tagView;

    private TagFragment tagFragment;

    private RecyclerView recentList;
    private RecentAdapter recentAdapter;

    private TextView mPIDText;
    private Button mTButton;
    private Button mSButton;
    private Button mSpButton;

    public static void actionStart(Context context) {
        Intent i = new Intent(context, OnePoemActivity.class);
        context.startActivity(i);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.poem_main);

        // 得到 诗fragment
        FragmentManager fm = getSupportFragmentManager();
        poemFragment = (OnePoemFragment) fm.findFragmentById(R.id.fragment_one_poem);
        tagFragment = (TagFragment) fm.findFragmentById(R.id.fragment_tag);

        slider = (SlidingUpPanelLayout) findViewById(R.id.sliding_layout);
        slider.addPanelSlideListener(new SlidingUpPanelLayout.PanelSlideListener() {
            @Override
            public void onPanelSlide(View panel, float slideOffset) {

            }

            @Override
            public void onPanelStateChanged(View panel, SlidingUpPanelLayout.PanelState previousState, SlidingUpPanelLayout.PanelState newState) {
                if (newState == SlidingUpPanelLayout.PanelState.ANCHORED) {
                    Rect r = new Rect();
                    if (swichFrame.getGlobalVisibleRect(r)) {
                        swichFrame.getLayoutParams().height = r.height();
                        swichFrame.requestLayout();
                    }
                } else {
                    swichFrame.setLayoutParams(
                            new LinearLayout.LayoutParams(
                                    LinearLayout.LayoutParams.MATCH_PARENT,
                                    LinearLayout.LayoutParams.MATCH_PARENT)
                    );
                }
            }
        });
        swichFrame = (FrameLayout) findViewById(R.id.switch_frame);

        // 最近列表
        recentView = (LinearLayout) findViewById(R.id.recent_view);
        tagView = (LinearLayout) findViewById(R.id.tag_view);

        // RecyclerView
        recentList = (RecyclerView) findViewById(R.id.recent_list);
        // 布局管理
        LinearLayoutManager lm = new LinearLayoutManager(this);
        recentList.setLayoutManager(lm);
        // adapter
        recentAdapter = new RecentAdapter();
        recentList.setAdapter(recentAdapter);

        // 显示最近列表
        Button b = (Button) findViewById(R.id.show_drawer);
        b.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (slider.getPanelState() == SlidingUpPanelLayout.PanelState.COLLAPSED) {
                    slider.setPanelState(SlidingUpPanelLayout.PanelState.ANCHORED);
                }
                slider.setScrollableView(recentList);

                tagView.setVisibility(View.GONE);
                recentView.setVisibility(View.VISIBLE);
            }
        });

        // 显示tag
        b = (Button) findViewById(R.id.show_tag);
        b.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (slider.getPanelState() == SlidingUpPanelLayout.PanelState.COLLAPSED) {
                    slider.setPanelState(SlidingUpPanelLayout.PanelState.ANCHORED);
                }
                slider.setScrollableView(null);

                recentView.setVisibility(View.GONE);
                tagView.setVisibility(View.VISIBLE);
            }
        });

        // 诗id
        mPIDText = (TextView) findViewById(R.id.textview_poem_id);

        // 繁体、简体、简体+
        mTButton = (Button) findViewById(R.id.button_t);
        mTButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                poemFragment.setMode(0);
                setPoemMode(0);
            }
        });
        mSButton = (Button) findViewById(R.id.button_s);
        mSButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                poemFragment.setMode(1);
                setPoemMode(1);
            }
        });
        mSpButton = (Button) findViewById(R.id.button_sp);
        mSpButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                poemFragment.setMode(2);
                setPoemMode(2);
            }
        });

        // 下一首随机诗
        b = (Button) findViewById(R.id.next_random);
        b.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                randomPoem();
                recentList.scrollToPosition(0);
            }
        });

        setPoemMode(2);
        // load上回的
        SharedPreferences pref = getPreferences(MODE_PRIVATE);
        int id = pref.getInt("poem_id", -1);
        if (id != -1) {
            toPoemByID(id);
        } else {
            randomPoem();
        }

        TextView tv = (TextView) findViewById(R.id.recent_title);
        tv.setText("最近" + recentLimit + "条");
        // 配置SlidingUpPanelLayout
        //SlidingUpPanelLayout slide = (SlidingUpPanelLayout) findViewById(R.id.sliding_layout);
    }

    @Override
    protected void onDestroy() {
        if (currentPoem != null) {
            SharedPreferences.Editor editor = getPreferences(MODE_PRIVATE).edit();
            editor.putInt("poem_id", currentPoem.getId());
            editor.apply();
        }

        super.onDestroy();
    }

    private void randomPoem() {
        // 随机一首诗
        currentPoem = MyDatabaseHelper.randomPoem();
        updateUIForPoem();
    }

    public void toPoemByID(int id) {
        currentPoem = MyDatabaseHelper.getPoemById(id);
        updateUIForPoem();
    }

    private void updateUIForPoem() {
        // 显示此诗
        poemFragment.setPoem(currentPoem);

        // 更新本活动的ui
        mPIDText.setText(String.valueOf(currentPoem.getId()));

        // 显示tag
        tagFragment.setPoemId(currentPoem.getId());

        // 添加 到 最近列表
        MyDatabaseHelper.addToRecentList(currentPoem, recentLimit);

        // 刷新最近列表
        ArrayList<RecentInfo> recent_list = MyDatabaseHelper.getRecentList();
        recentAdapter.setArrayList(recent_list);
    }


    private void setPoemMode(int mode) {
        poemFragment.setMode(mode);

        if (mode == 0) {
            mTButton.setTextColor(Color.BLUE);
            mSButton.setTextColor(Color.BLACK);
            mSpButton.setTextColor(Color.BLACK);
        } else if (mode == 1) {
            mTButton.setTextColor(Color.BLACK);
            mSButton.setTextColor(Color.BLUE);
            mSpButton.setTextColor(Color.BLACK);
        } else {
            mTButton.setTextColor(Color.BLACK);
            mSButton.setTextColor(Color.BLACK);
            mSpButton.setTextColor(Color.BLUE);
        }
    }

    public class RecentAdapter
            extends RecyclerView.Adapter<RecentAdapter.MyHolder> {

        private static final String TAG = "RecentAdapter";
        private List<RecentInfo> mRecentList;

        public void setArrayList(ArrayList<RecentInfo> al) {
            mRecentList = al;
            notifyDataSetChanged();
        }

        @Override
        public MyHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = LayoutInflater
                    .from(parent.getContext())
                    .inflate(R.layout.recent_list_item, parent, false);
            final MyHolder holder = new MyHolder(v);

            holder.root.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int posi = holder.getAdapterPosition();
                    RecentInfo ri = mRecentList.get(posi);

                    OnePoemActivity.this.toPoemByID(ri.getId());
                    OnePoemActivity.this.recentList.scrollToPosition(0);
                }
            });

            return holder;
        }

        @Override
        public void onBindViewHolder(MyHolder holder, int position) {
            RecentInfo ri = mRecentList.get(position);

            if (position % 2 == 0) {
                holder.root.setBackgroundColor(Color.rgb(0xff, 0xcc, 0xcc));
            } else {
                holder.root.setBackgroundColor(Color.rgb(0xcc, 0xcc, 0xff));
            }

            holder.order.setText(String.valueOf(position + 1));
            holder.title.setText(ri.getTitle());
            holder.author.setText(ri.getAuthor());
        }

        @Override
        public int getItemCount() {
            return mRecentList.size();
        }

        public class MyHolder extends RecyclerView.ViewHolder {
            private LinearLayout root;
            private TextView order;
            private TextView title;
            private TextView author;

            public MyHolder(View itemView) {
                super(itemView);

                root = (LinearLayout) itemView.findViewById(R.id.recent_item);
                order = (TextView) itemView.findViewById(R.id.recent_item_order);
                title = (TextView) itemView.findViewById(R.id.recent_item_title);
                author = (TextView) itemView.findViewById(R.id.recent_item_author);
            }
        }
    }
}
