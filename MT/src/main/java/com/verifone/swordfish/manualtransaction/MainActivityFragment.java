/**
 * Copyright (C) 2016,2017 Verifone, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included
 * in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
 * OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL
 * VERIFONE, INC. BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 * Except as contained in this notice, the name of Verifone, Inc. shall not be
 * used in advertising or otherwise to promote the sale, use or other dealings
 * in this Software without prior written authorization from Verifone, Inc.
 */

package com.verifone.swordfish.manualtransaction;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.verifone.swordfish.manualtransaction.HistoryFragments.HistoryDetail;
import com.verifone.swordfish.manualtransaction.MTDataModel.MTTransaction;
import com.verifone.swordfish.manualtransaction.System.SyncManager;
import com.verifone.swordfish.manualtransaction.Tools.BaseFragment;
import com.verifone.swordfish.manualtransaction.Tools.PagerAdapter;

public class MainActivityFragment extends BaseFragment implements
        HistoryController.OnFragmentInteractionListener {
    ViewPager viewPager;
    TabLayout tabLayout;
    private static String TAG = MainActivityFragment.class.getSimpleName();
    private FrameLayout historyDetailFrameLayout;


    public MainActivityFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final View root = inflater.inflate(R.layout.mt_activity_main, container, false);
        tabLayout = (TabLayout) root.findViewById(R.id.tab_layout);
        viewPager = (ViewPager) root.findViewById(R.id.pager);
        TextView versionView = (TextView) root.findViewById(R.id.textViewVersion);
        PackageInfo pInfo = null;
        try {
            pInfo = getActivity().getApplicationContext().getPackageManager().getPackageInfo(getActivity().getApplicationContext().getPackageName(), 0);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        if (pInfo != null) {
            String version = pInfo.versionName;
            int verCode = pInfo.versionCode;
            String fingerprint = Build.FINGERPRINT;
            String[] fingerComponents = fingerprint.split("/");
            if (fingerComponents.length > 3) {
                String[] versionArray = fingerComponents[4].split(":");
                if (versionArray != null && versionArray.length > 0) {
                    version = version + "." + verCode;
                }
            }
            versionView.setText(getActivity().getResources().getString(R.string.app_name) + " " + version);
        }
        historyDetailFrameLayout = (FrameLayout) root.findViewById(R.id.mainHistoryDetailFrameLayout);
        return root;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setTabs();
    }

    @Override
    public void onResume() {
        super.onResume();
        //checkOrganization();
    }

    private void setTabs() {
        tabLayout.addTab(tabLayout.newTab().setText(R.string.tabOneTitle));
        tabLayout.addTab(tabLayout.newTab().setText(R.string.tabTwoTitle));

        tabLayout.setTabMode(TabLayout.MODE_SCROLLABLE);

        FragmentActivity activity = SyncManager.getInstance().getContext();
        final PagerAdapter adapter = new PagerAdapter
                (activity.getSupportFragmentManager(), tabLayout.getTabCount(), activity);
        viewPager.setAdapter(adapter);
        viewPager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(tabLayout));
        final MainActivityFragment fragment = this;
        tabLayout.setOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                viewPager.setCurrentItem(tab.getPosition());
                HistoryController controller = null;
                if(tab.getPosition() == 1)
                    controller = (HistoryController) adapter.getFragmentAtPosition(tab.getPosition());
                switch (tab.getPosition()) {
                    case 0:
                        hideDetailView("historyDetail");
                        break;
                    case 1: {
                        controller.setListener(fragment);
                        controller.refresh();
                        break;
                    }
                    default:
                        break;
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });
    }

    @Override
    public void presentDetailView(MTTransaction t, HistoryDetail h) {
        if (viewPager.getCurrentItem() == 1) {
            historyDetailFrameLayout.setVisibility(View.VISIBLE);
            FragmentManager fragmentManager = this.getFragmentManager();
            fragmentManager.beginTransaction()
                    .replace(R.id.mainHistoryDetailFrameLayout, h)
                    .commit();
        }

    }

    @Override
    public void hideDetailView(String label) {
        if (historyDetailFrameLayout != null) {
            historyDetailFrameLayout.setVisibility(View.INVISIBLE);
        }

    }

}