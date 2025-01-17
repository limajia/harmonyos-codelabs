/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2021-2021. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.huawei.codelab.utils;

import com.huawei.codelab.slice.HandleAbilitySlice;

import ohos.agp.components.Button;
import ohos.agp.components.Component;
import ohos.multimodalinput.event.TouchEvent;

/**
 * 摇杆滑动事件
 *
 * @since 2021-03-15
 */
public class CalculAngle {
    private static final String TAG = "AngleUtil";

    private int angle; // 记录手指移动相对于原点的角度

    private float startPosX; // 设置大圆圆点坐标为坐标原点

    private float startPosY;

    private float moveX; // 记录手指当前横坐标

    private float moveY; // 记录手指当前纵坐标

    private float smallCurrX; // 小圆当前坐标x

    private float smallCurrY; // 小圆当前坐标y

    private float disAbsX; // X轴移动距离绝对值

    private float disAbsY; // Y轴移动距离绝对值

    private Component layout;

    private int screenHeight;

    private int height;

    private Button smallCircle;

    private Button bigCircle;

    private int smallR; // 小圆半径

    private int bigR; // 大圆半径

    /**
     * 摇杆滑动事件
     */
    private Component.TouchEventListener onTouchEvent = new Component.TouchEventListener() {
        @Override
        public boolean onTouchEvent(Component component, TouchEvent touchEvent) {
            int layoutHeight = layout.getHeight();
            height = screenHeight - layoutHeight;
            smallR = smallCircle.getWidth() / Constants.QUADRANT_2; // 小圆半径
            bigR = bigCircle.getWidth() / Constants.QUADRANT_2; // 大圆半径
            int action = touchEvent.getAction();
            switch (action) {
                case TouchEvent.PRIMARY_POINT_DOWN:
                    // 设置大圆圆点坐标为坐标原点
                    startPosX = bigR + bigCircle.getContentPositionX();
                    startPosY = bigR + bigCircle.getContentPositionY();
                    break;
                case TouchEvent.PRIMARY_POINT_UP:
                    smallCircle.setContentPosition(startPosX - smallR, startPosY - smallR);
                    angle = 0;
                    HandleAbilitySlice.postData();
                    break;
                case TouchEvent.POINT_MOVE:
                    // 记录手指当前横坐标
                    moveX = touchEvent.getPointerPosition(0).getX();
                    // 记录手指当前纵坐标.
                    moveY = touchEvent.getPointerScreenPosition(0).getY() - height;
                    smallCurrX = moveX - smallR;
                    smallCurrY = moveY - smallR;
                    smallCircle.setVisibility(Component.VISIBLE);
                    // 设置小圆的坐标跟随手指的坐标 但不超出大圆范围
                    smallCircle.setContentPosition(getSmallCurrentPos(smallCurrX, smallCurrY)[0],
                        getSmallCurrentPos(smallCurrX, smallCurrY)[1]);
                    // 计算移动的角度 并发送到远程设备？
                    calculateAngle();
                    HandleAbilitySlice.postData();
                    break;
                default:
                    break;
            }
            return false;
        }
    };

    /**
     * 构造方法
     *
     * @param smallCircle 小环
     * @param bigCircle 大环
     */
    public CalculAngle(Button smallCircle, Button bigCircle, Component layout, int screenHeight) {
        this.smallCircle = smallCircle;
        this.bigCircle = bigCircle;
        this.layout = layout;
        this.screenHeight = screenHeight;
    }

    public Component.TouchEventListener getOnTouchEvent() {
        return onTouchEvent;
    }

    public void setOnTouchEvent(Component.TouchEventListener onTouchEvent) {
        this.onTouchEvent = onTouchEvent;
    }

    public int getAngle() {
        return angle;
    }

