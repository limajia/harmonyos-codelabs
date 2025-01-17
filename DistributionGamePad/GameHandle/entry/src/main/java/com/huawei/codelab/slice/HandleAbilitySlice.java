/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2021-2021. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.huawei.codelab.slice;

import com.huawei.codelab.ResourceTable;
import com.huawei.codelab.handle.Handle;
import com.huawei.codelab.proxy.GameRemoteProxy;
import com.huawei.codelab.service.GameServiceAbility;
import com.huawei.codelab.utils.CalculAngle;
import com.huawei.codelab.utils.Constants;
import com.huawei.codelab.utils.LogUtil;
import com.huawei.codelab.utils.ScreenUtils;

import ohos.aafwk.ability.AbilitySlice;
import ohos.aafwk.ability.IAbilityConnection;
import ohos.aafwk.content.Intent;
import ohos.aafwk.content.Operation;
import ohos.agp.components.Button;
import ohos.agp.components.Component;
import ohos.agp.components.Image;
import ohos.agp.components.Text;
import ohos.bundle.ElementName;
import ohos.data.distributed.common.KvManagerConfig;
import ohos.data.distributed.common.KvManagerFactory;
import ohos.multimodalinput.event.MmiPoint;
import ohos.multimodalinput.event.TouchEvent;
import ohos.rpc.IRemoteObject;
import ohos.rpc.RemoteException;
import ohos.vibrator.agent.VibratorAgent;

import java.util.List;

/**
 * 手柄操作
 *
 * @since 2021-03-15
 */
public class HandleAbilitySlice extends AbilitySlice {
    private static final String TAG = "HandleAbilitySlice";

    private static ScoreEntity scoreEntity;

    private static boolean isConn;

    private static GameRemoteProxy proxy;

    private CalculAngle calculAngle;

    private String localDeviceId; // 本机设备Id

    private Handle handle;

    private Component layout;

    private Text txtScore;

    private Button bigCircle; // 摇杆 大圆

    private Button smallCircle; // 摇杆 小圆

    private Image imgA; // A技能图片

    private Image imgB; // B技能图片

    private Button btnPause; // 暂停

    private Button btnStart; // 开始

    private VibratorAgent vibratorAgent;

    private boolean isFlagA = true;

    private boolean isFlagB = true;

    /**
     * A技能
     */
    private Component.ClickedListener listenerA = new Component.ClickedListener() {
        @Override
        public void onClick(Component component) {
            vibrator(Constants.VIBRATION_30); // 震动
            handle.setIsAbtnClick(1);
            isFlagA = true;
            postData();
            handle.setIsAbtnClick(0);
        }
    };

    /**
     * B技能
     */
    private Component.ClickedListener listenerB = new Component.ClickedListener() {
        @Override
        public void onClick(Component component) {
            vibrator(Constants.VIBRATION_80); // 震动
            handle.setIsBbtnClick(1);
            isFlagB = true;
            postData();
            handle.setIsBbtnClick(0);
        }
    };

    /**
     * 暂停
     */
    private Component.ClickedListener btnPauseListener = new Component.ClickedListener() {
        @Override
        public void onClick(Component component) {
            vibrator(Constants.VIBRATION_100); // 震动
            handle.setIsPause(1);
            postData();
            handle.setIsPause(0);
        }
    };

    /**
     * 开始游戏
     */
    private Component.ClickedListener btnRestartListener = new Component.ClickedListener() {
        @Override
        public void onClick(Component component) {
            vibrator(Constants.VIBRATION_100); // 震动
            handle.setIsStart(1);
            postData();
            handle.setIsStart(0);
        }
    };

    /**
     * 摇杆与A、B技能多点触控
     */
    private Component.TouchEventListener layoutTouchEvent = new Component.TouchEventListener() {
        @Override
        public boolean onTouchEvent(Component component, TouchEvent touchEvent) {
            int action = touchEvent.getAction();
            int pointerCount = touchEvent.getPointerCount();
            switch (action) {
                case TouchEvent.OTHER_POINT_DOWN:
                    for (int i = 0; i < pointerCount; i++) {
                        MmiPoint pointerPosition = touchEvent.getPointerPosition(i);
                        boolean isInComponentArea =
                            inComponentArea(imgA, pointerPosition.getX(), pointerPosition.getY());
                        boolean isInComponentAreaB =
                            inComponentArea(imgB, pointerPosition.getX(), pointerPosition.getY());
                        if (isInComponentArea && isFlagA) {
                            isFlagA = false;
                            imgA.callOnClick();
                        }
                        if (isInComponentAreaB && isFlagB) {
                            isFlagB = false;
                            imgB.callOnClick();
                        }
                    }
                    break;
                default:
                    break;
            }
            return false;
        }
    };

    @Override
    public void onStart(Intent intent) {
        super.onStart(intent);
        super.setUIContent(ResourceTable.Layout_ability_handle);
        ScreenUtils.setWindows();
        Object obj = intent.getParams().getParam("deviceId");
        if (obj instanceof String) {
            String deviceId = (String) obj;
            isConn = connectRemotePa(deviceId, 1);
        }
        initComponent(); // 初始化
        setListener(); // 控件绑定事件
        startService(); // start service
    }

    private void startService() {
        Intent intent = new Intent();
        Operation operation = new Intent.OperationBuilder().withDeviceId("")
            .withBundleName(getBundleName())
            .withAbilityName(GameServiceAbility.class.getName())
            .withFlags(Intent.FLAG_ABILITYSLICE_MULTI_DEVICE)
            .build();
        intent.setOperation(operation);
        startAbility(intent);
    }

    // 绑定事件
    private void setListener() {
        bigCircle.setTouchEventListener(calculAngle.getOnTouchEvent());
        imgA.setClickedListener(listenerA);
        imgB.setClickedListener(listenerB);
        btnPause.setClickedListener(btnPauseListener);
        btnStart.setClickedListener(btnRestartListener);
    }

    // 初始化控件
    private void initComponent() {
        layout = findComponentById(ResourceTable.Id_layout);
        int screenHeight = ScreenUtils.getScreenHeight(this);
        Component comScore = findComponentById(ResourceTable.Id_score);
        if (comScore instanceof Text) {
            txtScore = (Text) comScore; // 得分
        }
        Component comImgA = findComponentById(ResourceTable.Id_btn_A);
        if (comImgA instanceof Image) {
            imgA = (Image) comImgA;
        }
        Component comImgB = findComponentById(ResourceTable.Id_btn_B);
        if (comImgB instanceof Image) {
            imgB = (Image) comImgB;
        }
        Component comBtnPause = findComponentById(ResourceTable.Id_pause);
        if (comBtnPause instanceof Button) {
            btnPause = (Button) comBtnPause;
        }
        Component comBtnStart = findComponentById(ResourceTable.Id_start);
        if (comBtnStart instanceof Button) {
            btnStart = (Button) comBtnStart;
        }
        findComponentById(ResourceTable.Id_layout).setTouchEventListener(layoutTouchEvent);
        Component comSlide = findComponentById(ResourceTable.Id_btn);
        if (comSlide instanceof Button) {
            bigCircle = (Button) comSlide;
        }
        Component comSmallcircle = findComponentById(ResourceTable.Id_small_circle);
        if (comSmallcircle instanceof Button) {
            smallCircle = (Button) comSmallcircle;
        }
        scoreEntity = new ScoreEntity(); // 实时显示得分
        vibratorAgent = new VibratorAgent();
        handle = new Handle();
        localDeviceId =
            KvManagerFactory.getInstance().createKvManager(new KvManagerConfig(this)).getLocalDeviceInfo().getId();
        calculAngle = new CalculAngle(smallCircle, bigCircle, layout, screenHeight); // 摇杆事件
    }

    private boolean connectRemotePa(String deviceId, int requestType) {
        Intent connectPaIntent = new Intent();
        Operation operation = new Intent.OperationBuilder().withDeviceId(deviceId)
            .withBundleName(getBundleName())
            .withAbilityName(GameServiceAbility.class.getName())
            .withFlags(Intent.FLAG_ABILITYSLICE_MULTI_DEVICE)
            .build();
        connectPaIntent.setOperation(operation);

        IAbilityConnection conn = new IAbilityConnection() {
            @Override
            public void onAbilityConnectDone(ElementName elementName, IRemoteObject remote, int resultCode) {
                LogUtil.info(TAG, "onAbilityConnectDone......");
                proxy = new GameRemoteProxy(remote, localDeviceId, calculAngle, handle);
                LogUtil.error(TAG, "connectRemoteAbility done");
                if (proxy != null) {
                    try {
                        proxy.senDataToRemote(requestType);
                    } catch (RemoteException e) {
                        LogUtil.error(TAG, "onAbilityConnectDone RemoteException");
                    }
                }
            }

            @Override
            public void onAbilityDisconnectDone(ElementName elementName, int index) {
                if (proxy != null) {
                    proxy = null;
                }
                LogUtil.info(TAG, "onAbilityDisconnectDone......");
            }
        };
        boolean ret = connectAbility(connectPaIntent, conn);
        return ret;
    }

    /**
     * 远程发数据
     */
    public static void postData() {
        if (isConn && proxy != null) {
            try {
                proxy.senDataToRemote(1);
            } catch (RemoteException e) {
                LogUtil.error(TAG, "Send Data to Remote Failed......................");
            }
        }
    }

    /**
     * 震动
     *
     * @param duration 震动时长ms
     */
    private void vibrator(int duration) {
        // 查询硬件设备上的振动器列表
        List<Integer> vibratorList = vibratorAgent.getVibratorIdList();
        if (vibratorList.isEmpty()) {
            return;
        }
        int vibratorId = vibratorList.get(0);
        // 创建指定振动时长的一次性振动
        vibratorAgent.startOnce(vibratorId, duration);
    }

    // 判断某个点在不在View范围之内，代码如下：
    private boolean inComponentArea(Component component, float pointX, float pointY) {
        int top = component.getTop();
        int bottom = component.getBottom();
        int left = component.getLeft();
        int right = component.getRight();
        if (pointX > left && pointX < right && pointY > top && pointY < bottom) {
            return true;
        }
        return false;
    }

    /**
     * 设置分数
     *
     * @param score 分数
     */
    public static void setScore(int score) {
        scoreEntity.setScore(score);
    }

    /**
     * 实时显示得分
     *
     * @since 2021-03-15
     */
    class ScoreEntity {
        /**
         * 实时显示得分
         *
         * @param score 得分
         */
        public void setScore(int score) {
            getContext().getUITaskDispatcher().asyncDispatch(new Runnable() {
                @Override
                public void run() {
                    txtScore.setText(String.valueOf(score));
                }
            });
        }
    }
}