    /**
     * 计算角度
     *
     * @return 角度
     */
    private int calculateAngle() {
        int degree = (int) Math.toDegrees(Math.atan(getDisAbsY() / getDisAbsX()));
        int quadrant = quadrant();
        switch (quadrant) {
            case Constants.QUADRANT_1:
                // 向右上移动
                angle = degree;
                break;
            case Constants.QUADRANT_2:
                // 向左上移动
                angle = Constants.DEGREE_180 - degree;
                break;
            case Constants.QUADRANT_3:
                // 向左下移动
                angle = -Constants.DEGREE_180 + degree;
                break;
            case Constants.QUADRANT_4:
                // 向右下移动
                angle = -degree;
                break;
            default:
                angle = 0;
                break;
        }
        return angle;
    }

    /**
     * 返回手指所在象限(坐标原点为大圆圆心)
     *
     * @return 象限
     */
    private int quadrant() {
        if (getFlagX() && !getFlagY()) {
            return Constants.QUADRANT_1;
        } else if (!getFlagX() && !getFlagY()) {
            return Constants.QUADRANT_2;
        } else if (!getFlagX() && getFlagY()) {
            return Constants.QUADRANT_3;
        } else if (getFlagX() && getFlagY()) {
            return Constants.QUADRANT_4;
        } else {
            return 0;
        }
    }

    /**
     * 返回 true:向右移动，计算被移动物体当前X轴坐标值时+
     * 返回 false:向坐移动，计算被移动物体当前X轴坐标值时-
     *
     * @return X轴移动方向
     */
    private boolean getFlagX() {
        return 0 < moveX - startPosX ? true : false;
    }

    /**
     * 返回 true:向右移动，计算被移动物体当前Y轴坐标值时+
     * 返回 false:向坐移动，计算被移动物体当前Y轴坐标值时-
     *
     * @return Y轴移动方向
     */
    private boolean getFlagY() {
        return 0 < moveY - startPosY ? true : false;
    }

    // 计算移动距离的绝对值
    private float getDisAbsX() {
        disAbsX = Math.abs(moveX - startPosX);
        if (Constants.MIN_SLIDE > disAbsX) {
            return 1f;
        }
        return disAbsX;
    }

    // 计算移动距离的绝对值
    private float getDisAbsY() {
        disAbsY = Math.abs(moveY - startPosY);
        if (Constants.MIN_SLIDE > disAbsY) {
            return 1f;
        }
        return disAbsY;
    }

    // 计算手指(即小圆)移动距离大圆圆心的距离
    private double getDisZ() {
        return Math.sqrt(Math.abs(moveX - startPosX) * Math.abs(moveX - startPosX)
            + Math.abs(moveY - startPosY) * Math.abs(moveY - startPosY));
    }

    // 计算小圆的坐标，跟随手指当前坐标且不超过大圆范围
    private float[] getSmallCurrentPos(float currX, float currY) {
        float[] smallCurrentPos = new float[Constants.QUADRANT_2];
        if (getDisZ() < bigR) {
            smallCurrentPos[0] = currX;
            smallCurrentPos[1] = currY;
            return smallCurrentPos;
        } else {
            // 手指滑出大圆外后，由于小圆不能超出大圆，此时小圆与大圆边界相切，切点与大圆圆心形成的直角三角形的边长
            double disX = (getDisAbsX() * bigR) / getDisZ();
            double disY = (getDisAbsY() * bigR) / getDisZ();
            int quadrant = quadrant(); // 手指所在象限
            switch (quadrant) {
                case Constants.QUADRANT_1:
                    smallCurrentPos[0] = (float) (disX + startPosX - smallR);
                    smallCurrentPos[1] = (float) (startPosY - disY - smallR);
                    break;
                case Constants.QUADRANT_2:
                    smallCurrentPos[0] = (float) (startPosX - disX - smallR);
                    smallCurrentPos[1] = (float) (startPosY - disY - smallR);
                    break;
                case Constants.QUADRANT_3:
                    smallCurrentPos[0] = (float) (startPosX - disX - smallR);
                    smallCurrentPos[1] = (float) (disY + startPosY - smallR);
                    break;
                case Constants.QUADRANT_4:
                    smallCurrentPos[0] = (float) (disX + startPosX - smallR);
                    smallCurrentPos[1] = (float) (disY + startPosY - smallR);
                    break;
                default:
                    break;
            }
        }
        return smallCurrentPos;
    }
}
